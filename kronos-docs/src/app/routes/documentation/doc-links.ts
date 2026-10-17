/**
 * Builds a stable link for a heading when Angular uses hash-based routing.
 * The router path is the first fragment and the heading id is the second.
 */
export function documentationAnchorUrl(
    origin: string,
    pathname: string,
    route: string,
    anchor: string
): string {
    const normalizedRoute = route.startsWith('/') ? route : `/${route}`;
    const normalizedPathname = pathname || '/';
    return `${origin}${normalizedPathname}#${normalizedRoute}#${encodeURIComponent(anchor)}`;
}

/** Adds the documentation application's repository subdirectory to an NgDoc source URL. */
export function repositorySourceUrl(url: string): string {
    return url.replace(
        /\/(edit|blob)\/([^/]+)\/(?!kronos-docs\/)(src\/app\/docs\/)/,
        '/$1/$2/kronos-docs/$3'
    );
}
