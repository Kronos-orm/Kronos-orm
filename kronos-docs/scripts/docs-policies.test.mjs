import {mergeSite} from './merge-site.mjs';
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync, mkdtempSync, writeFileSync, mkdirSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import ts from 'typescript';
import {root, documentationPages} from './docs-model.mjs';
import {checkPages, checkBlogs} from './check-docs.mjs';
async function policy(path) {
    const source = readFileSync(join(root, 'src/app', path), 'utf8');
    const {outputText} = ts.transpileModule(source, {compilerOptions: {target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext}});
    return import(`data:text/javascript,${encodeURIComponent(outputText)}`);
}
const locale = await policy('locale/locale-policy.ts');
const search = await policy('search-policy.ts');
const links = await policy('routes/documentation/doc-links.ts');
const applicationLinks = await policy('application-links.ts');
test('blog links preserve the hash router, document language, query and heading', () => {
    const origin = 'http://localhost:3307';
    const current = '/blog?blog=kotlin-multiplatform-support';
    assert.equal(applicationLinks.applicationRoute('/documentation/zh-CN/resources/kefs?tab=setup#versions', origin, current), '/documentation/zh-CN/resources/kefs?tab=setup#versions');
    assert.equal(applicationLinks.applicationRoute('/#/documentation/en/resources/kefs', origin, current), '/documentation/en/resources/kefs');
    assert.equal(applicationLinks.applicationRoute('#shared-model', origin, current), '/blog?blog=kotlin-multiplatform-support#shared-model');
    assert.equal(applicationLinks.applicationRoute('https://github.com/Kronos-orm/Kronos-orm', origin, current), undefined);
    assert.equal(applicationLinks.applicationRoute('/assets/example.md', origin, current), undefined);
});
test('canonical documentation entry keeps its locale and anchor before Angular bootstrap', () => {
    assert.equal(applicationLinks.documentationEntryRoute('/documentation/zh-CN/resources/kefs', '?mode=all', '#setup'), '/documentation/zh-CN/resources/kefs?mode=all#setup');
    assert.equal(applicationLinks.documentationEntryRoute('/documentation/en/resources/kefs', '', '#/documentation/zh-CN/resources/kefs'), undefined);
    assert.equal(applicationLinks.documentationEntryRoute('/', '', '#/blog'), undefined);
});
test('all bilingual blog links and images resolve, and malformed links fail the gate', () => {
    const pages = documentationPages();
    assert.equal(checkBlogs(pages), 6);
    const directory = mkdtempSync(join(tmpdir(), 'kronos-blog-links-'));
    try {
        for (const locale of ['en', 'zh-CN']) {
            mkdirSync(join(directory, locale));
            writeFileSync(join(directory, locale, 'article.md'), `[Setup](/documentation/${locale}/resources/kefs)`);
        }
        assert.equal(checkBlogs(pages, directory), 2);
        writeFileSync(join(directory, 'en/article.md'), '[Broken](/documentation/en/resources/missing)');
        assert.throws(() => checkBlogs(pages, directory), /Broken blog documentation link/);
        writeFileSync(join(directory, 'en/article.md'), '<img src="/assets/missing-blog-image.png" alt="Missing">');
        assert.throws(() => checkBlogs(pages, directory), /Missing blog image/);
    } finally { rmSync(directory, {recursive: true, force: true}); }
});
test('the shared KEFS compatibility table includes the catalogued IDE and KEFS pair', () => {
    const catalog = readFileSync(join(root, '../libs.versions.toml'), 'utf8');
    const macro = readFileSync(join(root, 'src/app/docs/macros/common.njk'), 'utf8');
    const version = name => new RegExp(`^${name} = "([^"]+)"`, 'm').exec(catalog)[1];
    assert.equal(macro.split('\n').find(line => line.startsWith('| {{ kronosVersion() }}')), `| {{ kronosVersion() }} | \`${version('kefs')}\` | \`${version('idea')}\` | \`2.4.20-ij262-52\` |`);
    for (const locale of ['en', 'zh-CN']) {
        const page = pagesFor(locale);
        assert.equal(page.match(/\{\{ \$\.kefsCompatibilityTable\(\) \}\}/g)?.length, 1);
        assert(!page.includes('IDEA **2026.2.3**'));
    }
    function pagesFor(locale) { return readFileSync(join(root, `src/app/docs/${locale}/8.resources/kefs/index.md`), 'utf8'); }
});
test('initial locale uses URL, valid preference, then browser language in that order', () => {
    assert.equal(locale.resolveLocale('/documentation/zh-CN/query/select?from=en#heading', 'en', 'en-US'), 'zh-CN');
    assert.equal(locale.resolveLocale('/home', null, 'zh-TW'), 'zh-CN');
    assert.equal(locale.resolveLocale('/home', 'en', 'zh-CN'), 'en');
    assert.equal(locale.resolveLocale('/home', 'xx', 'de-DE'), 'en');
    assert.equal(locale.routeLocale('/documentation/english/query/select'), undefined);
});
test('switching language preserves the complete document path, query and encoded fragment', () => {
    assert.equal(locale.localizedUrl('/documentation/en/query/select?tag=a%2Fb#%E6%9F%A5%E8%AF%A2', 'zh-CN'), '/documentation/zh-CN/query/select?tag=a%2Fb#%E6%9F%A5%E8%AF%A2');
    assert.equal(locale.localizedUrl('/blog?language=en#entry', 'zh-CN'), '/blog?language=en#entry');
});
test('heading links preserve legacy hash navigation and repository URLs remain idempotent', () => {
    assert.equal(links.documentationAnchorUrl('https://kotlinorm.com', '/', '/documentation/en/query/select?mode=all', 'where clause'), 'https://kotlinorm.com/#/documentation/en/query/select?mode=all#where%20clause');
    const url = 'https://github.com/Kronos-orm/Kronos-orm/blob/main/src/app/docs/en/page.md';
    assert.equal(links.repositorySourceUrl(links.repositorySourceUrl(url)), 'https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-docs/src/app/docs/en/page.md');
});
const index = (title, route, content = '') => ({title, route, content, breadcrumbs: [], section: title, pageType: 'guide'});
test('search separates languages, ranks titles before bodies and supports Unicode and all tokens', () => {
    const indexes = [index('Other', '/documentation/en/a', 'SQLite transaction'), index('SQLite transaction', '/documentation/en/b'), index('SQLite transaction', '/documentation/zh-CN/b'), index('事务', '/documentation/zh-CN/c', 'SQLite 事务')];
    assert.deepEqual(search.rankDocumentation(indexes, ' SQLite transaction ', 'en').map(result => result.index.route), ['/documentation/en/b', '/documentation/en/a']);
    assert.deepEqual(search.rankDocumentation(indexes, '事务', 'zh-CN').map(result => result.index.title), ['事务']);
    assert.equal(search.rankDocumentation(indexes, '   ', 'en').length, 0);
    assert.equal(search.rankDocumentation(indexes, 'SQLite missing', 'en').length, 0);
    assert.equal(search.rankDocumentation(Array.from({length: 20}, (_, n) => index('SQL', `/documentation/en/${n}`)), 'sql', 'en').length, 10);
});
test('bilingual route and link audit accepts every page in the current complete site', () => {
    const pages = documentationPages();
    assert(pages.some(page => page.locale === 'en') && pages.some(page => page.locale === 'zh-CN'));
    assert.equal(checkPages(pages).size, pages.length);
});
test('audit rejects duplicate routes, category order collisions and missing translations', () => {
    const pages = documentationPages();
    assert.throws(() => checkPages([...pages, pages[0]]), /Duplicate route/);
    const collision = pages.map((page, index) => index === 1 ? {...page, category: pages[0].category, order: pages[0].order} : page);
    assert.throws(() => checkPages(collision), /Duplicate category order/);
    assert.throws(() => checkPages(pages.slice(1)), /Missing translation/);
});

