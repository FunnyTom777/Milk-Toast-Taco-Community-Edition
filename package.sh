#!/bin/sh
set -e

cd "$(dirname "$0")"

./build.sh

BIN=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
    BIN="$JAVA_HOME/bin"
elif [ -x "/usr/lib/jvm/java-26-openjdk/bin/jpackage" ]; then
    BIN="/usr/lib/jvm/java-26-openjdk/bin"
elif command -v jpackage >/dev/null 2>&1; then
    BIN="$(dirname "$(command -v jpackage)")"
fi

if [ -z "$BIN" ]; then
    echo "Could not find a JDK with jpackage. Install a JDK 14+ or set JAVA_HOME." >&2
    exit 1
fi

JAR="$BIN/jar"
JPKG="$BIN/jpackage"

STAGE="dist/app"
DEST="dist"
APP_NAME="MilkToastTaco"

rm -rf "$STAGE"
mkdir -p "$STAGE"

echo "Creating jar..."
"$JAR" --create --file "$STAGE/mtt.jar" --main-class mtt.dev.DevConsole -C out .

echo "Running jpackage..."
"$JPKG" --type app-image --name "$APP_NAME" --input "$STAGE" --main-jar mtt.jar --dest "$DEST" --app-version 0.1.0

echo "Done. Output in $DEST/$APP_NAME"
