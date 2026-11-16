#!/usr/bin/env bash
# Copyright (c) 2026 NotXoot. Todos los derechos reservados.
# Prueba el nucleo (sin Minecraft): necesita JDK 8+ y ffmpeg.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
ffmpeg -v error -y -f lavfi -i "testsrc2=size=640x360:rate=24:duration=3" -pix_fmt yuv420p "$OUT/test.mp4"
# GIFs de prueba: uno animado (20 fotogramas) y otro mitad roja / mitad azul
ffmpeg -v error -y -f lavfi -i "testsrc2=size=160x90:rate=10:duration=2" "$OUT/anim.gif"
ffmpeg -v error -y -f lavfi -i "color=c=red:s=80x90:d=1,format=rgb24" -f lavfi -i "color=c=blue:s=80x90:d=1,format=rgb24" \
  -filter_complex "[0][1]hstack" -frames:v 1 "$OUT/split.gif"
ffmpeg -v error -y -i "$OUT/anim.gif" -vf "scale=32:18:flags=area" -fps_mode passthrough -f rawvideo -pix_fmt rgb24 "$OUT/ref.rgb" \
  || ffmpeg -v error -y -i "$OUT/anim.gif" -vf "scale=32:18:flags=area" -vsync 0 -f rawvideo -pix_fmt rgb24 "$OUT/ref.rgb"
mkdir -p "$OUT/cls"
javac -nowarn -encoding UTF-8 -d "$OUT/cls" src/main/java/com/videoblocks/core/*.java tools/CoreSmokeTest.java tools/GifSmokeTest.java
java -Djava.awt.headless=true -cp "$OUT/cls" CoreSmokeTest "$OUT/test.mp4" "$OUT"
java -Djava.awt.headless=true -cp "$OUT/cls" GifSmokeTest "$OUT/anim.gif" "$OUT/split.gif" "$OUT/ref.rgb" "$OUT"
