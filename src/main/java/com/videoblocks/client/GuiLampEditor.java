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
 * Pantalla para marcar el video (archivo del PC o URL) y sus ajustes. Sirve para crear una lampara
 * nueva (editId == null) o para editar una existente (resolucion, fps, modo de Redstone...).
 */
@SideOnly(Side.CLIENT)
public class GuiLampEditor extends GuiScreen {
    private static final int B_OK = 0, B_CANCEL = 1, B_BROWSE = 2, B_LOOP = 3, B_FIT = 4, B_MODE = 5, B_DITHER = 6;

    private final GuiScreen parent;
    private final String editId;
    private final LampConfig cfg;

    private GuiTextField fSrc, fTitle, fW, fH, fFps;
    private GuiButton bLoop, bFit, bMode, bDither;
    private String status = "";

    private volatile String picked;
    private volatile String pickError;
    private volatile boolean choosing;

    public GuiLampEditor(GuiScreen parent, String editId, LampConfig initial) {
        this.parent = parent;
        this.editId = editId;
        this.cfg = initial;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        int cx = width / 2;
        int y0 = Math.max(8, height / 2 - 105);

        fSrc = new GuiTextField(10, fontRenderer, cx - 150, y0 + 12, 235, 18);
        fSrc.setMaxStringLength(500);
        fSrc.setText(cfg.src);
        fSrc.setFocused(true);
        fTitle = new GuiTextField(11, fontRenderer, cx - 150, y0 + 48, 300, 18);
        fTitle.setMaxStringLength(48);
        fTitle.setText(cfg.title);
        fW = new GuiTextField(12, fontRenderer, cx - 150, y0 + 84, 90, 18);
        fW.setMaxStringLength(4);
        fW.setText(String.valueOf(cfg.w));
        fH = new GuiTextField(13, fontRenderer, cx - 45, y0 + 84, 90, 18);
        fH.setMaxStringLength(4);
        fH.setText(String.valueOf(cfg.h));
        fFps = new GuiTextField(14, fontRenderer, cx + 60, y0 + 84, 90, 18);
        fFps.setMaxStringLength(2);
        fFps.setText(String.valueOf(cfg.fps));

        buttonList.add(new GuiButton(B_BROWSE, cx + 90, y0 + 11, 60, 20, "Examinar..."));
        bLoop = new GuiButton(B_LOOP, cx - 150, y0 + 110, 148, 20, "");
        bFit = new GuiButton(B_FIT, cx + 2, y0 + 110, 148, 20, "");
        bMode = new GuiButton(B_MODE, cx - 150, y0 + 134, 148, 20, "");
        bDither = new GuiButton(B_DITHER, cx + 2, y0 + 134, 148, 20, "");
        buttonList.add(bLoop);
        buttonList.add(bFit);
        buttonList.add(bMode);
        buttonList.add(bDither);
        buttonList.add(new GuiButton(B_OK, cx - 150, y0 + 162, 148, 20,
            editId == null ? "Crear lámpara" : "Guardar cambios"));
        buttonList.add(new GuiButton(B_CANCEL, cx + 2, y0 + 162, 148, 20, "Cancelar"));
        refreshLabels();
    }

    private void refreshLabels() {
        bLoop.displayString = "Repetir en bucle: " + (cfg.loop ? "Sí" : "No");
        bFit.displayString = "Imagen: " + (cfg.fit ? "Respetar proporción" : "Estirar");
        bMode.displayString = "Redstone: " + (cfg.mode == LampConfig.MODE_LEVEL ? "Mientras haya señal" : "Cada pulso alterna");
        bDither.displayString = "Dithering: " + (cfg.dither ? "Sí" : "No");
    }

    private static int parse(GuiTextField f, int def) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            return def;
        }
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
                    FileDialog fd = new FileDialog((Frame) null, "Elige un video", FileDialog.LOAD);
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
        fSrc.updateCursorCounter();
        fTitle.updateCursorCounter();
        fW.updateCursorCounter();
        fH.updateCursorCounter();
        fFps.updateCursorCounter();
        String p = picked;
        if (p != null) {
            picked = null;
            fSrc.setText(p);
            if (fTitle.getText().trim().isEmpty()) {
                fTitle.setText(new File(p).getName());
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
        switch (b.id) {
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
        cfg.src = fSrc.getText().trim();
        cfg.title = fTitle.getText().trim();
        cfg.w = parse(fW, cfg.w);
        cfg.h = parse(fH, cfg.h);
        cfg.fps = parse(fFps, cfg.fps);
        if (cfg.src.isEmpty()) {
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

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_TAB) {
            GuiTextField[] all = {fSrc, fTitle, fW, fH, fFps};
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
        GuiTextField[] all = {fSrc, fTitle, fW, fH, fFps};
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
        fSrc.mouseClicked(mx, my, btn);
        fTitle.mouseClicked(mx, my, btn);
        fW.mouseClicked(mx, my, btn);
        fH.mouseClicked(mx, my, btn);
        fFps.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int y0 = Math.max(8, height / 2 - 105);
        drawCenteredString(fontRenderer, editId == null ? "Nueva lámpara de video" : "Editar lámpara de video",
            cx, y0 - 6, 0xFFFFFF);
        drawString(fontRenderer, "Video: archivo del PC o URL (http/https)", cx - 150, y0 + 2, 0xAAAAAA);
        drawString(fontRenderer, "Nombre de la lámpara", cx - 150, y0 + 38, 0xAAAAAA);
        drawString(fontRenderer, "Ancho (bloques)", cx - 150, y0 + 74, 0xAAAAAA);
        drawString(fontRenderer, "Alto (bloques)", cx - 45, y0 + 74, 0xAAAAAA);
        drawString(fontRenderer, "FPS (1-20)", cx + 60, y0 + 74, 0xAAAAAA);
        fSrc.drawTextBox();
        fTitle.drawTextBox();
        fW.drawTextBox();
        fH.drawTextBox();
        fFps.drawTextBox();
        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, cx, y0 + 188, 0xFF5555);
        } else if (choosing) {
            drawCenteredString(fontRenderer, "Abriendo el selector de archivos...", cx, y0 + 188, 0xFFFF55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
