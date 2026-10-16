@echo off
setlocal
rem Local pre-flight with the JetBrains Plugin Verifier - the same tool the
rem Marketplace review runs. Builds + packs the plugin, then checks it against
rem every IDE passed in IDE_DIRS (the verifier's own "IDE run" step is not
rem covered). Needs the verifier CLI jar and unpacked IDE builds:
rem
rem   VERIFIER_JAR  default %USERPROFILE%\.jetbrains-verifier\verifier-cli-1.410-all.jar
rem                 (github.com/JetBrains/intellij-plugin-verifier releases)
rem   IDE_DIRS      quoted, ';'-separated unpacked IDE dirs, default
rem                 "%USERPROFILE%\.jetbrains-verifier\ide-261"
rem                 (261.27258.48 = the build the Marketplace checks 1.0.x
rem                 against; append more, e.g.
rem                 set IDE_DIRS=%IDE_DIRS%;"C:\Program Files\JetBrains\PyCharm 2026.2.3")
rem
rem The verifier CLI rejects a zip whose root holds only lib/, so it gets the
rem wrapped dist\verify\...-verify.zip; the jar inside is the uploaded one.
cd /d "%~dp0"
set "IDE=C:\Program Files\JetBrains\PyCharm 2026.2.3"
if defined MDL_IDE set "IDE=%MDL_IDE%"
set "JAVAC=%IDE%\jbr\bin\javac.exe"
set "JAVA=%IDE%\jbr\bin\java.exe"
set "PY=py -3"
if not defined VERIFIER_JAR set "VERIFIER_JAR=%USERPROFILE%\.jetbrains-verifier\verifier-cli-1.410-all.jar"
if not defined IDE_DIRS set IDE_DIRS="%USERPROFILE%\.jetbrains-verifier\ide-261"

if not exist "%VERIFIER_JAR%" (
    echo SKIP: verifier not found at %VERIFIER_JAR%
    echo   download verifier-cli-^<ver^>-all.jar from
    echo   https://github.com/JetBrains/intellij-plugin-verifier/releases
    echo   and set VERIFIER_JAR, plus an unpacked IDE in IDE_DIRS.
    exit /b 0
)

echo [1/2] building + packing ...
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
"%JAVAC%" -encoding UTF-8 --release 21 -cp "%IDE%\lib\*" -d build\classes src\mdlide\*.java
if errorlevel 1 (
    echo FAIL: compile error
    exit /b 1
)
%PY% pack.py --zip --verify-zip
if errorlevel 1 (
    echo FAIL: pack error
    exit /b 1
)
for %%f in ("%ROOT%dist\verify\mdl-ide-*-verify.zip") do set "VERIFY_ZIP=%%~ff"

echo [2/2] verifying %VERIFY_ZIP% ...
rem deps the verifier downloads (documented-problems page, plugin deps) need
rem github.com / plugins.jetbrains.com to be reachable; it writes its report
rem dir to the working directory, hence the pushd into gitignored build\
pushd "%ROOT%build"
"%JAVA%" -jar "%VERIFIER_JAR%" check-plugin "%VERIFY_ZIP%" %IDE_DIRS%
set "RC=%errorlevel%"
popd
if not "%RC%"=="0" (
    echo.
    echo FAIL: verifier did not report OK - read its output above. Problems are
    echo listed as "Plugin problems"; network errors fetching github.com also
    echo land here ^(re-run when the network is back^).
    exit /b 1
)
echo.
echo OK: verifier found no problems.
