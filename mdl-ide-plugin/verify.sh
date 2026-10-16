#!/usr/bin/env bash
# Local pre-flight with the JetBrains Plugin Verifier - the same tool the
# Marketplace review runs. Builds + packs the plugin, then checks it against
# every IDE passed in IDE_DIRS (the verifier's own "IDE run" step is not
# covered). Needs the verifier CLI jar and unpacked IDE builds:
#
#   VERIFIER_JAR  default ~/.jetbrains-verifier/verifier-cli-1.410-all.jar
#                 (github.com/JetBrains/intellij-plugin-verifier releases)
#   IDE_DIRS      ';'-separated unpacked IDE dirs, default
#                 ~/.jetbrains-verifier/ide-261
#                 (261.27258.48 = the build the Marketplace checks 1.0.x
#                 against; append more, e.g.
#                 IDE_DIRS="$IDE_DIRS;C:/Program Files/JetBrains/PyCharm 2026.2.3")
#
# The verifier CLI rejects a zip whose root holds only lib/, so it gets the
# wrapped dist/verify/...-verify.zip; the jar inside is the uploaded one.
set -e
IDE="${MDL_IDE:-C:/Program Files/JetBrains/PyCharm 2026.2.3}"
JAVAC="$IDE/jbr/bin/javac.exe"
JAVA="$IDE/jbr/bin/java.exe"
PY="${PY:-py -3}"
VERIFIER_JAR="${VERIFIER_JAR:-$HOME/.jetbrains-verifier/verifier-cli-1.410-all.jar}"
IDE_DIRS="${IDE_DIRS:-$HOME/.jetbrains-verifier/ide-261}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
IFS=';' read -r -a IDE_LIST <<< "$IDE_DIRS"

if [ ! -f "$VERIFIER_JAR" ]; then
    echo "SKIP: verifier not found at $VERIFIER_JAR"
    echo "  download verifier-cli-<ver>-all.jar from"
    echo "  https://github.com/JetBrains/intellij-plugin-verifier/releases"
    echo "  and set VERIFIER_JAR, plus an unpacked IDE in IDE_DIRS."
    exit 0
fi

echo "[1/2] building + packing ..."
rm -rf "$ROOT/build/classes"
mkdir -p "$ROOT/build/classes"
"$JAVAC" -encoding UTF-8 --release 21 -cp "$IDE/lib/*" -d "$ROOT/build/classes" "$ROOT"/src/mdlide/*.java
$PY "$ROOT/pack.py" --zip --verify-zip

VERIFY_ZIP=$(ls "$ROOT"/dist/verify/mdl-ide-*-verify.zip | head -1)
echo "[2/2] verifying $VERIFY_ZIP ..."
# deps the verifier downloads (documented-problems page, plugin deps) need
# github.com / plugins.jetbrains.com to be reachable; a fetch failure lands
# here as a non-zero exit too, so read the output before blaming the plugin.
# It writes its report dir to the working directory, hence the cd into
# gitignored build/.
(cd "$ROOT/build" && "$JAVA" -jar "$VERIFIER_JAR" check-plugin "$VERIFY_ZIP" "${IDE_LIST[@]}")
echo
echo "OK: verifier found no problems."
