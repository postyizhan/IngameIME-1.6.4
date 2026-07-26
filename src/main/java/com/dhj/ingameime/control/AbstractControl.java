package com.dhj.ingameime.control;

import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public abstract class AbstractControl<T> implements IControl {
    /**
     * GuiScreen.keyTyped 是全项目唯一保留反射的入口。
     *
     * 不用 AccessTransformer 放宽它：GuiScreen 的大量子类各自以 protected 覆写 keyTyped，
     * 把父类改成 public 会让 dev 依赖重编译时因“缩小可见性”而失败。而且这只是兜底
     * 路径（没有具体 control 时逐字符喂给当前界面），句柄静态缓存一次，开销可忽略。
     */
    private static final Method KEY_TYPED = findKeyTyped();

    private static Method findKeyTyped() {
        try {
            Method method;
            try {
                method = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
            } catch (NoSuchMethodException ignored) {
                method = GuiScreen.class.getDeclaredMethod("func_73869_a", char.class, int.class);
            }
            method.setAccessible(true);
            return method;
        } catch (Throwable t) {
            // 不抛异常：静态初始化失败会级联成 NoClassDefFoundError，比降级难排查得多。
            IngameIME_Forge.LOG.log(Level.SEVERE, "Failed to locate GuiScreen.keyTyped, fallback text input disabled", t);
            return null;
        }
    }

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

    public static void writeCurrentScreenText(String text) {
        if (KEY_TYPED == null) return;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null || text == null) return;
        try {
            for (int i = 0; i < text.length(); i++) {
                KEY_TYPED.invoke(screen, Character.valueOf(text.charAt(i)), Integer.valueOf(Keyboard.KEY_NONE));
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
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
