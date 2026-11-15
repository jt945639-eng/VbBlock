/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Decodifica un video llamando a ffmpeg (que debe estar instalado) y entrega los frames ya
 * convertidos a indices de paleta. Corre en un hilo aparte; si el juego no consume frames,
 * la cola se llena y ffmpeg se detiene solo (no hace falta limitar la velocidad).
 */
public final class FfmpegVideoSource {
    private final List<String> cmd = new ArrayList<String>();
    private final int w, h;
    private final FrameMapper mapper;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(4);
    private volatile boolean closed;
    private volatile boolean finished;
    private volatile String error;
    private volatile Process process;
    private Thread thread;

    public FfmpegVideoSource(String ffmpeg, String input, int w, int h, int fps,
                             double startSeconds, boolean fit, boolean loop, FrameMapper mapper) {
        this.w = w;
        this.h = h;
        this.mapper = mapper;
        cmd.add(ffmpeg);
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("error");
        // Solo archivos locales y http(s): evita que ffmpeg abra otros protocolos.
        cmd.add("-protocol_whitelist");
        cmd.add("file,http,https,tcp,tls,crypto");
        if (startSeconds > 0.01) {
            cmd.add("-ss");
            cmd.add(String.format(java.util.Locale.ROOT, "%.3f", startSeconds));
        }
        if (loop) {
            cmd.add("-stream_loop");
            cmd.add("-1");
        }
        cmd.add("-i");
        cmd.add(input);
        cmd.add("-an");
        cmd.add("-sn");
        cmd.add("-vf");
        if (fit) {
            cmd.add("fps=" + fps + ",scale=" + w + ":" + h
                + ":force_original_aspect_ratio=decrease:flags=area,pad=" + w + ":" + h
                + ":(ow-iw)/2:(oh-ih)/2:black");
        } else {
            cmd.add("fps=" + fps + ",scale=" + w + ":" + h + ":flags=area");
        }
        cmd.add("-f");
        cmd.add("rawvideo");
        cmd.add("-pix_fmt");
        cmd.add("rgb24");
        cmd.add("-");
    }

    public void start() {
        thread = new Thread(new Runnable() {
            public void run() {
                decode();
            }
        }, "VideoBlocks-decoder");
        thread.setDaemon(true);
        thread.start();
    }

    private void decode() {
        Process p = null;
        final StringBuilder err = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            p = pb.start();
            process = p;
            final InputStream es = p.getErrorStream();
            Thread et = new Thread(new Runnable() {
                public void run() {
                    try {
                        byte[] b = new byte[1024];
                        int n;
                        while ((n = es.read(b)) > 0) {
                            if (err.length() < 2000) {
                                err.append(new String(b, 0, n));
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }, "VideoBlocks-ffmpeg-stderr");
            et.setDaemon(true);
            et.start();

            DataInputStream in = new DataInputStream(new BufferedInputStream(p.getInputStream(), 1 << 16));
            byte[] buf = new byte[w * h * 3];
            boolean any = false;
            while (!closed) {
                try {
                    in.readFully(buf);
                } catch (EOFException eof) {
                    break;
                }
                any = true;
                byte[] idx = mapper.map(buf, w, h);
                while (!closed && !queue.offer(idx, 100, TimeUnit.MILLISECONDS)) {
                    // cola llena: esperar a que el juego consuma
                }
            }
            if (!any && !closed) {
                String m = err.toString().trim();
                error = m.isEmpty() ? "ffmpeg no produjo ningun frame" : "ffmpeg: " + m;
            }
        } catch (IOException e) {
            if (!closed) {
                error = "No se pudo ejecutar ffmpeg (" + cmd.get(0) + "): " + e.getMessage();
            }
        } catch (InterruptedException ignored) {
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
            finished = true;
        }
    }

    /** Siguiente frame (indices de paleta, w*h) o null si todavia no hay. */
    public byte[] poll() {
        return queue.poll();
    }

    /** true cuando el video termino y ya no quedan frames pendientes. */
    public boolean isFinished() {
        return finished && queue.isEmpty();
    }

    public String error() {
        return error;
    }

    public void close() {
        closed = true;
        Process p = process;
        if (p != null) {
            p.destroyForcibly();
        }
        if (thread != null) {
            thread.interrupt();
        }
        queue.clear();
    }
}
