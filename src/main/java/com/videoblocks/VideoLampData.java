/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;

/** Guarda en el mundo la lista de lamparas de video (es lo que muestra el menu). */
public class VideoLampData extends WorldSavedData {
    public static final String NAME = "videoblocks_lamps";

    public VideoLampData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList list = nbt.getTagList("lamps", 10);
        List<LampEntry> entries = new ArrayList<LampEntry>();
        for (int i = 0; i < list.tagCount(); i++) {
            entries.add(LampNBT.readEntry(list.getCompoundTagAt(i)));
        }
        VideoBlocksMod.LAMPS.load(entries);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (LampEntry e : VideoBlocksMod.LAMPS.list()) {
            list.appendTag(LampNBT.writeEntry(e));
        }
        nbt.setTag("lamps", list);
        return nbt;
    }
}
