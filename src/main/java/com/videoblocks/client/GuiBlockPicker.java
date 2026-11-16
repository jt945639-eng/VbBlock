/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.core.LampConfig;
import com.videoblocks.core.Palette;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lista de los 48 bloques que puede usar una pantalla (concrete, lana y terracota, todos de supervivencia).
 * Cada bloque tiene su interruptor: los desactivados no se generan en el video.
 * Se edita directamente el LampConfig del editor que la abrio.
 */
@SideOnly(Side.CLIENT)
public class GuiBlockPicker extends GuiScreen {
    private static final int B_DONE = 1, B_ALL = 2, B_NONE = 3, B_ALL_GROUPS = 4, B_TAB = 10, B_CELL = 100;
    private static final String[] GROUP_NAMES = {"Concrete", "Lana", "Terracota"};
    private static final String[] DYES_ES = {
        "Blanco", "Naranja", "Magenta", "Azul claro", "Amarillo", "Lima", "Rosa", "Gris",
        "Gris claro", "Cian", "Morado", "Azul", "Marrón", "Verde", "Rojo", "Negro"
    };
    private static final int COLS = 4, CELL_W = 74, CELL_H = 20, GAP_X = 1, GAP_Y = 2;

    private final GuiScreen parent;
    private final LampConfig cfg;
    private int group;
    private String status = "";
    private final GuiButton[] cells = new GuiButton[16];

    public GuiBlockPicker(GuiScreen parent, LampConfig cfg) {
        this.parent = parent;
        this.cfg = cfg;
    }

    private int top() {
        return Math.max(4, height / 2 - 100);
    }

    private int gridLeft() {
        return width / 2 - (COLS * CELL_W + (COLS - 1) * GAP_X) / 2;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int cx = width / 2;
        int y0 = top();
        for (int g = 0; g < 3; g++) {
            GuiButton t = new GuiButton(B_TAB + g, cx - 150 + g * 101, y0 + 16, 98, 20, "");
            t.enabled = g != group;
            buttonList.add(t);
        }
        int gx = gridLeft();
        for (int i = 0; i < 16; i++) {
            int col = i % COLS;
            int row = i / COLS;
            cells[i] = new GuiButton(B_CELL + i, gx + col * (CELL_W + GAP_X), y0 + 42 + row * (CELL_H + GAP_Y),
                CELL_W, CELL_H, "");
            buttonList.add(cells[i]);
        }
        int fy = y0 + 42 + 4 * (CELL_H + GAP_Y) + 4;
        buttonList.add(new GuiButton(B_ALL, cx - 150, fy, 98, 20, "Activar todos"));
        buttonList.add(new GuiButton(B_NONE, cx - 49, fy, 98, 20, "Desactivar todos"));
        buttonList.add(new GuiButton(B_ALL_GROUPS, cx + 52, fy, 98, 20, "Los 48 bloques"));
        buttonList.add(new GuiButton(B_DONE, cx - 100, fy + 24, 200, 20, "Listo"));
        refreshLabels();
    }

    private int groupCount(int g) {
        return Long.bitCount((cfg.blocks >>> (16 * g)) & 0xFFFFL);
    }

    private void refreshLabels() {
        for (int g = 0; g < 3; g++) {
            for (Object o : buttonList) {
                GuiButton b = (GuiButton) o;
                if (b.id == B_TAB + g) {
                    b.displayString = GROUP_NAMES[g] + " (" + groupCount(g) + "/16)";
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            boolean on = cfg.hasBlock(group * 16 + i);
            // los dos espacios dejan sitio a la muestra de color (se dibuja en drawScreen)
            cells[i].displayString = on ? "§a  " + DYES_ES[i] : "§c§m  " + DYES_ES[i];
        }
    }

    private boolean setBits(long mask, boolean on) {
        long next = on ? (cfg.blocks | mask) : (cfg.blocks & ~mask);
        next &= LampConfig.BLOCKS_ALL;
        if (next == 0) {
            status = "Debe quedar al menos un bloque activado.";
            return false;
        }
        cfg.blocks = next;
        status = "";
        refreshLabels();
        return true;
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= B_CELL && b.id < B_CELL + 16) {
            int idx = group * 16 + (b.id - B_CELL);
            setBits(1L << idx, !cfg.hasBlock(idx));
            return;
        }
        if (b.id >= B_TAB && b.id < B_TAB + 3) {
            group = b.id - B_TAB;
            status = "";
            initGui();
            return;
        }
        long groupMask = 0xFFFFL << (16 * group);
        switch (b.id) {
            case B_ALL:
                setBits(groupMask, true);
                break;
            case B_NONE:
                setBits(groupMask, false);
                break;
            case B_ALL_GROUPS:
                setBits(LampConfig.BLOCKS_ALL, true);
                break;
            case B_DONE:
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { // Esc: vuelve al editor conservando lo elegido
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int y0 = top();
        drawCenteredString(fontRenderer, "Bloques que se usan en el video (verde = activo, rojo tachado = desactivado)",
            cx, y0 + 2, 0xCCCCCC);
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int i = 0; i < 16; i++) {
            Palette.Entry e = Palette.catalog(group * 16 + i);
            int x = cells[i].x + 3;
            int y = cells[i].y + 5;
            int rgb = 0xFF000000 | (e.r << 16) | (e.g << 8) | e.b;
            drawRect(x - 1, y - 1, x + 11, y + 11, 0xFF000000);
            drawRect(x, y, x + 10, y + 10, rgb);
            if (!cfg.hasBlock(group * 16 + i)) {
                drawRect(x, y, x + 10, y + 10, 0x99000000); // oscurece la muestra de los desactivados
            }
        }
        int fy = y0 + 42 + 4 * (CELL_H + GAP_Y) + 4 + 48;
        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, cx, fy, 0xFF5555);
        } else {
            drawCenteredString(fontRenderer, cfg.colorCount() + " de 48 bloques activos"
                + (cfg.colorCount() <= 16 ? " (pocos: la imagen perderá matices)" : ""), cx, fy, 0xCCCCCC);
        }
    }
}
