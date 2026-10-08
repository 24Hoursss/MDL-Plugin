#!/usr/bin/env python3
"""Check a .vsix file for the minimum structure and manifest sanity."""
import json
import sys
import zipfile


def main() -> int:
    path = sys.argv[1]
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        required = ["[Content_Types].xml", "extension.vsixmanifest", "extension/package.json"]
        missing = [r for r in required if r not in names]
        if missing:
            print("BAD: missing", missing, "| entries:", len(names))
            return 1
        pkg = json.loads(z.read("extension/package.json"))
        main = pkg.get("main", "?")
        if main.startswith("./"):
            main = main[2:]
        has_main = "extension/" + main in names
        if not has_main:
            print("BAD: main file", pkg.get("main"), "not in vsix")
            return 1
        langs = [g["language"] for g in pkg.get("contributes", {}).get("grammars", [])]
        print(f"OK: vsix valid | entries={len(names)} | {pkg.get('name')} {pkg.get('version')}"
              f" | grammars={langs}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
