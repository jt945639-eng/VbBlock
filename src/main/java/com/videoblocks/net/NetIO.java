/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.net;

import com.videoblocks.core.BlockSet;
import com.videoblocks.core.LampConfig;
import com.videoblocks.core.LampEntry;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

final class NetIO {
    private NetIO() {
    }

    static void writeConfig(ByteBuf b, LampConfig c) {
        ByteBufUtils.writeUTF8String(b, c.src);
        ByteBufUtils.writeUTF8String(b, c.title);
        b.writeInt(c.w);
        b.writeInt(c.h);
        b.writeInt(c.fps);
        b.writeBoolean(c.loop);
        b.writeBoolean(c.fit);
        b.writeBoolean(c.dither);
        b.writeInt(c.style);
        b.writeInt(c.mode);
        b.writeInt(c.stopMode);
        b.writeLong(c.blocks.word(0));
        b.writeLong(c.blocks.word(1));
        b.writeLong(c.blocks.word(2));
    }

    static LampConfig readConfig(ByteBuf b) {
        LampConfig c = new LampConfig();
        c.src = ByteBufUtils.readUTF8String(b);
        c.title = ByteBufUtils.readUTF8String(b);
        c.w = b.readInt();
        c.h = b.readInt();
        c.fps = b.readInt();
        c.loop = b.readBoolean();
        c.fit = b.readBoolean();
        c.dither = b.readBoolean();
        c.style = b.readInt();
        c.mode = b.readInt();
        c.stopMode = b.readInt();
        c.blocks = BlockSet.ofWords(b.readLong(), b.readLong(), b.readLong());
        return c;
    }

    static void writeEntry(ByteBuf b, LampEntry e) {
        ByteBufUtils.writeUTF8String(b, e.id);
        b.writeInt(e.dim);
        b.writeInt(e.x);
        b.writeInt(e.y);
        b.writeInt(e.z);
        b.writeInt(e.dx);
        b.writeInt(e.dz);
        ByteBufUtils.writeUTF8String(b, e.owner);
        b.writeBoolean(e.on);
        writeConfig(b, e.cfg);
    }

    static LampEntry readEntry(ByteBuf b) {
        String id = ByteBufUtils.readUTF8String(b);
        int dim = b.readInt(), x = b.readInt(), y = b.readInt(), z = b.readInt(), dx = b.readInt(), dz = b.readInt();
        String owner = ByteBufUtils.readUTF8String(b);
        boolean on = b.readBoolean();
        LampEntry e = new LampEntry(id, dim, x, y, z, dx, dz, owner, readConfig(b));
        e.on = on;
        return e;
    }
}
