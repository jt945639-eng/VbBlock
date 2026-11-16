/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.BlockSet;
import com.videoblocks.core.LampConfig;
import com.videoblocks.core.LampEntry;
import net.minecraft.nbt.NBTTagCompound;

/** Conversion de ajustes y lamparas a NBT (item y datos guardados del mundo). */
public final class LampNBT {
    private LampNBT() {
    }

    public static NBTTagCompound writeConfig(LampConfig c) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("src", c.src);
        t.setString("title", c.title);
        t.setInteger("w", c.w);
        t.setInteger("h", c.h);
        t.setInteger("fps", c.fps);
        t.setBoolean("loop", c.loop);
        t.setBoolean("fit", c.fit);
        t.setBoolean("dither", c.dither);
        t.setInteger("mode", c.mode);
        t.setInteger("stop", c.stopMode);
        t.setLong("bs0", c.blocks.word(0));
        t.setLong("bs1", c.blocks.word(1));
        t.setLong("bs2", c.blocks.word(2));
        return t;
    }

    public static LampConfig readConfig(NBTTagCompound t) {
        LampConfig c = new LampConfig();
        c.src = t.getString("src");
        c.title = t.getString("title");
        if (t.hasKey("w")) {
            c.w = t.getInteger("w");
        }
        if (t.hasKey("h")) {
            c.h = t.getInteger("h");
        }
        if (t.hasKey("fps")) {
            c.fps = t.getInteger("fps");
        }
        if (t.hasKey("loop")) {
            c.loop = t.getBoolean("loop");
        }
        if (t.hasKey("fit")) {
            c.fit = t.getBoolean("fit");
        }
        c.dither = t.getBoolean("dither");
        c.mode = t.getInteger("mode");
        c.stopMode = t.getInteger("stop");
        if (t.hasKey("bs0")) {
            c.blocks = BlockSet.ofWords(t.getLong("bs0"), t.getLong("bs1"), t.getLong("bs2"));
        } else if (t.hasKey("blockMask")) {
            c.blocks = BlockSet.ofWords(t.getLong("blockMask"), 0L, 0L); // version con 48 bloques (indices 0-47 iguales)
        } else if (t.hasKey("blocks")) {
            c.blocks = LampConfig.groupBitsToSet(t.getInteger("blocks")); // version con 3 grupos
        } else {
            c.blocks = BlockSet.DEFAULT;
        }
        return c.sanitize(VideoBlocksMod.LAMPS.maxSide);
    }

    public static NBTTagCompound writeEntry(LampEntry e) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("id", e.id);
        t.setInteger("dim", e.dim);
        t.setInteger("x", e.x);
        t.setInteger("y", e.y);
        t.setInteger("z", e.z);
        t.setInteger("dx", e.dx);
        t.setInteger("dz", e.dz);
        t.setString("owner", e.owner);
        t.setBoolean("on", e.on);
        t.setTag("cfg", writeConfig(e.cfg));
        return t;
    }

    public static LampEntry readEntry(NBTTagCompound t) {
        LampEntry e = new LampEntry(t.getString("id"), t.getInteger("dim"), t.getInteger("x"),
            t.getInteger("y"), t.getInteger("z"), t.getInteger("dx"), t.getInteger("dz"),
            t.getString("owner"), readConfig(t.getCompoundTag("cfg")));
        e.on = !t.hasKey("on") || t.getBoolean("on");
        return e;
    }
}
