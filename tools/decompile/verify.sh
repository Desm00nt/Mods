#!/usr/bin/env bash
# Compile the reconstructed sources against the generated stubs (no Minecraft/Forge jars needed).
# Usage: verify.sh <src-dir> [out-dir]
set -u
JAVA=${JAVA:-/tmp/tools/jdk4py/jdk4py/java-runtime/bin/java}
ECJ=${ECJ:-/tmp/tools/ecj.jar}
STUBS=${STUBS:-/tmp/work/stubs.jar}
LIBS=${LIBS:-/tmp/work/libs}
SRC=${1:?src dir}
OUT=${2:-/tmp/verify-out}
CP=$STUBS:$(ls "$LIBS"/*.jar | tr '\n' ':')
rm -rf "$OUT"; find "$SRC" -name '*.java' > /tmp/verify-files.txt
"$JAVA" -Xmx2g -jar "$ECJ" -17 -nowarn -proceedOnError -encoding UTF-8 -cp "$CP" -d "$OUT" @/tmp/verify-files.txt > /tmp/verify.log 2>&1
tail -1 /tmp/verify.log
