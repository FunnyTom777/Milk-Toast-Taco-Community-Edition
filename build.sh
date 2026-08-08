#!/bin/sh
set -e

cd "$(dirname "$0")"

SRC="Systems"
OUT="out"
LIBS="Libs"

mkdir -p "$OUT"

find "$SRC" -name "*.java" > "$OUT/sources.txt"

CP="$OUT"
for j in "$LIBS"/*.jar; do
    if [ -e "$j" ]; then
        CP="$CP:$j"
    fi
done

echo "Building Milk Toast Taco Community Edition..."
javac -encoding UTF-8 --release 21 -cp "$CP" -d "$OUT" "@$OUT/sources.txt"

echo "Build OK. Classes in \"$OUT\"."
