#!/usr/bin/env python3
"""Check a JetBrains plugin distribution zip: structure + descriptor sanity."""
import sys
import zipfile
import xml.dom.minidom as minidom


def main() -> int:
    path = sys.argv[1]
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        required = ["lib/mdl-ide.jar", "META-INF/plugin.xml"]
        missing = [r for r in required if r not in names]
        if missing:
            print("BAD: missing", missing, "| entries:", len(names))
            return 1
        doc = minidom.parseString(z.read("META-INF/plugin.xml"))
        root = doc.documentElement
        if root.tagName != "idea-plugin":
            print("BAD: plugin.xml root is", root.tagName)
            return 1
        iv = root.getElementsByTagName("idea-version")
        since = iv[0].getAttribute("since-build") if iv else "?"
        until = iv[0].getAttribute("until-build") if iv else "?"
        exts = [n.getAttribute("language") for n in root.getElementsByTagName("*")
                if n.tagName.endswith("parserDefinition") and n.hasAttribute("language")]
        with zipfile.ZipFile("build/mdl-ide.jar") as jar:
            classes = sum(1 for n in jar.namelist() if n.endswith(".class"))
        print(f"OK: zip valid | entries={len(names)} | classes={classes}"
              f" | since={since} until={until} | lang({exts})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
