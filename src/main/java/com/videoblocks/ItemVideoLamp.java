/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import com.videoblocks.core.LampConfig;
import com.videoblocks.core.LampEntry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lampara de Redstone que lleva un video guardado en su NBT. Al colocarla se registra y genera la
 * pantalla en el suelo, delante de la direccion en la que mira el jugador.
 */
public class ItemVideoLamp extends ItemBlock {
    public static final String TAG = "videoblocks";

    public ItemVideoLamp() {
        super(Blocks.REDSTONE_LAMP);
        setRegistryName(VideoBlocksMod.MODID, "video_lamp");
        setUnlocalizedName(VideoBlocksMod.MODID + ".video_lamp");
    }

    @Nullable
    public static LampConfig readConfig(ItemStack stack) {
        if (stack.getTagCompound() == null || !stack.getTagCompound().hasKey(TAG)) {
            return null;
        }
        return LampNBT.readConfig(stack.getTagCompound().getCompoundTag(TAG));
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float hitX, float hitY, float hitZ, IBlockState newState) {
        boolean ok = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
        if (ok && !world.isRemote) {
            LampConfig cfg = readConfig(stack);
            if (cfg != null) {
                Vec3i d = player.getHorizontalFacing().getDirectionVec();
                LampEntry e = VideoBlocksMod.LAMPS.add(world.provider.getDimension(), pos.getX(), pos.getY(),
                    pos.getZ(), d.getX(), d.getZ(), player.getName(), cfg);
                player.sendMessage(new TextComponentString("[VideoBlocks] Lámpara '" + e.cfg.title
                    + "' colocada: la pantalla de " + e.cfg.w + "x" + e.cfg.h + " se genera delante de ella."));
            }
        }
        return ok;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        LampConfig c = readConfig(stack);
        return c != null ? "Lámpara de video: " + c.title : super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        LampConfig c = readConfig(stack);
        if (c == null) {
            return;
        }
        String s = c.src.length() > 40 ? "..." + c.src.substring(c.src.length() - 37) : c.src;
        tooltip.add(s);
        tooltip.add(c.w + "x" + c.h + " bloques @ " + c.fps + " fps" + (c.loop ? ", en bucle" : ""));
        tooltip.add("Redstone: " + (c.mode == LampConfig.MODE_LEVEL ? "reproduce mientras haya señal"
            : "cada pulso activa/detiene"));
        tooltip.add("Colócala en el suelo: la pantalla sale delante de ti.");
    }
}
