/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

/** Ajustes de una lampara de video (se guardan en el item y en el mundo). */
public final class LampConfig {
    /** Cada pulso de Redstone alterna reproducir / detener. */
    public static final int MODE_PULSE = 0;
    /** Reproduce mientras haya senal de Redstone y se detiene cuando no hay. */
    public static final int MODE_LEVEL = 1;

    /** Al detener, la pantalla vuelve al primer fotograma del video. */
    public static final int STOP_POSTER = 0;
    /** Al detener, la pantalla se queda congelada en el fotograma actual. */
    public static final int STOP_FREEZE = 1;

    /** Maximo de fotogramas por segundo del video. */
    public static final int MAX_FPS = 60;

    public String src = "";
    public String title = "";
    public int w = 128, h = 72, fps = 10;
    public boolean loop = true, fit = true, dither = false;
    public int mode = MODE_PULSE;
    public int stopMode = STOP_POSTER;
    public BlockSet blocks = BlockSet.DEFAULT;

    public LampConfig copy() {
        LampConfig c = new LampConfig();
        c.src = src;
        c.title = title;
        c.w = w;
        c.h = h;
        c.fps = fps;
        c.loop = loop;
        c.fit = fit;
        c.dither = dither;
        c.mode = mode;
        c.stopMode = stopMode;
        c.blocks = blocks;
        return c;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Limita los valores a rangos validos y rellena el titulo si esta vacio. */
    public LampConfig sanitize(int maxSide) {
        src = src == null ? "" : src.trim();
        title = title == null ? "" : title.trim();
        w = clamp(w, 1, maxSide);
        h = clamp(h, 1, maxSide);
        fps = clamp(fps, 1, MAX_FPS);
        mode = clamp(mode, 0, 1);
        stopMode = clamp(stopMode, 0, 1);
        // al menos 2 bloques: si faltan se completan con lana negra y piedra luminosa
        blocks = blocks == null ? BlockSet.MINIMAL : blocks.ensureMinimum();
        if (title.isEmpty()) {
            String t = src;
            int cut = Math.max(t.lastIndexOf('/'), t.lastIndexOf('\\'));
            if (cut >= 0 && cut < t.length() - 1) {
                t = t.substring(cut + 1);
            }
            title = t;
        }
        if (title.length() > 48) {
            title = title.substring(0, 48);
        }
        return this;
    }

    /** Convierte el formato antiguo (3 bits: 1 concrete, 2 lana, 4 terracota) a un conjunto de bloques. */
    public static BlockSet groupBitsToSet(int old) {
        BlockSet s = BlockSet.of();
        if ((old & 1) != 0) {
            s = s.with(Palette.categoryIndices(0), true);
        }
        if ((old & 2) != 0) {
            s = s.with(Palette.categoryIndices(1), true);
        }
        if ((old & 4) != 0) {
            s = s.with(Palette.categoryIndices(2), true);
        }
        return s.isEmpty() ? BlockSet.DEFAULT : s;
    }

    public boolean hasBlock(int index) {
        return blocks.has(index);
    }

    /** Texto corto para el menu: "concrete 16, lana 12" o "140 bloques". */
    public String blocksSummary() {
        int total = blocks.count();
        if (total == Palette.CATALOG_SIZE) {
            return "todos los bloques (" + total + ")";
        }
        StringBuilder sb = new StringBuilder();
        int cats = 0;
        for (int c = 0; c < Palette.categoryCount(); c++) {
            int n = blocks.count(Palette.categoryIndices(c));
            if (n > 0) {
                cats++;
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(Palette.categoryTitle(c).toLowerCase()).append(' ').append(n);
            }
        }
        return cats <= 3 ? sb.toString() : total + " bloques de " + cats + " categorías";
    }

    /** Cantidad de bloques (colores) activos. */
    public int colorCount() {
        return blocks.count();
    }

    /** true si no hace falta reiniciar ffmpeg al pasar de this a o. */
    public boolean sameVideo(LampConfig o) {
        return src.equals(o.src) && fps == o.fps && loop == o.loop && fit == o.fit;
    }
}
