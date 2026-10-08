@echo off
setlocal
rem VSCode extension publish checker: packages a .vsix, validates it, installs
rem it locally, and -- if a Marketplace PAT is available -- uploads it to the
rem VS Code Marketplace (https://marketplace.visualstudio.com).
rem
rem Marketplace publishing needs (steps in README.md):
rem   1. your vsce token from https://dev.azure.com/<org>/_usersSettings/tokens  or
rem      the "Manage Publishers" page of the marketplace site
rem   2. a registered publisher name (package.json "publisher" must match it)
rem Run:   set VSCE_TOKEN=<token>  then  publish.cmd
rem Safe test without publishing: set SKIP_PUBLISH=1
cd /d "%~dp0"
set "VSIX=iccap-mdl-1.0.0.vsix"

echo [1/4] packaging .vsix ...
call npx --yes @vscode/vsce package --no-yarn --allow-missing-repository
if errorlevel 1 (
    echo FAIL: vsce package failed ^(check npm/network^)
    exit /b 1
)
if not exist "%VSIX%" (
    echo FAIL: %VSIX% not produced
    exit /b 1
)

echo [2/4] validating .vsix ...
python validate_vsix.py "%VSIX%"
if errorlevel 1 exit /b 1

echo [3/4] installing locally ...
if not defined SKIP_INSTALL (
    code --install-extension "%VSIX%" --force
    if errorlevel 1 echo warn: code --install-extension failed ^(VSCode may be busy; the vsix itself is fine^)
)

echo [4/4] Marketplace publish ...
if defined SKIP_PUBLISH (
    echo SKIP: SKIP_PUBLISH set - publish step skipped.
    exit /b 0
)
if not defined VSCE_TOKEN (
    echo SKIP: no Marketplace token. To publish:
    echo   set VSCE_TOKEN=^<token^>     from https://marketplace.visualstudio.com/manage/publishers
    echo   create a publisher, set package.json "publisher" to its name, then re-run.
    exit /b 0
)
echo uploading to Marketplace using VSCE_TOKEN ...
call npx --yes @vscode/vsce publish --no-yarn --allow-missing-repository -p %VSCE_TOKEN%
if errorlevel 1 echo FAIL: publish failed - see vsce error above.
exit /b %errorlevel%
