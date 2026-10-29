# Bootstrap the catalogued official Kotlin script runner on Windows.
$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $PSScriptRoot
$catalog = Get-Content -Raw (Join-Path $scriptRoot 'libs.versions.toml')
$version = [regex]::Match($catalog, '(?m)^kotlin = "([^"]+)"\r?$').Groups[1].Value
$jackson = [regex]::Match($catalog, '(?m)^jackson = "([^"]+)"\r?$').Groups[1].Value
if (!$version -or !$jackson) { throw 'Missing Kotlin or Jackson version in catalog' }
$cache = if ($env:KRONOS_SCRIPT_CACHE) { $env:KRONOS_SCRIPT_CACHE } else { Join-Path $env:LOCALAPPDATA 'kronos-scripts' }
$sdk = Join-Path $cache "kotlin-$version"
if (!(Test-Path (Join-Path $sdk 'kotlinc/bin/kotlin.bat'))) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $temporary = Join-Path $cache ([guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $temporary | Out-Null
    try {
        $url = "https://github.com/JetBrains/kotlin/releases/download/v$version/kotlin-compiler-$version.zip"
        $archive = Join-Path $temporary 'compiler.zip'
        Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive
        $checksumFile = Join-Path $temporary 'compiler.sha256'
        Invoke-WebRequest -UseBasicParsing -Uri "$url.sha256" -OutFile $checksumFile
        $expected = (Get-Content -Raw $checksumFile).Trim()
        if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'Kotlin script SDK checksum mismatch' }
        Expand-Archive -Path $archive -DestinationPath (Join-Path $temporary 'sdk')
        Move-Item (Join-Path $temporary 'sdk') $sdk
    } finally { Remove-Item -Recurse -Force $temporary }
}
$dependencies = Join-Path $scriptRoot 'build/scripts/dependencies.main.kts'
New-Item -ItemType Directory -Force -Path (Split-Path $dependencies) | Out-Null
$text = '@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:{0}")' -f $jackson
if (!(Test-Path $dependencies) -or (Get-Content -Raw $dependencies).Trim() -ne $text) { [IO.File]::WriteAllText($dependencies, $text + "`n", [Text.UTF8Encoding]::new($false)) }
if ($args.Count -eq 0) { throw 'Usage: scripts\run.cmd <name.main.kts> [arguments]' }
$entry = $args[0]
if (!$entry.EndsWith('.main.kts')) { $entry += '.main.kts' }
if (![IO.Path]::IsPathRooted($entry)) { $entry = $(if (Test-Path (Join-Path $scriptRoot $entry)) { Join-Path $scriptRoot $entry } else { Join-Path $scriptRoot "scripts/$entry" }) }
$env:KRONOS_ROOT = $scriptRoot
$env:KRONOS_SCRIPT_RUNNER = Join-Path $PSScriptRoot 'run.cmd'
# The SDK's kotlinc.bat appends to KOTLIN_OPTS and passes it to its JVM children.
# Nested scripts must start with their own arguments rather than the parent's entry point.
$env:KOTLIN_OPTS = ''
$scriptSources = (Get-ChildItem (Join-Path $scriptRoot 'scripts/*.main.kts') | Sort-Object Name | ForEach-Object { [IO.File]::ReadAllText($_.FullName) }) -join ''
$scriptHash = [Security.Cryptography.SHA256]::Create()
try { $fingerprint = ([BitConverter]::ToString($scriptHash.ComputeHash([Text.Encoding]::UTF8.GetBytes($scriptSources + $text + $env:JAVA_HOME)))).Replace('-', '').ToLowerInvariant() }
finally { $scriptHash.Dispose() }
$compiledCache = Join-Path $cache "compiled/$version/$fingerprint"
$env:KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR = $compiledCache
& (Join-Path $sdk 'kotlinc/bin/kotlin.bat') $entry @($args | Select-Object -Skip 1)
exit $LASTEXITCODE
