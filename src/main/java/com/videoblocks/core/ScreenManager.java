/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.ArrayList;
import java.util.List;

/** Mantiene la pantalla activa y las que se estan borrando (al quitar o cambiar de resolucion). */
public final class ScreenManager {

    public interface SinkProvider {
        /** Sink de la dimension o null si no esta cargada. */
        BlockSink get(int dim);
    }

    private Screen active;
    private final List<Screen> dying = new ArrayList<Screen>();

    public Screen active() {
        return active;
    }

    public Screen create(int dim, int ax, int ay, int az, int dx, int dz, int w, int h,
                         boolean floor, Palette palette) {
        remove();
        active = Screen.create(dim, ax, ay, az, dx, dz, w, h, floor, palette);
        return active;
    }

    /** Cambia la resolucion: borra la pantalla vieja y dibuja la nueva en el mismo lugar. */
    public Screen resize(int w, int h) {
        if (active == null) {
            return null;
        }
        Screen old = active;
        active = old.resized(w, h);
        old.kill();
        dying.add(old);
        return active;
    }

    public void remove() {
        if (active != null) {
            active.kill();
            dying.add(active);
            active = null;
        }
    }

    /** Mientras se borra una pantalla vieja no se dibuja la nueva (evita que se pisen). */
    public void tick(SinkProvider sinks, int budget) {
        if (!dying.isEmpty()) {
            Screen s = dying.get(0);
            BlockSink sink = sinks.get(s.dim);
            if (sink == null || s.tick(sink, budget)) {
                dying.remove(0);
            }
            return;
        }
        if (active != null) {
            BlockSink sink = sinks.get(active.dim);
            if (sink != null) {
                active.tick(sink, budget);
            }
        }
    }

    /** Mensaje pendiente de la pantalla activa (fin de video, errores de ffmpeg) o null. */
    public String pollMessage() {
        return active != null ? active.takeMessage() : null;
    }

    public void shutdown() {
        if (active != null) {
            active.stop();
        }
        active = null;
        for (Screen s : dying) {
            s.stop();
        }
        dying.clear();
    }
}
