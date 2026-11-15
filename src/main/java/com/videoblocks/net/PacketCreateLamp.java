/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.net;

import com.videoblocks.ServerLogic;
import com.videoblocks.core.LampConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Cliente -> servidor: "dame una lampara con este video". */
public class PacketCreateLamp implements IMessage {
    public LampConfig cfg = new LampConfig();

    public PacketCreateLamp() {
    }

    public PacketCreateLamp(LampConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        cfg = NetIO.readConfig(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetIO.writeConfig(buf, cfg);
    }

    public static class Handler implements IMessageHandler<PacketCreateLamp, IMessage> {
        @Override
        public IMessage onMessage(final PacketCreateLamp m, MessageContext ctx) {
            final EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (!ServerLogic.allowed(p)) {
                        ServerLogic.msg(p, "No tienes permiso para crear lámparas de video.");
                        return;
                    }
                    ServerLogic.giveLamp(p, m.cfg);
                }
            });
            return null;
        }
    }
}
