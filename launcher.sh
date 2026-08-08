#!/bin/sh

# ============================================================
#  Milk Toast Taco Community Edition - Launcher
#  Builds MTT and UMML, then lets you pick what to launch.
# ============================================================

cd "$(dirname "$0")"

build_mtt() {
    mkdir -p out
    find Systems -name "*.java" > out/sources.txt
    CP="out"
    for j in Libs/*.jar; do
        if [ -e "$j" ]; then
            CP="$CP:$j"
        fi
    done
    echo "Building Milk Toast Taco Community Edition..."
    javac -encoding UTF-8 --release 21 -cp "$CP" -d out "@out/sources.txt" || {
        echo "BUILD FAILED. See errors above." >&2
        exit 1
    }
}

build_umml() {
    mkdir -p UMML/out
    echo "Building UMML..."
    javac -encoding UTF-8 -d UMML/out UMML/src/umml/*.java || {
        echo "UMML BUILD FAILED. See errors above." >&2
        exit 1
    }
    mkdir -p UMML/lib
    if command -v jar >/dev/null 2>&1; then
        jar cf UMML/lib/umml.jar -C UMML/out umml
        echo "Built UMML/lib/umml.jar"
    else
        echo "JAR packaging skipped - jar not found. UMML classes in UMML/out are still usable."
    fi
}

package_binary() {
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
        return 1
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
}

echo "=========================================="
echo "  Building Milk Toast Taco Community Edition..."
echo "=========================================="
build_mtt

echo ""
echo "=========================================="
echo "  Building UMML..."
echo "=========================================="
build_umml

while true; do
    clear 2>/dev/null || true
    echo "=========================================="
    echo "  Milk Toast Taco Community Edition"
    echo "=========================================="
    echo ""
    echo "  [1] Run MTT (main game)"
    echo "  [2] Run MTT Dev Console"
    echo "  [3] Launch UMML Dashboard"
    echo "  [4] Run UMML Self Tests"
    echo "  [5] Package a Binary (jpackage)"
    echo "  [6] Exit"
    echo ""
    printf "Pick an option: "
    read CHOICE || break
    case "$CHOICE" in
        1)
            CP="out"
            for j in Libs/*.jar; do
                if [ -e "$j" ]; then CP="$CP:$j"; fi
            done
            java -cp "$CP" mtt.Main
            ;;
        2)
            CP="out"
            for j in Libs/*.jar; do
                if [ -e "$j" ]; then CP="$CP:$j"; fi
            done
            java -cp "$CP" mtt.dev.DevConsole
            ;;
        3)
            cd UMML
            java -cp out umml.UMMLDashboard
            cd ..
            ;;
        4)
            cd UMML
            echo "Running mod loading self test..."
            java -cp out umml.UMMLSelfTest
            echo ""
            echo "Running save system self test..."
            java -cp out umml.UMMLSaveSystemTest
            echo ""
            echo "Running scan against MTT_Mods..."
            java -cp out umml.UMMLMain ../MTT_Mods
            cd ..
            ;;
        5)
            package_binary
            ;;
        6)
            exit 0
            ;;
        *)
            continue
            ;;
    esac
    echo ""
    printf "Press Enter to return to the menu..."
    read dummy || true
done
