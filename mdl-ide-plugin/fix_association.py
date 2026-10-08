#!/usr/bin/env python3
"""One-time cleanup so .mdl files open as the MDL file type in PyCharm.

Removes (with timestamped backups):
  1. the "UnknownFeatures" stub for com.intellij.fileTypeFactory *.mdl
     in the project's .idea/workspace.xml (recorded while the plugin was not
     installed; it can keep the extension pinned to plain text)
  2. any *.mdl pattern in the PyCharm-level Text file type options
     (options/filetypes.xml, options/ide.general.xml)

Run AFTER closing PyCharm: the IDE rewrites workspace.xml on exit, so editing
it while PyCharm is running would be overwritten.
"""
import datetime
import os
import re
import shutil

BACKUP_SUFFIX = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")


def backup(path):
    if os.path.exists(path):
        bak = f"{path}.bak-{BACKUP_SUFFIX}"
        shutil.copy2(path, bak)
        print(f"backup: {bak}")


def main() -> int:
    ws = r"C:\Users\PC\Desktop\python\.idea\workspace.xml"
    if os.path.exists(ws):
        src = open(ws, encoding="utf-8", errors="replace").read()
        pat = re.compile(
            r'<option featureType="com\.intellij\.fileTypeFactory" '
            r'implementationName="\*\.mdl"\s*/>\s*')
        new, n = pat.subn("", src)
        if n:
            backup(ws)
            open(ws, "w", encoding="utf-8").write(new)
            print(f"workspace.xml: removed {n} UnknownFeatures *.mdl stub(s)")
        else:
            print("workspace.xml: no *.mdl UnknownFeatures stub found")
    else:
        print(f"workspace.xml not found: {ws}")

    opts = os.path.expandvars(r"%APPDATA%\JetBrains\PyCharm2026.1\options")
    for name in ("filetypes.xml", "ide.general.xml"):
        path = os.path.join(opts, name)
        if not os.path.exists(path):
            continue
        src = open(path, encoding="utf-8", errors="replace").read()
        if "*.mdl" not in src:
            print(f"{name}: no *.mdl pattern")
            continue
        backup(path)
        open(path, "w", encoding="utf-8").write(
            re.sub(r'\s*<pattern>\*\.mdl</pattern>', "", src))
        print(f"{name}: removed *.mdl patterns")

    print("Done. Reopen PyCharm and check the status bar of a .mdl file: it")
    print('must show "MDL".')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
