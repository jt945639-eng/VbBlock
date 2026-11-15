/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 *
 * Prueba del nucleo sin Minecraft (mundo falso): pantallas, lamparas y Redstone.
 * Uso: java -cp out CoreSmokeTest <video> <carpeta-salida>
 */
import com.videoblocks.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class CoreSmokeTest {
    static final Map<String, Palette.Entry> world = new HashMap<String, Palette.Entry>();
    static int maxPerTick = 0, thisTick = 0;
    static int failures = 0;

    static final BlockSink SINK = new BlockSink() {
        public boolean isLoaded(int x, int y, int z) { return true; }
        public void setBlock(int x, int y, int z, Palette.Entry e) {
            thisTick++;
            String k = x + "," + y + "," + z;
            if (e == null) world.remove(k); else world.put(k, e);
        }
    };
    static final ScreenManager.SinkProvider SINKS = new ScreenManager.SinkProvider() {
        public BlockSink get(int d) { return SINK; }
    };

    // mundo falso para las lamparas
    static boolean loaded = true, lampPresent = true, powered = false;
    static final LampController.WorldAccess WA = new LampController.WorldAccess() {
        public boolean isLoaded(int d, int x, int y, int z) { return loaded; }
        public boolean isLamp(int d, int x, int y, int z) { return lampPresent; }
        public boolean isPowered(int d, int x, int y, int z) { return powered; }
    };

    static void check(boolean ok, String msg) {
        java.lang.System.out.println((ok ? "OK   " : "FAIL ") + msg);
        if (!ok) failures++;
    }

    static void tickFor(LampController c, long ms) throws Exception {
        long end = java.lang.System.currentTimeMillis() + ms;
        while (java.lang.System.currentTimeMillis() < end) {
            thisTick = 0;
            c.tick(WA, SINKS, 5000);
            maxPerTick = Math.max(maxPerTick, thisTick);
            Thread.sleep(8);
        }
    }

    static void drain(LampController c) {
        for (int i = 0; i < 100; i++) c.tick(WA, SINKS, 5000);
    }

    static int[] bounds() {
        int[] b = {1 << 30, -(1 << 30), 1 << 30, -(1 << 30), 1 << 30, -(1 << 30)};
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            int x = Integer.parseInt(p[0]), y = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
            b[0] = Math.min(b[0], x); b[1] = Math.max(b[1], x);
            b[2] = Math.min(b[2], y); b[3] = Math.max(b[3], y);
            b[4] = Math.min(b[4], z); b[5] = Math.max(b[5], z);
        }
        return b;
    }

    public static void main(String[] a) throws Exception {
        String video = a[0];
        File outDir = new File(a[1]);
        outDir.mkdirs();
        Palette pal = Palette.of("concrete,wool");
        check(pal.size() == 32, "paleta concrete+wool = 32 bloques");
        check(pal.get(pal.nearest(0, 0, 0)).name.equals("black"), "negro -> bloque negro");
        check(pal.get(pal.nearest(255, 255, 255)).name.equals("white"), "blanco -> bloque blanco");

        // ---------- lamparas ----------
        LampController c = new LampController();
        LampConfig cfg = new LampConfig();
        cfg.src = video; cfg.w = 32; cfg.h = 18; cfg.fps = 10; cfg.loop = true; cfg.title = "";
        LampEntry lamp = c.add(0, 10, 64, 10, 1, 0, "NotXoot", cfg);   // mira al este
        check(lamp.cfg.title.equals("test.mp4"), "titulo automatico = nombre de archivo (" + lamp.cfg.title + ")");
        check(c.consumeDirty() && !c.consumeDirty(), "dirty se consume una vez");

        tickFor(c, 2000);
        drain(c);
        int[] b = bounds();
        check(world.size() == 32 * 18, "colocar lampara genera la pantalla: " + world.size() + " bloques");
        check(b[2] == 64 && b[3] == 64, "la pantalla es plana en la capa y=64 (suelo)");
        check(b[0] == 11 && b[1] == 11 + 17, "empieza justo delante de la lampara (x 11..28)");
        check(b[4] == 10 - 16 && b[5] == 10 + 15, "centrada en z (" + b[4] + ".." + b[5] + ")");
        check(!world.containsKey("10,64,10"), "la lampara misma no se pisa");

        // referencia: primer fotograma (lampara en modo nivel, sin senal, misma posicion)
        Map<String, Palette.Entry> snapshot = new HashMap<String, Palette.Entry>(world);
        world.clear();
        LampController cRef = new LampController();
        LampConfig refCfg = lamp.cfg.copy(); refCfg.mode = LampConfig.MODE_LEVEL;
        cRef.add(0, 10, 64, 10, 1, 0, "ref", refCfg);
        tickFor(cRef, 1200); drain(cRef);
        Map<String, Palette.Entry> firstFrame = new HashMap<String, Palette.Entry>(world);
        check(firstFrame.size() == 32 * 18, "sin senal (modo nivel) la pantalla muestra el primer fotograma: " + firstFrame.size() + " bloques");
        check(cRef.screens.get(cRef.list().iterator().next().id).isPoster() && !cRef.screens.get(cRef.list().iterator().next().id).isPlaying(),
            "el primer fotograma es imagen fija (ffmpeg cerrado)");
        cRef.clear();
        world.clear(); world.putAll(snapshot);

        // pulso: flanco de subida alterna
        powered = true; tickFor(c, 100); drain(c);
        check(!lamp.on, "pulso 1 -> detenida");
        check(world.equals(firstFrame), "al detener vuelve al primer fotograma (no se queda negra ni congelada)");
        check(!c.screens.get(lamp.id).isPlaying(), "detenida: ffmpeg cerrado, imagen fija");
        powered = false; tickFor(c, 800);
        check(!lamp.on, "bajar la senal no cambia el estado en modo pulso");
        int hStop = world.hashCode();
        tickFor(c, 800);
        check(world.hashCode() == hStop, "detenida: la imagen no cambia");
        powered = true; tickFor(c, 100);
        check(lamp.on && c.screens.get(lamp.id).isPlaying() && !c.screens.get(lamp.id).isPoster(), "pulso 2 -> reproduciendo desde el inicio");
        powered = false;
        tickFor(c, 1200);
        check(world.hashCode() != hStop, "al reanudar la imagen vuelve a cambiar");

        // modo congelar: al detener se queda el fotograma actual
        LampConfig fz = lamp.cfg.copy(); fz.stopMode = LampConfig.STOP_FREEZE;
        c.update(lamp.id, fz);
        tickFor(c, 700);
        Map<String, Palette.Entry> beforeStop = null;
        powered = true; tickFor(c, 60); powered = false; tickFor(c, 20);
        check(!lamp.on, "modo congelar: pulso -> detenida");
        drain(c);
        beforeStop = new HashMap<String, Palette.Entry>(world);
        tickFor(c, 600);
        check(world.equals(beforeStop), "modo congelar: la imagen se queda quieta");
        powered = true; tickFor(c, 60); powered = false; tickFor(c, 20);
        check(lamp.on, "modo congelar: pulso -> reanuda");
        LampConfig back = lamp.cfg.copy(); back.stopMode = LampConfig.STOP_POSTER;
        c.update(lamp.id, back);

        // modo nivel
        LampConfig lv = lamp.cfg.copy(); lv.mode = LampConfig.MODE_LEVEL;
        c.update(lamp.id, lv);
        tickFor(c, 100);
        check(!lamp.on, "modo nivel sin senal -> detenida");
        powered = true; tickFor(c, 100);
        check(lamp.on, "modo nivel con senal -> reproduce");
        powered = false; tickFor(c, 100);
        check(!lamp.on, "modo nivel sin senal -> se detiene");
        powered = true; tickFor(c, 100);

        // resolucion en caliente
        LampConfig rs = lamp.cfg.copy(); rs.w = 16; rs.h = 9;
        c.update(lamp.id, rs);
        tickFor(c, 1500);
        drain(c);
        check(world.size() == 16 * 9, "resolucion en caliente 16x9: " + world.size() + " bloques");
        check(c.screens.get(lamp.id).isPlaying(), "el video sigue tras cambiar la resolucion");

        // detenida + cambio de resolucion: la pantalla nueva tambien muestra el primer fotograma
        LampConfig st = lamp.cfg.copy(); st.mode = LampConfig.MODE_PULSE;
        c.update(lamp.id, st);
        powered = false; tickFor(c, 50);
        powered = true; tickFor(c, 60); powered = false; tickFor(c, 20);   // pulso -> detener
        check(!lamp.on, "pulso -> detenida (antes de cambiar resolucion)");
        LampConfig rs2 = lamp.cfg.copy(); rs2.w = 24; rs2.h = 12;
        c.update(lamp.id, rs2);
        tickFor(c, 1200); drain(c);
        check(world.size() == 24 * 12 && c.screens.get(lamp.id).isPoster(),
            "detenida y cambio a 24x12: se dibuja el primer fotograma (" + world.size() + " bloques)");
        LampConfig rs3 = lamp.cfg.copy(); rs3.w = 16; rs3.h = 9;
        c.update(lamp.id, rs3);
        tickFor(c, 1200); drain(c);
        powered = true; tickFor(c, 60); powered = false; tickFor(c, 20);   // pulso -> reanudar
        check(lamp.on, "pulso -> reproduce otra vez");
        tickFor(c, 600);

        // chunk descargado: se suspende pero no se borra
        loaded = false; tickFor(c, 100);
        check(c.screens.get(lamp.id) == null, "chunk descargado: pantalla suspendida (ffmpeg cerrado)");
        check(world.size() == 16 * 9, "chunk descargado: los bloques no se tocan");
        loaded = true; tickFor(c, 1500); drain(c);
        check(c.screens.get(lamp.id) != null && world.size() == 16 * 9, "chunk cargado de nuevo: se recrea la pantalla");

        // lampara rota
        lampPresent = false; tickFor(c, 100); drain(c);
        check(c.get(lamp.id) == null, "lampara rota: se quita del registro");
        check(world.isEmpty(), "lampara rota: se borra la pantalla (" + world.size() + ")");
        check(c.consumeDirty(), "lampara rota: marca cambios para guardar");
        lampPresent = true;

        // video inexistente -> mensaje
        LampConfig bad = new LampConfig(); bad.src = "/no/existe.mp4"; bad.w = 8; bad.h = 8;
        c.add(0, 50, 64, 50, 0, -1, "NotXoot", bad);
        tickFor(c, 300);
        List<String> msgs = c.drainMessages();
        check(!msgs.isEmpty() && msgs.get(0).contains("No existe"), "video inexistente avisa: " + msgs);

        // rutas
        boolean rej = false;
        try { VideoPaths.resolve("ftp://x/y.mp4"); } catch (IllegalArgumentException ex) { rej = true; }
        check(rej, "esquemas distintos de http(s) se rechazan");
        check(VideoPaths.resolve("https://a.b/c.mp4").equals("https://a.b/c.mp4"), "URL https pasa tal cual");
        check(new File(VideoPaths.resolve(video)).isFile(), "ruta absoluta del PC se acepta");

        // orientacion: lampara mirando al norte con dithering
        c.clear();
        world.clear();
        LampController c2 = new LampController();
        LampConfig big = new LampConfig(); big.src = video; big.w = 96; big.h = 54; big.dither = true; big.fps = 20;
        c2.add(0, 0, 64, 0, 0, -1, "x", big);
        loaded = true; lampPresent = true; powered = false;
        tickFor(c2, 2500); drain(c2);
        int[] fb = bounds();
        check(world.size() == 96 * 54, "96x54 con dithering: " + world.size());
        check(fb[5] == -1 && fb[4] == -54, "mirando al norte: z de -1 a -54");
        BufferedImage img = new BufferedImage(96, 54, BufferedImage.TYPE_INT_RGB);
        for (String k : world.keySet()) {
            String[] p = k.split(",");
            Palette.Entry e = world.get(k);
            img.setRGB(Integer.parseInt(p[0]) - fb[0], Integer.parseInt(p[2]) - fb[4], (e.r << 16) | (e.g << 8) | e.b);
        }
        ImageIO.write(img, "png", new File(outDir, "lamp_north.png"));
        check(maxPerTick <= 5000, "presupuesto por tick respetado (" + maxPerTick + ")");
        c2.clear();
        java.lang.System.exit(failures == 0 ? 0 : 1);
    }
}
