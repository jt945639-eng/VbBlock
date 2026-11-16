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

    /**
     * Bloques que puede usar la pantalla: mascara de 48 bits, un bit por bloque del catalogo
     * (bits 0-15 concrete, 16-31 lana, 32-47 terracota; orden de tintes de Palette). Todos son de supervivencia 1.12.2.
     */
    public static final long BLOCK_CONCRETE = 0xFFFFL;
    public static final long BLOCK_WOOL = 0xFFFFL << 16;
    public static final long BLOCK_TERRACOTTA = 0xFFFFL << 32;
    public static final long BLOCKS_ALL = BLOCK_CONCRETE | BLOCK_WOOL | BLOCK_TERRACOTTA;
    public static final long BLOCKS_DEFAULT = BLOCK_CONCRETE | BLOCK_WOOL;

    public String src = "";
    public String title = "";
    public int w = 128, h = 72, fps = 10;
    public boolean loop = true, fit = true, dither = false;
    public int mode = MODE_PULSE;
    public int stopMode = STOP_POSTER;
    public long blocks = BLOCKS_DEFAULT;

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
        blocks &= BLOCKS_ALL;
        if (blocks == 0) {
            blocks = BLOCKS_DEFAULT; // siempre debe quedar al menos un bloque
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

    /** Convierte el formato antiguo (3 bits: 1 concrete, 2 lana, 4 terracota) a la mascara de 48 bits. */
    public static long groupBitsToMask(int old) {
        long m = 0;
        if ((old & 1) != 0) {
            m |= BLOCK_CONCRETE;
        }
        if ((old & 2) != 0) {
            m |= BLOCK_WOOL;
        }
        if ((old & 4) != 0) {
            m |= BLOCK_TERRACOTTA;
        }
        return m == 0 ? BLOCKS_DEFAULT : m;
    }

    public boolean hasBlock(int index) {
        return index >= 0 && index < 48 && (blocks & (1L << index)) != 0;
    }

    /** Texto corto para el menu: "concrete 16, lana 12". */
    public String blocksSummary() {
        String[] names = {"concrete", "lana", "terracota"};
        StringBuilder sb = new StringBuilder();
        for (int g = 0; g < 3; g++) {
            int n = Long.bitCount((blocks >>> (16 * g)) & 0xFFFFL);
            if (n > 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(names[g]).append(' ').append(n);
            }
        }
        return sb.toString();
    }

    /** Cantidad de bloques (colores) activos, de 1 a 48. */
    public int colorCount() {
        return Long.bitCount(blocks & BLOCKS_ALL);
    }

    /** true si no hace falta reiniciar ffmpeg al pasar de this a o. */
    public boolean sameVideo(LampConfig o) {
        return src.equals(o.src) && fps == o.fps && loop == o.loop && fit == o.fit;
    }
}
