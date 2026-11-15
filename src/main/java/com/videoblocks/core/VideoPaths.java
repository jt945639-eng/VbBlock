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
