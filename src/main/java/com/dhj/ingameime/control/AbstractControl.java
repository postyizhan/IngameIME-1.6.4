package com.dhj.ingameime.control;

import com.dhj.ingameime.ActiveScreen;
import com.dhj.ingameime.IngameIME_Fish;
import com.dhj.ingameime.mixin.GuiScreenAccessor;
import net.minecraft.FontRenderer;
import net.minecraft.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Point;
import java.io.IOException;

public abstract class AbstractControl<T> implements IControl {
    protected final T controlObject;

    public AbstractControl(T controlObject) {
        this.controlObject = controlObject;
    }

    @Override
    public T getControlObject() {
        return controlObject;
    }

    @Override
    public void writeText(String text) throws IOException {
        writeCurrentScreenText(text);
    }

    /**
     * 兜底文本输入：没有具体 control 时，把提交的文本逐字符喂给当前界面。
     */
    public static void writeCurrentScreenText(String text) {
        // 用 ActiveScreen 而非 currentScreen：MITE 的聊天是 imposed chat，不写 currentScreen。
        GuiScreen screen = ActiveScreen.get();
        if (screen == null || text == null) return;
        try {
            for (int i = 0; i < text.length(); i++) {
                ((GuiScreenAccessor) screen).ingameime$keyTyped(text.charAt(i), Keyboard.KEY_NONE);
            }
        } catch (Throwable t) {
            IngameIME_Fish.LOG.error("Failed to feed committed text to current screen", t);
        }
    }

    protected static Point getCursorPos(FontRenderer font, String text, int x, int y, int width, int height,
                                       int lineScrollOffset, int cursorPosition, int selectionEnd,
                                       boolean enableBackgroundDrawing) {
        if (text == null) text = "";
        if (lineScrollOffset < 0) lineScrollOffset = 0;
        if (lineScrollOffset > text.length()) lineScrollOffset = text.length();
        String visibleText = font.trimStringToWidth(text.substring(lineScrollOffset), width);
        int cursorY = (enableBackgroundDrawing ? y + (height - 8) / 2 : y) - 1;

        int cursorPosRelative = cursorPosition - lineScrollOffset;
        int selectionEndRelative = selectionEnd - lineScrollOffset;
        int currentDrawX = enableBackgroundDrawing ? x + 4 : x;

        if (cursorPosRelative < 0) cursorPosRelative = 0;
        if (cursorPosRelative > visibleText.length()) cursorPosRelative = visibleText.length();
        if (selectionEndRelative > visibleText.length()) selectionEndRelative = visibleText.length();

        if (visibleText.length() > 0) {
            if (selectionEndRelative != cursorPosRelative) {
                return new Point(currentDrawX - 1, cursorY);
            }
            currentDrawX += font.getStringWidth(visibleText.substring(0, cursorPosRelative));
        }

        return new Point(currentDrawX - 1, cursorY);
    }
}
