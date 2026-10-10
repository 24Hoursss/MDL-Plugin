@echo off
setlocal
rem JetBrains plugin publish checker: builds the plugin jar, packs the
rem Marketplace distribution zip (lib/mdl-ide.jar + META-INF/plugin.xml at zip
rem root), validates it, and -- if a Marketplace token is available -- uploads
rem it to plugins.jetbrains.com. Does NOT touch the local IDE plugins dir
rem (use build.bat for that).
rem
rem Marketplace upload needs:
rem   JB_PLUGIN_ID          numeric plugin id from https://plugins.jetbrains.com/plugin/<id>
rem   JB_MARKETPLACE_TOKEN  personal access token from the Marketplace account page
rem Run:   set JB_PLUGIN_ID=<id>  &  set JB_MARKETPLACE_TOKEN=<token>  then  publish.cmd
rem Safe test without publishing: set SKIP_PUBLISH=1
cd /d "%~dp0"
set "IDE=C:\Program Files\JetBrains\PyCharm 2026.1.2"
set "JAVAC=%IDE%\jbr\bin\javac.exe"
set "PLUGIN_ZIP=dist\mdl-ide-1.0.1.zip"

echo [1/4] building plugin jar ...
if not exist build\classes mkdir build\classes
"%JAVAC%" -encoding UTF-8 --release 21 -cp "%IDE%\lib\*" -d build\classes src\mdlide\*.java
if errorlevel 1 (
    echo FAIL: compile error
    exit /b 1
)
jar cf build\mdl-ide.jar -C build\classes . -C resources .
if not exist build\mdl-ide.jar (
    echo FAIL: jar not produced
    exit /b 1
)

echo [2/4] packing Marketplace zip ...
rem Marketplace layout: zip root holds only lib/; plugin.xml must live inside
rem the jar (jar building above already embeds resources\META-INF\plugin.xml).
rem A top-level META-INF next to lib/ is rejected by the upload validator.
if exist "dist\mdl-ide" rmdir /s /q "dist\mdl-ide"
if not exist "dist\mdl-ide\lib" mkdir "dist\mdl-ide\lib"
copy /y build\mdl-ide.jar "dist\mdl-ide\lib\mdl-ide.jar" >nul
powershell -NoProfile -Command "Compress-Archive -Path 'dist\mdl-ide\*' -DestinationPath '%PLUGIN_ZIP%' -Force"
if not exist "%PLUGIN_ZIP%" (
    echo FAIL: zip not produced
    exit /b 1
)

echo [3/4] validating zip ...
python validate_plugin.py "%PLUGIN_ZIP%"
if errorlevel 1 exit /b 1
echo manual install in PyCharm: Settings -^> Plugins -^> Gear -^> Install Plugin from Disk -^> %PLUGIN_ZIP%

echo [4/4] Marketplace upload ...
if defined SKIP_PUBLISH (
    echo SKIP: SKIP_PUBLISH set - publish step skipped.
    exit /b 0
)
if not defined JB_MARKETPLACE_TOKEN (
    echo SKIP: no Marketplace token. To publish:
    echo   set JB_PLUGIN_ID=^<numeric id^>        e.g. from https://plugins.jetbrains.com/plugin/12345
    echo   set JB_MARKETPLACE_TOKEN=^<token^>     from your plugins.jetbrains.com account
    echo   then re-run publish.cmd
    echo   or upload %PLUGIN_ZIP% manually on the plugin page's Update tab.
    exit /b 0
)
echo uploading to plugins.jetbrains.com ...
curl -s -X POST "https://plugins.jetbrains.com/api/uploadPlugin" ^
  -H "Authorization: Bearer %JB_MARKETPLACE_TOKEN%" ^
  -F "pluginId=%JB_PLUGIN_ID%" ^
  -F "channel=stable" ^
  -F "file=@%PLUGIN_ZIP%"
echo.
if errorlevel 1 echo FAIL: upload request errored - see output above.
exit /b %errorlevel%
