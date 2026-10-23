/** Resolve application links without treating a Markdown heading as the router fragment. */
export function applicationRoute(href: string, origin: string, currentRoute: string): string | undefined {
    if (href.startsWith('#') && !href.startsWith('#/')) {
        return currentRoute.split('#')[0] + href;
    }
    let url: URL;
    try { url = new URL(href, origin); } catch { return undefined; }
    if (url.origin !== origin) return undefined;
    const route = url.hash.startsWith('#/') ? url.hash.slice(1) : url.pathname + url.search + url.hash;
    return /^\/(?:documentation\/(?:en|zh-CN)(?:\/|[?#]|$)|blog(?:[?#]|$)|home(?:[?#]|$))/.test(route)
        ? route : undefined;
}

/** Accept canonical documentation links when the development server serves the SPA fallback. */
export function documentationEntryRoute(path: string, query: string, hash: string): string | undefined {
    return /^\/documentation\/(?:en|zh-CN)(?:\/|$)/.test(path) && !hash.startsWith('#/')
        ? path + query + hash : undefined;
}
