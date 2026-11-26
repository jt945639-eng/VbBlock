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
    private final Map<BlockSet, Palette> palettes = new HashMap<BlockSet, Palette>();

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
            if (!s.isPlaying() || s.isPoster()) {
                startPlay(e, s);
            } else {
                s.paused = false;
            }
        } else if (e.cfg.stopMode == LampConfig.STOP_POSTER) {
            showPoster(e, s);
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
        boolean dims = old.w != nc.w || old.h != nc.h;
        boolean blocks = !old.blocks.equals(nc.blocks) || old.style != nc.style;
        boolean video = !old.sameVideo(nc);
        // Si solo cambian fps/ajuste, el video sigue en el mismo segundo; si cambia el archivo, empieza de cero.
        double at = old.src.equals(nc.src) && s.isPlaying() && !s.isPoster() ? s.elapsedSeconds() : 0;
        if (dims) {
            // la pantalla vieja se borra y la nueva (con la paleta nueva) se dibuja en el mismo sitio
            s = screens.resize(id, nc.w, nc.h, paletteFor(nc));
        } else if (blocks) {
            // mismo tamano: se redibuja encima con la paleta nueva, sin borrar (no parpadea)
            screens.detach(id);
            s = buildScreen(e);
            applyState(e, s, at);
            video = false;
        }
        s.fit = nc.fit;
        s.mapper.dither = nc.dither;
        if (video || (dims && !e.on)) {
            applyState(e, s, at);
        }
    }

    /** Pone la pantalla en el estado que le toca: reproduciendo, o en reposo (primer fotograma / congelada). */
    private void applyState(LampEntry e, Screen s) {
        applyState(e, s, 0);
    }

    private void applyState(LampEntry e, Screen s, double startSeconds) {
        if (e.on) {
            startPlay(e, s, startSeconds);
        } else if (e.cfg.stopMode == LampConfig.STOP_POSTER) {
            showPoster(e, s);
        } else {
            startPlay(e, s, 0);
            s.paused = true;
        }
    }

    private void showPoster(LampEntry e, Screen s) {
        try {
            s.showPoster(ffmpeg, VideoPaths.resolve(e.cfg.src), e.cfg.fps);
        } catch (IllegalArgumentException ex) {
            messages.add(new ScreenManager.Msg(e.id, ex.getMessage()));
        }
    }

    private Palette paletteFor(LampConfig c) {
        return c.style == LampConfig.STYLE_LAMPS ? Palette.lamps() : palette(c.blocks);
    }

    private Palette palette(BlockSet mask) {
        Palette p = palettes.get(mask);
        if (p == null) {
            if (palettes.size() >= 32) {
                palettes.clear(); // cada paleta pesa ~32 KB; no dejar que crezca sin limite
            }
            p = Palette.ofMask(mask);
            palettes.put(mask, p);
        }
        return p;
    }

    private Screen buildScreen(LampEntry e) {
        // La pantalla queda en el suelo, en la misma capa que la lampara, con un bloque de separacion
        // (asi bloques como el de redstone de la pantalla no tocan la lampara ni le dan senal).
        Screen s = screens.create(e.id, e.dim, e.x + 2 * e.dx, e.y, e.z + 2 * e.dz, e.dx, e.dz,
            e.cfg.w, e.cfg.h, true, paletteFor(e.cfg));
        s.fit = e.cfg.fit;
        s.mapper.dither = e.cfg.dither;
        return s;
    }

    private void startPlay(LampEntry e, Screen s) {
        startPlay(e, s, 0);
    }

    private void startPlay(LampEntry e, Screen s, double startSeconds) {
        try {
            s.play(ffmpeg, VideoPaths.resolve(e.cfg.src), e.cfg.fps, e.cfg.loop, startSeconds);
        } catch (IllegalArgumentException ex) {
            messages.add(new ScreenManager.Msg(e.id, ex.getMessage()));
        }
    }

    // ---- tick ----

    /** Milisegundos maximos por tick dedicados a colocar bloques (0 = sin limite). Protege los TPS del servidor. */
    public void setMaxMillisPerTick(int ms) {
        screens.maxNanos = ms <= 0 ? 0 : ms * 1000000L;
    }

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
                applyState(e, s);
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
