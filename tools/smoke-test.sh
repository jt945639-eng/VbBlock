#!/usr/bin/env bash
# Copyright (c) 2026 NotXoot. Todos los derechos reservados.
# Prueba el nucleo (sin Minecraft): necesita JDK 8+ y ffmpeg.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
ffmpeg -v error -y -f lavfi -i "testsrc2=size=640x360:rate=24:duration=3" -pix_fmt yuv420p "$OUT/test.mp4"
javac -nowarn -encoding UTF-8 -d "$OUT/cls" src/main/java/com/videoblocks/core/*.java tools/CoreSmokeTest.java
java -Djava.awt.headless=true -cp "$OUT/cls" CoreSmokeTest "$OUT/test.mp4" "$OUT"
