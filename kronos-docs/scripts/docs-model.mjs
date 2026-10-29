import {readFileSync, readdirSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import ts from 'typescript';
export const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export function files(directory, name) {
    return readdirSync(directory, {withFileTypes: true}).flatMap(entry => entry.isDirectory() ? files(join(directory, entry.name), name) : entry.name === name ? [join(directory, entry.name)] : []);
}
export function literal(source, key) {
    const ast = ts.createSourceFile('metadata.ts', source, ts.ScriptTarget.Latest, true);
    let value;
    function visit(node) {
        if ((ts.isPropertyAssignment(node) && node.name.getText(ast) === key) || (ts.isVariableDeclaration(node) && node.name.getText(ast) === key)) {
            const item = node.initializer;
            if (item && (ts.isStringLiteralLike(item) || ts.isNumericLiteral(item))) value = item.text;
        }
        ts.forEachChild(node, visit);
    }
    visit(ast);
    return value;
}
export function documentationPages() {
    return ['en', 'zh-CN'].flatMap(locale => files(join(root, 'src/app/docs', locale), 'ng-doc.page.ts').map(file => {
        const source = readFileSync(file, 'utf8');
        const categorySource = readFileSync(join(dirname(dirname(file)), 'ng-doc.category.ts'), 'utf8');
        const category = literal(categorySource, 'route');
        const slug = literal(source, 'route');
        if (!category || !slug) throw new Error(`Page/category requires a literal route: ${file}`);
        return {file, locale, route: `/documentation/${category}/${slug}`, slug,
            order: literal(source, 'order'), categoryOrder: literal(categorySource, 'order'), category, title: literal(source, 'title'),
            markdown: join(dirname(file), literal(source, 'mdFile') ?? 'index.md'),
            generated: join(root, 'ng-doc/kronos-orm-pro/guides', dirname(file).slice(join(root, 'src/').length), 'index/page.ts')};
    }));
}
