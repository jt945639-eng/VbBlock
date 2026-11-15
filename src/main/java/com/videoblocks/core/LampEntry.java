/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

/** Una lampara de video colocada en el mundo. */
public final class LampEntry {
    public final String id;
    public final int dim, x, y, z;
    /** Direccion horizontal en la que mira la pantalla (uno de dx/dz es +-1). */
    public final int dx, dz;
    public final String owner;
    public LampConfig cfg;
    /** Estado deseado: true = reproduciendo, false = detenida. */
    public boolean on = true;

    public LampEntry(String id, int dim, int x, int y, int z, int dx, int dz, String owner, LampConfig cfg) {
        this.id = id;
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dz = dz;
        this.owner = owner == null ? "" : owner;
        this.cfg = cfg;
    }
}
