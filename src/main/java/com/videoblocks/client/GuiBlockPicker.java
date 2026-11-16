/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.core.BlockSet;
import com.videoblocks.core.LampConfig;
import com.videoblocks.core.Palette;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lista de todos los bloques solidos de supervivencia que puede usar una pantalla, por categorias.
 * Cada bloque tiene su interruptor: los desactivados no se generan en el video.
 * Se edita directamente el LampConfig del editor que la abrio.
 */
@SideOnly(Side.CLIENT)
public class GuiBlockPicker extends GuiScreen {
    private static final int B_DONE = 1, B_ALL = 2, B_NONE = 3, B_ALL_GROUPS = 4, B_MIN = 7, B_PREV = 5, B_NEXT = 6, B_CELL = 100;
    private static final int COLS = 4, CELL_W = 74, CELL_H = 20, GAP_X = 1, GAP_Y = 2;

    private final GuiScreen parent;
    private final LampConfig cfg;
    private int page;
    private int[] pageIdx = new int[0];
    private final java.util.List<int[]> pages = new java.util.ArrayList<int[]>();
    private final java.util.List<String> pageTitles = new java.util.ArrayList<String>();
    private boolean disabledPage;
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
        rebuildPages();
        buttonList.add(new GuiButton(B_PREV, cx - 150, y0 + 16, 30, 20, "<"));
        buttonList.add(new GuiButton(B_NEXT, cx + 120, y0 + 16, 30, 20, ">"));
        int gx = gridLeft();
        for (int i = 0; i < 16; i++) {
            int col = i % COLS;
            int row = i / COLS;
            cells[i] = new GuiButton(B_CELL + i, gx + col * (CELL_W + GAP_X), y0 + 42 + row * (CELL_H + GAP_Y),
                CELL_W, CELL_H, "");
            cells[i].visible = i < pageIdx.length;
            buttonList.add(cells[i]);
        }
        int fy = y0 + 42 + 4 * (CELL_H + GAP_Y) + 4;
        buttonList.add(new GuiButton(B_ALL, cx - 150, fy, 74, 20, "Activar pág."));
        buttonList.add(new GuiButton(B_NONE, cx - 75, fy, 74, 20, "Desact. pág."));
        buttonList.add(new GuiButton(B_ALL_GROUPS, cx, fy, 74, 20, "Todos (" + Palette.CATALOG_SIZE + ")"));
        buttonList.add(new GuiButton(B_MIN, cx + 75, fy, 75, 20, "Ninguno"));
        buttonList.add(new GuiButton(B_DONE, cx - 100, fy + 24, 200, 20, "Listo"));
        refreshLabels();
    }

    /** Paginas de 16: cada categoria (en varias paginas si hace falta) y al final "Desactivados" (se calcula al momento). */
    private void rebuildPages() {
        pages.clear();
        pageTitles.clear();
        for (int c = 0; c < Palette.categoryCount(); c++) {
            addChunks(Palette.categoryIndices(c), Palette.categoryTitle(c));
        }
        int[] off = new int[Palette.CATALOG_SIZE - cfg.blocks.count()];
        int k = 0;
        for (int i = 0; i < Palette.CATALOG_SIZE; i++) {
            if (!cfg.hasBlock(i)) {
                off[k++] = i;
            }
        }
        addChunks(off, "Desactivados");
        if (page >= pages.size()) {
            page = pages.size() - 1;
        }
        pageIdx = pages.get(page);
        disabledPage = page >= pages.size() - Math.max(1, (off.length + 15) / 16);
    }

    private void addChunks(int[] all, String title) {
        int n = Math.max(1, (all.length + 15) / 16);
        for (int p = 0; p < n; p++) {
            int from = p * 16;
            int to = Math.min(all.length, from + 16);
            pages.add(java.util.Arrays.copyOfRange(all, from, to));
            pageTitles.add(n > 1 ? title + " " + (p + 1) + "/" + n : title);
        }
    }

    /** Despues de activar/desactivar: recalcula la pagina actual (en "Desactivados" la lista cambia sola). */
    private void refreshPage() {
        rebuildPages();
        for (int i = 0; i < 16; i++) {
            cells[i].visible = i < pageIdx.length;
        }
        refreshLabels();
    }

    private void refreshLabels() {
        for (int i = 0; i < pageIdx.length; i++) {
            boolean on = cfg.hasBlock(pageIdx[i]);
            // los dos espacios dejan sitio a la muestra de color (se dibuja en drawScreen)
            String name = Palette.catalog(pageIdx[i]).es;
            String shown = fontRenderer != null && fontRenderer.getStringWidth(name) > CELL_W - 18
                ? fontRenderer.trimStringToWidth(name, CELL_W - 18) : name;
            cells[i].displayString = on ? "\u00a7a  " + shown : "\u00a7c\u00a7m  " + shown;
        }
    }

    private boolean setBits(int[] idx, boolean on) {
        BlockSet next = cfg.blocks.with(idx, on);
        status = "";
        if (next.count() < BlockSet.MIN_BLOCKS) {
            next = next.ensureMinimum();
            status = "Mínimo 2 bloques: se completa con lana negra y piedra luminosa.";
        }
        cfg.blocks = next;
        refreshPage();
        return true;
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= B_CELL && b.id < B_CELL + 16) {
            int k = b.id - B_CELL;
            if (k < pageIdx.length) {
                setBits(new int[] {pageIdx[k]}, !cfg.hasBlock(pageIdx[k]));
            }
            return;
        }
        int n = pages.size();
        switch (b.id) {
            case B_PREV:
                page = (page + n - 1) % n;
                status = "";
                initGui();
                break;
            case B_NEXT:
                page = (page + 1) % n;
                status = "";
                initGui();
                break;
            case B_ALL:
                setBits(pageIdx, true);
                break;
            case B_NONE:
                setBits(pageIdx, false);
                break;
            case B_ALL_GROUPS:
                cfg.blocks = BlockSet.ALL;
                status = "";
                refreshPage();
                break;
            case B_MIN:
                cfg.blocks = BlockSet.MINIMAL;
                status = "Quedan solo lana negra y piedra luminosa.";
                refreshPage();
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
        drawCenteredString(fontRenderer, "Verde = se usa en el video, rojo tachado = desactivado", cx, y0 + 2, 0xCCCCCC);
        drawCenteredString(fontRenderer, pageTitles.get(page) + (disabledPage
            ? "  (" + (Palette.CATALOG_SIZE - cfg.blocks.count()) + " en total)" : "  (" + cfg.blocks.count(pageIdx) + "/"
            + pageIdx.length + ")") + "  -  pág. " + (page + 1) + "/" + pages.size(), cx, y0 + 22, 0xFFFFFF);
        if (pageIdx.length == 0) {
            drawCenteredString(fontRenderer, "No hay bloques desactivados: se usan todos.", cx, y0 + 80, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int i = 0; i < pageIdx.length; i++) {
            Palette.Entry e = Palette.catalog(pageIdx[i]);
            int x = cells[i].x + 3;
            int y = cells[i].y + 5;
            int rgb = 0xFF000000 | (e.r << 16) | (e.g << 8) | e.b;
            drawRect(x - 1, y - 1, x + 11, y + 11, 0xFF000000);
            drawRect(x, y, x + 10, y + 10, rgb);
            if (!cfg.hasBlock(pageIdx[i])) {
                drawRect(x, y, x + 10, y + 10, 0x99000000); // oscurece la muestra de los desactivados
            }
        }
        int fy = y0 + 42 + 4 * (CELL_H + GAP_Y) + 4 + 48;
        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, cx, fy, 0xFF5555);
        } else {
            drawCenteredString(fontRenderer, cfg.colorCount() + " de " + Palette.CATALOG_SIZE + " bloques activos"
                + (cfg.colorCount() <= 16 ? " (pocos: la imagen perderá matices)" : ""), cx, fy, 0xCCCCCC);
        }
    }
}
