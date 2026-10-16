#!/usr/bin/env python3
"""Pack the compiled plugin: build/mdl-ide.jar, optionally the Marketplace zip.

A jar is a plain zip, so this replaces the JDK `jar` tool (the PyCharm JBR
ships javac but not jar). The jar holds the compiled classes plus
resources/ (META-INF/plugin.xml). With --zip, the Marketplace distribution
zip dist/mdl-ide-<version>.zip is written as well: its root contains only
lib/, because a top-level META-INF next to lib/ is rejected by the upload
validator.
"""
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
CLASSES = os.path.join(ROOT, "build", "classes")
RESOURCES = os.path.join(ROOT, "resources")
JAR = os.path.join(ROOT, "build", "mdl-ide.jar")
PLUGIN_XML = os.path.join(RESOURCES, "META-INF", "plugin.xml")

MANIFEST = "Manifest-Version: 1.0\nCreated-By: pack.py\n\n"


def add_tree(zf: zipfile.ZipFile, base: str) -> int:
    count = 0
    for dirpath, _dirnames, filenames in os.walk(base):
        for name in filenames:
            full = os.path.join(dirpath, name)
            arc = os.path.relpath(full, base).replace(os.sep, "/")
            zf.write(full, arc, compress_type=zipfile.ZIP_DEFLATED)
            count += 1
    return count


def plugin_version() -> str:
    with open(PLUGIN_XML, encoding="utf-8") as fh:
        for line in fh:
            if "<version>" in line:
                return line.split("<version>", 1)[1].split("</version>", 1)[0].strip()
    raise SystemExit("FAIL: no <version> in resources/META-INF/plugin.xml")


def main() -> int:
    if not os.path.isdir(CLASSES):
        print("FAIL: build/classes missing - compile first")
        return 1
    classes = [n for _, _, fs in os.walk(CLASSES) for n in fs if n.endswith(".class")]
    if not classes:
        print("FAIL: build/classes is empty - compile first")
        return 1
    os.makedirs(os.path.dirname(JAR), exist_ok=True)
    with zipfile.ZipFile(JAR, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("META-INF/MANIFEST.MF", MANIFEST)
        n_cls = add_tree(zf, CLASSES)
        n_res = add_tree(zf, RESOURCES)
    print(f"jar: {JAR} | classes={n_cls} resources={n_res}")

    if "--zip" in sys.argv:
        version = plugin_version()
        dist = os.path.join(ROOT, "dist")
        os.makedirs(dist, exist_ok=True)
        plugin_zip = os.path.join(dist, f"mdl-ide-{version}.zip")
        with zipfile.ZipFile(plugin_zip, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.write(JAR, "lib/mdl-ide.jar")
        print(f"zip: {plugin_zip}")

        if "--verify-zip" in sys.argv:
            # verify.cmd layout: the Plugin Verifier CLI rejects a zip whose
            # root holds only lib/, so the jar is wrapped in one folder there.
            verify_dir = os.path.join(dist, "verify")
            os.makedirs(verify_dir, exist_ok=True)
            verify_zip = os.path.join(verify_dir, f"mdl-ide-{version}-verify.zip")
            with zipfile.ZipFile(verify_zip, "w", zipfile.ZIP_DEFLATED) as zf:
                zf.write(JAR, "mdl-ide/lib/mdl-ide.jar")
            print(f"verify zip: {verify_zip}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
