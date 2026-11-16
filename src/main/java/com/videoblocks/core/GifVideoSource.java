/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Node;

/**
 * Lee un GIF animado (archivo o URL) con el ImageIO que ya trae Java: no necesita ffmpeg, asi que
 * funciona en telefonos (PojavLauncher) y en cualquier equipo. Compone los fotogramas (disposicion y
 * transparencia), los reduce con un filtro de promedio de area a w x h y los convierte a bloques.
 * Los fotogramas se generan al ritmo de "fps" segun el reloj del GIF, asi que el cambio de fps o de
 * resolucion en caliente conserva la velocidad real y la posicion.
 */
public final class GifVideoSource implements FrameSource {
    /** Carpeta donde se guardan los GIF descargados por URL. */
    public static volatile File cacheDir = new File("videos", "cache");
    private static final long MAX_DOWNLOAD = 64L << 20;
    private static final long MAX_CANVAS = 8L << 20; // pixeles de la pantalla logica del GIF

    private final String input;
    private final int w, h, fps;
    private final double startSeconds;
    private final boolean fit, loop;
    private final FrameMapper mapper;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(4);
    private volatile boolean closed, finished;
    private volatile String error;
    private Thread thread;

    public GifVideoSource(String input, int w, int h, int fps, double startSeconds, boolean fit,
                          boolean loop, FrameMapper mapper) {
        this.input = input;
        this.w = w;
        this.h = h;
        this.fps = Math.max(1, fps);
        this.startSeconds = Math.max(0, startSeconds);
        this.fit = fit;
        this.loop = loop;
        this.mapper = mapper;
    }

    @Override
    public void start() {
        thread = new Thread(new Runnable() {
            public void run() {
                decode();
            }
        }, "VideoBlocks-gif");
        thread.setDaemon(true);
        thread.start();
    }

    // ---- utilidades de metadatos ----

