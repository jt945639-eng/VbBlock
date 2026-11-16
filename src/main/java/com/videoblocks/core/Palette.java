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
        /** Categoria (concrete, wool, terracotta, stone, ...). */
        public final String group;
        /** Nombre corto en ingles (white, granite...). */
        public final String name;
        /** Nombre en español para el menu. */
        public final String es;
        /** Registro de vanilla sin "minecraft:" (concrete, stained_hardened_clay, stone...). */
        public final String block;
        public final int meta;
        public final int r, g, b;

        Entry(String group, String block, int meta, String name, String es, int rgb) {
            this.group = group;
            this.block = block;
            this.meta = meta;
            this.name = name;
            this.es = es;
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
    private static final String[] DYES_ES = {
        "Blanco", "Naranja", "Magenta", "Azul claro", "Amarillo", "Lima", "Rosa", "Gris",
        "Gris claro", "Cian", "Morado", "Azul", "Marrón", "Verde", "Rojo", "Negro"
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
    private static final int[] GLAZED = {
        0xD6DCD8, 0xC58A4E, 0xC86CC0, 0x6DB5D1, 0xEBC565, 0x9BC74B, 0xE59DB5, 0x555A5C,
        0x9DA4A5, 0x2F7F84, 0x7C3FA0, 0x3A56A0, 0x7B5A44, 0x6E8C3B, 0xB04439, 0x2C2327
    };

    /** Categorias en el orden en que se muestran en el menu (clave, titulo). */
    private static final String[][] CATEGORIES = {
        {"concrete", "Concrete"}, {"wool", "Lana"}, {"terracotta", "Terracota"},
        {"stone", "Piedra"}, {"bricks", "Ladrillos, cuarzo y prismarina"}, {"nether_end", "Nether y End"},
        {"sand", "Arenisca, tierra y arcilla"}, {"wood", "Madera"}, {"ore", "Minerales y metales"},
        {"misc", "Otros bloques"}, {"glazed", "Terracota vidriada"},
        {"powder", "Cemento en polvo (cae)"}, {"falling", "Arena y grava (caen)"},
        {"redstone", "Redstone, magma y almas"}, {"function", "Bloques con función"}, {"nature", "Naturaleza"}
    };

    /** Cantidad de bloques del catalogo. Cada bloque tiene un indice fijo (bit en BlockSet). */
    public static final int CATALOG_SIZE = 176;

    /** Catalogo completo. Los indices 0-47 NO deben cambiar de orden (estan guardados en las lamparas): solo se anade al final. */
    private static final List<Entry> CATALOG = new ArrayList<Entry>();
    private static final Map<String, List<Entry>> GROUPS = new LinkedHashMap<String, List<Entry>>();

    private static void add(String group, String block, int meta, String name, String es, int rgb) {
        Entry e = new Entry(group, block, meta, name, es, rgb);
        CATALOG.add(e);
        List<Entry> l = GROUPS.get(group);
        if (l == null) {
            l = new ArrayList<Entry>();
            GROUPS.put(group, l);
        }
        l.add(e);
    }

    static {
        for (int i = 0; i < 16; i++) {
            add("concrete", "concrete", i, DYES[i], DYES_ES[i], CONCRETE[i]);
        }
        for (int i = 0; i < 16; i++) {
            add("wool", "wool", i, DYES[i], DYES_ES[i], WOOL[i]);
        }
        for (int i = 0; i < 16; i++) {
            add("terracotta", "stained_hardened_clay", i, DYES[i], DYES_ES[i], TERRACOTTA[i]);
        }
        // ---- a partir de aqui (indice 48) solo se puede anadir al final ----
        // Piedra
        add("stone", "stone", 0, "stone", "Piedra", 0x7D7D7D);
        add("stone", "stone", 1, "granite", "Granito", 0x9A6B5B);
        add("stone", "stone", 2, "polished_granite", "Granito pulido", 0xA06C5D);
        add("stone", "stone", 3, "diorite", "Diorita", 0xBFBFBF);
        add("stone", "stone", 4, "polished_diorite", "Diorita pulida", 0xC3C3C5);
        add("stone", "stone", 5, "andesite", "Andesita", 0x888888);
        add("stone", "stone", 6, "polished_andesite", "Andesita pulida", 0x868989);
        add("stone", "cobblestone", 0, "cobblestone", "Adoquín", 0x7A7A7A);
        add("stone", "mossy_cobblestone", 0, "mossy_cobblestone", "Adoquín musgoso", 0x667A5A);
        add("stone", "stonebrick", 0, "stone_bricks", "Ladrillos de piedra", 0x7A7A7A);
        add("stone", "stonebrick", 1, "mossy_stone_bricks", "Ladrillos musgosos", 0x727A68);
        add("stone", "stonebrick", 2, "cracked_stone_bricks", "Ladrillos agrietados", 0x767676);
        add("stone", "stonebrick", 3, "chiseled_stone_bricks", "Ladrillos cincelados", 0x787878);
        add("stone", "obsidian", 0, "obsidian", "Obsidiana", 0x0F0B19);
        // Ladrillos, cuarzo y prismarina
        add("bricks", "brick_block", 0, "bricks", "Ladrillos", 0x966454);
        add("bricks", "quartz_block", 0, "quartz", "Cuarzo", 0xECE6E0);
        add("bricks", "quartz_block", 1, "chiseled_quartz", "Cuarzo cincelado", 0xECE7E0);
        add("bricks", "quartz_block", 2, "quartz_pillar", "Pilar de cuarzo", 0xECE6DF);
        add("bricks", "prismarine", 0, "prismarine", "Prismarina", 0x639B8F);
        add("bricks", "prismarine", 1, "prismarine_bricks", "Ladrillos de prismarina", 0x63AB9B);
        add("bricks", "prismarine", 2, "dark_prismarine", "Prismarina oscura", 0x335A48);
        add("bricks", "sea_lantern", 0, "sea_lantern", "Linterna marina", 0xACC8BF);
        // Nether y End
        add("nether_end", "netherrack", 0, "netherrack", "Netherrack", 0x6F3635);
        add("nether_end", "nether_brick", 0, "nether_bricks", "Ladrillos del Nether", 0x2D1618);
        add("nether_end", "red_nether_brick", 0, "red_nether_bricks", "Ladrillos rojos del Nether", 0x450709);
        add("nether_end", "nether_wart_block", 0, "nether_wart_block", "Verrugas del Nether", 0x730B0B);
        add("nether_end", "glowstone", 0, "glowstone", "Piedra luminosa", 0xAB8147);
        add("nether_end", "end_stone", 0, "end_stone", "Piedra del End", 0xDBDE9E);
        add("nether_end", "end_bricks", 0, "end_stone_bricks", "Ladrillos del End", 0xE3E8A8);
        add("nether_end", "purpur_block", 0, "purpur", "Purpur", 0xA97DA9);
        add("nether_end", "purpur_pillar", 0, "purpur_pillar", "Pilar de purpur", 0xAD81AD);
        // Arenisca, tierra y arcilla
        add("sand", "sandstone", 0, "sandstone", "Arenisca", 0xDFD7A5);
        add("sand", "sandstone", 1, "chiseled_sandstone", "Arenisca cincelada", 0xDFD7A5);
        add("sand", "sandstone", 2, "smooth_sandstone", "Arenisca lisa", 0xE0D8A8);
        add("sand", "red_sandstone", 0, "red_sandstone", "Arenisca roja", 0xA95821);
        add("sand", "red_sandstone", 1, "chiseled_red_sandstone", "Arenisca roja cincelada", 0xA95821);
        add("sand", "red_sandstone", 2, "smooth_red_sandstone", "Arenisca roja lisa", 0xA95C24);
        add("sand", "dirt", 0, "dirt", "Tierra", 0x866043);
        add("sand", "dirt", 1, "coarse_dirt", "Tierra infértil", 0x77553B);
        add("sand", "dirt", 2, "podzol", "Podzol", 0x5B4017);
        add("sand", "mycelium", 0, "mycelium", "Micelio", 0x6F6369);
        add("sand", "clay", 0, "clay", "Arcilla", 0x9FA4B1);
        add("sand", "hardened_clay", 0, "terracotta_plain", "Terracota sin teñir", 0x985D43);
        // Madera
        add("wood", "planks", 0, "oak_planks", "Tablones de roble", 0xA0824F);
        add("wood", "planks", 1, "spruce_planks", "Tablones de abeto", 0x6B4F2E);
        add("wood", "planks", 2, "birch_planks", "Tablones de abedul", 0xC3B079);
        add("wood", "planks", 3, "jungle_planks", "Tablones de jungla", 0xA17353);
        add("wood", "planks", 4, "acacia_planks", "Tablones de acacia", 0xA95D33);
        add("wood", "planks", 5, "dark_oak_planks", "Tablones de roble oscuro", 0x3F2A14);
        add("wood", "log", 0, "oak_log", "Tronco de roble", 0x9D8050);
        add("wood", "log", 1, "spruce_log", "Tronco de abeto", 0x745A36);
        add("wood", "log", 2, "birch_log", "Tronco de abedul", 0xC8B77A);
        add("wood", "log", 3, "jungle_log", "Tronco de jungla", 0xA17B4B);
        add("wood", "log2", 0, "acacia_log", "Tronco de acacia", 0xA77544);
        add("wood", "log2", 1, "dark_oak_log", "Tronco de roble oscuro", 0x4A3519);
        // Minerales y metales
        add("ore", "coal_ore", 0, "coal_ore", "Mena de carbón", 0x6F6F6F);
        add("ore", "iron_ore", 0, "iron_ore", "Mena de hierro", 0x88807A);
        add("ore", "gold_ore", 0, "gold_ore", "Mena de oro", 0x8F8C7C);
        add("ore", "diamond_ore", 0, "diamond_ore", "Mena de diamante", 0x818C8F);
        add("ore", "emerald_ore", 0, "emerald_ore", "Mena de esmeralda", 0x7B8A80);
        add("ore", "lapis_ore", 0, "lapis_ore", "Mena de lapislázuli", 0x6B7387);
        add("ore", "quartz_ore", 0, "quartz_ore", "Mena de cuarzo", 0x7A5651);
        add("ore", "coal_block", 0, "coal_block", "Bloque de carbón", 0x101010);
        add("ore", "iron_block", 0, "iron_block", "Bloque de hierro", 0xDCDCDC);
        add("ore", "gold_block", 0, "gold_block", "Bloque de oro", 0xF6D03E);
        add("ore", "diamond_block", 0, "diamond_block", "Bloque de diamante", 0x67E8D9);
        add("ore", "emerald_block", 0, "emerald_block", "Bloque de esmeralda", 0x2BCB58);
        add("ore", "lapis_block", 0, "lapis_block", "Bloque de lapislázuli", 0x1F4B99);
        // Otros
        add("misc", "snow", 0, "snow_block", "Bloque de nieve", 0xF8FEFE);
        add("misc", "sponge", 0, "sponge", "Esponja", 0xC8C34E);
        add("misc", "sponge", 1, "wet_sponge", "Esponja mojada", 0xA9A53D);
        add("misc", "bone_block", 0, "bone_block", "Bloque de hueso", 0xE5E1CF);
        add("misc", "melon_block", 0, "melon", "Melón", 0x7C9B20);
        add("misc", "pumpkin", 0, "pumpkin", "Calabaza", 0xC57611);
        add("misc", "hay_block", 0, "hay_bale", "Bala de heno", 0xA58A12);
        // Terracota vidriada
        for (int i = 0; i < 16; i++) {
            add("glazed", DYES[i] + "_glazed_terracotta", 0, "glazed_" + DYES[i], DYES_ES[i] + " vidriada", GLAZED[i]);
        }
        // Estos se pueden usar pero empiezan DESACTIVADOS (BlockSet.DEFAULT solo activa concrete y lana).
        // Excluidos a peticion del usuario: cristal, hielo, limo, TNT, pistones, bloques de comandos, bedrock y dispensador.
        for (int i = 0; i < 16; i++) {
            add("powder", "concrete_powder", i, "powder_" + DYES[i], DYES_ES[i] + " en polvo", CONCRETE[i]);
        }
        add("falling", "sand", 0, "sand", "Arena", 0xDBCFA3);
        add("falling", "sand", 1, "red_sand", "Arena roja", 0xA95821);
        add("falling", "gravel", 0, "gravel", "Grava", 0x837F7E);
        add("redstone", "redstone_block", 0, "redstone_block", "Bloque de Redstone", 0xA81B0A);
        add("redstone", "redstone_lamp", 0, "redstone_lamp", "Lámpara de Redstone apagada", 0x6E4A2C);
        add("redstone", "lit_redstone_lamp", 0, "lit_redstone_lamp", "Lámpara de Redstone encendida", 0xF0C26E);
        add("redstone", "redstone_ore", 0, "redstone_ore", "Mena de Redstone", 0x8F6F6F);
        add("redstone", "magma", 0, "magma", "Bloque de magma", 0x8A3B0E);
        add("redstone", "soul_sand", 0, "soul_sand", "Arena de almas", 0x544033);
        add("function", "crafting_table", 0, "crafting_table", "Mesa de crafteo", 0x9A6C3F);
        add("function", "furnace", 0, "furnace", "Horno", 0x6F6F6F);
        add("function", "bookshelf", 0, "bookshelf", "Estantería", 0xA0824F);
        add("function", "noteblock", 0, "noteblock", "Bloque musical", 0x69452A);
        add("function", "jukebox", 0, "jukebox", "Tocadiscos", 0x5B3D2A);
        add("function", "dropper", 0, "dropper", "Soltador", 0x707070);
        add("function", "observer", 0, "observer", "Observador", 0x6A6A6A);
        add("nature", "grass", 0, "grass_block", "Bloque de hierba", 0x79C05A);
        add("nature", "red_mushroom_block", 14, "red_mushroom_block", "Champiñón rojo (bloque)", 0xC53A32);
        add("nature", "brown_mushroom_block", 14, "brown_mushroom_block", "Champiñón marrón (bloque)", 0x9A7352);
        add("nature", "red_mushroom_block", 15, "mushroom_stem", "Tallo de champiñón", 0xCBC4B8);
        add("nature", "lit_pumpkin", 0, "jack_o_lantern", "Calabaza iluminada", 0xC57611);
        if (CATALOG.size() != CATALOG_SIZE) {
            throw new IllegalStateException("CATALOG_SIZE=" + CATALOG_SIZE + " pero hay " + CATALOG.size() + " bloques");
        }
    }

    /** Bloque del catalogo por indice. */
    public static Entry catalog(int i) {
        if (i < 0 || i >= CATALOG.size()) {
            throw new IllegalArgumentException("Bloque fuera del catalogo: " + i);
        }
        return CATALOG.get(i);
    }

    public static int categoryCount() {
        return CATEGORIES.length;
    }

    public static String categoryTitle(int c) {
        return CATEGORIES[c][1];
    }

    /** Indices del catalogo de la categoria c, en orden. */
    public static int[] categoryIndices(int c) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < CATALOG.size(); i++) {
            if (CATALOG.get(i).group.equals(CATEGORIES[c][0])) {
                l.add(i);
            }
        }
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = l.get(i);
        }
        return a;
    }

    /** Paleta con solo los bloques activados. */
    public static Palette ofMask(BlockSet set) {
        List<Entry> l = new ArrayList<Entry>();
        for (int i = 0; i < CATALOG_SIZE; i++) {
            if (set.has(i)) {
                l.add(CATALOG.get(i));
            }
        }
        if (l.isEmpty()) {
            throw new IllegalArgumentException("La paleta esta vacia");
        }
        return new Palette(l, "bloques:" + l.size());
    }

    /** Categorias permitidas: todos son bloques solidos completos de vanilla 1.12.2 obtenibles en supervivencia. */
    public static final Set<String> SURVIVAL_GROUPS = new java.util.LinkedHashSet<String>(GROUPS.keySet());

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
