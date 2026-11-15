/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.CommonProxy;
import com.videoblocks.core.LampEntry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    public static KeyBinding keyMenu;

    @Override
    public void preInit() {
        keyMenu = new KeyBinding("key.videoblocks.menu", Keyboard.KEY_V, "key.categories.videoblocks");
        ClientRegistry.registerKeyBinding(keyMenu);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    @Override
    public void showMenu(final List<LampEntry> entries, final boolean open) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                GuiScreen cur = mc.currentScreen;
                if (cur instanceof GuiLampMenu) {
                    ((GuiLampMenu) cur).setEntries(entries);
                } else if (open) {
                    mc.displayGuiScreen(new GuiLampMenu(entries));
                }
            }
        });
    }
}
