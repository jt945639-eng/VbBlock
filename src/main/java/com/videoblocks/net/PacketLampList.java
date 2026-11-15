/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.net;

import com.videoblocks.VideoBlocksMod;
import com.videoblocks.core.LampEntry;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Servidor -> cliente: lista de lamparas guardadas (para el menu). */
public class PacketLampList implements IMessage {
    public List<LampEntry> entries = new ArrayList<LampEntry>();
    public boolean open;

    public PacketLampList() {
    }

    public PacketLampList(Collection<LampEntry> entries, boolean open) {
        this.entries = new ArrayList<LampEntry>(entries);
        this.open = open;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        open = buf.readBoolean();
        int n = buf.readInt();
        entries = new ArrayList<LampEntry>();
        for (int i = 0; i < n; i++) {
            entries.add(NetIO.readEntry(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeInt(entries.size());
        for (LampEntry e : entries) {
            NetIO.writeEntry(buf, e);
        }
    }

    public static class Handler implements IMessageHandler<PacketLampList, IMessage> {
        @Override
        public IMessage onMessage(PacketLampList m, MessageContext ctx) {
            VideoBlocksMod.proxy.showMenu(m.entries, m.open);
            return null;
        }
    }
}
