@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "COMPOSE_FILE=%SCRIPT_DIR%kronos-testing\docker-compose.integration.yml"
call "%SCRIPT_DIR%envsetup.bat"

docker info >nul 2>&1
if errorlevel 1 (
  echo Docker daemon is not reachable. Start Docker Desktop or set up Docker before running test.bat.
  exit /b 1
)

echo === Starting integration databases with Docker ===
docker compose -f "%COMPOSE_FILE%" up -d --wait
if errorlevel 1 exit /b 1

echo === Preparing SQL Server database ===
docker exec kronos-testing-sqlserver /opt/mssql-tools18/bin/sqlcmd -S localhost -U "%SQLSERVER_USERNAME%" -P "%SQLSERVER_PASSWORD%" -C -Q "IF DB_ID('%SQLSERVER_DATABASE%') IS NULL CREATE DATABASE %SQLSERVER_DATABASE%; SELECT DB_NAME(DB_ID('%SQLSERVER_DATABASE%'));"
if errorlevel 1 docker exec kronos-testing-sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U "%SQLSERVER_USERNAME%" -P "%SQLSERVER_PASSWORD%" -Q "IF DB_ID('%SQLSERVER_DATABASE%') IS NULL CREATE DATABASE %SQLSERVER_DATABASE%; SELECT DB_NAME(DB_ID('%SQLSERVER_DATABASE%'));"
if errorlevel 1 goto :fail

echo === Running tests ===
pushd "%SCRIPT_DIR%" || exit /b 1

call :run_gradle :kronos-testing:test kronos-testing-output.log kronos-testing
if errorlevel 1 goto :fail

call :run_gradle :kronos-core:test kronos-core-output.log kronos-core
if errorlevel 1 goto :fail

call :run_gradle :kronos-syntax:test kronos-syntax-output.log kronos-syntax
if errorlevel 1 goto :fail

call :run_gradle :kronos-codegen:test kronos-codegen-output.log kronos-codegen
if errorlevel 1 goto :fail

call :run_gradle :kronos-compiler-plugin:test kronos-compiler-plugin-output.log kronos-compiler-plugin
if errorlevel 1 goto :fail

popd
if /I not "%KRONOS_TESTING_KEEP_DOCKER%"=="true" docker compose -f "%COMPOSE_FILE%" down
endlocal
exit /b 0

:run_gradle
echo === Running %~1 ===
call gradlew.bat %~1 --stacktrace --console=plain > "%SCRIPT_DIR%%~2" 2>&1
if errorlevel 1 exit /b 1
echo === Test report: %~3/build/reports/tests/test/index.html ===
exit /b 0

:fail
popd >nul 2>&1
if /I not "%KRONOS_TESTING_KEEP_DOCKER%"=="true" docker compose -f "%COMPOSE_FILE%" down
endlocal
exit /b 1
