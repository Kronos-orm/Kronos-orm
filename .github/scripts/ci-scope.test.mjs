import assert from 'node:assert/strict';
import test from 'node:test';
import {comparison, docsOnly, verifyResults} from './ci-scope.mjs';

test('website, translated guides, README and skill changes use documentation CI', () => {
    assert.equal(docsOnly(['kronos-docs/src/app/app.component.html', 'kronos-docs/package.json',
        'kronos-docs/src/app/docs/zh-CN/3.query/index.md', 'README.MD', 'README-zh_CN.MD',
        '.agents/skills/kronos-orm-guide/SKILL.md']), true);
});

test('source, build, CI, deployment and unknown files require full CI, including mixed changes', () => {
    for (const path of ['kronos-core/src/KPojo.kt', 'kronos-core/README.md', 'module.yaml',
        '.github/workflows/kronos-kmp-testing.yml', '.github/scripts/ci-scope.mjs',
        'kronos-docs/deploy-docs.sh', 'scripts/run', 'unknown-file']) {
        assert.equal(docsOnly(['kronos-docs/README.md', path]), false, path);
    }
    assert.equal(docsOnly([]), false);
});

test('PRs use the merge base, merge queues use their full range, other events run full CI', () => {
    assert.deepEqual(comparison('pull_request', {pull_request: {base: {sha: 'base'}, head: {sha: 'head'}}}),
        ['base', 'head', '--merge-base']);
    assert.deepEqual(comparison('merge_group', {merge_group: {base_sha: 'base', head_sha: 'head'}}), ['base', 'head']);
    for (const event of ['push', 'workflow_dispatch', 'workflow_call']) assert.equal(comparison(event, {}), null);
});

const results = kotlin => ({changes: {result: 'success', outputs: {kotlin}}, docs: {result: 'success'},
    jvm: {result: 'success'}, 'unit-receipts': {result: 'success'}, 'kronos-core-coverage': {result: 'success'}});

test('documentation gate accepts skipped Kotlin lanes but rejects failures and cancellations', () => {
    const needs = results('false');
    needs.jvm.result = 'skipped';
    assert.doesNotThrow(() => verifyResults(needs));
    for (const name of Object.keys(needs)) {
        for (const result of ['failure', 'cancelled']) {
            const failed = structuredClone(needs);
            failed[name].result = result;
            assert.throws(() => verifyResults(failed), undefined, `${name}: ${result}`);
        }
    }
    needs.docs.result = 'skipped';
    assert.throws(() => verifyResults(needs));
});

test('full gate requires every lane and change detection cannot be bypassed', () => {
    assert.doesNotThrow(() => verifyResults(results('true')));
    for (const name of Object.keys(results('true'))) {
        const needs = results('true');
        needs[name].result = 'skipped';
        assert.throws(() => verifyResults(needs), undefined, name);
    }
    const needs = results('');
    assert.throws(() => verifyResults(needs));
    assert.throws(() => verifyResults({docs: {result: 'success'}}));
});

test('real Git ranges include every path and both sides of a rename', async () => {
    const {execFileSync} = await import('node:child_process');
    const {mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync} = await import('node:fs');
    const {tmpdir} = await import('node:os');
    const {join} = await import('node:path');
    const {fileURLToPath} = await import('node:url');
    const directory = mkdtempSync(join(tmpdir(), 'kronos-ci-scope-'));
    const git = (...args) => execFileSync('git', args, {cwd: directory, encoding: 'utf8'}).trim();
    const commit = () => {
        git('add', '.');
        git('-c', 'user.name=CI Scope Test', '-c', 'user.email=test@example.com', 'commit', '-qm', 'fixture');
        return git('rev-parse', 'HEAD');
    };
    const script = fileURLToPath(new URL('./ci-scope.mjs', import.meta.url));
    const classify = (eventName, event) => {
        const eventPath = join(directory, 'event.json');
        const output = join(directory, 'output');
        writeFileSync(eventPath, JSON.stringify(event));
        writeFileSync(output, '');
        execFileSync(process.execPath, [script], {cwd: directory, env: {...process.env,
            GITHUB_EVENT_NAME: eventName, GITHUB_EVENT_PATH: eventPath, GITHUB_OUTPUT: output}});
        return readFileSync(output, 'utf8');
    };
    try {
        git('init', '-q');
        mkdirSync(join(directory, 'kronos-core'));
        mkdirSync(join(directory, 'kronos-docs'));
        writeFileSync(join(directory, 'kronos-core/Model.kt'), 'class Model');
        const base = commit();
        // Avoid GitHub API file limits and preserve filenames containing spaces/newlines.
        for (let index = 0; index < 350; index++) writeFileSync(join(directory, `kronos-docs/page ${index}.md`), '# Guide');
        writeFileSync(join(directory, 'kronos-docs/line\nbreak.md'), '# Guide');
        const docsHead = commit();
        const pr = head => ({pull_request: {base: {sha: base}, head: {sha: head}}});
        assert.equal(classify('pull_request', pr(docsHead)), 'kotlin=false\n');
        assert.equal(classify('merge_group', {merge_group: {base_sha: base, head_sha: docsHead}}), 'kotlin=false\n');
        git('mv', 'kronos-core/Model.kt', 'kronos-docs/Model.kt');
        const renamedHead = commit();
        assert.equal(classify('pull_request', pr(renamedHead)), 'kotlin=true\n');
        assert.equal(classify('workflow_dispatch', {}), 'kotlin=true\n');
    } finally {
        rmSync(directory, {recursive: true, force: true});
    }
});
