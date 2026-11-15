/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampEntry;
import java.util.List;

/** Lado servidor / comun. ClientProxy hereda de aqui y agrega tecla y menu. */
public class CommonProxy {
    public void preInit() {
    }

    /** Llega la lista de lamparas desde el servidor; el cliente abre o refresca el menu. */
    public void showMenu(List<LampEntry> entries, boolean open) {
    }
}
