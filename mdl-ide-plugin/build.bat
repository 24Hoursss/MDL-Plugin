@echo off
setlocal
rem Build mdl-ide.jar against the installed PyCharm platform jars, then install
rem it into the per-user plugins directory.
rem javac comes from the PyCharm-bundled JBR; pack.py assembles the jar (the
rem JBR ships no jar tool). Override the IDE path with  set MDL_IDE=<path>.
set "IDE=C:\Program Files\JetBrains\PyCharm 2026.2.3"
if defined MDL_IDE set "IDE=%MDL_IDE%"
set "JAVAC=%IDE%\jbr\bin\javac.exe"
set "PY=py -3"
set "IDELIB=%IDE%\lib"
set "PLUGIN_DIR=%APPDATA%\JetBrains\PyCharm2026.2\plugins\mdl-ide"
set "ROOT=%~dp0"
set "OUT=%ROOT%build"

rem wipe stale classes: a removed source file would otherwise stay in the jar
if exist "%OUT%\classes" rmdir /s /q "%OUT%\classes"
mkdir "%OUT%\classes"
dir /b /s "%ROOT%src\*.java" > "%OUT%\sources.txt"

"%JAVAC%" -encoding UTF-8 --release 21 -cp "%IDELIB%\*" -d "%OUT%\classes" @"%OUT%\sources.txt"
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

%PY% "%ROOT%pack.py"
if errorlevel 1 (
    echo JAR FAILED
    exit /b 1
)

if not exist "%PLUGIN_DIR%\lib" mkdir "%PLUGIN_DIR%\lib"
copy /y "%OUT%\mdl-ide.jar" "%PLUGIN_DIR%\lib\mdl-ide.jar" >nul

echo.
echo OK: %OUT%\mdl-ide.jar
echo installed: %PLUGIN_DIR%\lib\mdl-ide.jar
echo Restart PyCharm to load the plugin.
