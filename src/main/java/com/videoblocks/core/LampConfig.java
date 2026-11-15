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

    /** Tipos de bloque que puede usar la pantalla (mascara de bits). Todos son de supervivencia 1.12.2. */
    public static final int BLOCK_CONCRETE = 1, BLOCK_WOOL = 2, BLOCK_TERRACOTTA = 4;
    public static final int BLOCKS_DEFAULT = BLOCK_CONCRETE | BLOCK_WOOL;

    public String src = "";
    public String title = "";
    public int w = 128, h = 72, fps = 10;
    public boolean loop = true, fit = true, dither = false;
    public int mode = MODE_PULSE;
    public int stopMode = STOP_POSTER;
    public int blocks = BLOCKS_DEFAULT;

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
        fps = clamp(fps, 1, 20);
        mode = clamp(mode, 0, 1);
        stopMode = clamp(stopMode, 0, 1);
        blocks &= 7;
        if (blocks == 0) {
            blocks = BLOCKS_DEFAULT; // siempre debe quedar al menos un tipo de bloque
        }
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

    /** Lista para Palette.of(): "concrete,wool". */
    public String blocksSpec() {
        StringBuilder sb = new StringBuilder();
        if ((blocks & BLOCK_CONCRETE) != 0) {
            sb.append("concrete,");
        }
        if ((blocks & BLOCK_WOOL) != 0) {
            sb.append("wool,");
        }
        if ((blocks & BLOCK_TERRACOTTA) != 0) {
            sb.append("terracotta,");
        }
        return sb.substring(0, sb.length() - 1);
    }

    /** Texto corto para el menu: "concrete + lana". */
    public String blocksSummary() {
        return blocksSpec().replace("wool", "lana").replace("terracotta", "terracota").replace(",", " + ");
    }

    /** Cantidad de colores distintos disponibles (16 por tipo de bloque). */
    public int colorCount() {
        return 16 * Integer.bitCount(blocks & 7);
    }

    /** true si no hace falta reiniciar ffmpeg al pasar de this a o. */
    public boolean sameVideo(LampConfig o) {
        return src.equals(o.src) && fps == o.fps && loop == o.loop && fit == o.fit;
    }
}
