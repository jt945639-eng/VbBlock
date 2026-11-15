/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.VideoBlocksMod;
import com.videoblocks.core.LampConfig;
import com.videoblocks.core.LampEntry;
import com.videoblocks.net.PacketLampAction;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Menu con las lamparas de video guardadas en el mundo. */
@SideOnly(Side.CLIENT)
public class GuiLampMenu extends GuiScreen {
    private static final int ROW_H = 34;
    private static final int TOP = 36;

    private List<LampEntry> entries;
    private int page;
    private int perPage = 5;
    private String confirmId;

    public GuiLampMenu(List<LampEntry> entries) {
        this.entries = entries;
    }

    /** El servidor mando una lista nueva (tras una accion): se refresca sin cerrar el menu. */
    public void setEntries(List<LampEntry> list) {
        this.entries = list;
        this.confirmId = null;
        if (this.mc != null) {
            initGui();
        }
    }

    private int pages() {
        return Math.max(1, (entries.size() + perPage - 1) / perPage);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        perPage = Math.max(2, Math.min(6, (height - TOP - 40) / ROW_H));
        page = Math.max(0, Math.min(page, pages() - 1));
        int cx = width / 2;
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            LampEntry e = entries.get(start + i);
            int y = TOP + i * ROW_H + 4;
            int bx = cx + 14;
            int base = 100 + i * 10;
            buttonList.add(new GuiButton(base, bx, y, 24, 20, e.on ? "||" : ">"));
            buttonList.add(new GuiButton(base + 1, bx + 26, y, 44, 20, "Editar"));
            buttonList.add(new GuiButton(base + 2, bx + 72, y, 24, 20, "Ir"));
            buttonList.add(new GuiButton(base + 3, bx + 98, y, 34, 20, "Dar"));
            buttonList.add(new GuiButton(base + 4, bx + 134, y, 24, 20, e.id.equals(confirmId) ? "?" : "X"));
        }
        int by = height - 28;
        buttonList.add(new GuiButton(1, cx - 200, by, 30, 20, "<"));
        buttonList.add(new GuiButton(2, cx - 168, by, 30, 20, ">"));
        buttonList.add(new GuiButton(3, cx - 70, by, 140, 20, "Nueva lámpara"));
        buttonList.add(new GuiButton(4, cx + 140, by, 60, 20, "Cerrar"));
    }

    private void send(String id, int action) {
        VideoBlocksMod.NETWORK.sendToServer(new PacketLampAction(id, action, null));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        int id = b.id;
        if (id == 1) {
            page = Math.max(0, page - 1);
            confirmId = null;
            initGui();
        } else if (id == 2) {
            page = Math.min(pages() - 1, page + 1);
            confirmId = null;
            initGui();
        } else if (id == 3) {
            mc.displayGuiScreen(new GuiLampEditor(this, null, new LampConfig()));
        } else if (id == 4) {
            mc.displayGuiScreen(null);
        } else if (id >= 100) {
            int row = (id - 100) / 10;
            int act = (id - 100) % 10;
            int idx = page * perPage + row;
            if (idx < 0 || idx >= entries.size()) {
                return;
            }
            LampEntry e = entries.get(idx);
            switch (act) {
                case 0:
                    send(e.id, PacketLampAction.TOGGLE);
                    break;
                case 1:
                    mc.displayGuiScreen(new GuiLampEditor(this, e.id, e.cfg.copy()));
                    break;
                case 2:
                    send(e.id, PacketLampAction.TELEPORT);
                    mc.displayGuiScreen(null);
                    break;
                case 3:
                    send(e.id, PacketLampAction.GIVE);
                    break;
                case 4:
                    if (e.id.equals(confirmId)) {
                        confirmId = null;
                        send(e.id, PacketLampAction.DELETE);
                    } else {
                        confirmId = e.id;
                        initGui();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        drawCenteredString(fontRenderer, "Lámparas de video guardadas", cx, 12, 0xFFFFFF);
        drawCenteredString(fontRenderer, "> / ||: iniciar o detener   Editar   Ir: teletransportar   Dar: copia de la lámpara   X: olvidar",
            cx, 24, 0x999999);
        int start = page * perPage;
        if (entries.isEmpty()) {
            drawCenteredString(fontRenderer, "No hay lámparas todavía. Pulsa 'Nueva lámpara', elige el video y colócala en el suelo.",
                cx, TOP + 20, 0xCCCCCC);
        }
        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            LampEntry e = entries.get(start + i);
            int y = TOP + i * ROW_H;
            String title = fontRenderer.trimStringToWidth(e.cfg.title, 205);
            String l2 = e.x + " " + e.y + " " + e.z + " (dim " + e.dim + ") | " + e.cfg.w + "x" + e.cfg.h
                + " @" + e.cfg.fps + "fps | "
                + (e.on ? TextFormatting.GREEN + "Reproduciendo" : TextFormatting.RED + "Detenida");
            String l3 = "Redstone: " + (e.cfg.mode == LampConfig.MODE_LEVEL ? "mientras haya señal" : "pulso alterna")
                + (e.owner.isEmpty() ? "" : " | " + e.owner);
            drawString(fontRenderer, title, cx - 200, y + 2, 0xFFFFFF);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(l2, 205), cx - 200, y + 12, 0xCCCCCC);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(l3, 205), cx - 200, y + 22, 0x888888);
        }
        drawCenteredString(fontRenderer, "Página " + (page + 1) + "/" + pages() + "  (" + entries.size() + " lámparas)",
            cx - 100, height - 22, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
