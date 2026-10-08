@echo off
setlocal
rem One-time fix for ".mdl opens as plain text" in PyCharm.
rem
rem IMPORTANT: close PyCharm FIRST (File -> Exit). The IDE rewrites
rem .idea/workspace.xml on exit, so editing it while PyCharm is running
rem would be overwritten.
rem
rem What it removes (with backups):
rem   1. the "UnknownFeatures" entry for com.intellij.fileTypeFactory *.mdl
rem      in the project's .idea/workspace.xml (a stub recorded while the
rem      plugin was not installed; it can keep the extension pinned to text)
rem   2. any *.mdl pattern in the PyCharm-level Text file type options
echo.
echo Check: is PyCharm running?
tasklist | findstr /i "pycharm64.exe" >nul
if not errorlevel 1 (
    echo   YES - close PyCharm completely (File -^> Exit) and re-run this script.
    exit /b 1
)
echo   NO - proceeding.

python - <<'PYEOF'
import os, re, sys, shutil, datetime

def backup(path):
    if os.path.exists(path):
        bak = path + ".bak-" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        shutil.copy2(path, bak)
        print("backup:", bak)

# 1) remove UnknownFeatures *.mdl stub from the project workspace.xml
ws = r"C:\Users\PC\Desktop\python\.idea\workspace.xml"
if os.path.exists(ws):
    src = open(ws, encoding="utf-8", errors="replace").read()
    pat = re.compile(r'<option featureType="com\.intellij\.fileTypeFactory" implementationName="\*\.mdl"\s*/>\s*')
    new, n = pat.subn("", src)
    if n:
        backup(ws)
        open(ws, "w", encoding="utf-8").write(new)
        print(f"workspace.xml: removed {n} UnknownFeatures *.mdl stub(s)")
        # if the component became empty, leave it; the IDE cleans it up
    else:
        print("workspace.xml: no *.mdl UnknownFeatures stub found")
else:
    print("workspace.xml not found:", ws)

# 2) remove *.mdl from the Text file type patterns (options/filetypes.xml)
opts = os.path.expandvars(r"%APPDATA%\JetBrains\PyCharm2026.1\options")
for name in ("filetypes.xml", "ide.general.xml"):
    p = os.path.join(opts, name)
    if not os.path.exists(p):
        continue
    src = open(p, encoding="utf-8", errors="replace").read()
    if "*.mdl" not in src:
        print(name, ": no *.mdl pattern")
        continue
    backup(p)
    new = re.sub(r'\s*<pattern>\*\.mdl</pattern>', "", src)
    open(p, "w", encoding="utf-8").write(new)
    print(name, ": removed *.mdl patterns")
PYEOF

echo.
echo Done. Reopen PyCharm, then check the status bar (bottom-right) of any .mdl
echo file: it must show "MDL" (with colored keywords and fold arrows).
echo If it still says Text: Settings -^> Editor -^> File Types -^> Text -^> remove
echo *.mdl from the patterns there, reopen the file.
