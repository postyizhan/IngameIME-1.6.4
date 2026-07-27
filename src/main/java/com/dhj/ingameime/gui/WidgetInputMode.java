package com.dhj.ingameime.gui;

import com.dhj.ingameime.config.Config;
import ingameime.InputMode;
import net.minecraft.Minecraft;
import net.minecraft.FontRenderer;

public class WidgetInputMode extends Widget {
    public final long ActiveTime = 3000;
    private long LastActive = 0;
    private InputMode Mode = InputMode.AlphaNumeric;

    public WidgetInputMode() {
        DrawInline = false;
    }

    @Override
    public boolean isActive() {
        return System.currentTimeMillis() - LastActive <= ActiveTime;
    }

    public void setActive(boolean active) {
        if (active) LastActive = System.currentTimeMillis();
        else LastActive = 0;
    }

    public void setMode(InputMode mode) {
        Mode = mode;
        setActive(true);
        isDirty = true;
        layout();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        updateThemeColors();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        Height = font.FONT_HEIGHT;
        Width = font.getStringWidth(Mode == InputMode.AlphaNumeric ? Config.AlphaModeText : Config.NativeModeText);
        super.layout();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        if (isDirty) layout();
        super.draw();
        Minecraft.getMinecraft().fontRenderer.drawString(
                Mode == InputMode.AlphaNumeric ? Config.AlphaModeText : Config.NativeModeText,
                X + Padding, Y + Padding, TextColor
        );
    }
}
