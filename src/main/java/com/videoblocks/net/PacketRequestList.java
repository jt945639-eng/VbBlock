/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.net;

import com.videoblocks.ServerLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Cliente -> servidor: "mandame la lista de lamparas" (open = abrir el menu al recibirla). */
public class PacketRequestList implements IMessage {
    public boolean open;

    public PacketRequestList() {
    }

    public PacketRequestList(boolean open) {
        this.open = open;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        open = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(open);
    }

    public static class Handler implements IMessageHandler<PacketRequestList, IMessage> {
        @Override
        public IMessage onMessage(final PacketRequestList m, MessageContext ctx) {
            final EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (!ServerLogic.allowed(p)) {
                        ServerLogic.msg(p, "No tienes permiso para usar las lámparas de video.");
                        return;
                    }
                    ServerLogic.sendList(p, m.open);
                }
            });
            return null;
        }
    }
}
