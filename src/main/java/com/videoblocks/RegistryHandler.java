/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Registro del item (en 1.12.2 debe ir en un EventBusSubscriber para llegar a tiempo). */
@Mod.EventBusSubscriber(modid = VideoBlocksMod.MODID)
public class RegistryHandler {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        VideoBlocksMod.lampItem = new ItemVideoLamp();
        event.getRegistry().register(VideoBlocksMod.lampItem);
    }

    /** La lampara de video se ve igual que la lampara de Redstone normal. */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(VideoBlocksMod.lampItem, 0,
            new ModelResourceLocation("minecraft:redstone_lamp", "inventory"));
    }
}
