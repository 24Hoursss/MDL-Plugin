@echo off
setlocal
rem One-time fix for ".mdl opens as plain text" in PyCharm.
rem
rem IMPORTANT: close PyCharm FIRST (File -> Exit). The IDE rewrites
rem .idea/workspace.xml on exit, so editing it while PyCharm is running
rem would be overwritten.
rem
rem The cleanup itself lives in fix_association.py (backups + removals).
echo Check: is PyCharm running?
tasklist | findstr /i "pycharm64.exe" >nul
if not errorlevel 1 (
    echo.
    echo   YES - PyCharm is running. Close it completely, then re-run this script.
    exit /b 1
)
echo   NO - proceeding.
echo.
python "%~dp0fix_association.py"
if errorlevel 1 (
    echo FAIL: python error - fix_association.py did not finish.
    exit /b 1
)
echo.
echo Done. Reopen PyCharm, then check the status bar (bottom-right) of any .mdl
echo file: it must show "MDL" - with colored keywords and fold arrows.
echo If it still shows Text, also check Settings - Editor - File Types - Text.
