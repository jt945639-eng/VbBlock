/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampConfig;
import com.videoblocks.core.LampEntry;
import com.videoblocks.core.VideoPaths;
import com.videoblocks.net.PacketLampAction;
import com.videoblocks.net.PacketLampList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/** Acciones del menu y de los comandos, ejecutadas en el hilo del servidor. */
public final class ServerLogic {
    private ServerLogic() {
    }

    public static void msg(EntityPlayerMP p, String text) {
        p.sendMessage(new TextComponentString("[VideoBlocks] " + text));
    }

    /** Operador (nivel 2), o el dueno del mundo en un solo jugador / LAN. Configurable con requireOp. */
    public static boolean allowed(EntityPlayerMP p) {
        if (!VideoBlocksMod.requireOp || p.canUseCommand(2, "vblocks")) {
            return true;
        }
        MinecraftServer s = p.getServer();
        return s != null && !s.isDedicatedServer() && s.getServerOwner() != null
            && s.getServerOwner().equalsIgnoreCase(p.getName());
    }

    public static void sendList(EntityPlayerMP p, boolean open) {
        VideoBlocksMod.NETWORK.sendTo(new PacketLampList(VideoBlocksMod.LAMPS.list(), open), p);
    }

    /** Entrega al jugador una lampara con el video marcado. */
    public static void giveLamp(EntityPlayerMP p, LampConfig cfg) {
        cfg = cfg.copy().sanitize(VideoBlocksMod.LAMPS.maxSide);
        try {
            VideoPaths.resolve(cfg.src);
        } catch (IllegalArgumentException ex) {
            msg(p, ex.getMessage());
            return;
        }
        ItemStack st = new ItemStack(VideoBlocksMod.lampItem);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(ItemVideoLamp.TAG, LampNBT.writeConfig(cfg));
        st.setTagCompound(tag);
        if (!p.inventory.addItemStackToInventory(st)) {
            p.dropItem(st, false);
        }
        msg(p, "Lámpara '" + cfg.title + "' lista. Colócala en el suelo mirando hacia donde quieres la pantalla.");
    }

    public static void action(EntityPlayerMP p, String id, int action, LampConfig cfg) {
        LampEntry e = VideoBlocksMod.LAMPS.get(id);
        if (e == null) {
            msg(p, "Esa lámpara ya no existe.");
            sendList(p, false);
            return;
        }
        switch (action) {
            case PacketLampAction.TOGGLE:
                VideoBlocksMod.LAMPS.setOn(id, !e.on);
                break;
            case PacketLampAction.DELETE:
                VideoBlocksMod.LAMPS.remove(id);
                msg(p, "Lámpara '" + e.cfg.title + "' olvidada y pantalla borrada (el bloque de lámpara se queda).");
                break;
            case PacketLampAction.TELEPORT:
                if (p.dimension == e.dim) {
                    p.setPositionAndUpdate(e.x + 0.5, e.y + 1, e.z + 0.5);
                } else {
                    msg(p, "Esa lámpara está en otra dimensión (" + e.dim + ").");
                }
                break;
            case PacketLampAction.UPDATE:
                if (cfg != null) {
                    cfg.sanitize(VideoBlocksMod.LAMPS.maxSide);
                    try {
                        VideoPaths.resolve(cfg.src);
                        VideoBlocksMod.LAMPS.update(id, cfg);
                    } catch (IllegalArgumentException ex) {
                        msg(p, ex.getMessage());
                    }
                }
                break;
            case PacketLampAction.GIVE:
                giveLamp(p, e.cfg);
                break;
            default:
                break;
        }
        sendList(p, false);
    }
}
