package com.dhj.ingameime.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Method;

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

    public static void writeCurrentScreenText(String text) throws IOException {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null || text == null) return;
        try {
            Method keyTyped;
            try {
                keyTyped = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
            } catch (NoSuchMethodException ignored) {
                keyTyped = GuiScreen.class.getDeclaredMethod("func_73869_a", char.class, int.class);
            }
            keyTyped.setAccessible(true);
            for (int i = 0; i < text.length(); i++) {
                keyTyped.invoke(screen, Character.valueOf(text.charAt(i)), Integer.valueOf(Keyboard.KEY_NONE));
            }
        } catch (Exception e) {
            IOException ioe = new IOException("Failed to invoke GuiScreen.keyTyped");
            ioe.initCause(e);
            throw ioe;
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
