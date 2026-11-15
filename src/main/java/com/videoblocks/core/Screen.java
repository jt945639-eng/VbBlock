/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.util.Arrays;

/**
 * Una pantalla de w x h bloques. Guarda lo que hay colocado en el mundo (current) y lo que
 * deberia haber (target); en cada tick aplica como maximo "budget" diferencias.
 */
public final class Screen {
    public static final byte AIR = -1;
    public static final byte UNKNOWN = -2;

    public final String key;
    public final int dim, ax, ay, az, dx, dz, w, h;
    public final boolean floor;
    public final Palette palette;
    public final FrameMapper mapper;

    private final int ox, oy, oz, rx, rz, ux, uy, uz;
    private byte[] current, target;
    private int cursor;
    private boolean dying;

    private FfmpegVideoSource source;
    public String input, ffmpeg;
    public int fps = 10;
    public boolean loop, paused;
    public boolean fit = true;
    public boolean autoGain = true;
    private double acc;
    /** true = solo se muestra el primer fotograma y luego se cierra ffmpeg (imagen fija). */
    private boolean posterOnly;
    private long framesShown;
    private double startOffset;
    private String message;
    /** Bloques colocados en el ultimo tick. */
    public int lastApplied;

    private Screen(String key, int dim, int ax, int ay, int az, int dx, int dz, int w, int h, boolean floor, Palette palette) {
        this.key = key;
        this.dim = dim;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.dx = dx;
        this.dz = dz;
        this.w = w;
        this.h = h;
        this.floor = floor;
        this.palette = palette;
        this.mapper = new FrameMapper(palette);
        // "derecha" vista desde quien mira en la direccion (dx,dz)
        this.rx = -dz;
        this.rz = dx;
        int half = w / 2;
        this.ox = ax - rx * half;
        this.oy = ay;
        this.oz = az - rz * half;
        if (floor) {
            this.ux = dx;
            this.uy = 0;
            this.uz = dz;
        } else {
            this.ux = 0;
            this.uy = 1;
            this.uz = 0;
        }
        this.current = new byte[w * h];
        this.target = new byte[w * h];
        Arrays.fill(current, UNKNOWN);
        Arrays.fill(target, UNKNOWN);
        applyAutoGain();
    }

    /**
     * @param ax,ay,az centro del borde inferior (pared) o del borde cercano (suelo)
     * @param dx,dz    direccion horizontal hacia la que mira el jugador (uno de ellos es +-1, el otro 0)
     */
    public static Screen create(String key, int dim, int ax, int ay, int az, int dx, int dz, int w, int h,
                                boolean floor, Palette palette) {
        return new Screen(key, dim, ax, ay, az, dx, dz, w, h, floor, palette);
    }

    /** Nueva pantalla con otra resolucion, mismo ancla, ajustes y video (continua donde iba). */
    public Screen resized(int nw, int nh) {
        return resized(nw, nh, palette);
    }

    /** Igual, pero ademas con otra paleta de bloques. */
    public Screen resized(int nw, int nh, Palette pal) {
        Screen s = new Screen(key, dim, ax, ay, az, dx, dz, nw, nh, floor, pal);
        s.fps = fps;
        s.loop = loop;
        s.fit = fit;
        s.autoGain = autoGain;
        s.mapper.gain = mapper.gain;
        s.mapper.dither = mapper.dither;
        if (input != null && source != null) {
            s.play(ffmpeg, input, fps, loop, elapsedSeconds());
            s.paused = paused;
        }
        return s;
    }

    /** Sombreado aproximado de la cara visible: arriba 1.0, norte/sur 0.8, este/oeste 0.6. */
    public void applyAutoGain() {
        if (!autoGain) {
            return;
        }
        float shade = floor ? 1.0f : (dx != 0 ? 0.6f : 0.8f);
        mapper.gain = 1.0f / shade;
    }

    public void setGain(float g) {
        autoGain = false;
        mapper.gain = g;
    }

    // ---- reproduccion ----

