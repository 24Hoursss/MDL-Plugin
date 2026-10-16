#!/usr/bin/env bash
# Build mdl-ide.jar against the installed PyCharm platform jars and install it
# into the per-user plugins directory.
# javac comes from the PyCharm-bundled JBR; pack.py assembles the jar (the JBR
# ships no jar tool). Override with  MDL_IDE=<path>  /  PY=<python>.
set -e
IDE="${MDL_IDE:-C:/Program Files/JetBrains/PyCharm 2026.2.3}"
JAVAC="$IDE/jbr/bin/javac.exe"
PY="${PY:-py -3}"
IDELIB="$IDE/lib"
PLUGIN_DIR="$APPDATA/JetBrains/PyCharm2026.2/plugins/mdl-ide"
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build"

# wipe stale classes: a removed source file would otherwise stay in the jar
rm -rf "$OUT/classes"
mkdir -p "$OUT/classes"

"$JAVAC" -encoding UTF-8 --release 21 -cp "$IDELIB/*" -d "$OUT/classes" "$ROOT"/src/mdlide/*.java
$PY "$ROOT/pack.py"

mkdir -p "$PLUGIN_DIR/lib"
cp "$OUT/mdl-ide.jar" "$PLUGIN_DIR/lib/mdl-ide.jar"

echo "OK: $OUT/mdl-ide.jar"
echo "installed: $PLUGIN_DIR/lib/mdl-ide.jar"
echo "Restart PyCharm to load the plugin."
