/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 *
 * Prueba del nucleo sin Minecraft: decodifica un video con ffmpeg, lo dibuja en un "mundo" falso
 * y comprueba orientacion, cambio de resolucion y borrado.
 * Uso: java -cp out CoreSmokeTest <video> <carpeta-salida>
 */
import com.videoblocks.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class CoreSmokeTest {
    static final Map<String, Palette.Entry> world = new HashMap<String, Palette.Entry>();
    static int maxPerTick = 0, thisTick = 0;

    static final BlockSink SINK = new BlockSink() {
        public boolean isLoaded(int x, int y, int z) { return true; }
        public void setBlock(int x, int y, int z, Palette.Entry e) {
            thisTick++;
            String k = x + "," + y + "," + z;
            if (e == null) world.remove(k); else world.put(k, e);
        }
    };

    static void check(boolean ok, String msg) {
        java.lang.System.out.println((ok ? "OK   " : "FAIL ") + msg);
        if (!ok) exitCode = 1;
    }

    static int exitCode = 0;

    static void runUntilMessage(ScreenManager m, int budget, long timeoutMs) throws Exception {
        long end = java.lang.System.currentTimeMillis() + timeoutMs;
        while (java.lang.System.currentTimeMillis() < end) {
            thisTick = 0;
            m.tick(new ScreenManager.SinkProvider() { public BlockSink get(int d) { return SINK; } }, budget);
            maxPerTick = Math.max(maxPerTick, thisTick);
            String msg = m.pollMessage();
            if (msg != null) { java.lang.System.out.println("mensaje: " + msg); return; }
            Thread.sleep(3);
        }
        throw new RuntimeException("timeout");
    }

    static void tickFor(ScreenManager m, long ms) throws Exception {
        long end = java.lang.System.currentTimeMillis() + ms;
        while (java.lang.System.currentTimeMillis() < end) {
            m.tick(new ScreenManager.SinkProvider() { public BlockSink get(int d) { return SINK; } }, 5000);
            Thread.sleep(10);
        }
    }

    static void drain(ScreenManager m, int budget) {
        for (int i = 0; i < 100; i++) {
            m.tick(new ScreenManager.SinkProvider() { public BlockSink get(int d) { return SINK; } }, budget);
        }
    }

    public static void main(String[] a) throws Exception {
        String video = a[0];
        File outDir = new File(a[1]);
        outDir.mkdirs();
        Palette pal = Palette.of("concrete,wool");
        check(pal.size() == 32, "paleta concrete+wool = 32 bloques");
        check(pal.get(pal.nearest(0, 0, 0)).name.equals("black"), "negro -> bloque negro");
        check(pal.get(pal.nearest(255, 255, 255)).name.equals("white"), "blanco -> bloque blanco");
        check(pal.get(pal.nearest(240, 120, 20)).name.equals("orange"), "naranja -> bloque naranja");

        // --- pared mirando al este (dx=1): derecha = +z
        ScreenManager m = new ScreenManager();
        Screen s = m.create(0, 100, 64, 200, 1, 0, 128, 72, false, pal);
        check(Math.abs(s.mapper.gain - 1.6667f) < 0.01f, "gain auto pared E/O = 1/0.6");
        s.play("ffmpeg", video, 10, false, 0);
        runUntilMessage(m, 5000, 20000);
        drain(m, 5000);
        int minX = 1 << 30, maxX = -(1 << 30), minZ = 1 << 30, maxZ = -(1 << 30), minY = 1 << 30, maxY = -(1 << 30);
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            int x = Integer.parseInt(p[0]), y = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        check(world.size() == 128 * 72, "pared: " + world.size() + " bloques (esperado " + 128 * 72 + ")");
        check(minX == 100 && maxX == 100, "pared E: plano x constante");
        check(minY == 64 && maxY == 64 + 71, "pared: alto 72 desde y=64");
        check(maxZ - minZ == 127 && minZ == 200 - 64, "pared: ancho 128 centrado en z=200");
        check(maxPerTick <= 5000, "maximo por tick respetado (" + maxPerTick + ")");
        BufferedImage img = new BufferedImage(128, 72, BufferedImage.TYPE_INT_RGB);
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            int y = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
            Palette.Entry e = world.get(k);
            img.setRGB(z - minZ, maxY - y, (e.r << 16) | (e.g << 8) | e.b);
        }
        ImageIO.write(img, "png", new File(outDir, "wall_east.png"));

        // --- cambio de resolucion en caliente
        s.play("ffmpeg", video, 10, true, 0);
        tickFor(m, 1500);
        Screen s2 = m.resize(64, 36);
        check(s2.w == 64 && s2.h == 36, "resize 64x36");
        check(s2.isPlaying(), "el video continua tras el resize");
        tickFor(m, 2500);
        drain(m, 5000);
        check(world.size() == 64 * 36, "tras resize solo quedan " + world.size() + " bloques (esperado " + 64 * 36 + ")");

        // --- quitar
        m.remove();
        drain(m, 5000);
        check(world.isEmpty(), "remove deja el mundo vacio (" + world.size() + ")");

        // --- suelo mirando al norte con dithering
        world.clear();
        Screen f = m.create(0, 0, 63, 0, 0, -1, 96, 54, true, pal);
        f.mapper.dither = true;
        f.play("ffmpeg", video, 20, false, 0);
        runUntilMessage(m, 6000, 20000);
        drain(m, 6000);
        BufferedImage img2 = new BufferedImage(96, 54, BufferedImage.TYPE_INT_RGB);
        int fMinX = 1 << 30, fMinZ = 1 << 30, fMaxZ = -(1 << 30);
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            fMinX = Math.min(fMinX, Integer.parseInt(p[0]));
            fMinZ = Math.min(fMinZ, Integer.parseInt(p[2]));
            fMaxZ = Math.max(fMaxZ, Integer.parseInt(p[2]));
        }
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            int x = Integer.parseInt(p[0]), z = Integer.parseInt(p[2]);
            Palette.Entry e = world.get(k);
            // vista desde arriba mirando al norte: arriba de la imagen = z menor, derecha = +x
            img2.setRGB(x - fMinX, z - fMinZ, (e.r << 16) | (e.g << 8) | e.b);
        }
        ImageIO.write(img2, "png", new File(outDir, "floor_north_dither.png"));
        check(world.size() == 96 * 54, "suelo: " + world.size() + " bloques");
        check(fMaxZ == 0 && fMinZ == -53, "suelo: se extiende hacia el norte (z de 0 a -53)");

        // --- error de ffmpeg (archivo inexistente) se reporta
        m.remove();
        drain(m, 6000);
        Screen e = m.create(0, 0, 63, 0, 0, -1, 16, 9, false, pal);
        e.play("ffmpeg", "/no/existe.mp4", 10, false, 0);
        long end = java.lang.System.currentTimeMillis() + 10000;
        String msg = null;
        while (msg == null && java.lang.System.currentTimeMillis() < end) {
            m.tick(new ScreenManager.SinkProvider() { public BlockSink get(int d) { return SINK; } }, 100);
            msg = m.pollMessage();
            Thread.sleep(5);
        }
        check(msg != null && msg.startsWith("Error de video"), "error ffmpeg reportado: " + msg);
        m.shutdown();
        java.lang.System.exit(exitCode);
    }
}
