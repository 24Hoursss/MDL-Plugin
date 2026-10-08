#!/usr/bin/env bash
# Build mdl-ide.jar against the installed PyCharm platform jars and install it
# into the per-user plugins directory.
# Uses the PyCharm-bundled JBR (JDK 25, includes javac) to match the platform
# bytecode; the jar tool from any JDK works.
set -e
IDE="C:/Program Files/JetBrains/PyCharm 2026.1.2"
JAVAC="$IDE/jbr/bin/javac.exe"
JAR="${JAR:-jar}"
IDELIB="$IDE/lib"
PLUGIN_DIR="$APPDATA/JetBrains/PyCharm2026.1/plugins/mdl-ide"
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build"

mkdir -p "$OUT/classes"

"$JAVAC" -encoding UTF-8 --release 21 -cp "$IDELIB/*" -d "$OUT/classes" "$ROOT"/src/mdlide/*.java
"$JAR" cf "$OUT/mdl-ide.jar" -C "$OUT/classes" . -C "$ROOT/resources" .

mkdir -p "$PLUGIN_DIR/lib"
cp "$OUT/mdl-ide.jar" "$PLUGIN_DIR/lib/mdl-ide.jar"

echo "OK: $OUT/mdl-ide.jar"
echo "installed: $PLUGIN_DIR/lib/mdl-ide.jar"
echo "Restart PyCharm to load the plugin."
