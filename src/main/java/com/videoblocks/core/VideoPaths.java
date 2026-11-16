/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

import java.io.File;

/** Resuelve de donde se lee el video: URL http(s), ruta absoluta del PC o carpeta "videos". */
public final class VideoPaths {
    private VideoPaths() {
    }

    public static boolean isUrl(String s) {
        String l = s.toLowerCase();
        return l.startsWith("http://") || l.startsWith("https://");
    }

    /** true si el origen es un GIF (por la extension o, en archivos locales, por su cabecera "GIF"). */
    public static boolean isGif(String src) {
        if (src == null) {
            return false;
        }
        String s = src.trim().toLowerCase();
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        if (s.endsWith(".gif")) {
            return true;
        }
        if (isUrl(src)) {
            return false;
        }
        File f = new File(src.trim());
        if (!f.isFile()) {
            return false;
        }
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] h = new byte[3];
            return in.read(h) == 3 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F';
        } catch (java.io.IOException e) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }

    /** @throws IllegalArgumentException con un mensaje listo para mostrar al jugador. */
    public static String resolve(String src) {
        if (src == null || src.trim().isEmpty()) {
            throw new IllegalArgumentException("No se indico ningun video.");
        }
        src = src.trim();
        if (isUrl(src)) {
            return src;
        }
        if (src.contains("://")) {
            throw new IllegalArgumentException("Solo se admiten URL http:// o https://");
        }
        File f = new File(src);
        if (!f.isAbsolute() || !f.isFile()) {
            f = new File("videos", src);
        }
        if (!f.isFile()) {
            throw new IllegalArgumentException("No existe el archivo '" + src
                + "' en el equipo donde corre el juego/servidor (usa una URL o la carpeta 'videos').");
        }
        return f.getAbsolutePath();
    }
}
