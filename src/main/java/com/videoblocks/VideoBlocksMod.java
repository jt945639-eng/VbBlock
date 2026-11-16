/*
 * Video Blocks - reproduce videos en Minecraft usando un bloque por pixel.
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.BlockSink;
import com.videoblocks.core.LampController;
import com.videoblocks.core.ScreenManager;
import com.videoblocks.net.PacketCreateLamp;
import com.videoblocks.net.PacketLampAction;
import com.videoblocks.net.PacketLampList;
import com.videoblocks.net.PacketRequestList;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = VideoBlocksMod.MODID, name = "Video Blocks", version = "1.0.0",
    acceptedMinecraftVersions = "[1.12.2]")
public class VideoBlocksMod {
    public static final String MODID = "videoblocks";

    /** Lamparas registradas y todas las pantallas (tambien la pantalla manual de /vblocks create). */
    public static final LampController LAMPS = new LampController();
    public static SimpleNetworkWrapper NETWORK;
    public static Item lampItem;

    public static int blocksPerTick = 5000;
    public static boolean requireOp = true;

    @SidedProxy(clientSide = "com.videoblocks.client.ClientProxy", serverSide = "com.videoblocks.CommonProxy")
    public static CommonProxy proxy;

    private static VideoLampData data;
    private final LampWorldAccess access = new LampWorldAccess();
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
        LAMPS.ffmpeg = cfg.getString("ffmpegPath", "general", LAMPS.ffmpeg,
            "Ruta del ejecutable de ffmpeg (o solo 'ffmpeg' si esta en el PATH).");
        LAMPS.maxSide = cfg.getInt("maxSide", "general", LAMPS.maxSide, 16, 2048,
            "Ancho y alto maximos (en bloques) de una pantalla.");
        blocksPerTick = cfg.getInt("blocksPerTick", "general", blocksPerTick, 100, 200000,
            "Maximo de bloques que se cambian por tick entre todas las pantallas. Bajalo si hay lag.");
        LAMPS.paletteSpec = cfg.getString("palette", "general", LAMPS.paletteSpec,
            "Bloques de la pantalla manual de /vblocks create: concrete, wool, terracotta, stone, bricks, nether_end, sand, wood, ore, misc, glazed o all (combinables con comas).");
        requireOp = cfg.getBoolean("requireOp", "general", requireOp,
            "Si es true, solo operadores (y el dueno del mundo en un solo jugador/LAN) usan lamparas y menu.");
        if (cfg.hasChanged()) {
            cfg.save();
        }

        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NETWORK.registerMessage(PacketRequestList.Handler.class, PacketRequestList.class, 0, Side.SERVER);
        NETWORK.registerMessage(PacketCreateLamp.Handler.class, PacketCreateLamp.class, 1, Side.SERVER);
        NETWORK.registerMessage(PacketLampAction.Handler.class, PacketLampAction.class, 2, Side.SERVER);
        NETWORK.registerMessage(PacketLampList.Handler.class, PacketLampList.class, 3, Side.CLIENT);

        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandVideoBlocks());
        LAMPS.clear();
        MapStorage storage = e.getServer().getWorld(0).getMapStorage();
        VideoLampData d = (VideoLampData) storage.getOrLoadData(VideoLampData.class, VideoLampData.NAME);
        if (d == null) {
            d = new VideoLampData(VideoLampData.NAME);
            storage.setData(VideoLampData.NAME, d);
        }
        data = d;
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent e) {
        LAMPS.clear();
        data = null;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        LAMPS.tick(access, sinks, blocksPerTick);
        if (data != null && LAMPS.consumeDirty()) {
            data.markDirty();
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (String msg : LAMPS.drainMessages()) {
            if (server != null) {
                server.getPlayerList().sendMessage(new TextComponentString("[VideoBlocks] " + msg));
            }
        }
    }
}