    private static Node child(Node parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

    private static int intAttr(Node n, String name, int def) {
        if (n == null || n.getAttributes() == null) {
            return def;
        }
        Node a = n.getAttributes().getNamedItem(name);
        if (a == null) {
            return def;
        }
        try {
            return Integer.parseInt(a.getNodeValue().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String strAttr(Node n, String name) {
        if (n == null || n.getAttributes() == null) {
            return "";
        }
        Node a = n.getAttributes().getNamedItem(name);
        return a == null ? "" : a.getNodeValue();
    }

    // ---- archivo local o descarga ----

    private static String sha1(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b & 255));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private File localFile() throws IOException {
        if (!VideoPaths.isUrl(input)) {
            return new File(input);
        }
        File dir = cacheDir;
        dir.mkdirs();
        File dest = new File(dir, sha1(input) + ".gif");
        if (dest.isFile() && dest.length() > 0) {
            return dest;
        }
        String url = input;
        HttpURLConnection c = null;
        for (int hops = 0; hops < 6; hops++) {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "VideoBlocks/1.0");
            int code = c.getResponseCode();
            if (code / 100 == 3 && c.getHeaderField("Location") != null) {
                url = new URL(new URL(url), c.getHeaderField("Location")).toString();
                c.disconnect();
                continue;
            }
            if (code != 200) {
                c.disconnect();
                throw new IOException("la URL respondio con el codigo " + code);
            }
            break;
        }
        if (c == null || c.getResponseCode() != 200) {
            throw new IOException("demasiadas redirecciones");
        }
        if (c.getContentLengthLong() > MAX_DOWNLOAD) {
            throw new IOException("el archivo es demasiado grande (maximo " + (MAX_DOWNLOAD >> 20) + " MB)");
        }
        File tmp = File.createTempFile("gif", ".part", dir);
        try {
            InputStream in = c.getInputStream();
            OutputStream out = new FileOutputStream(tmp);
            try {
                byte[] buf = new byte[1 << 15];
                long total = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_DOWNLOAD) {
                        throw new IOException("el archivo es demasiado grande (maximo " + (MAX_DOWNLOAD >> 20) + " MB)");
                    }
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
                in.close();
            }
            if (!tmp.renameTo(dest)) {
                throw new IOException("no se pudo guardar el GIF descargado");
            }
        } finally {
            if (tmp.exists()) {
                tmp.delete();
            }
            c.disconnect();
        }
        return dest;
    }

    // ---- decodificacion ----

    private void decode() {
        ImageReader reader = null;
        ImageInputStream iis = null;
        try {
            File f = localFile();
            iis = ImageIO.createImageInputStream(f);
            if (iis == null) {
                throw new IOException("no se pudo abrir el archivo");
            }
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
            if (!it.hasNext()) {
                throw new IOException("este Java no incluye lector de GIF");
            }
            reader = it.next();
            reader.setInput(iis, false, false);
            final int n = reader.getNumImages(true);
            if (n <= 0) {
                throw new IOException("el GIF no tiene fotogramas");
            }

            int sw = 0, sh = 0;
            IIOMetadata sm = reader.getStreamMetadata();
            if (sm != null) {
                Node lsd = child(sm.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                sw = intAttr(lsd, "logicalScreenWidth", 0);
                sh = intAttr(lsd, "logicalScreenHeight", 0);
            }
            final int[] left = new int[n], top = new int[n], disposal = new int[n];
            final double[] start = new double[n + 1];
            for (int i = 0; i < n; i++) {
                IIOMetadata m = reader.getImageMetadata(i);
                Node root = m.getAsTree("javax_imageio_gif_image_1.0");
                Node d = child(root, "ImageDescriptor");
                left[i] = intAttr(d, "imageLeftPosition", 0);
                top[i] = intAttr(d, "imageTopPosition", 0);
                Node g = child(root, "GraphicControlExtension");
                String dm = strAttr(g, "disposalMethod");
                disposal[i] = dm.equals("restoreToBackgroundColor") ? 2 : (dm.equals("restoreToPrevious") ? 3 : 0);
                int cs = intAttr(g, "delayTime", 10);
                if (cs <= 1) {
                    cs = 10; // como los navegadores: retardos de 0-1 centesimas se tratan como 0.1 s
                }
                start[i + 1] = start[i] + cs / 100.0;
                if (sw <= 0 || sh <= 0) {
                    sw = Math.max(sw, left[i] + reader.getWidth(i));
                    sh = Math.max(sh, top[i] + reader.getHeight(i));
                }
            }
            if (sw <= 0 || sh <= 0 || (long) sw * sh > MAX_CANVAS) {
                throw new IOException("el GIF es demasiado grande (" + sw + "x" + sh + ")");
            }
            final double total = start[n];

            final int[] canvas = new int[sw * sh];
            int[] saved = null;

            // geometria de salida (se calcula una vez)
            int dw = w, dh = h, ox = 0, oy = 0;
            if (fit) {
                double sc = Math.min((double) w / sw, (double) h / sh);
                dw = Math.max(1, Math.min(w, (int) Math.round(sw * sc)));
                dh = Math.max(1, Math.min(h, (int) Math.round(sh * sc)));
                ox = (w - dw) / 2;
                oy = (h - dh) / 2;
            }
            final int[] xs = new int[dw + 1], ys = new int[dh + 1];
            for (int i = 0; i <= dw; i++) {
                xs[i] = (int) ((long) i * sw / dw);
            }
            for (int i = 0; i <= dh; i++) {
                ys[i] = (int) ((long) i * sh / dh);
            }

            double clock = startSeconds;
            if (loop) {
                clock = clock % total;
            } else if (clock >= total) {
                return; // empezaba despues del final: no hay nada que mostrar
            }

            while (!closed) {
                Arrays.fill(canvas, 0);
                saved = null;
                int idx = -1;
                while (!closed) {
                    while (idx + 1 < n && start[idx + 1] <= clock + 1e-9) {
                        idx++;
                        // 1) disposicion del fotograma anterior
                        if (idx > 0) {
                            int p = idx - 1;
                            if (disposal[p] == 2) {
                                clearRect(canvas, sw, sh, left[p], top[p], reader.getWidth(p), reader.getHeight(p));
                            } else if (disposal[p] == 3 && saved != null) {
                                System.arraycopy(saved, 0, canvas, 0, canvas.length);
                            }
                        }
                        // 2) si este se descarta al volver al anterior, guardar el estado
                        if (disposal[idx] == 3) {
                            if (saved == null) {
                                saved = new int[canvas.length];
                            }
                            System.arraycopy(canvas, 0, saved, 0, canvas.length);
                        }
                        // 3) dibujar (el GIF solo tiene transparencia total o ninguna)
                        BufferedImage img = reader.read(idx);
                        int fw = img.getWidth(), fh = img.getHeight();
                        int[] px = img.getRGB(0, 0, fw, fh, null, 0, fw);
                        for (int y = 0; y < fh; y++) {
                            int cy = top[idx] + y;
                            if (cy < 0 || cy >= sh) {
                                continue;
                            }
                            for (int x = 0; x < fw; x++) {
                                int cx = left[idx] + x;
                                if (cx < 0 || cx >= sw) {
                                    continue;
                                }
                                int v = px[y * fw + x];
                                if ((v >>> 24) != 0) {
                                    canvas[cy * sw + cx] = v | 0xFF000000;
                                }
                            }
                        }
                    }
                    if (clock >= total - 1e-9) {
                        break; // fin de esta pasada
                    }
                    byte[] out = mapper.map(scale(canvas, sw, xs, ys, dw, dh, ox, oy), w, h);
                    while (!closed && !queue.offer(out, 100, TimeUnit.MILLISECONDS)) {
                        // cola llena: esperar a que el juego consuma
                    }
                    clock += 1.0 / fps;
                }
                if (!loop) {
                    break;
                }
                clock = Math.max(0, clock - total);
            }
        } catch (IOException e) {
            if (!closed) {
                error = "No se pudo leer el GIF: " + e.getMessage();
            }
        } catch (InterruptedException ignored) {
        } catch (RuntimeException e) {
            if (!closed) {
                error = "No se pudo leer el GIF: " + e;
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (iis != null) {
                try {
                    iis.close();
                } catch (IOException ignored) {
                }
            }
            finished = true;
        }
    }

    private static void clearRect(int[] c, int sw, int sh, int x0, int y0, int rw, int rh) {
        for (int y = Math.max(0, y0); y < Math.min(sh, y0 + rh); y++) {
            for (int x = Math.max(0, x0); x < Math.min(sw, x0 + rw); x++) {
                c[y * sw + x] = 0;
            }
        }
    }

    /** Promedio de area del lienzo a dw x dh, centrado en una imagen w x h negra (barras negras del modo "fit"). */
    private byte[] scale(int[] canvas, int sw, int[] xs, int[] ys, int dw, int dh, int ox, int oy) {
        byte[] rgb = new byte[w * h * 3];
        for (int dy = 0; dy < dh; dy++) {
            int y0 = ys[dy], y1 = Math.max(y0 + 1, ys[dy + 1]);
            for (int dx = 0; dx < dw; dx++) {
                int x0 = xs[dx], x1 = Math.max(x0 + 1, xs[dx + 1]);
                long r = 0, g = 0, b = 0;
                for (int y = y0; y < y1; y++) {
                    int row = y * sw;
                    for (int x = x0; x < x1; x++) {
                        int v = canvas[row + x];
                        r += (v >> 16) & 255;
                        g += (v >> 8) & 255;
                        b += v & 255;
                    }
                }
                int cnt = (y1 - y0) * (x1 - x0);
                int o = ((oy + dy) * w + (ox + dx)) * 3;
                rgb[o] = (byte) (r / cnt);
                rgb[o + 1] = (byte) (g / cnt);
                rgb[o + 2] = (byte) (b / cnt);
            }
        }
        return rgb;
    }

    @Override
    public byte[] poll() {
        return queue.poll();
    }

    @Override
    public boolean isFinished() {
        return finished && queue.isEmpty();
    }

    @Override
    public String error() {
        return error;
    }

    @Override
    public void close() {
        closed = true;
        if (thread != null) {
            thread.interrupt();
        }
        queue.clear();
    }
}
