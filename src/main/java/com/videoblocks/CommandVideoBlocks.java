/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampConfig;
import com.videoblocks.core.Palette;
import com.videoblocks.core.Screen;
import com.videoblocks.core.ScreenManager;
import com.videoblocks.core.VideoPaths;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;

/**
 * Comandos. Lo normal es usar el menu (tecla V) y las lamparas; los comandos "create/play/..."
 * manejan una pantalla manual suelta (clave "manual") y sirven para probar rapido.
 */
public class CommandVideoBlocks extends CommandBase {
    private static final String KEY = "manual";
    private static final List<String> SUBS = Arrays.asList(
        "menu", "lamp", "create", "resolution", "play", "pause", "resume", "stop", "fps", "fit",
        "dither", "gain", "remove", "info");

    @Override
    public String getName() {
        return "vblocks";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("videoblocks");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vblocks <menu|lamp|create|resolution|play|pause|resume|stop|fps|fit|dither|gain|remove|info>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static void reply(ICommandSender s, String msg) {
        s.sendMessage(new TextComponentString("[VideoBlocks] " + msg));
    }

    private static CommandException fail(String msg) {
        return new CommandException("%s", msg);
    }

    private static ScreenManager screens() {
        return VideoBlocksMod.LAMPS.screens;
    }

    private static Screen need() throws CommandException {
        Screen s = screens().get(KEY);
        if (s == null) {
            throw fail("No hay pantalla manual. Crea una con /vblocks create <ancho> <alto> [floor|wall]");
        }
        return s;
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean parseOnOff(String s) throws CommandException {
        if (s.equalsIgnoreCase("on")) {
            return true;
        }
        if (s.equalsIgnoreCase("off")) {
            return false;
        }
        throw new WrongUsageException("Usa on u off");
    }

    private static String resolveInput(String name) throws CommandException {
        try {
            return VideoPaths.resolve(name);
        } catch (IllegalArgumentException ex) {
            throw fail(ex.getMessage());
        }
    }

    private static String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        ScreenManager m = screens();
        String sub = args[0].toLowerCase();
        int max = VideoBlocksMod.LAMPS.maxSide;

        switch (sub) {
            case "menu": {
                ServerLogic.sendList(getCommandSenderAsPlayer(sender), true);
                break;
            }
            case "lamp": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks lamp <archivo|url>  (luego ajusta resolución y más en el menú)");
                }
                EntityPlayerMP p = getCommandSenderAsPlayer(sender);
                LampConfig cfg = new LampConfig();
                cfg.src = joinFrom(args, 1);
                ServerLogic.giveLamp(p, cfg);
                break;
            }
            case "create": {
                if (args.length < 3) {
                    throw new WrongUsageException("/vblocks create <ancho> <alto> [floor|wall]");
                }
                int w = parseInt(args[1], 1, max);
                int h = parseInt(args[2], 1, max);
                // Por defecto la pantalla va acostada en el suelo; "wall" (vertical) es opcional.
                boolean floor = true;
                if (args.length > 3) {
                    if (args[3].equalsIgnoreCase("wall")) {
                        floor = false;
                    } else if (!args[3].equalsIgnoreCase("floor")) {
                        throw new WrongUsageException("/vblocks create <ancho> <alto> [floor|wall]");
                    }
                }
                EntityPlayerMP p = getCommandSenderAsPlayer(sender);
                Vec3i d = p.getHorizontalFacing().getDirectionVec();
                BlockPos pos = p.getPosition();
                int dist = floor ? 2 : 3;
                int ax = pos.getX() + d.getX() * dist;
                int az = pos.getZ() + d.getZ() * dist;
                int ay = floor ? pos.getY() - 1 : pos.getY();
                Palette pal;
                try {
                    pal = Palette.of(VideoBlocksMod.LAMPS.paletteSpec);
                } catch (IllegalArgumentException ex) {
                    throw fail(ex.getMessage());
                }
                Screen s = m.create(KEY, p.dimension, ax, ay, az, d.getX(), d.getZ(), w, h, floor, pal);
                reply(sender, "Pantalla creada: " + s.describe());
                reply(sender, "Ahora usa /vblocks play <archivo> [fps] [loop]");
                break;
            }
            case "resolution": {
                if (args.length < 3) {
                    throw new WrongUsageException("/vblocks resolution <ancho> <alto>");
                }
                need();
                Screen s = m.resize(KEY, parseInt(args[1], 1, max), parseInt(args[2], 1, max));
                reply(sender, "Resolución cambiada: " + s.describe());
                break;
            }
            case "play": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks play <archivo|url> [fps 1-20] [loop]");
                }
                Screen s = need();
                int i = 1;
                StringBuilder name = new StringBuilder();
                for (; i < args.length; i++) {
                    if (isInt(args[i]) || args[i].equalsIgnoreCase("loop")) {
                        break;
                    }
                    if (name.length() > 0) {
                        name.append(' ');
                    }
                    name.append(args[i]);
                }
                if (name.length() == 0) {
                    throw new WrongUsageException("/vblocks play <archivo|url> [fps 1-20] [loop]");
                }
                int fps = s.fps;
                boolean loop = false;
                for (; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("loop")) {
                        loop = true;
                    } else {
                        fps = parseInt(args[i], 1, 20);
                    }
                }
                s.play(VideoBlocksMod.LAMPS.ffmpeg, resolveInput(name.toString()), fps, loop, 0);
                reply(sender, "Reproduciendo: " + s.describe());
                break;
            }
            case "pause":
                need().paused = true;
                reply(sender, "En pausa.");
                break;
            case "resume":
                need().paused = false;
                reply(sender, "Continuando.");
                break;
            case "stop":
                need().stop();
                reply(sender, "Video detenido (la imagen se queda).");
                break;
            case "fps": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks fps <1-20>");
                }
                Screen s = need();
                int n = parseInt(args[1], 1, 20);
                if (s.input != null) {
                    s.play(s.ffmpeg, s.input, n, s.loop, s.elapsedSeconds());
                } else {
                    s.fps = n;
                }
                reply(sender, "FPS: " + n);
                break;
            }
            case "fit": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks fit <on|off>  (on = barras negras, off = estirar)");
                }
                Screen s = need();
                s.fit = parseOnOff(args[1]);
                s.restart();
                reply(sender, "Ajuste: " + (s.fit ? "fit (respeta proporción)" : "stretch (estira)"));
                break;
            }
            case "dither": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks dither <on|off>");
                }
                Screen s = need();
                s.mapper.dither = parseOnOff(args[1]);
                reply(sender, "Dither: " + (s.mapper.dither ? "on" : "off"));
                break;
            }
            case "gain": {
                if (args.length < 2) {
                    throw new WrongUsageException("/vblocks gain <0.25-4|auto>");
                }
                Screen s = need();
                if (args[1].equalsIgnoreCase("auto")) {
                    s.autoGain = true;
                    s.applyAutoGain();
                } else {
                    s.setGain((float) parseDouble(args[1], 0.25, 4.0));
                }
                reply(sender, "Brillo (gain): " + s.mapper.gain);
                break;
            }
            case "remove":
                need();
                m.remove(KEY);
                reply(sender, "Pantalla manual eliminada (se borran sus bloques).");
                break;
            case "info":
                reply(sender, need().describe() + " | máx " + max + "x" + max
                    + ", " + VideoBlocksMod.blocksPerTick + " bloques/tick");
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBS);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 4 && sub.equals("create")) {
            return getListOfStringsMatchingLastWord(args, "floor", "wall");
        }
        if (args.length == 2 && (sub.equals("fit") || sub.equals("dither"))) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        if (args.length == 2 && sub.equals("gain")) {
            return getListOfStringsMatchingLastWord(args, "auto");
        }
        if (args.length == 2 && (sub.equals("play") || sub.equals("lamp"))) {
            List<String> names = new ArrayList<String>();
            File[] files = new File("videos").listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        names.add(f.getName());
                    }
                }
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        return Collections.emptyList();
    }
}
