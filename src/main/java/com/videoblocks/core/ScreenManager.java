/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Varias pantallas a la vez, cada una con su clave. Reparte el presupuesto de bloques por tick
 * entre ellas, y mientras se borra una pantalla vieja no se dibuja otra con la misma clave.
 */
public final class ScreenManager {

    public interface SinkProvider {
        /** Sink de la dimension o null si no esta cargada. */
        BlockSink get(int dim);
    }

    public static final class Msg {
        public final String key, text;

        public Msg(String key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private final Map<String, Screen> active = new LinkedHashMap<String, Screen>();
    private final List<Screen> dying = new ArrayList<Screen>();

    public Screen get(String key) {
        return active.get(key);
    }

    public Collection<Screen> all() {
        return new ArrayList<Screen>(active.values());
    }

    public Screen create(String key, int dim, int ax, int ay, int az, int dx, int dz, int w, int h,
                         boolean floor, Palette palette) {
        remove(key);
        Screen s = Screen.create(key, dim, ax, ay, az, dx, dz, w, h, floor, palette);
        active.put(key, s);
        return s;
    }

    /** Cambia la resolucion: borra la pantalla vieja y dibuja la nueva en el mismo lugar. */
    public Screen resize(String key, int w, int h) {
        Screen old = active.get(key);
        if (old == null) {
            return null;
        }
        Screen n = old.resized(w, h);
        active.put(key, n);
        old.kill();
        dying.add(old);
        return n;
    }

    /** Quita la pantalla y borra sus bloques. */
    public void remove(String key) {
        Screen s = active.remove(key);
        if (s != null) {
            s.kill();
            dying.add(s);
        }
    }

    /** Quita la pantalla de memoria (cierra ffmpeg) SIN borrar los bloques del mundo. */
    public void detach(String key) {
        Screen s = active.remove(key);
        if (s != null) {
            s.release();
        }
    }

    public void tick(SinkProvider sinks, int budget) {
        int left = budget;
        for (Iterator<Screen> it = dying.iterator(); it.hasNext() && left > 0; ) {
            Screen s = it.next();
            BlockSink sink = sinks.get(s.dim);
            if (sink == null) {
                it.remove();
                continue;
            }
            boolean done = s.tick(sink, left);
            left -= s.lastApplied;
            if (done) {
                it.remove();
            }
        }
        Set<String> blocked = new HashSet<String>();
        for (Screen d : dying) {
            blocked.add(d.key);
        }
        List<Screen> run = new ArrayList<Screen>();
        for (Screen s : active.values()) {
            if (!blocked.contains(s.key)) {
                run.add(s);
            }
        }
        int r = run.size();
        for (Screen s : run) {
            int share = Math.max(0, left / r);
            r--;
            BlockSink sink = sinks.get(s.dim);
            if (sink == null) {
                continue;
            }
            s.tick(sink, share);
            left -= s.lastApplied;
        }
    }

    /** Mensajes pendientes de las pantallas (fin de video, errores de ffmpeg). */
    public List<Msg> drainMessages() {
        List<Msg> out = new ArrayList<Msg>();
        for (Screen s : active.values()) {
            String m = s.takeMessage();
            if (m != null) {
                out.add(new Msg(s.key, m));
            }
        }
        return out;
    }

    public void shutdown() {
        for (Screen s : active.values()) {
            s.stop();
        }
        active.clear();
        for (Screen s : dying) {
            s.stop();
        }
        dying.clear();
    }
}
