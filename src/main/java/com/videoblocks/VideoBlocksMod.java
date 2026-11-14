/*
 * Video Blocks - reproduce videos en Minecraft usando un bloque por pixel.
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.BlockSink;
import com.videoblocks.core.ScreenManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

// acceptableRemoteVersions="*": basta con instalarlo en el servidor; clientes vanilla pueden entrar.
@Mod(modid = VideoBlocksMod.MODID, name = "Video Blocks", version = "1.0.0",
    acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*")
public class VideoBlocksMod {
    public static final String MODID = "videoblocks";

    public static final ScreenManager MANAGER = new ScreenManager();

    public static String ffmpegPath = "ffmpeg";
    public static int maxSide = 256;
    public static int blocksPerTick = 5000;
    public static String paletteSpec = "concrete,wool";

    private final ScreenManager.SinkProvider sinks = new ScreenManager.SinkProvider() {
        @Override
        public BlockSink get(int dim) {
            WorldServer w = DimensionManager.getWorld(dim);
            return w == null ? null : new WorldSink(w);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration cfg = new Configuration(e.getSuggestedConfigurationFile());
        ffmpegPath = cfg.getString("ffmpegPath", "general", ffmpegPath,
            "Ruta del ejecutable de ffmpeg (o solo 'ffmpeg' si esta en el PATH).");
        maxSide = cfg.getInt("maxSide", "general", maxSide, 16, 2048,
            "Ancho y alto maximos (en bloques) de una pantalla.");
        blocksPerTick = cfg.getInt("blocksPerTick", "general", blocksPerTick, 100, 200000,
            "Maximo de bloques que se cambian por tick. Bajalo si hay lag, subelo si el video se ve atrasado.");
        paletteSpec = cfg.getString("palette", "general", paletteSpec,
            "Bloques usados como pixeles: concrete, wool, terracotta o all (se pueden combinar con comas).");
        if (cfg.hasChanged()) {
            cfg.save();
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandVideoBlocks());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent e) {
        MANAGER.shutdown();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MANAGER.tick(sinks, blocksPerTick);
        String msg = MANAGER.pollMessage();
        if (msg != null) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                server.getPlayerList().sendMessage(new TextComponentString("[VideoBlocks] " + msg));
            }
        }
    }
}
