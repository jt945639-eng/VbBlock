
/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

/** Destino donde se colocan los bloques (en el mod es un mundo de Minecraft). */
public interface BlockSink {
    boolean isLoaded(int x, int y, int z);

    /** @param entry bloque a colocar, o null para aire. */
    void setBlock(int x, int y, int z, Palette.Entry entry);
}
