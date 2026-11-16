/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Paleta de bloques de colores solidos. Solo bloques de vanilla 1.12.2 que se consiguen en supervivencia:
 * concrete (polvo de concrete + agua), lana (ovejas + tinte) y terracota (arcilla cocida + tinte).
 * Convierte un color RGB en el bloque mas parecido.
 * Los colores son aproximaciones del promedio de cada textura; se pueden ajustar aqui.
 */
public final class Palette {

    public static final class Entry {
        public final String group;
        public final String name;
        public final int meta;
        public final int r, g, b;

        Entry(String group, String name, int meta, int rgb) {
            this.group = group;
            this.name = name;
            this.meta = meta;
            this.r = (rgb >> 16) & 255;
            this.g = (rgb >> 8) & 255;
            this.b = rgb & 255;
        }

        @Override
        public String toString() {
            return group + ":" + name;
        }
    }

    // Orden de metadata de BlockColored en 1.12.2
    private static final String[] DYES = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final int[] CONCRETE = {
        0xCFD5D6, 0xE06101, 0xA9309F, 0x2489C7, 0xF1AF15, 0x5EA818, 0xD5658E, 0x373A3E,
        0x7D7D73, 0x157788, 0x641F9C, 0x2D2F8F, 0x603C20, 0x495B24, 0x8E2121, 0x080A0F
    };
    private static final int[] WOOL = {
        0xE9ECEC, 0xF07613, 0xBD44B3, 0x3AAFD9, 0xF8C527, 0x70B919, 0xED8DAC, 0x3E4447,
        0x8E8E86, 0x158991, 0x792AAC, 0x35399D, 0x724728, 0x546D1B, 0xA12722, 0x141519
    };
    private static final int[] TERRACOTTA = {
        0xD1B2A1, 0xA15325, 0x95586C, 0x716C89, 0xBA8523, 0x677535, 0xA14E4E, 0x392923,
        0x876B62, 0x575B5B, 0x764656, 0x4A3B5B, 0x4D3324, 0x4C522A, 0x8F3D2E, 0x251610
    };

    private static final Map<String, List<Entry>> GROUPS = new LinkedHashMap<String, List<Entry>>();

    static {
        GROUPS.put("concrete", build("concrete", CONCRETE));
        GROUPS.put("wool", build("wool", WOOL));
        GROUPS.put("terracotta", build("terracotta", TERRACOTTA));
    }

    private static List<Entry> build(String group, int[] colors) {
        List<Entry> l = new ArrayList<Entry>();
        for (int i = 0; i < 16; i++) {
            l.add(new Entry(group, DYES[i], i, colors[i]));
        }
        return l;
    }

    /** Grupos permitidos: todos son bloques completos de vanilla 1.12.2 obtenibles en supervivencia. */
    public static final Set<String> SURVIVAL_GROUPS = new java.util.LinkedHashSet<String>(
        java.util.Arrays.asList("concrete", "wool", "terracotta"));

    /** Cantidad de bloques del catalogo: concrete (0-15), lana (16-31), terracota (32-47). */
    public static final int CATALOG_SIZE = 48;

    /** Bloque del catalogo: indice 0-47 (grupo = i / 16, tinte = i % 16). */
    public static Entry catalog(int i) {
        if (i < 0 || i >= CATALOG_SIZE) {
            throw new IllegalArgumentException("Bloque fuera del catalogo: " + i);
        }
        int g = i / 16;
        return GROUPS.get(g == 0 ? "concrete" : g == 1 ? "wool" : "terracotta").get(i % 16);
    }

    /** Paleta con solo los bloques activados en la mascara (bit i = catalog(i)). */
    public static Palette ofMask(long mask) {
        List<Entry> l = new ArrayList<Entry>();
        for (int i = 0; i < CATALOG_SIZE; i++) {
            if ((mask & (1L << i)) != 0) {
                l.add(catalog(i));
            }
        }
        if (l.isEmpty()) {
            throw new IllegalArgumentException("La paleta esta vacia");
        }
        return new Palette(l, "bloques:" + l.size());
    }

    public static Set<String> groupNames() {
        return GROUPS.keySet();
    }

    private final List<Entry> entries;
    private final String spec;
    private final byte[] lut = new byte[32768];

    private Palette(List<Entry> entries, String spec) {
        this.entries = entries;
        this.spec = spec;
        for (int k = 0; k < 32768; k++) {
            int r = ((k >> 10) & 31) << 3 | 4;
            int g = ((k >> 5) & 31) << 3 | 4;
            int b = (k & 31) << 3 | 4;
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                double d = dist(r, g, b, e.r, e.g, e.b);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            lut[k] = (byte) best;
        }
    }

    /** Ejemplos: "concrete", "concrete,wool", "all". */
    public static Palette of(String spec) {
        Set<Entry> set = new LinkedHashSet<Entry>();
        for (String tok : spec.toLowerCase().split("[,+ ]+")) {
            tok = tok.trim();
            if (tok.isEmpty()) {
                continue;
            }
            if (tok.equals("all")) {
                for (List<Entry> g : GROUPS.values()) {
                    set.addAll(g);
                }
            } else {
                List<Entry> g = GROUPS.get(tok);
                if (g == null) {
                    throw new IllegalArgumentException("Grupo de bloques desconocido: " + tok
                        + " (validos: " + GROUPS.keySet() + ", all)");
                }
                set.addAll(g);
            }
        }
        if (set.isEmpty()) {
            throw new IllegalArgumentException("La paleta esta vacia");
        }
        return new Palette(new ArrayList<Entry>(set), spec);
    }

    private static double dist(int r1, int g1, int b1, int r2, int g2, int b2) {
        double rm = (r1 + r2) / 2.0;
        double dr = r1 - r2, dg = g1 - g2, db = b1 - b2;
        return (2 + rm / 256.0) * dr * dr + 4 * dg * dg + (2 + (255 - rm) / 256.0) * db * db;
    }

    public int size() {
        return entries.size();
    }

    public Entry get(int index) {
        return entries.get(index);
    }

    public String spec() {
        return spec;
    }

    /** Indice del bloque mas parecido al color dado (0-255 por canal). */
    public byte nearest(int r, int g, int b) {
        return lut[((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3)];
    }
}
