import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {appendFileSync, readFileSync} from 'node:fs';
import {pathToFileURL} from 'node:url';

export function docsOnly(paths) {
    return paths.length > 0 && paths.every(path =>
        (path.startsWith('kronos-docs/') && path !== 'kronos-docs/deploy-docs.sh') ||
        /^(README(?:[-_][\w-]+)?\.md|CONTRIBUTING\.md|CHANGELOG\.md)$/i.test(path) ||
        path.startsWith('.agents/skills/'));
}

export function comparison(eventName, event) {
    if (eventName === 'pull_request') {
        return [event.pull_request.base.sha, event.pull_request.head.sha, '--merge-base'];
    }
    if (eventName === 'merge_group') return [event.merge_group.base_sha, event.merge_group.head_sha];
    if (eventName === 'push' && event.ref === 'refs/heads/main' &&
        /^[0-9a-f]{40}$/i.test(event.before ?? '') && !/^0+$/.test(event.before) &&
        /^[0-9a-f]{40}$/i.test(event.after ?? '')) {
        return [event.before, event.after];
    }
    // Release pushes, manual runs and unknown ranges always run the complete suite.
    return null;
}

export function verifyResults(needs) {
    assert.equal(needs.changes?.result, 'success', 'Change detection must succeed');
    assert.equal(needs.docs?.result, 'success', 'Documentation checks must succeed');
    const kotlin = needs.changes.outputs.kotlin;
    assert(['true', 'false'].includes(kotlin), 'Missing CI scope');
    for (const [name, job] of Object.entries(needs)) {
        const allowed = kotlin === 'false' && name !== 'changes' && name !== 'docs'
            ? ['success', 'skipped'] : ['success'];
        assert(allowed.includes(job.result), `${name}: ${job.result}`);
    }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    if (process.argv[2] === 'gate') {
        const needs = JSON.parse(process.env.NEEDS_JSON);
        console.log(Object.fromEntries(Object.entries(needs).map(([key, job]) => [key, job.result])));
        verifyResults(needs);
    } else {
        const event = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
        const refs = comparison(process.env.GITHUB_EVENT_NAME, event);
        const paths = refs ? execFileSync('git', ['diff', '--name-only', '--no-renames', '-z', ...refs],
            {encoding: 'utf8', maxBuffer: 16 * 1024 * 1024}).split('\0').filter(Boolean) : [];
        const kotlin = !refs || !docsOnly(paths);
        appendFileSync(process.env.GITHUB_OUTPUT, `kotlin=${kotlin}\n`);
        console.log(kotlin ? 'Run full Kotlin and documentation CI.' : 'Run documentation CI; skip Kotlin builds and tests.');
        if (refs && kotlin) {
            const requiredBy = paths.filter(path => !docsOnly([path]));
            console.log('Full CI required by:', JSON.stringify(requiredBy));
        }
    }
}