    public void play(String ffmpegPath, String in, int fps, boolean loop, double startSeconds) {
        closeSource();
        this.ffmpeg = ffmpegPath;
        this.input = in;
        this.fps = fps;
        this.loop = loop;
        this.startOffset = startSeconds;
        this.framesShown = 0;
        this.acc = 0;
        this.paused = false;
        this.posterOnly = false;
        this.message = null;
        source = new FfmpegVideoSource(ffmpegPath, in, w, h, fps, startSeconds, fit, loop, mapper);
        source.start();
    }

    /** Muestra solo el primer fotograma del video como imagen fija (hecha con bloques). */
    public void showPoster(String ffmpegPath, String in, int fps) {
        play(ffmpegPath, in, fps, false, 0);
        posterOnly = true;
        paused = true; // en pausa, pero el primer fotograma si se dibuja
    }

    /** Reinicia ffmpeg en el segundo actual (para aplicar cambios de fps / ajuste de imagen). */
    public void restart() {
        if (input != null && source != null) {
            play(ffmpeg, input, fps, loop, elapsedSeconds());
        }
    }

    public void stop() {
        closeSource();
        input = null;
    }

    /** Cierra ffmpeg sin tocar los bloques ya colocados. */
    public void release() {
        closeSource();
    }

    /** true si la pantalla muestra solo el primer fotograma (sin video en marcha). */
    public boolean isPoster() {
        return posterOnly;
    }

    public boolean isPlaying() {
        return source != null;
    }

    public double elapsedSeconds() {
        return startOffset + framesShown / (double) fps;
    }

    private void closeSource() {
        if (source != null) {
            source.close();
            source = null;
        }
    }

    /** Apaga la pantalla: cierra el video y deja el objetivo en aire para borrar lo dibujado. */
    public void kill() {
        closeSource();
        dying = true;
        Arrays.fill(target, AIR);
        cursor = 0;
    }

    public String takeMessage() {
        String m = message;
        message = null;
        return m;
    }

    // ---- tick ----

    /** @return true si no queda nada pendiente por colocar. */
    public boolean tick(BlockSink sink, int budget) {
        if (!dying && source != null && (!paused || framesShown == 0)) {
            acc = Math.min(acc + fps / 20.0, 2.0);
            if (acc >= 1.0) {
                byte[] f = source.poll();
                if (f != null && f.length == target.length) {
                    acc -= 1.0;
                    target = f;
                    framesShown++;
                    if (posterOnly) {
                        closeSource(); // ya tenemos el primer fotograma: la imagen queda fija sin ffmpeg
                    }
                } else if (f == null && source.isFinished()) {
                    String err = source.error();
                    message = err != null ? "Error de video: " + err : "Reproduccion terminada.";
                    closeSource();
                }
            }
        }
        final int n = w * h;
        int applied = 0;
        boolean pending = false;
        int i = cursor;
        for (int scanned = 0; scanned < n; scanned++) {
            byte t = target[i];
            byte c = current[i];
            if (t != c && !(t == AIR && c == UNKNOWN) && t != UNKNOWN) {
                int px = i % w;
                int py = i / w;
                int up = h - 1 - py;
                int bx = ox + rx * px + ux * up;
                int by = oy + uy * up;
                int bz = oz + rz * px + uz * up;
                if (by < 0 || by > 255) {
                    current[i] = t;
                } else if (!sink.isLoaded(bx, by, bz)) {
                    if (dying) {
                        current[i] = AIR;
                    } else {
                        pending = true;
                    }
                } else {
                    if (applied >= budget) {
                        pending = true;
                        break;
                    }
                    sink.setBlock(bx, by, bz, t == AIR ? null : palette.get(t));
                    current[i] = t;
                    applied++;
                }
            }
            if (++i == n) {
                i = 0;
            }
        }
        cursor = i;
        lastApplied = applied;
        return !pending;
    }

    public String describe() {
        return w + "x" + h + " (" + (w * h) + " bloques), " + (floor ? "suelo" : "pared")
            + ", paleta " + palette.spec() + ", gain " + String.format(java.util.Locale.ROOT, "%.2f", mapper.gain)
            + (autoGain ? " (auto)" : "") + ", dither " + (mapper.dither ? "on" : "off")
            + ", ajuste " + (fit ? "fit" : "stretch")
            + (input != null ? ", video " + input + " @" + fps + "fps" + (loop ? " loop" : "")
                + (paused ? " [pausa]" : "") : ", sin video");
    }
}
