package com.dhj.ingameime.control;

import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;

import java.awt.Point;
import java.io.IOException;
import java.util.logging.Level;

/**
 * 告示牌编辑界面。
 *
 * width/height 来自 GuiScreen，本来就是 public；entitySign/editLine 由 AccessTransformer 放宽。
 */
public class SignControl extends AbstractControl<Object> {
    public SignControl(Object control) {
        super(control);
    }

    private GuiEditSign screen() {
        return (GuiEditSign) controlObject;
    }

    @Override
    public void writeText(String text) throws IOException {
        if (text == null || text.length() == 0) return;
        try {
            GuiEditSign screen = screen();
            TileEntitySign sign = screen.entitySign;
            int line = screen.editLine;
            if (sign == null || sign.signText == null || line < 0 || line >= sign.signText.length) return;

            String old = sign.signText[line];
            if (old == null) old = "";
            int room = 15 - old.length();
            if (room <= 0) return;
            String insert = text.length() > room ? text.substring(0, room) : text;
            sign.signText[line] = old + insert;
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write text to sign");
            ioe.initCause(t);
            throw ioe;
        }
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Point getCursorPos() {
        try {
            GuiEditSign screen = screen();
            TileEntitySign sign = screen.entitySign;
            int line = screen.editLine;
            String text = sign != null && sign.signText != null && line >= 0 && line < sign.signText.length && sign.signText[line] != null
                    ? sign.signText[line] : "";
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int x = screen.width / 2 + font.getStringWidth(text) / 2 + 8;
            int y = screen.height / 4 + 58 + line * 10;
            return new Point(x, y);
        } catch (Throwable t) {
            IngameIME_Forge.LOG.log(Level.WARNING, "Failed to get sign cursor position", t);
            Minecraft mc = Minecraft.getMinecraft();
            return new Point(mc.currentScreen != null ? mc.currentScreen.width / 2 : 0,
                    mc.currentScreen != null ? mc.currentScreen.height / 2 : 0);
        }
    }
}
