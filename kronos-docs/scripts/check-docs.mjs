import assert from 'node:assert/strict';
import {existsSync, readFileSync, readdirSync} from 'node:fs';
import {join} from 'node:path';
import {documentationPages, root} from './docs-model.mjs';
export function checkPages(pages) {
    const routes = new Set(), orders = new Set(), categoryOrders = new Map();
    for (const page of pages) {
        const categoryOrderKey = `${page.locale}:${page.categoryOrder}`;
        assert(page.categoryOrder !== undefined, `Missing category order ${page.category}`);
        assert(!categoryOrders.has(categoryOrderKey) || categoryOrders.get(categoryOrderKey) === page.category,
            `Duplicate category navigation order ${categoryOrderKey}`);
        categoryOrders.set(categoryOrderKey, page.category);
        assert(!routes.has(page.route), `Duplicate route ${page.route}`); routes.add(page.route);
        const orderKey = `${page.category}:${page.order}`;
        assert(page.order !== undefined, `Missing order ${page.file}`);
        assert(!orders.has(orderKey), `Duplicate category order ${orderKey}`); orders.add(orderKey);
    }
    for (const page of pages) {
        const siblingLocale = page.locale === 'en' ? 'zh-CN' : 'en';
        assert(routes.has(page.route.replace(`/documentation/${page.locale}/`, `/documentation/${siblingLocale}/`)), `Missing translation ${page.route}`);
        const markdown = readFileSync(page.markdown, 'utf8');
        assert(markdown.includes(`macros-${page.locale}.njk`), `Wrong macro language ${page.file}`);
        for (const match of markdown.matchAll(/\$\.keyword\(\s*["']([^"']+)["']/g)) {
            assert(routes.has(`/documentation/${page.locale}/${match[1]}`), `Broken keyword ${match[1]} in ${page.markdown}`);
        }
        for (const match of markdown.matchAll(/(?:href=["']|\]\()(\/?(?:#\/)?documentation\/[^\s"')]+)/g)) {
            const target = match[1].replace(/^\/?#?\/?/, '/').split(/[?#]/)[0].replace(/\/$/, '');
            assert(routes.has(target), `Broken documentation link ${match[1]} in ${page.markdown}`);
        }
    }
    return routes;
}
export function checkBlogs(pages, directory = join(root, 'src/assets/blogs')) {
    const routes = new Set(pages.map(page => page.route));
    const slugs = new Map();
    let count = 0;
    for (const locale of ['en', 'zh-CN']) {
        const files = readdirSync(join(directory, locale)).filter(name => name.endsWith('.md')).sort();
        slugs.set(locale, files);
        for (const file of files) {
            const path = join(directory, locale, file);
            const markdown = readFileSync(path, 'utf8');
            for (const match of markdown.matchAll(/(?:href=["']|\]\()(\/?(?:#\/)?documentation\/[^\s"')]+)/g)) {
                const target = match[1].replace(/^\/?#?\/?/, '/').split(/[?#]/)[0].replace(/\/$/, '');
                assert(routes.has(target), `Broken blog documentation link ${match[1]} in ${path}`);
                assert(target.startsWith(`/documentation/${locale}/`), `Wrong blog link language in ${path}`);
            }
            for (const match of markdown.matchAll(/(?:src=["']|!\[[^\]]*\]\()(\/assets\/[^\s"')]+)/g)) {
                assert(existsSync(join(root, 'src', match[1].slice(1))), `Missing blog image ${match[1]} in ${path}`);
            }
            for (const match of markdown.matchAll(/<img\b[^>]*>/g)) {
                assert(/\balt=["'][^"']+["']/.test(match[0]), `Missing blog image description in ${path}`);
            }
            count++;
        }
    }
    assert.deepEqual(slugs.get('en'), slugs.get('zh-CN'), 'Missing blog translation');
    return count;
}
if (process.argv[1]?.endsWith('check-docs.mjs')) {
    const pages = documentationPages(); checkPages(pages);
    const blogs = checkBlogs(pages);
    console.log(`Verified ${pages.length} bilingual routes and ${blogs} blog articles, including links and local images.`);
}
