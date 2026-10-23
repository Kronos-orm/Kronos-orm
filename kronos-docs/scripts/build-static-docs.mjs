import assert from 'node:assert/strict';
import {mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {JSDOM} from 'jsdom';
import {checkPages} from './check-docs.mjs';
import {documentationPages, literal, root} from './docs-model.mjs';

const pages = documentationPages();
checkPages(pages);
const output = join(root, 'docs');
const site = new URL(process.env.DOCS_SITE_URL ?? 'https://kotlinorm.com');
const escape = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('"', '&quot;');
const indexes = [];
function readableText(node) {
    if (node.nodeType === 3) return node.textContent ?? '';
    const content = [...node.childNodes].map(readableText).join('');
    return /^(H[1-6]|P|PRE|LI|TD|TH|TR|BLOCKQUOTE|SECTION|DIV)$/i.test(node.nodeName) ? ` ${content} ` : content;
}
const css = `:root{font-family:system-ui,sans-serif;color:#dce1ea;background:#10131a;color-scheme:dark}body{margin:auto;max-width:1000px;padding:24px;line-height:1.7}a{color:#92b8ff}nav{display:flex;flex-wrap:wrap;gap:16px;border-bottom:1px solid #3a4252;padding:12px 0}h1,h2,h3,h4{scroll-margin-top:20px;line-height:1.3}pre{overflow:auto;padding:16px;border:1px solid #3a4252;border-radius:8px;white-space:pre;background:#151a23!important;color:#e2e8f0!important}code{font-family:ui-monospace,monospace}p code,li code{background:#202735;padding:2px 4px;border-radius:3px}table{border-collapse:collapse;display:block;overflow:auto}td,th{border:1px solid #3a4252;padding:8px 12px}blockquote{border-left:4px solid #92b8ff;padding:8px 20px;background:#191f2a}img{max-width:100%}.code-variant{border-top:1px solid #3a4252}.code-variant>h3{font-size:14px;color:#a9bad5}pre span{color:var(--shiki-dark,inherit)!important}footer{margin-top:40px;border-top:1px solid #3a4252;padding-top:20px}#contents{columns:2;list-style:none;padding:0}@media(max-width:600px){body{padding:16px}#contents{columns:1}}`;
mkdirSync(join(output, 'assets'), {recursive: true});
writeFileSync(join(output, 'assets/static-docs.css'), css);
for (const page of pages) {
    const html = literal(readFileSync(page.generated, 'utf8'), 'pageContent');
    assert(html, `No generated content: ${page.generated}`);
    const header = literal(readFileSync(join(dirname(dirname(page.generated)), 'page.ts'), 'utf8'), 'headerContent');
    assert(header, `No generated header: ${page.generated}`);
    const document = new JSDOM(`<article>${header}${html}</article>`).window.document;
    const article = document.querySelector('article');
    for (const demo of article.querySelectorAll('ng-doc-demo')) {
        if (demo.getAttribute('componentname') === 'AnimateLogoComponent') demo.remove();
        else {
            const link = document.createElement('a');
            link.href = `/#${page.route}`;
            link.textContent = page.locale === 'en' ? 'Open interactive example' : '打开交互示例';
            demo.replaceWith(link);
        }
    }
    article.querySelectorAll('ng-doc-heading-anchor').forEach(node => node.remove());
    article.querySelectorAll('h1 code, h2 code, h3 code, h4 code, h5 code, h6 code').forEach(code => {
        const next = code.nextSibling;
        if (next?.nodeType === 3 && /^[\p{L}\p{N}]/u.test(next.textContent ?? '')) next.textContent = ` ${next.textContent}`;
    });
    article.querySelectorAll('.noun svg').forEach(node => node.remove());
    article.querySelectorAll('[onclick]').forEach(node => { node.removeAttribute('onclick'); node.removeAttribute('style'); });
    for (const node of article.querySelectorAll('ng-doc-blockquote, ng-doc-tab')) {
        const replacement = document.createElement(node.localName === 'ng-doc-tab' ? 'section' : 'blockquote');
        if (node.localName === 'ng-doc-tab') {
            replacement.className = 'code-variant';
            const label = document.createElement('h3'); label.textContent = node.getAttribute('name') ?? 'Code'; replacement.append(label);
        }
        replacement.append(...node.childNodes); node.replaceWith(replacement);
    }
    for (const link of article.querySelectorAll('a[href]')) {
        const href = link.getAttribute('href');
        if (/^\/?(?:#\/)?documentation\//.test(href)) {
            const normalized = href.replace(/^\/?#?\/?/, '/');
            const [route, fragment] = normalized.split('#');
            link.setAttribute('href', `${route.replace(/\/$/, '')}/${fragment ? `#${fragment}` : ''}`);
        }
    }
    for (const image of article.querySelectorAll('img[src]')) {
        if (image.getAttribute('src').startsWith('assets/')) image.setAttribute('src', `/${image.getAttribute('src')}`);
    }
    const toc = [...article.querySelectorAll('h2[id]')].map(heading => `<li><a href="#${escape(heading.id)}">${escape(heading.textContent)}</a></li>`).join('');
    const otherLocale = page.locale === 'en' ? 'zh-CN' : 'en';
    const alternate = page.route.replace(`/documentation/${page.locale}/`, `/documentation/${otherLocale}/`);
    const canonical = new URL(`${page.route}/`, site).href;
    const searchableContent = readableText(article).replace(/\s+/g, ' ').trim();
    const entry = `<html lang="${page.locale}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(page.title)} | Kronos ORM</title><meta name="description" content="${escape(searchableContent.slice(0, 180))}"><link rel="canonical" href="${canonical}"><link rel="alternate" hreflang="${page.locale}" href="${canonical}"><link rel="alternate" hreflang="${otherLocale}" href="${new URL(`${alternate}/`, site).href}"><link rel="stylesheet" href="/assets/static-docs.css"></head><body><nav aria-label="Documentation"><a href="/documentation/${page.locale}/">Kronos ORM</a><a href="${alternate}/">${otherLocale === 'en' ? 'English' : '简体中文'}</a><a href="/#${page.route}">${page.locale === 'en' ? 'Interactive documentation' : '交互文档'}</a></nav><ul id="contents">${toc}</ul>${article.outerHTML}<footer><a href="https://github.com/Kronos-orm/Kronos-orm">GitHub</a> · Apache 2.0</footer></body></html>`;
    const destination = join(output, page.route, 'index.html'); mkdirSync(dirname(destination), {recursive: true}); writeFileSync(destination, `<!doctype html>${entry}`);
    indexes.push({title: page.title, section: page.title, breadcrumbs: [page.locale, page.category.split('/')[1]],
        content: searchableContent, pageType: 'guide', route: page.route});
    document.defaultView.close();
}
for (const locale of ['en', 'zh-CN']) {
    const navigation = pages.filter(page => page.locale === locale).map(page => `<li><a href="${page.route}/">${escape(page.title)}</a></li>`).join('');
    const alternate = locale === 'en' ? 'zh-CN' : 'en';
    const canonical = new URL(`/documentation/${locale}/`, site).href;
    writeFileSync(join(output, 'documentation', locale, 'index.html'), `<!doctype html><html lang="${locale}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Kronos ORM Documentation</title><link rel="canonical" href="${canonical}"><link rel="alternate" hreflang="${locale}" href="${canonical}"><link rel="alternate" hreflang="${alternate}" href="${new URL(`/documentation/${alternate}/`, site).href}"><link rel="stylesheet" href="/assets/static-docs.css"></head><body><nav><a href="/">Kronos ORM</a><a href="/documentation/${alternate}/">${alternate === 'en' ? 'English' : '简体中文'}</a></nav><h1>Kronos ORM</h1><ul>${navigation}</ul></body></html>`);
}
writeFileSync(join(output, 'assets/ng-doc/indexes.json'), JSON.stringify(indexes));
writeFileSync(join(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${pages.map(page => `<url><loc>${new URL(`${page.route}/`, site).href}</loc></url>`).join('')}</urlset>`);
writeFileSync(join(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${new URL('/sitemap.xml', site).href}\n`);
// Explicit static paths are resolved before the SPA fallback, including on hosts using _redirects.
writeFileSync(join(output, '_redirects'), `${pages.map(page => `${page.route} ${page.route}/index.html 200\n${page.route}/ ${page.route}/index.html 200`).join('\n')}\n/* /index.html 200\n`);
console.log(`Built ${pages.length} readable static documentation pages, search indexes, sitemap and redirects.`);
