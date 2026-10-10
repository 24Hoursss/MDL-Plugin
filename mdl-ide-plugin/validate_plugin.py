#!/usr/bin/env python3
"""Check a JetBrains plugin distribution zip: structure + descriptor sanity.

Marketplace layout: the zip root contains only the lib/ folder; lib/mdl-ide.jar
embeds META-INF/plugin.xml. A top-level META-INF/plugin.xml next to lib/ is
rejected by the upload validator.
"""
import io
import sys
import zipfile
import xml.dom.minidom as minidom


def main() -> int:
    path = sys.argv[1]
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        root_entries = [n for n in names if "/" not in n]
        if root_entries:
            print("BAD: zip root must contain only the lib/ folder, got", root_entries)
            return 1
        if "lib/mdl-ide.jar" not in names:
            print("BAD: missing lib/mdl-ide.jar | entries:", len(names))
            return 1
        with zipfile.ZipFile(io.BytesIO(z.read("lib/mdl-ide.jar"))) as jar:
            jar_names = jar.namelist()
            if "META-INF/plugin.xml" not in jar_names:
                print("BAD: jar missing META-INF/plugin.xml")
                return 1
            doc = minidom.parseString(jar.read("META-INF/plugin.xml"))
            root = doc.documentElement
            if root.tagName != "idea-plugin":
                print("BAD: plugin.xml root is", root.tagName)
                return 1
            iv = root.getElementsByTagName("idea-version")
            since = iv[0].getAttribute("since-build") if iv else "?"
            until = iv[0].getAttribute("until-build") if iv else "?"
            exts = [n.getAttribute("language") for n in root.getElementsByTagName("*")
                    if n.tagName.endswith("parserDefinition") and n.hasAttribute("language")]
            classes = sum(1 for n in jar_names if n.endswith(".class"))
        print(f"OK: zip valid | entries={len(names)} | classes={classes}"
              f" | since={since} until={until} | lang({exts})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
