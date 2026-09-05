# kronos-docs

Official documentation website for Kronos ORM, hosted at [kotlinorm.com](https://kotlinorm.com).

## Tech Stack

- Angular 21
- ng-doc for documentation rendering
- Transloco for i18n (English + Chinese)
- PrimeNG + Tailwind CSS for UI
- CodeMirror for code highlighting
- pnpm package manager

## Project Structure

```
src/
├── app/
│   ├── docs/          # Documentation pages (en/, zh-CN/, macros/)
│   ├── components/    # Shared UI components
│   ├── routes/        # Route definitions
│   └── app.routes.ts  # Top-level routing
├── assets/
│   ├── blogs/         # Blog posts (en/, zh-CN/)
│   ├── i18n/          # Translation files (en.json, zh-CN.json)
│   ├── images/        # Static images
│   └── icons/         # Icon assets
└── styles.css         # Global styles (Tailwind)
```

## Development

```bash
pnpm install
pnpm start          # serves at http://localhost:3307
```

## Build

```bash
pnpm build
```

Build output goes to `docs/`, matching the `angular.json` `outputPath`.

## Full Site Build

```bash
./deploy-docs.sh
```

The full site build generates Dokka API docs from the repository root, builds the Angular docs app, and merges everything into `dist/site/`.

## Cloudflare Pages

Use these settings when deploying the full documentation site, including Dokka API docs:

- Root directory: `kronos-docs`
- Build command: `./deploy-docs.sh`
- Build output directory: `dist/site`