test('Unicode case expansion and surrogate pairs keep highlight offsets in the original content', () => {
    const content = 'İ🙂 SQLite';
    const results = search.rankDocumentation([index('Example', '/documentation/en/example', content)], 'sqlite', 'en');
    const position = results[0].positions.content[0];
    assert.equal(content.slice(position.start, position.start + position.length), 'SQLite');
    assert.equal(position.start, 4);
});

test('audit rejects a broken documentation keyword and colliding category navigation orders', () => {
    const pages = documentationPages();
    const directory = mkdtempSync(join(tmpdir(), 'kronos-doc-links-'));
    try {
        const markdown = join(directory, 'index.md');
        writeFileSync(markdown, `{% import "macros-en.njk" as $ %}{{ $.keyword("query/missing", ["Missing"]) }}`);
        assert.throws(() => checkPages(pages.map((page, index) => index === 0 ? {...page, markdown} : page)), /Broken keyword/);
        const category = pages.find(page => page.locale === pages[0].locale && page.category !== pages[0].category);
        assert.throws(() => checkPages(pages.map(page => page.category === category.category ? {...page, categoryOrder: pages[0].categoryOrder} : page)), /Duplicate category navigation order/);
    } finally { rmSync(directory, {recursive: true, force: true}); }
});

test('deployment merge serves all API navigation targets and preserves static pages before the SPA fallback', () => {
    const directory = mkdtempSync(join(tmpdir(), 'kronos-doc-merge-'));
    const angular = join(directory, 'angular'), dokka = join(directory, 'dokka'), plugin = join(directory, 'plugin'), site = join(directory, 'site');
    try {
        mkdirSync(join(angular, 'documentation/en/page'), {recursive: true});
        const modules = ['kronos-codegen', 'kronos-compiler-plugin', 'kronos-core', 'kronos-gradle-plugin', 'kronos-jdbc-wrapper', 'kronos-logging', 'kronos-maven-plugin', 'kronos-syntax'];
        mkdirSync(plugin);
        for (const name of modules.filter(name => name !== 'kronos-gradle-plugin')) {
            mkdirSync(join(dokka, name), {recursive: true});
            writeFileSync(join(dokka, name, 'index.html'), `${name} API`);
        }
        writeFileSync(join(angular, 'documentation/en/page/index.html'), '<article>Real content</article>');
        writeFileSync(join(angular, '_redirects'), '/documentation/en/page /documentation/en/page/index.html 200\n/* /index.html 200\n');
        writeFileSync(join(plugin, 'index.html'), 'kronos-gradle-plugin API');
        mergeSite(angular, dokka, plugin, site);
        assert.equal(readFileSync(join(site, 'api/kronos-core/index.html'), 'utf8'), 'kronos-core API');
        assert.equal(readFileSync(join(site, 'api/kronos-gradle-plugin/index.html'), 'utf8'), 'kronos-gradle-plugin API');
        const apiIndex = readFileSync(join(site, 'api/index.html'), 'utf8');
        const apiLinks = [...apiIndex.matchAll(/href="([^"]+)"/g)].map(match => match[1]);
        assert.deepEqual(apiLinks, modules.map(name => `./${name}/index.html`));
        assert.deepEqual(apiLinks.map(link => readFileSync(join(site, 'api', link), 'utf8')), modules.map(name => `${name} API`));
        for (const source of ['home/home.component.html', 'home/home.component.ts', 'home/components/layout-menu-bar.component.ts', 'blog/blog.component.html']) {
            const navigation = readFileSync(join(root, 'src/app/routes', source), 'utf8');
            const targets = [...navigation.matchAll(/['"](\/api\/[^'"]+)['"]/g)].map(match => match[1]);
            assert(targets.length > 0, `Missing API navigation in ${source}`);
            for (const target of targets) {
                assert.equal(readFileSync(join(site, target), 'utf8'), `${target.split('/')[2]} API`);
            }
        }
        assert.equal(readFileSync(join(site, 'documentation/en/page/index.html'), 'utf8'), '<article>Real content</article>');
        const redirects = readFileSync(join(site, '_redirects'), 'utf8').trim().split('\n');
        assert.equal(redirects[0], '/api/* /api/:splat 200');
        assert.equal(redirects[1], '/documentation/en/page /documentation/en/page/index.html 200');
        assert.equal(redirects.at(-1), '/* /index.html 200');
    } finally { rmSync(directory, {recursive: true, force: true}); }
});

test('search shows a bounded excerpt around a deep match with snippet-relative highlights', () => {
    const content = 'Earlier text. '.repeat(100) + 'SQLite connection' + ' Later text.'.repeat(100);
    const [result] = search.rankDocumentation([index('Database', '/documentation/en/database', content)], 'sqlite', 'en');
    assert(result.index.content.length <= 242);
    assert(result.index.content.startsWith('…') && result.index.content.endsWith('…'));
    const position = result.positions.content[0];
    assert.equal(result.index.content.slice(position.start, position.start + position.length), 'SQLite');
});
