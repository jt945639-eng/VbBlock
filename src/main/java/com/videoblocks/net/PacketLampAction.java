/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.net;

import com.videoblocks.ServerLogic;
import com.videoblocks.core.LampConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Cliente -> servidor: accion sobre una lampara del menu. */
public class PacketLampAction implements IMessage {
    public static final int TOGGLE = 0, DELETE = 1, TELEPORT = 2, UPDATE = 3, GIVE = 4;

    public String id = "";
    public int action;
    public LampConfig cfg;

    public PacketLampAction() {
    }

    public PacketLampAction(String id, int action, LampConfig cfg) {
        this.id = id;
        this.action = action;
        this.cfg = cfg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = ByteBufUtils.readUTF8String(buf);
        action = buf.readInt();
        if (buf.readBoolean()) {
            cfg = NetIO.readConfig(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, id);
        buf.writeInt(action);
        buf.writeBoolean(cfg != null);
        if (cfg != null) {
            NetIO.writeConfig(buf, cfg);
        }
    }

    public static class Handler implements IMessageHandler<PacketLampAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketLampAction m, MessageContext ctx) {
            final EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (!ServerLogic.allowed(p)) {
                        ServerLogic.msg(p, "No tienes permiso para usar las lámparas de video.");
                        return;
                    }
                    ServerLogic.action(p, m.id, m.action, m.cfg);
                }
            });
            return null;
        }
    }
}
