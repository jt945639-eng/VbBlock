/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Logica de las lamparas de video: crea la pantalla al colocar la lampara, reacciona a la Redstone
 * (pulso que alterna, o nivel mientras haya senal), limpia al romperse la lampara y suspende las
 * pantallas cuyo chunk no esta cargado. No depende de Minecraft (ver {@link WorldAccess}).
 */
public final class LampController {

    public interface WorldAccess {
        boolean isLoaded(int dim, int x, int y, int z);

        boolean isLamp(int dim, int x, int y, int z);

        boolean isPowered(int dim, int x, int y, int z);
    }

    public final ScreenManager screens = new ScreenManager();
    public String ffmpeg = "ffmpeg";
    public String paletteSpec = "concrete,wool";
    public int maxSide = 256;

    private final Map<String, LampEntry> lamps = new LinkedHashMap<String, LampEntry>();
    private final Map<String, Boolean> lastPowered = new HashMap<String, Boolean>();
    private final List<ScreenManager.Msg> messages = new ArrayList<ScreenManager.Msg>();
    private boolean dirty;
    private Palette palette;
    private String paletteBuilt;

    // ---- registro ----

    public Collection<LampEntry> list() {
        return new ArrayList<LampEntry>(lamps.values());
    }

    public LampEntry get(String id) {
        return lamps.get(id);
    }

    public LampEntry add(int dim, int x, int y, int z, int dx, int dz, String owner, LampConfig cfg) {
        cfg = cfg.copy().sanitize(maxSide);
        LampEntry e = new LampEntry(UUID.randomUUID().toString(), dim, x, y, z, dx, dz, owner, cfg);
        lamps.put(e.id, e);
        dirty = true;
        return e;
    }

    /** Carga lamparas guardadas (sustituye lo que hubiera). */
    public void load(Collection<LampEntry> entries) {
        clear();
        for (LampEntry e : entries) {
            lamps.put(e.id, e);
        }
    }

    public void clear() {
        screens.shutdown();
        lamps.clear();
        lastPowered.clear();
        messages.clear();
    }

    public boolean remove(String id) {
        LampEntry e = lamps.remove(id);
        if (e == null) {
            return false;
        }
        lastPowered.remove(id);
        screens.remove(id);
        dirty = true;
        return true;
    }

    /** true una vez si hubo cambios que guardar. */
    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    // ---- control ----

    public void setOn(String id, boolean on) {
        LampEntry e = lamps.get(id);
        if (e == null) {
            return;
        }
        e.on = on;
        dirty = true;
        Screen s = screens.get(id);
        if (s == null) {
            return;
        }
        if (on) {
            if (!s.isPlaying()) {
                startPlay(e, s);
            } else {
                s.paused = false;
            }
        } else {
            s.paused = true;
        }
    }

    /** Aplica nuevos ajustes (resolucion en caliente, video, modo...). */
    public void update(String id, LampConfig nc) {
        LampEntry e = lamps.get(id);
        if (e == null) {
            return;
        }
        nc = nc.copy().sanitize(maxSide);
        LampConfig old = e.cfg;
        e.cfg = nc;
        dirty = true;
        if (old.mode != nc.mode) {
            lastPowered.remove(id); // en el proximo tick se sincroniza con la senal actual
        }
        Screen s = screens.get(id);
        if (s == null) {
            return;
        }
        if (old.w != nc.w || old.h != nc.h) {
            s = screens.resize(id, nc.w, nc.h);
        }
        s.fit = nc.fit;
        s.mapper.dither = nc.dither;
        if (!old.sameVideo(nc)) {
            startPlay(e, s);
            s.paused = !e.on;
        }
    }

    private Palette palette() {
        if (palette == null || !paletteSpec.equals(paletteBuilt)) {
            palette = Palette.of(paletteSpec);
            paletteBuilt = paletteSpec;
        }
        return palette;
    }

    private Screen buildScreen(LampEntry e) {
        // La pantalla queda en el suelo, en la misma capa que la lampara, empezando justo delante de ella.
        Screen s = screens.create(e.id, e.dim, e.x + e.dx, e.y, e.z + e.dz, e.dx, e.dz,
            e.cfg.w, e.cfg.h, true, palette());
        s.fit = e.cfg.fit;
        s.mapper.dither = e.cfg.dither;
        return s;
    }

    private void startPlay(LampEntry e, Screen s) {
        try {
            s.play(ffmpeg, VideoPaths.resolve(e.cfg.src), e.cfg.fps, e.cfg.loop, 0);
        } catch (IllegalArgumentException ex) {
            messages.add(new ScreenManager.Msg(e.id, ex.getMessage()));
        }
    }

    // ---- tick ----

    public void tick(WorldAccess wa, ScreenManager.SinkProvider sinks, int budget) {
        List<String> dead = null;
        for (LampEntry e : lamps.values()) {
            if (!wa.isLoaded(e.dim, e.x, e.y, e.z)) {
                if (screens.get(e.id) != null) {
                    screens.detach(e.id);
                }
                lastPowered.remove(e.id);
                continue;
            }
            if (!wa.isLamp(e.dim, e.x, e.y, e.z)) {
                if (dead == null) {
                    dead = new ArrayList<String>();
                }
                dead.add(e.id);
                continue;
            }
            Screen s = screens.get(e.id);
            if (s == null) {
                s = buildScreen(e);
                startPlay(e, s);
                s.paused = !e.on;
            }
            boolean p = wa.isPowered(e.dim, e.x, e.y, e.z);
            Boolean last = lastPowered.put(e.id, p);
            if (e.cfg.mode == LampConfig.MODE_LEVEL) {
                if (last == null || last.booleanValue() != p) {
                    setOn(e.id, p);
                }
            } else if (last != null && !last.booleanValue() && p) {
                setOn(e.id, !e.on);
            }
        }
        if (dead != null) {
            for (String id : dead) {
                remove(id);
            }
        }
        screens.tick(sinks, budget);
    }

    /** Mensajes para el chat (errores de video, fin de reproduccion). */
    public List<String> drainMessages() {
        List<ScreenManager.Msg> all = new ArrayList<ScreenManager.Msg>(messages);
        messages.clear();
        all.addAll(screens.drainMessages());
        List<String> out = new ArrayList<String>();
        for (ScreenManager.Msg m : all) {
            LampEntry e = lamps.get(m.key);
            out.add(e != null ? "Lampara '" + e.cfg.title + "': " + m.text : m.text);
        }
        return out;
    }
}
