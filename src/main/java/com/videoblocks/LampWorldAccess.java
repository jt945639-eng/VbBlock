/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampController;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/** Consultas del controlador al mundo real: chunk cargado, sigue siendo lampara, hay Redstone. */
final class LampWorldAccess implements LampController.WorldAccess {

    @Override
    public boolean isLoaded(int dim, int x, int y, int z) {
        World w = DimensionManager.getWorld(dim);
        return w != null && w.isBlockLoaded(new BlockPos(x, y, z));
    }

    @Override
    public boolean isLamp(int dim, int x, int y, int z) {
        World w = DimensionManager.getWorld(dim);
        if (w == null) {
            return false;
        }
        Block b = w.getBlockState(new BlockPos(x, y, z)).getBlock();
        return b == Blocks.REDSTONE_LAMP || b == Blocks.LIT_REDSTONE_LAMP;
    }

    @Override
    public boolean isPowered(int dim, int x, int y, int z) {
        World w = DimensionManager.getWorld(dim);
        return w != null && w.isBlockPowered(new BlockPos(x, y, z));
    }
}
