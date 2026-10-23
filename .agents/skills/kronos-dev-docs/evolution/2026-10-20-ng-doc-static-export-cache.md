# NgDoc page wrappers must be regenerated for static export

The static documentation exporter reads each generated NgDoc page wrapper, including its
compiled parent page. Angular can complete an incremental build using cached application
output even when a generated wrapper is absent. The subsequent export then fails with
`ENOENT` for a generated `parent-page.ts` file.

Set `cache: false` in `kronos-docs/ng-doc.config.ts`. Keep Angular's own build cache;
NgDoc regenerates the complete wrapper tree on each build. Do not hide missing wrappers
or reduce the static route list.

Verified with the production `pnpm build`: all 150 static pages, route anchors, metadata,
search entries and sitemap checks pass after full NgDoc generation.
