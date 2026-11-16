/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.core;

/** Origen de fotogramas ya convertidos a indices de paleta (ffmpeg o GIF). */
public interface FrameSource {
    /** Arranca el hilo que decodifica. */
    void start();

    /** Siguiente fotograma (indices de paleta, w*h) o null si todavia no hay. */
    byte[] poll();

    /** true cuando el video termino y ya no quedan fotogramas pendientes. */
    boolean isFinished();

    /** Mensaje de error listo para mostrar, o null. */
    String error();

    void close();
}
