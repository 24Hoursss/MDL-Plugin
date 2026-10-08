@echo off
setlocal
rem Build mdl-ide.jar against the installed PyCharm platform jars, then install
rem it into the per-user plugins directory.
rem Uses the PyCharm-bundled JBR (JDK 25, includes javac) to match the platform
rem bytecode; the jar tool from any JDK works.
set "IDE=C:\Program Files\JetBrains\PyCharm 2026.1.2"
set "JAVAC=%IDE%\jbr\bin\javac.exe"
set "JAR=jar"
set "IDELIB=%IDE%\lib"
set "PLUGIN_DIR=%APPDATA%\JetBrains\PyCharm2026.1\plugins\mdl-ide"
set "ROOT=%~dp0"
set "OUT=%ROOT%build"

if not exist "%OUT%\classes" mkdir "%OUT%\classes"
dir /b /s "%ROOT%src\*.java" > "%OUT%\sources.txt"

"%JAVAC%" -encoding UTF-8 --release 21 -cp "%IDELIB%\*" -d "%OUT%\classes" @"%OUT%\sources.txt"
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

"%JAR%" cf "%OUT%\mdl-ide.jar" -C "%OUT%\classes" . -C "%ROOT%resources" .
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
