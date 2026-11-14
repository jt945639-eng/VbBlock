/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.Arrays;

/** Convierte un frame RGB24 en un arreglo de indices de la paleta (uno por pixel/bloque). */
public final class FrameMapper {
    private final Palette palette;
    /** Multiplicador de brillo previo al mapeo (compensa el sombreado de las caras de los bloques). */
    public volatile float gain = 1f;
    /** Dithering Floyd-Steinberg: mas colores aparentes, pero mas cambios de bloque por frame. */
    public volatile boolean dither = false;

    public FrameMapper(Palette palette) {
        this.palette = palette;
    }

    private static int clamp(float v) {
        return v < 0 ? 0 : (v > 255 ? 255 : (int) v);
    }

    public byte[] map(byte[] rgb, int w, int h) {
        final byte[] out = new byte[w * h];
        final float gn = gain;
        if (!dither) {
            for (int i = 0, p = 0; i < out.length; i++, p += 3) {
                out[i] = palette.nearest(clamp((rgb[p] & 255) * gn),
                    clamp((rgb[p + 1] & 255) * gn), clamp((rgb[p + 2] & 255) * gn));
            }
            return out;
        }
        float[] cur = new float[(w + 2) * 3];
        float[] nxt = new float[(w + 2) * 3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = (y * w + x) * 3;
                int c = (x + 1) * 3;
                int ri = clamp((rgb[p] & 255) * gn + cur[c]);
                int gi = clamp((rgb[p + 1] & 255) * gn + cur[c + 1]);
                int bi = clamp((rgb[p + 2] & 255) * gn + cur[c + 2]);
                byte idx = palette.nearest(ri, gi, bi);
                out[y * w + x] = idx;
                Palette.Entry e = palette.get(idx);
                float er = ri - e.r, eg = gi - e.g, eb = bi - e.b;
                cur[c + 3] += er * 7f / 16f;
                cur[c + 4] += eg * 7f / 16f;
                cur[c + 5] += eb * 7f / 16f;
                nxt[c - 3] += er * 3f / 16f;
                nxt[c - 2] += eg * 3f / 16f;
                nxt[c - 1] += eb * 3f / 16f;
                nxt[c] += er * 5f / 16f;
                nxt[c + 1] += eg * 5f / 16f;
                nxt[c + 2] += eb * 5f / 16f;
                nxt[c + 3] += er / 16f;
                nxt[c + 4] += eg / 16f;
                nxt[c + 5] += eb / 16f;
            }
            float[] t = cur;
            cur = nxt;
            nxt = t;
            Arrays.fill(nxt, 0f);
        }
        return out;
    }
}
