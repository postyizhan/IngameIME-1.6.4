package com.dhj.ingameime.compat;

import com.dhj.ingameime.IngameIME_Fish;
import com.dhj.ingameime.control.AbstractControl;
import net.minecraft.Minecraft;
import shims.java.net.minecraft.client.gui.widget.TextFieldWidget;

import java.awt.Point;
import java.io.IOException;

public class EmiSearchControl extends AbstractControl<Object> {
    public EmiSearchControl(Object control) {
        super(control);
    }

    private TextFieldWidget widget() {
        return (TextFieldWidget) controlObject;
    }

    @Override
    public boolean isVisible() {
        try {
            return widget().isVisible();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public Point getCursorPos() {
        try {
            TextFieldWidget w = widget();
            String text = w.getText() == null ? "" : w.getText();
            int cursor = w.getCursor();
            int caretX = w.getX() + 4
                    + Minecraft.getMinecraft().fontRenderer.getStringWidth(text.substring(0, Math.min(cursor, text.length())));
            int caretY = w.getY() + (w.getHeight() - 8) / 2 - 1;
            return new Point(caretX - 1, caretY);
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("Failed to get EMI search cursor position", t);
            return new Point(0, 0);
        }
    }

    @Override
    public void writeText(String text) {
        try {
            widget().write(text);
        } catch (Throwable t) {
            IngameIME_Fish.LOG.error("Failed to write committed text to EMI search box", t);
        }
    }
}
