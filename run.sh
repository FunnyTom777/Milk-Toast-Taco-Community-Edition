#!/bin/sh

cd "$(dirname "$0")"

if [ ! -f "out/mtt/Main.class" ]; then
    echo "No build found. Run ./build.sh first."
    exit 1
fi

CP="out"
for j in "Libs"/*.jar; do
    if [ -e "$j" ]; then
        CP="$CP:$j"
    fi
done

exec java -cp "$CP" mtt.Main "$@"
