export type Locale = 'en' | 'zh-CN';
export function isLocale(value: unknown): value is Locale { return value === 'en' || value === 'zh-CN'; }
export function routeLocale(url: string): Locale | undefined {
    const match = /^\/?documentation\/(en|zh-CN)(?:\/|[?#]|$)/.exec(url);
    return match?.[1] as Locale | undefined;
}
export function resolveLocale(url: string, preference: unknown, browserLanguage: string): Locale {
    return routeLocale(url) ?? (isLocale(preference) ? preference : browserLanguage.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en');
}
/** Replace only the language segment; preserve encoded paths, query parameters and heading fragments. */
export function localizedUrl(url: string, locale: Locale): string {
    return url.replace(/^(\/?documentation\/)(?:en|zh-CN)(?=\/|[?#]|$)/, `$1${locale}`);
}
