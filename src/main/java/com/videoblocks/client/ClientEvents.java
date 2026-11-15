/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.VideoBlocksMod;
import com.videoblocks.net.PacketRequestList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (ClientProxy.keyMenu != null && ClientProxy.keyMenu.isPressed()) {
            VideoBlocksMod.NETWORK.sendToServer(new PacketRequestList(true));
        }
    }
}
