import assert from 'node:assert/strict';
import {cpSync, existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync} from 'node:fs';
import {join, resolve} from 'node:path';

/** Merge the same production outputs on every deployment; preserve static routes ahead of SPA fallback. */
export function mergeSite(angularOutput, dokkaOutput, gradlePluginOutput, siteOutput) {
    for (const source of [angularOutput, dokkaOutput, gradlePluginOutput]) {
        assert(existsSync(source), `Missing documentation output ${source}`);
        assert(resolve(source) !== resolve(siteOutput), 'Site output must differ from every input directory');
    }
    const redirects = readFileSync(join(angularOutput, '_redirects'), 'utf8');
    rmSync(siteOutput, {recursive: true, force: true});
    cpSync(angularOutput, siteOutput, {recursive: true});
    const apiOutput = join(siteOutput, 'api'); mkdirSync(apiOutput, {recursive: true});
    for (const entry of readdirSync(dokkaOutput, {withFileTypes: true})) {
        if (entry.isDirectory()) cpSync(join(dokkaOutput, entry.name), join(apiOutput, entry.name), {recursive: true});
    }
    cpSync(gradlePluginOutput, join(apiOutput, 'kronos-gradle-plugin'), {recursive: true});
    const modules = readdirSync(apiOutput, {withFileTypes: true})
        .filter(entry => entry.isDirectory() && existsSync(join(apiOutput, entry.name, 'index.html')))
        .map(entry => entry.name).sort();
    assert(modules.length > 0, 'The API site must contain module documentation');
    const items = modules.map(name => {
        const label = name.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
        return `<li><a href="./${encodeURIComponent(name)}/index.html">${label}</a></li>`;
    }).join('\n');
    writeFileSync(join(apiOutput, 'index.html'), `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Kronos API documentation</title></head>
<body><main><h1>Kronos API documentation</h1><ul>${items}</ul></main></body></html>\n`);
    writeFileSync(join(siteOutput, '_redirects'), `/api/* /api/:splat 200\n${redirects}`);
}
if (process.argv[1]?.endsWith('merge-site.mjs')) {
    assert.equal(process.argv.length, 6, 'Expected Angular, Dokka, Gradle-plugin and site output directories');
    mergeSite(...process.argv.slice(2));
    console.log(`Merged static documentation and Dokka API into ${process.argv[5]}`);
}
