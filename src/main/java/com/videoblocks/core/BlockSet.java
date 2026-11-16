/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.Arrays;

/** Conjunto inmutable de bloques activados del catalogo de Palette (un bit por bloque, hasta 192). */
public final class BlockSet {
    public static final int WORDS = 3;

    /** Todos los bloques del catalogo. */
    public static final BlockSet ALL = range(0, Palette.CATALOG_SIZE);
    /** Por defecto: concrete + lana (los 32 primeros). */
    public static final BlockSet DEFAULT = range(0, 32);

    /** Lo que queda cuando se desactivan todos los bloques: lana negra y piedra luminosa. */
    public static final BlockSet MINIMAL = of(Palette.indexOf("wool:black"), Palette.indexOf("nether_end:glowstone"));

    /** Minimo de bloques activos para poder dibujar un video. */
    public static final int MIN_BLOCKS = 2;

    private final long[] w;

    private BlockSet(long[] w) {
        this.w = w;
    }

    public static BlockSet ofWords(long w0, long w1, long w2) {
        long[] a = {w0, w1, w2};
        for (int i = 0; i < WORDS; i++) {
            int lo = i * 64;
            int n = Palette.CATALOG_SIZE - lo;
            if (n <= 0) {
                a[i] = 0;
            } else if (n < 64) {
                a[i] &= (1L << n) - 1;
            }
        }
        return new BlockSet(a);
    }

    public static BlockSet range(int from, int to) {
        long[] a = new long[WORDS];
        for (int i = from; i < to && i < WORDS * 64; i++) {
            a[i >> 6] |= 1L << (i & 63);
        }
        return new BlockSet(a);
    }

    public static BlockSet of(int... indices) {
        long[] a = new long[WORDS];
        for (int i : indices) {
            a[i >> 6] |= 1L << (i & 63);
        }
        return new BlockSet(a);
    }

    /** Si hay menos de 2 bloques activos, completa con lana negra y piedra luminosa hasta tener 2. */
    public BlockSet ensureMinimum() {
        BlockSet s = this;
        int[] fill = {Palette.indexOf("wool:black"), Palette.indexOf("nether_end:glowstone")};
        for (int i = 0; i < fill.length && s.count() < MIN_BLOCKS; i++) {
            s = s.with(fill[i], true);
        }
        return s;
    }

    public long word(int i) {
        return w[i];
    }

    public boolean has(int i) {
        return i >= 0 && i < Palette.CATALOG_SIZE && (w[i >> 6] & (1L << (i & 63))) != 0;
    }

    public BlockSet with(int i, boolean on) {
        if (i < 0 || i >= Palette.CATALOG_SIZE) {
            return this;
        }
        long[] a = w.clone();
        if (on) {
            a[i >> 6] |= 1L << (i & 63);
        } else {
            a[i >> 6] &= ~(1L << (i & 63));
        }
        return new BlockSet(a);
    }

    public BlockSet with(int[] indices, boolean on) {
        BlockSet s = this;
        for (int i : indices) {
            s = s.with(i, on);
        }
        return s;
    }

    public int count() {
        int n = 0;
        for (long x : w) {
            n += Long.bitCount(x);
        }
        return n;
    }

    public int count(int[] indices) {
        int n = 0;
        for (int i : indices) {
            if (has(i)) {
                n++;
            }
        }
        return n;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockSet && Arrays.equals(w, ((BlockSet) o).w);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(w);
    }
}
