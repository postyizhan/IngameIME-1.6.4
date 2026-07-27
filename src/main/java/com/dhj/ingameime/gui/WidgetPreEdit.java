package com.dhj.ingameime.gui;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.config.Config;
import net.minecraft.Minecraft;
import net.minecraft.FontRenderer;
import net.minecraft.ScaledResolution;

public class WidgetPreEdit extends Widget {
    private final int CursorWidth = 3;
    private String Content = null;
    private int Cursor = -1;
    private int CaretHeight = 0;

    public void setContent(String content, int cursor) {
        Cursor = cursor;
        Content = content;
        isDirty = true;
        layout();
    }

    public String getContentForDebug() {
        return Content;
    }

    public int getCursorForDebug() {
        return Cursor;
    }

    public void setCaretPos(int x, int y, int height) {
        boolean changed = offsetX != x || offsetY != y || CaretHeight != height;
        offsetX = x;
        offsetY = y;
        CaretHeight = Math.max(1, height);
        if (changed) isDirty = true;
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        updateThemeColors();
        if (isActive()) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            Width = font.getStringWidth(Content) + CursorWidth;
            Height = font.FONT_HEIGHT;
        } else {
            Width = Height = 0;
        }

        X = offsetX - Padding;
        Y = getFloatingY();
        keepInsideDisplay();
        isDirty = false;
    }

    @Override
    public boolean isActive() {
        return Content != null && Content.length() > 0;
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        if (isDirty) layout();

        WidgetCandidateList list = ClientProxy.Screen.CandidateList;
        if (ClientProxy.Screen.shouldDrawCandidateList() && list != null && list.isActive()) {
            Minecraft mc = Minecraft.getMinecraft();
            list.DrawInline = true;
            list.setPos(X, getTargetY(list, mc));
        }
        keepInsideDisplay();
        super.draw();

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String beforeCursor = "";
        String afterCursor = "";
        if (Content != null && Cursor >= 0 && Cursor <= Content.length()) {
            beforeCursor = Content.substring(0, Cursor);
            afterCursor = Content.substring(Cursor);
        } else if (Content != null) {
            beforeCursor = Content;
        }

        int x = font.drawString(beforeCursor, X + Padding, Y + Padding, TextColor);
        drawRect(x + 1, Y + Padding, x + 2, Y + Padding + Height, Config.CursorColor);
        font.drawString(afterCursor, x + CursorWidth, Y + Padding, TextColor);
    }

    private int getFloatingY() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int displayHeight = sr.getScaledHeight();
        int totalHeight = Height + 2 * Padding;
        int margin = 2;
        int aboveY = offsetY - totalHeight - margin;
        int caretHeight = CaretHeight > 0 ? CaretHeight : mc.fontRenderer.FONT_HEIGHT;
        int belowY = offsetY + caretHeight + margin;

        // Chat and other bottom-aligned inputs put the caret near the bottom edge. Prefer
        // a floating preedit widget above the caret there, so pinyin does not cover the
        // existing input text. Upper/mid-screen fields (for example creative search) prefer
        // below the caret/input line, so the preedit overlay does not cover existing text.
        // Native IME/candidate positioning still uses the real caret coordinates from
        // ClientProxy/Internal; this only moves the rendered overlay.
        boolean nearBottom = offsetY >= displayHeight - (mc.fontRenderer.FONT_HEIGHT * 3 + totalHeight);
        if (nearBottom && aboveY >= 0) return aboveY;
        if (!nearBottom && belowY + totalHeight <= displayHeight) return belowY;
        if (aboveY >= 0) return aboveY;
        if (belowY + totalHeight <= displayHeight) return belowY;
        return Math.max(0, Math.min(offsetY - Padding, displayHeight - totalHeight));
    }

    private void keepInsideDisplay() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int displayHeight = sr.getScaledHeight();
        int displayWidth = sr.getScaledWidth();
        int totalWidth = Width + 2 * Padding;
        int totalHeight = Height + 2 * Padding;
        if (X + totalWidth > displayWidth) X = Math.max(0, displayWidth - totalWidth);
        if (X < 0) X = 0;
        if (Y + totalHeight > displayHeight) Y = Math.max(0, displayHeight - totalHeight);
        if (Y < 0) Y = 0;
    }

    private int getTargetY(WidgetCandidateList list, Minecraft mc) {
        int myTotalHeight = Height + 2 * Padding;
        int listExpectedHeight = (list.Height > 0 ? list.Height : mc.fontRenderer.FONT_HEIGHT) + 2 * list.Padding;
        ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int displayHeight = sr.getScaledHeight();
        int targetY = Y + myTotalHeight;
        if (targetY + listExpectedHeight >= displayHeight - 5) targetY = Y - listExpectedHeight;
        if (targetY < 0) targetY = 0;
        if (targetY + listExpectedHeight > displayHeight) targetY = Math.max(0, displayHeight - listExpectedHeight);
        return targetY;
    }
}
