#!/usr/bin/env bash
#
# deploy-docs.sh — Build and merge kronos-docs + Dokka API docs into a single site.
#
# Output: dist/site/  (ready for Cloudflare Pages deployment)
#
# Usage:
#   ./deploy-docs.sh              # full build (Dokka + Angular)
#   ./deploy-docs.sh --skip-dokka # skip Dokka, only rebuild Angular (faster iteration)
#
# Requirements:
#   - JDK 21+ (for Gradle / Dokka)
#   - Node.js 20+ and pnpm (for Angular)
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
SITE_DIR="$REPO_ROOT/dist/site"
DOCS_DIR="$REPO_ROOT/kronos-docs"
DOKKA_OUT="$REPO_ROOT/docs"

SKIP_DOKKA=false
for arg in "$@"; do
  case "$arg" in
    --skip-dokka) SKIP_DOKKA=true ;;
    -h|--help)
      echo "Usage: $0 [--skip-dokka]"
      echo "  --skip-dokka  Skip Dokka API docs generation (reuse existing docs/ output)"
      exit 0
      ;;
    *) echo "Unknown option: $arg"; exit 1 ;;
  esac
done

# ── Step 1: Build Dokka API docs ──────────────────────────────────────────────
if [ "$SKIP_DOKKA" = false ]; then
  echo "▸ Building Dokka API docs..."
  "$REPO_ROOT/gradlew" -p "$REPO_ROOT" :dokkaGenerateAll --no-daemon -q
else
  echo "▸ Skipping Dokka build (--skip-dokka)"
fi

if [ ! -d "$DOKKA_OUT" ]; then
  echo "✗ Dokka output not found at $DOKKA_OUT"
  echo "  Run without --skip-dokka first, or run: ./gradlew :dokkaGenerateAll"
  exit 1
fi

# ── Step 2: Build kronos-docs (Angular) ──────────────────────────────────────
echo "▸ Building kronos-docs..."
cd "$DOCS_DIR"

if [ ! -d "node_modules" ]; then
  echo "  Installing dependencies..."
  pnpm install --no-frozen-lockfile
fi

pnpm build

ANGULAR_OUT="$DOCS_DIR/docs"
if [ ! -d "$ANGULAR_OUT" ]; then
  echo "✗ Angular build output not found at $ANGULAR_OUT"
  exit 1
fi

# ── Step 3: Merge into dist/site/ ────────────────────────────────────────────
echo "▸ Merging outputs into $SITE_DIR ..."
rm -rf "$SITE_DIR"
mkdir -p "$SITE_DIR"

# Copy Angular site as the base
cp -R "$ANGULAR_OUT"/. "$SITE_DIR"/

# Move Dokka API docs under /api/<module>/
mkdir -p "$SITE_DIR/api"
for module_dir in "$DOKKA_OUT"/*/; do
  module_name="$(basename "$module_dir")"
  mv "$module_dir" "$SITE_DIR/api/$module_name"
done
rmdir "$DOKKA_OUT" 2>/dev/null || true

echo "▸ Site structure:"
echo "  $SITE_DIR/"
echo "  ├── index.html          (kronos-docs)"
echo "  ├── api/"
for module_dir in "$DOKKA_OUT"/*/; do
  echo "  │   ├── $(basename "$module_dir")/"
done
echo "  └── ..."

# ── Step 4: Cloudflare Pages SPA routing ─────────────────────────────────────
# _redirects: let /api/* serve static Dokka files, everything else falls back to
# Angular's index.html for client-side routing.
cat > "$SITE_DIR/_redirects" <<'EOF'
/api/*  /api/:splat  200
/*      /index.html  200
EOF

echo ""
echo "✓ Done! Deploy $SITE_DIR to Cloudflare Pages."
echo ""
echo "  Quick deploy (wrangler):"
echo "    npx wrangler pages deploy $SITE_DIR --project-name=kotlinorm"
echo ""
echo "  Or connect your repo in the Cloudflare dashboard and set:"
echo "    Build command:  bash deploy-docs.sh"
echo "    Output dir:     dist/site"
