import type {NgDocPageIndex} from '@ng-doc/core';
import type {NgDocSearchResult} from '@ng-doc/app';
import type {Locale} from './locale/locale-policy';

/** All tokens must match. Title and route matches rank ahead of body-only matches. */
export function rankDocumentation(indexes: NgDocPageIndex[], query: string, locale: Locale): NgDocSearchResult[] {
    const tokens = query.trim().toLocaleLowerCase(locale).split(/\s+/u).filter(Boolean);
    if (!tokens.length) return [];
    const normalize = (value: string) => value.toLocaleLowerCase(locale);
    const foldContent = (value: string) => {
        let text = '', offset = 0;
        const starts: number[] = [], ends: number[] = [];
        for (const character of value) {
            const folded = normalize(character);
            text += folded;
            for (let index = 0; index < folded.length; index++) { starts.push(offset); ends.push(offset + character.length); }
            offset += character.length;
        }
        return {text, starts, ends};
    };
    return indexes.filter(index => index.route.startsWith(`/documentation/${locale}/`))
        .map(index => {
            const title = normalize(index.title), route = normalize(index.route), folded = foldContent(index.content ?? '');
            const content = folded.text;
            if (!tokens.every(token => `${title} ${route} ${content}`.includes(token))) return null;
            const score = tokens.reduce((total, token) => total + (title === token ? 100 : title.startsWith(token) ? 60 : title.includes(token) ? 40 : route.includes(token) ? 20 : 1), 0);
            return {index, score, positions: {content: tokens.flatMap(token => {
                const start = content.indexOf(token);
                return start < 0 ? [] : [{start: folded.starts[start], length: folded.ends[start + token.length - 1] - folded.starts[start]}];
            })}};
        }).filter((result): result is NonNullable<typeof result> => result !== null)
        .sort((a, b) => b.score - a.score || a.index.route.localeCompare(b.index.route))
        .slice(0, 10).map(({index, positions}) => {
            const content = index.content ?? '';
            const firstMatch = positions.content.length ? Math.min(...positions.content.map(position => position.start)) : 0;
            let start = Math.max(0, firstMatch - 60), end = Math.min(content.length, start + 240);
            // Keep snippet boundaries outside UTF-16 surrogate pairs.
            if (start > 0 && /[\uDC00-\uDFFF]/u.test(content[start])) start--;
            if (end < content.length && /[\uDC00-\uDFFF]/u.test(content[end])) end--;
            const prefix = start ? '…' : '';
            return {index: {...index, content: prefix + content.slice(start, end) + (end < content.length ? '…' : '')},
                positions: {content: positions.content.filter(position => position.start >= start && position.start + position.length <= end)
                    .map(position => ({start: position.start - start + prefix.length, length: position.length}))}};
        });
}
