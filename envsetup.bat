@echo off

set "SCRIPT_DIR=%~dp0"
set "DEFAULTS_FILE=%SCRIPT_DIR%envsetup.defaults"

if exist "%DEFAULTS_FILE%" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%DEFAULTS_FILE%") do (
    if not defined %%A set "%%A=%%B"
  )
)
if not defined SQLSERVER_JDBC_URL set "SQLSERVER_JDBC_URL=jdbc:sqlserver://localhost:1433;databaseName=%SQLSERVER_DATABASE%;encrypt=true;trustServerCertificate=true"
