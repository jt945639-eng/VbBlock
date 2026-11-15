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

    public String src = "";
    public String title = "";
    public int w = 128, h = 72, fps = 10;
    public boolean loop = true, fit = true, dither = false;
    public int mode = MODE_PULSE;

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

    /** true si no hace falta reiniciar ffmpeg al pasar de this a o. */
    public boolean sameVideo(LampConfig o) {
        return src.equals(o.src) && fps == o.fps && loop == o.loop && fit == o.fit;
    }
}
