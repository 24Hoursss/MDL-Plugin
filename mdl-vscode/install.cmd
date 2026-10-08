@echo off
setlocal
rem Install the IC-CAP MDL extension into the user VSCode extensions directory
rem (same effect as "Install from VSIX"; restart VSCode to load it).
set "EXT_DIR=%USERPROFILE%\.vscode\extensions\iccap-mdl-1.0.0"
set "ROOT=%~dp0"

if exist "%EXT_DIR%" rmdir /s /q "%EXT_DIR%"
mkdir "%EXT_DIR%"
copy /y "%ROOT%package.json" "%EXT_DIR%\" >nul
copy /y "%ROOT%language-configuration.json" "%EXT_DIR%\" >nul
copy /y "%ROOT%extension.js" "%EXT_DIR%\" >nul
copy /y "%ROOT%README.md" "%EXT_DIR%\" >nul
if not exist "%EXT_DIR%\syntaxes" mkdir "%EXT_DIR%\syntaxes"
copy /y "%ROOT%syntaxes\mdl.tmLanguage.json" "%EXT_DIR%\syntaxes\" >nul

echo.
echo installed: %EXT_DIR%
echo Restart VSCode to load the extension.
