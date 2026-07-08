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
MIN_JAVA_MAJOR="${DOCS_MIN_JAVA_MAJOR:-17}"
BOOTSTRAP_JDK_FEATURE="${DOCS_BOOTSTRAP_JDK_FEATURE:-21}"
BOOTSTRAP_JDK_DIR="${DOCS_BOOTSTRAP_JDK_DIR:-$DOCS_ROOT/.cache/temurin-jdk-$BOOTSTRAP_JDK_FEATURE}"
BOOTSTRAP_JDK_URL="${DOCS_BOOTSTRAP_JDK_URL:-https://api.adoptium.net/v3/binary/latest/$BOOTSTRAP_JDK_FEATURE/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk}"

java_major_version() {
  if ! command -v java >/dev/null 2>&1; then
    return 1
  fi

  local version
  version="$(java -XshowSettings:properties -version 2>&1 | awk -F '=' '/java.specification.version/ { gsub(/[[:space:]]/, "", $2); print $2; exit }')"

  if [ -z "$version" ]; then
    return 1
  fi

  case "$version" in
    1.*) echo "${version#1.}" | cut -d. -f1 ;;
    *) echo "$version" | cut -d. -f1 ;;
  esac
}

download_file() {
  local url="$1"
  local output="$2"

  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$url" -o "$output"
  elif command -v wget >/dev/null 2>&1; then
    wget -qO "$output" "$url"
  else
    echo "✗ Neither curl nor wget is available to download a JDK."
    exit 1
  fi
}

ensure_java() {
  local current_major=""

  if current_major="$(java_major_version)"; then
    if [ "$current_major" -ge "$MIN_JAVA_MAJOR" ]; then
      echo "▸ Using Java $(java -version 2>&1 | head -n 1)"
      return
    fi
  fi

  if [ "$(uname -s)" != "Linux" ] || ! uname -m | grep -Eq '^(x86_64|amd64)$'; then
    echo "✗ Java $MIN_JAVA_MAJOR+ is required. Install a JDK or set JAVA_HOME before running this script."
    exit 1
  fi

  if [ ! -x "$BOOTSTRAP_JDK_DIR/bin/java" ]; then
    echo "▸ Java $MIN_JAVA_MAJOR+ not found. Downloading Temurin JDK $BOOTSTRAP_JDK_FEATURE..."
    local archive
    archive="$(mktemp)"
    rm -rf "$BOOTSTRAP_JDK_DIR"
    mkdir -p "$BOOTSTRAP_JDK_DIR"
    download_file "$BOOTSTRAP_JDK_URL" "$archive"
    tar -xzf "$archive" --strip-components=1 -C "$BOOTSTRAP_JDK_DIR"
    rm -f "$archive"
  fi

  export JAVA_HOME="$BOOTSTRAP_JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "▸ Using bootstrapped Java $(java -version 2>&1 | head -n 1)"
}

ensure_java

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
