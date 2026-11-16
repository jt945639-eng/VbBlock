/*
 * Copyright (c) 2026 NotXoot. Todos los derechos reservados.
 */
package com.videoblocks.client;

import com.videoblocks.VideoBlocksMod;
import com.videoblocks.core.LampConfig;
import com.videoblocks.net.PacketCreateLamp;
import com.videoblocks.net.PacketLampAction;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * Editor de una lampara de video (nueva o existente). Dos pestanas:
 * "Video" (archivo/URL, nombre, resolucion, fps, bucle, ajuste) y
 * "Opciones" (Redstone, al detener, dithering y tipos de bloque permitidos).
 */
@SideOnly(Side.CLIENT)
public class GuiLampEditor extends GuiScreen {
    private static final int B_OK = 0, B_CANCEL = 1, B_BROWSE = 2, B_LOOP = 3, B_FIT = 4, B_MODE = 5,
        B_DITHER = 6, B_STOP = 7, B_TAB_VIDEO = 8, B_TAB_OPTIONS = 9,
        B_BLOCKS = 10, B_PRESET = 20;
    private static final int[] PRESET_W = {64, 128, 192, 256};
    private static final int[] PRESET_H = {36, 72, 108, 144};

    private final GuiScreen parent;
    private final String editId;
    private final LampConfig cfg;
    private int tab;

    private GuiTextField fSrc, fTitle, fW, fH, fFps;
    private GuiButton bLoop, bFit, bMode, bDither, bStop, bBlocks;
    private String status = "";

    private volatile String picked;
    private volatile String pickError;
    private volatile boolean choosing;

    public GuiLampEditor(GuiScreen parent, String editId, LampConfig initial) {
        this.parent = parent;
        this.editId = editId;
        this.cfg = initial;
    }

    private int top() {
        return Math.max(4, height / 2 - 112);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        build();
    }

    private void build() {
        buttonList.clear();
        int cx = width / 2;
        int y0 = top();

        GuiButton t0 = new GuiButton(B_TAB_VIDEO, cx - 150, y0, 148, 20, "Video");
        GuiButton t1 = new GuiButton(B_TAB_OPTIONS, cx + 2, y0, 148, 20, "Opciones y bloques");
        t0.enabled = tab != 0;
        t1.enabled = tab != 1;
        buttonList.add(t0);
        buttonList.add(t1);

        if (tab == 0) {
            fSrc = new GuiTextField(10, fontRenderer, cx - 150, y0 + 38, 235, 18);
            fSrc.setMaxStringLength(500);
            fSrc.setText(cfg.src);
            fSrc.setFocused(true);
            fTitle = new GuiTextField(11, fontRenderer, cx - 150, y0 + 72, 300, 18);
            fTitle.setMaxStringLength(48);
            fTitle.setText(cfg.title);
            fW = new GuiTextField(12, fontRenderer, cx - 150, y0 + 106, 90, 18);
            fW.setMaxStringLength(4);
            fW.setText(String.valueOf(cfg.w));
            fH = new GuiTextField(13, fontRenderer, cx - 45, y0 + 106, 90, 18);
            fH.setMaxStringLength(4);
            fH.setText(String.valueOf(cfg.h));
            fFps = new GuiTextField(14, fontRenderer, cx + 60, y0 + 106, 90, 18);
            fFps.setMaxStringLength(2);
            fFps.setText(String.valueOf(cfg.fps));

            buttonList.add(new GuiButton(B_BROWSE, cx + 90, y0 + 37, 60, 20, "Examinar..."));
            for (int i = 0; i < PRESET_W.length; i++) {
                buttonList.add(new GuiButton(B_PRESET + i, cx - 150 + i * 76, y0 + 130, 72, 20,
                    PRESET_W[i] + "x" + PRESET_H[i]));
            }
            bLoop = new GuiButton(B_LOOP, cx - 150, y0 + 156, 148, 20, "");
            bFit = new GuiButton(B_FIT, cx + 2, y0 + 156, 148, 20, "");
            buttonList.add(bLoop);
            buttonList.add(bFit);
        } else {
            fSrc = fTitle = fW = fH = fFps = null;
            bMode = new GuiButton(B_MODE, cx - 150, y0 + 28, 300, 20, "");
            bStop = new GuiButton(B_STOP, cx - 150, y0 + 52, 300, 20, "");
            bDither = new GuiButton(B_DITHER, cx - 150, y0 + 76, 300, 20, "");
            bBlocks = new GuiButton(B_BLOCKS, cx - 150, y0 + 122, 300, 20, "");
            buttonList.add(bMode);
            buttonList.add(bStop);
            buttonList.add(bDither);
            buttonList.add(bBlocks);
        }
        buttonList.add(new GuiButton(B_OK, cx - 150, y0 + 190, 148, 20,
            editId == null ? "Crear lámpara" : "Guardar cambios"));
        buttonList.add(new GuiButton(B_CANCEL, cx + 2, y0 + 190, 148, 20, "Cancelar"));
        refreshLabels();
    }

