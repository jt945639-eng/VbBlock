/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.BlockSink;
import com.videoblocks.core.Palette;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Coloca los bloques en un mundo real. Flags 2|16: envia al cliente, sin actualizar vecinos ni observers. */
final class WorldSink implements BlockSink {
    private static final Map<Palette.Entry, IBlockState> CACHE = new IdentityHashMap<Palette.Entry, IBlockState>();
    private static final int FLAGS = 2 | 16;

    private final World world;

    WorldSink(World world) {
        this.world = world;
    }

    private static IBlockState stateOf(Palette.Entry e) {
        IBlockState s = CACHE.get(e);
        if (s == null) {
            // Solo bloques de vanilla 1.12.2 que se consiguen en supervivencia. Cualquier otro grupo es un error.
            Block b;
            if (e.group.equals("concrete")) {
                b = Blocks.CONCRETE;
            } else if (e.group.equals("wool")) {
                b = Blocks.WOOL;
            } else if (e.group.equals("terracotta")) {
                b = Blocks.STAINED_HARDENED_CLAY;
            } else {
                throw new IllegalStateException("Bloque no permitido en la pantalla: " + e);
            }
            s = b.getStateFromMeta(e.meta);
            CACHE.put(e, s);
        }
        return s;
    }

    @Override
    public boolean isLoaded(int x, int y, int z) {
        return world.isBlockLoaded(new BlockPos(x, y, z));
    }

    @Override
    public void setBlock(int x, int y, int z, Palette.Entry entry) {
        IBlockState st = entry == null ? Blocks.AIR.getDefaultState() : stateOf(entry);
        world.setBlockState(new BlockPos(x, y, z), st, FLAGS);
    }
}
