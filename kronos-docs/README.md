# kronos-docs

Official documentation website for Kronos ORM, hosted at [kotlinorm.com](https://kotlinorm.com).

## Tech Stack

- Angular 21 with SSR (`@angular/ssr`)
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

SSR output goes to `dist/kronos-orm-pro/`.
