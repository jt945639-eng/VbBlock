/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 *
 * Prueba de la lectura de GIF (sin ffmpeg en el juego): conteo y ritmo de fotogramas, bucle, reanudar,
 * barras negras, orientacion, fidelidad frente a ffmpeg, descarga por URL con cache, errores y la
 * integracion con las lamparas.
 * Uso: java -cp out GifSmokeTest <anim.gif> <split.gif> <ref.rgb 32x18> <carpeta-salida>
 */
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.videoblocks.core.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;

public class GifSmokeTest {
    static int failures = 0;

    static void check(boolean ok, String msg) {
        System.out.println((ok ? "OK   " : "FAIL ") + msg);
        if (!ok) failures++;
    }

    static final Palette PAL = Palette.of("concrete,wool");

    /** Lee todos los fotogramas hasta que termine (o hasta max). */
    static List<byte[]> collect(GifVideoSource s, int max, long timeoutMs) throws Exception {
        List<byte[]> out = new ArrayList<byte[]>();
        s.start();
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end && out.size() < max) {
            byte[] f = s.poll();
            if (f != null) out.add(f);
            else if (s.isFinished()) break;
            else Thread.sleep(2);
        }
        s.close();
        return out;
    }

    static GifVideoSource src(String path, int w, int h, int fps, double start, boolean fit, boolean loop) {
        return new GifVideoSource(path, w, h, fps, start, fit, loop, new FrameMapper(PAL));
    }

    static byte[] readAll(File f) throws IOException {
        return Files.readAllBytes(f.toPath());
    }

    // ---- mundo falso para las lamparas ----
    static final Map<String, Palette.Entry> world = new HashMap<String, Palette.Entry>();
    static final BlockSink SINK = new BlockSink() {
        public boolean isLoaded(int x, int y, int z) { return true; }
        public void setBlock(int x, int y, int z, Palette.Entry e) {
            String k = x + "," + y + "," + z;
            if (e == null) world.remove(k); else world.put(k, e);
        }
    };
    static final ScreenManager.SinkProvider SINKS = new ScreenManager.SinkProvider() {
        public BlockSink get(int d) { return SINK; }
    };
    static boolean powered = false;
    static final LampController.WorldAccess WA = new LampController.WorldAccess() {
        public boolean isLoaded(int d, int x, int y, int z) { return true; }
        public boolean isLamp(int d, int x, int y, int z) { return true; }
        public boolean isPowered(int d, int x, int y, int z) { return powered; }
    };

    static void tickFor(LampController c, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            c.tick(WA, SINKS, 5000);
            Thread.sleep(8);
        }
    }

    static void drain(LampController c) {
        for (int i = 0; i < 100; i++) c.tick(WA, SINKS, 5000);
    }

    public static void main(String[] a) throws Exception {
        final String anim = a[0], split = a[1];
        File ref = new File(a[2]);
        File outDir = new File(a[3]);
        outDir.mkdirs();

        // ---------- conteo de fotogramas (el GIF dura 2 s a 10 fps) ----------
        int n10 = collect(src(anim, 32, 18, 10, 0, true, false), 500, 20000).size();
        check(n10 >= 19 && n10 <= 21, "10 fps -> ~20 fotogramas (" + n10 + ")");
        int n5 = collect(src(anim, 32, 18, 5, 0, true, false), 500, 20000).size();
        check(n5 >= 9 && n5 <= 11, "5 fps -> ~10 fotogramas, misma duracion (" + n5 + ")");
        int n20 = collect(src(anim, 32, 18, 20, 0, true, false), 500, 20000).size();
        check(n20 >= 39 && n20 <= 41, "20 fps -> ~40 fotogramas, misma duracion (" + n20 + ")");
        int nStart = collect(src(anim, 32, 18, 10, 1.0, true, false), 500, 20000).size();
        check(nStart >= 9 && nStart <= 11, "empezar en 1.0 s -> ~10 fotogramas (" + nStart + ")");
        int nPast = collect(src(anim, 32, 18, 10, 5.0, true, false), 500, 20000).size();
        check(nPast == 0, "empezar despues del final sin bucle -> nada (" + nPast + ")");
        List<byte[]> looped = collect(src(anim, 32, 18, 10, 0, true, true), 70, 20000);
        check(looped.size() == 70, "con bucle sigue produciendo mas alla de la duracion (" + looped.size() + ")");
        List<byte[]> wrap = collect(src(anim, 32, 18, 10, 5.0, true, true), 5, 20000);
        check(wrap.size() == 5, "con bucle, empezar en 5.0 s da la vuelta (modulo la duracion)");
        check(Arrays.equals(looped.get(0), looped.get(20)), "el fotograma 21 del bucle es igual al primero");

        // ---------- fidelidad frente a ffmpeg ----------
        List<byte[]> mine = collect(src(anim, 32, 18, 10, 0, true, false), 500, 20000);
        byte[] raw = readAll(ref);
        int fsz = 32 * 18 * 3;
        int refN = raw.length / fsz;
        FrameMapper fm = new FrameMapper(PAL);
        int cmp = Math.min(refN, mine.size());
        long same = 0, tot = 0;
        for (int i = 0; i < cmp; i++) {
            byte[] rgb = Arrays.copyOfRange(raw, i * fsz, (i + 1) * fsz);
            byte[] want = fm.map(rgb, 32, 18);
            for (int p = 0; p < want.length; p++) {
                tot++;
                if (want[p] == mine.get(i)[p]) same++;
            }
        }
        double pct = 100.0 * same / Math.max(1, tot);
        check(refN >= 19 && pct >= 97.0,
            String.format("coincide con la decodificacion de ffmpeg: %.2f%% de bloques iguales en %d fotogramas", pct, cmp));

        // ---------- orientacion y barras negras ----------
        byte[] sp = collect(src(split, 16, 9, 10, 0, false, false), 1, 20000).get(0);
        Palette.Entry l = PAL.get(sp[4 * 16 + 2]), r = PAL.get(sp[4 * 16 + 13]);
        check(l.r > l.b && r.b > r.r, "izquierda roja y derecha azul, sin espejo (" + l + " | " + r + ")");
        byte[] stretch = collect(src(split, 20, 20, 10, 0, false, false), 1, 20000).get(0);
        check(PAL.get(stretch[0 * 20 + 3]).r > PAL.get(stretch[0 * 20 + 3]).b, "estirar: la fila 0 tambien tiene imagen");
        byte[] fit = collect(src(split, 20, 20, 10, 0, true, false), 1, 20000).get(0);
        byte black = PAL.nearest(0, 0, 0);
        boolean barsTop = true, barsBottom = true, middleOk = true;
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 4; y++) barsTop &= fit[y * 20 + x] == black;
            for (int y = 15; y < 20; y++) barsBottom &= fit[y * 20 + x] == black;
            middleOk &= fit[10 * 20 + x] != black;
        }
        check(barsTop && barsBottom && middleOk, "respetar proporcion: barras negras arriba y abajo, imagen en medio");

        // ---------- URL con cache ----------
        File cache = new File(outDir, "cache");
        GifVideoSource.cacheDir = cache;
        final byte[] gifBytes = readAll(new File(anim));
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/a.gif", new HttpHandler() {
            public void handle(HttpExchange ex) throws IOException {
                ex.getResponseHeaders().add("Content-Type", "image/gif");
                ex.sendResponseHeaders(200, gifBytes.length);
                ex.getResponseBody().write(gifBytes);
                ex.close();
            }
        });
        srv.createContext("/go", new HttpHandler() {
            public void handle(HttpExchange ex) throws IOException {
                ex.getResponseHeaders().add("Location", "/a.gif");
                ex.sendResponseHeaders(302, -1);
                ex.close();
            }
        });
        srv.start();
        String base = "http://127.0.0.1:" + srv.getAddress().getPort();
        int nUrl = collect(src(base + "/a.gif", 32, 18, 10, 0, true, false), 500, 20000).size();
        check(nUrl >= 19 && nUrl <= 21, "URL http: se descarga y reproduce (" + nUrl + " fotogramas)");
        File[] cached = cache.listFiles();
        check(cached != null && cached.length == 1 && cached[0].length() == gifBytes.length, "queda guardado en la cache");
        int nRedir = collect(src(base + "/go?x=1.gif", 32, 18, 10, 0, true, false), 500, 20000).size();
        check(nRedir >= 19, "sigue redirecciones (" + nRedir + ")");
        srv.stop(0);
        int nCache = collect(src(base + "/a.gif", 32, 18, 10, 0, true, false), 500, 20000).size();
        check(nCache >= 19, "con el servidor apagado usa la cache (" + nCache + ")");
        GifVideoSource bad = src("http://127.0.0.1:1/nada.gif", 8, 8, 10, 0, true, false);
        collect(bad, 1, 15000);
        check(bad.error() != null && bad.error().contains("GIF"), "URL caida -> error claro: " + bad.error());

        // ---------- archivos que no son GIF ----------
        File fake = new File(outDir, "roto.gif");
        Files.write(fake.toPath(), "esto no es un gif".getBytes("UTF-8"));
        GifVideoSource broken = src(fake.getPath(), 8, 8, 10, 0, true, false);
        collect(broken, 1, 15000);
        check(broken.error() != null && broken.error().startsWith("No se pudo leer el GIF"), "archivo roto -> error: " + broken.error());

        // ---------- deteccion de GIF ----------
        File noext = new File(outDir, "sinextension");
        Files.copy(new File(anim).toPath(), noext.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        check(VideoPaths.isGif("A.GIF") && VideoPaths.isGif("http://x/y.gif?z=1#t") && VideoPaths.isGif(noext.getPath()),
            "isGif: extension, URL con parametros y archivo sin extension (por cabecera)");
        check(!VideoPaths.isGif("http://x/y.mp4") && !VideoPaths.isGif("/no/existe.mp4") && !VideoPaths.isGif(fake.getPath() + ".txt"),
            "isGif: mp4 y archivos que no existen no son GIF");

        // ---------- lamparas con GIF, sin ffmpeg ----------
        LampController c = new LampController();
        c.ffmpeg = "/no/existe/ffmpeg";   // si se intentara usar ffmpeg, fallaria
        LampConfig cfg = new LampConfig();
        cfg.src = anim; cfg.w = 32; cfg.h = 18; cfg.fps = 10; cfg.loop = true;
        LampEntry lamp = c.add(0, 10, 64, 10, 1, 0, "NotXoot", cfg);
        tickFor(c, 1500); drain(c);
        check(world.size() == 32 * 18, "lampara con GIF dibuja la pantalla sin ffmpeg: " + world.size() + " bloques");
        boolean allSurvival = true;
        for (Palette.Entry e : world.values()) allSurvival &= Palette.SURVIVAL_GROUPS.contains(e.group);
        check(allSurvival, "todos los bloques son de supervivencia");
        List<String> msgs = c.drainMessages();
        check(msgs.isEmpty(), "sin mensajes de error: " + msgs);
        check(c.screens.get(lamp.id).isPlaying(), "el GIF esta reproduciendose");

        // primer fotograma de referencia: otra lampara en modo "senal" sin senal
        Map<String, Palette.Entry> playing = new HashMap<String, Palette.Entry>(world);
        world.clear();
        LampController cRef = new LampController();
        cRef.ffmpeg = "/no/existe/ffmpeg";
        LampConfig rc = cfg.copy(); rc.mode = LampConfig.MODE_LEVEL;
        cRef.add(0, 10, 64, 10, 1, 0, "ref", rc);
        tickFor(cRef, 1000); drain(cRef);
        Map<String, Palette.Entry> first = new HashMap<String, Palette.Entry>(world);
        cRef.clear();
        world.clear(); world.putAll(playing);
        powered = false; tickFor(c, 40);
        powered = true; tickFor(c, 80); drain(c);
        check(!lamp.on && world.equals(first), "pulso -> detenida: vuelve al primer fotograma del GIF");
        check(!c.screens.get(lamp.id).isPlaying(), "detenida: no queda ningun hilo leyendo");
        powered = false; tickFor(c, 40);
        powered = true; tickFor(c, 80); powered = false;
        check(lamp.on && c.screens.get(lamp.id).isPlaying(), "otro pulso -> vuelve a reproducir");
        // resolucion y fps en caliente con GIF
        LampConfig hot = lamp.cfg.copy(); hot.w = 20; hot.h = 10; hot.fps = 5;
        c.update(lamp.id, hot);
        tickFor(c, 1500); drain(c);
        check(world.size() == 20 * 10 && c.screens.get(lamp.id).isPlaying(), "resolucion 20x10 y 5 fps en caliente: " + world.size() + " bloques");
        c.clear();

        // ---------- mp4 sin ffmpeg: el mensaje explica que hacer ----------
        world.clear();
        LampController c2 = new LampController();
        c2.ffmpeg = "/no/existe/ffmpeg";
        LampConfig mp4 = new LampConfig();
        File fakeMp4 = new File(outDir, "video.mp4");
        Files.write(fakeMp4.toPath(), new byte[] {0, 0, 0, 24});
        mp4.src = fakeMp4.getPath(); mp4.w = 8; mp4.h = 8;
        c2.add(0, 50, 64, 50, 0, -1, "NotXoot", mp4);
        List<String> m2 = new ArrayList<String>();
        long end = System.currentTimeMillis() + 10000;
        while (m2.isEmpty() && System.currentTimeMillis() < end) {
            c2.tick(WA, SINKS, 100);
            m2.addAll(c2.drainMessages());
            Thread.sleep(10);
        }
        check(!m2.isEmpty() && m2.get(0).contains("ffmpeg") && m2.get(0).contains(".gif"),
            "mp4 sin ffmpeg: el mensaje sugiere usar un .gif");
        c2.clear();
        System.exit(failures == 0 ? 0 : 1);
    }
}
