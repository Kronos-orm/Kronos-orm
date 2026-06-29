@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%envsetup.bat"

set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.4\bin"
set "PG_BIN=C:\Program Files\PostgreSQL\17\bin"
if exist "%MYSQL_BIN%" set "PATH=%MYSQL_BIN%;%PATH%"
if exist "%PG_BIN%" set "PATH=%PG_BIN%;%PATH%"

echo === Checking database connectivity ===
set "PGPASSWORD=%POSTGRES_PASSWORD%"
for /f %%i in ('psql -U "%POSTGRES_USERNAME%" -d postgres -tAc "SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_database WHERE datname = '%POSTGRES_DATABASE%') THEN 1 ELSE 0 END;"') do set "PG_DB_EXISTS=%%i"
if "%PG_DB_EXISTS%" NEQ "1" (
  psql -U "%POSTGRES_USERNAME%" -d postgres -c "CREATE DATABASE %POSTGRES_DATABASE%;"
  if errorlevel 1 exit /b 1
)
psql -U "%POSTGRES_USERNAME%" -d "%POSTGRES_DATABASE%" -c "SHOW server_version;"
if errorlevel 1 exit /b 1
psql -U kronos -d "%POSTGRES_DATABASE%" -c "SELECT current_user, current_database();"
if errorlevel 1 exit /b 1

set "MYSQL_ARGS=-u %MYSQL_USERNAME%"
if not "%MYSQL_PASSWORD%"=="" set "MYSQL_ARGS=%MYSQL_ARGS% -p%MYSQL_PASSWORD%"
mysql %MYSQL_ARGS% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DATABASE%; CREATE DATABASE IF NOT EXISTS kronos;"
if errorlevel 1 exit /b 1
mysql %MYSQL_ARGS% -D "%MYSQL_DATABASE%" -e "SELECT VERSION();"
if errorlevel 1 exit /b 1

echo === Running tests ===
pushd "%SCRIPT_DIR%" || exit /b 1

call gradlew.bat :kronos-testing:test --stacktrace > "%SCRIPT_DIR%kronos-testing-output.log" 2>&1
if errorlevel 1 exit /b 1
echo === Test report: kronos-testing/build/reports/tests/test/index.html ===

call gradlew.bat :kronos-core:test --stacktrace > "%SCRIPT_DIR%kronos-core-output.log" 2>&1
if errorlevel 1 exit /b 1
echo === Test report: kronos-core/build/reports/tests/test/index.html ===

call gradlew.bat :kronos-codegen:test --stacktrace > "%SCRIPT_DIR%kronos-codegen-output.log" 2>&1
if errorlevel 1 exit /b 1
echo === Test report: kronos-codegen/build/reports/tests/test/index.html ===

call gradlew.bat :kronos-compiler-plugin:test --stacktrace > "%SCRIPT_DIR%kronos-compiler-plugin-output.log" 2>&1
if errorlevel 1 exit /b 1
echo === Test report: kronos-compiler-plugin/build/reports/tests/test/index.html ===

popd
endlocal