    private void refreshLabels() {
        if (tab == 0) {
            bLoop.displayString = "Repetir en bucle: " + (cfg.loop ? "Sí" : "No");
            bFit.displayString = "Imagen: " + (cfg.fit ? "Respetar proporción" : "Estirar");
        } else {
            bMode.displayString = "Redstone: " + (cfg.mode == LampConfig.MODE_LEVEL
                ? "reproduce mientras haya señal" : "cada pulso activa / detiene");
            bStop.displayString = "Al detener: " + (cfg.stopMode == LampConfig.STOP_POSTER
                ? "volver al primer fotograma" : "dejar la imagen congelada");
            bDither.displayString = "Dithering (más matices de color): " + (cfg.dither ? "Sí" : "No");
            bBlocks.displayString = "Bloques: " + cfg.colorCount() + " de 48 activos  (clic para elegir)";
        }
    }

    private static int parse(GuiTextField f, int def) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Copia lo escrito en los campos de texto a cfg (solo existen en la pestana "Video"). */
    private void readFields() {
        if (fSrc == null) {
            return;
        }
        cfg.src = fSrc.getText().trim();
        cfg.title = fTitle.getText().trim();
        cfg.w = parse(fW, cfg.w);
        cfg.h = parse(fH, cfg.h);
        cfg.fps = parse(fFps, cfg.fps);
    }

    private void switchTab(int t) {
        readFields();
        tab = t;
        status = "";
        build();
    }

