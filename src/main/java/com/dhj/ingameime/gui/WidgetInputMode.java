package com.dhj.ingameime.gui;

import com.dhj.ingameime.config.Config;
import ingameime.InputMode;

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
        Height = OverlayFont.fontHeight();
        Width = OverlayFont.getStringWidth(modeText());
        super.layout();
    }

    private String modeText() {
        return Mode == InputMode.AlphaNumeric ? Config.AlphaModeText : Config.NativeModeText;
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        if (isDirty) layout();
        super.draw();
        OverlayFont.drawString(modeText(), X + Padding, Y + Padding, TextColor);
    }
}
