#!/usr/bin/env bash
#
# deploy-docs.sh - Build and merge kronos-docs + Dokka API docs into a single site.
#
# Output: kronos-docs/dist/site/
#
set -euo pipefail

DOCS_ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$DOCS_ROOT/.." && pwd)"
SITE_DIR="$DOCS_ROOT/dist/site"
DOKKA_OUT="$REPO_ROOT/docs"
GRADLE_PLUGIN_DOKKA_OUT="$REPO_ROOT/kronos-gradle-plugin/build/dokka/html"

echo "▸ Building Dokka API docs..."
"$REPO_ROOT/gradlew" -p "$REPO_ROOT" :dokkaGenerateAll --no-daemon -q
"$REPO_ROOT/gradlew" -p "$REPO_ROOT/kronos-gradle-plugin" :dokkaGenerate --no-daemon -q

if [ ! -d "$DOKKA_OUT" ]; then
  echo "✗ Dokka output not found at $DOKKA_OUT"
  exit 1
fi

if [ ! -d "$GRADLE_PLUGIN_DOKKA_OUT" ]; then
  echo "✗ Gradle plugin Dokka output not found at $GRADLE_PLUGIN_DOKKA_OUT"
  exit 1
fi

echo "▸ Building kronos-docs..."
cd "$DOCS_ROOT"

if [ ! -d "node_modules" ]; then
  echo "  Installing dependencies..."
  pnpm install --no-frozen-lockfile
fi

pnpm build

ANGULAR_OUT="$DOCS_ROOT/docs"
if [ ! -d "$ANGULAR_OUT" ]; then
  echo "✗ Angular build output not found at $ANGULAR_OUT"
  exit 1
fi

echo "▸ Merging outputs into $SITE_DIR ..."
rm -rf "$SITE_DIR"
mkdir -p "$SITE_DIR"

cp -R "$ANGULAR_OUT"/. "$SITE_DIR"/

mkdir -p "$SITE_DIR/api"
for module_dir in "$DOKKA_OUT"/*/; do
  module_name="$(basename "$module_dir")"
  cp -R "$module_dir" "$SITE_DIR/api/$module_name"
done
cp -R "$GRADLE_PLUGIN_DOKKA_OUT" "$SITE_DIR/api/kronos-gradle-plugin"

cat > "$SITE_DIR/_redirects" <<'EOF'
/api/*  /api/:splat  200
/*      /index.html  200
EOF

echo "▸ Site structure:"
echo "  $SITE_DIR/"
echo "  ├── index.html          (kronos-docs)"
echo "  ├── api/"
for module_dir in "$SITE_DIR/api"/*/; do
  echo "  │   ├── $(basename "$module_dir")/"
done
echo "  └── ..."
echo ""
echo "✓ Done! Deploy $SITE_DIR."