    private void browse() {
        if (choosing) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            status = "Este sistema no permite el selector de archivos; escribe la ruta a mano.";
            return;
        }
        choosing = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileDialog fd = new FileDialog((Frame) null, "Elige un video o GIF", FileDialog.LOAD);
                    fd.setVisible(true);
                    String f = fd.getFile();
                    if (f != null) {
                        picked = new File(fd.getDirectory(), f).getAbsolutePath();
                    }
                } catch (Throwable ex) {
                    pickError = "No se pudo abrir el selector; escribe la ruta a mano.";
                } finally {
                    choosing = false;
                }
            }
        }, "VideoBlocks-filedialog");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void updateScreen() {
        if (fSrc != null) {
            fSrc.updateCursorCounter();
            fTitle.updateCursorCounter();
            fW.updateCursorCounter();
            fH.updateCursorCounter();
            fFps.updateCursorCounter();
        }
        String p = picked;
        if (p != null) {
            picked = null;
            if (fSrc != null) {
                fSrc.setText(p);
                if (fTitle.getText().trim().isEmpty()) {
                    fTitle.setText(new File(p).getName());
                }
            } else {
                cfg.src = p;
            }
            status = "";
        }
        String err = pickError;
        if (err != null) {
            pickError = null;
            status = err;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= B_PRESET && b.id < B_PRESET + PRESET_W.length && fW != null) {
            fW.setText(String.valueOf(PRESET_W[b.id - B_PRESET]));
            fH.setText(String.valueOf(PRESET_H[b.id - B_PRESET]));
            return;
        }
        switch (b.id) {
            case B_TAB_VIDEO:
                switchTab(0);
                break;
            case B_TAB_OPTIONS:
                switchTab(1);
                break;
            case B_BROWSE:
                browse();
                break;
            case B_LOOP:
                cfg.loop = !cfg.loop;
                refreshLabels();
                break;
            case B_FIT:
                cfg.fit = !cfg.fit;
                refreshLabels();
                break;
            case B_MODE:
                cfg.mode = cfg.mode == LampConfig.MODE_LEVEL ? LampConfig.MODE_PULSE : LampConfig.MODE_LEVEL;
                refreshLabels();
                break;
            case B_DITHER:
                cfg.dither = !cfg.dither;
                refreshLabels();
                break;
            case B_STOP:
                cfg.stopMode = cfg.stopMode == LampConfig.STOP_POSTER ? LampConfig.STOP_FREEZE : LampConfig.STOP_POSTER;
                refreshLabels();
                break;
            case B_BLOCKS:
                mc.displayGuiScreen(new GuiBlockPicker(this, cfg));
                break;
            case B_CANCEL:
                mc.displayGuiScreen(parent);
                break;
            case B_OK:
                save();
                break;
            default:
                break;
        }
    }

    private void save() {
        readFields();
        if (cfg.src.isEmpty()) {
            if (tab != 0) {
                switchTab(0);
            }
            status = "Falta el video: elige un archivo o pega una URL.";
            return;
        }
        if (editId == null) {
            VideoBlocksMod.NETWORK.sendToServer(new PacketCreateLamp(cfg));
            mc.displayGuiScreen(null);
        } else {
            VideoBlocksMod.NETWORK.sendToServer(new PacketLampAction(editId, PacketLampAction.UPDATE, cfg));
            mc.displayGuiScreen(parent);
        }
    }

    private GuiTextField[] fields() {
        return fSrc == null ? new GuiTextField[0] : new GuiTextField[] {fSrc, fTitle, fW, fH, fFps};
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        GuiTextField[] all = fields();
        if (key == Keyboard.KEY_TAB && all.length > 0) {
            for (int i = 0; i < all.length; i++) {
                if (all[i].isFocused()) {
                    all[i].setFocused(false);
                    all[(i + 1) % all.length].setFocused(true);
                    return;
                }
            }
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            save();
            return;
        }
        boolean used = false;
        for (GuiTextField f : all) {
            if (f.isFocused()) {
                boolean numeric = f == fW || f == fH || f == fFps;
                if (numeric && !Character.isDigit(c) && c >= ' ' && c != 127) {
                    used = true; // ignora letras en campos numericos
                } else {
                    used = f.textboxKeyTyped(c, key);
                }
            }
        }
        if (!used) {
            super.keyTyped(c, key);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        for (GuiTextField f : fields()) {
            f.mouseClicked(mx, my, btn);
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
        int y0 = top();
        if (tab == 0) {
            drawString(fontRenderer, "Video o GIF: archivo del PC o URL (http/https)", cx - 150, y0 + 28, 0xAAAAAA);
            drawString(fontRenderer, "Nombre de la lámpara", cx - 150, y0 + 62, 0xAAAAAA);
            drawString(fontRenderer, "Ancho (bloques)", cx - 150, y0 + 96, 0xAAAAAA);
            drawString(fontRenderer, "Alto (bloques)", cx - 45, y0 + 96, 0xAAAAAA);
            drawString(fontRenderer, "FPS (1-20)", cx + 60, y0 + 96, 0xAAAAAA);
            for (GuiTextField f : fields()) {
                f.drawTextBox();
            }
        } else {
            drawString(fontRenderer, "Bloques que puede usar la pantalla (todos de supervivencia 1.12.2)",
                cx - 150, y0 + 108, 0xAAAAAA);
            drawCenteredString(fontRenderer, cfg.blocksSummary() + (cfg.colorCount() <= 16
                ? "  (pocos: la imagen perderá matices)" : ""), cx, y0 + 148, 0xCCCCCC);
            drawCenteredString(fontRenderer, "Activa o desactiva cada bloque: los desactivados no salen en el video.",
                cx, y0 + 162, 0x888888);
        }
        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, cx, y0 + 216, 0xFF5555);
        } else if (choosing) {
            drawCenteredString(fontRenderer, "Abriendo el selector de archivos...", cx, y0 + 216, 0xFFFF55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
