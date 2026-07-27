package com.dhj.ingameime.gui;

import com.dhj.ingameime.config.Config;
import net.minecraft.Minecraft;
import net.minecraft.Gui;
import net.minecraft.ScaledResolution;

public class Widget extends Gui {
    public int offsetX, offsetY;
    public int TextColor = Config.TextColor;
    public int Background = Config.BackgroundColor;
    public int Padding = Config.Padding;
    public int X, Y;
    public int Width, Height;
    public boolean DrawInline = true;
    protected boolean isDirty = true;

    protected void updateThemeColors() {
        TextColor = Config.TextColor;
        Background = Config.BackgroundColor;
        Padding = Config.Padding;
    }

    public boolean isActive() {
        return false;
    }

    public void layout() {
        updateThemeColors();
        Minecraft mc = Minecraft.getMinecraft();
        int totalWidth = Width + 2 * Padding;
        int totalHeight = Height + 2 * Padding;

        X = offsetX;
        Y = offsetY;
        if (!DrawInline) Y += mc.fontRenderer.FONT_HEIGHT;

        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int displayHeight = sr.getScaledHeight();
        int displayWidth = sr.getScaledWidth();

        if (X + totalWidth > displayWidth) X = Math.max(0, displayWidth - totalWidth);
        if (Y + totalHeight > displayHeight) {
            int yAbove = offsetY - totalHeight - 2;
            if (yAbove >= 0) Y = yAbove;
            else Y = displayHeight - totalHeight;
        }
        isDirty = false;
    }

    public void draw() {
        drawRect(X, Y, X + Width + 2 * Padding, Y + Height + 2 * Padding, Background);
        if (Config.BorderWidth > 0) {
            int right = X + Width + 2 * Padding;
            int bottom = Y + Height + 2 * Padding;
            drawRect(X, Y, right, Y + Config.BorderWidth, Config.BorderColor);
            drawRect(X, bottom - Config.BorderWidth, right, bottom, Config.BorderColor);
            drawRect(X, Y, X + Config.BorderWidth, bottom, Config.BorderColor);
            drawRect(right - Config.BorderWidth, Y, right, bottom, Config.BorderColor);
        }
    }

    public void setPos(int x, int y) {
        if (offsetX == x && offsetY == y) return;
        offsetX = x;
        offsetY = y;
        isDirty = true;
    }
}
