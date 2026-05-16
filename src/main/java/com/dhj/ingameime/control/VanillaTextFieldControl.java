package com.dhj.ingameime.control;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class VanillaTextFieldControl<T> extends AbstractControl<T> {
    private static final Class GUI_TEXT_FIELD_CLASS = GuiTextField.class;
    private static final Field FONT_RENDERER = findField(new String[]{"fontRenderer", "field_73815_a"});
    private static final Field X_POS = findField(new String[]{"xPos", "field_73813_b"});
    private static final Field Y_POS = findField(new String[]{"yPos", "field_73814_c"});
    private static final Field WIDTH = findField(new String[]{"width", "field_73811_d"});
    private static final Field HEIGHT = findField(new String[]{"height", "field_73812_e"});
    private static final Field LINE_SCROLL_OFFSET = findField(new String[]{"lineScrollOffset", "field_73816_n"});
    private static final Field TEXT = findField(new String[]{"text", "field_73809_f"});
    private static final Field CURSOR_POSITION = findField(new String[]{"cursorPosition", "field_73817_o"});
    private static final Field SELECTION_END = findField(new String[]{"selectionEnd", "field_73826_p"});
    private static final Method GET_VISIBLE = findMethod(new String[]{"getVisible", "func_73806_l"}, new Class[0]);
    private static final Method GET_TEXT = findMethod(new String[]{"getText", "func_73781_b"}, new Class[0]);
    private static final Method GET_CURSOR_POSITION = findMethod(new String[]{"getCursorPosition", "func_73799_h"}, new Class[0]);
    private static final Method GET_SELECTION_END = findMethod(new String[]{"getSelectionEnd", "func_73787_n"}, new Class[0]);
    private static final Method GET_MAX_STRING_LENGTH = findMethod(new String[]{"getMaxStringLength", "func_73808_g"}, new Class[0]);
    private static final Method GET_ENABLE_BACKGROUND_DRAWING = findMethod(new String[]{"getEnableBackgroundDrawing", "func_73783_i"}, new Class[0]);
    private static final Method SET_SELECTION_POS = findMethod(new String[]{"setSelectionPos", "func_73800_i"}, new Class[]{Integer.TYPE});

    protected VanillaTextFieldControl(T control) {
        super(control);
    }

    @Override
    public void writeText(String text) throws IOException {
        writeRawText(controlObject, text);
    }

    @Override
    public boolean isVisible() {
        try {
            return ((Boolean) call(controlObject, GET_VISIBLE)).booleanValue();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public Point getCursorPos() {
        try {
            FontRenderer font = (FontRenderer) getField(controlObject, FONT_RENDERER);
            int x = ((Integer) getField(controlObject, X_POS)).intValue();
            int y = ((Integer) getField(controlObject, Y_POS)).intValue();
            int width = ((Integer) getField(controlObject, WIDTH)).intValue();
            int height = ((Integer) getField(controlObject, HEIGHT)).intValue();
            int lineScrollOffset = ((Integer) getField(controlObject, LINE_SCROLL_OFFSET)).intValue();
            return AbstractControl.getCursorPos(
                    font, getText(controlObject), x, y, width, height,
                    lineScrollOffset, getCursorPosition(controlObject), getSelectionEnd(controlObject),
                    getEnableBackgroundDrawing(controlObject)
            );
        } catch (Throwable t) {
            IngameIME_Forge.LOG.log(Level.WARNING, "Failed to get GuiTextField cursor position", t);
            return new Point(0, 0);
        }
    }

    private static Object getField(Object object, Field field) throws Exception {
        return field.get(object);
    }

    private static void setField(Object object, Field field, Object value) throws Exception {
        field.set(object, value);
    }

    private static Object call(Object object, Method method, Object[] args) throws Exception {
        return method.invoke(object, args);
    }

    private static Object call(Object object, Method method) throws Exception {
        return call(object, method, new Object[0]);
    }

    private static String getText(Object field) throws Exception {
        return (String) call(field, GET_TEXT);
    }

    private static int getCursorPosition(Object field) throws Exception {
        return ((Integer) call(field, GET_CURSOR_POSITION)).intValue();
    }

    private static int getSelectionEnd(Object field) throws Exception {
        return ((Integer) call(field, GET_SELECTION_END)).intValue();
    }

    private static int getMaxStringLength(Object field) throws Exception {
        return ((Integer) call(field, GET_MAX_STRING_LENGTH)).intValue();
    }

    private static boolean getEnableBackgroundDrawing(Object field) throws Exception {
        return ((Boolean) call(field, GET_ENABLE_BACKGROUND_DRAWING)).booleanValue();
    }

    public static void writeRawText(Object field, String input) throws IOException {
        if (field == null || input == null || input.length() == 0) return;
        try {
            String oldText = getText(field);
            int cursor = getCursorPosition(field);
            int selection = getSelectionEnd(field);
            int start = Math.min(cursor, selection);
            int end = Math.max(cursor, selection);
            int maxLength = getMaxStringLength(field);

            int room = maxLength - oldText.length() + (end - start);
            if (room <= 0) return;
            String insert = input.length() > room ? input.substring(0, room) : input;
            String newText = oldText.substring(0, start) + insert + oldText.substring(end);
            int newCursor = start + insert.length();
            IngameIME_Forge.logVerboseInfo("GuiTextField raw write old='{}' input='{}' insert='{}' new='{}' cursor={} selection={}", oldText, input, insert, newText, Integer.valueOf(cursor), Integer.valueOf(selection));

            setField(field, TEXT, newText);
            setField(field, CURSOR_POSITION, Integer.valueOf(newCursor));
            // Keep both deobfuscated and SRG selection fields in sync before asking vanilla to recalc scroll.
            setField(field, SELECTION_END, Integer.valueOf(newCursor));
            call(field, SET_SELECTION_POS, new Object[]{Integer.valueOf(newCursor)});
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write raw text to GuiTextField");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private static Field findField(String[] names) {
        NoSuchFieldException last = null;
        for (int i = 0; i < names.length; i++) {
            try {
                Field field = GUI_TEXT_FIELD_CLASS.getDeclaredField(names[i]);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                last = e;
            }
        }
        RuntimeException runtimeException = new RuntimeException("Failed to find field");
        runtimeException.initCause(last == null ? new NoSuchFieldException() : last);
        throw runtimeException;
    }

    private static Method findMethod(String[] names, Class[] argTypes) {
        Exception last = null;
        for (int i = 0; i < names.length; i++) {
            try {
                Method method = GUI_TEXT_FIELD_CLASS.getDeclaredMethod(names[i], argTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                last = e;
            }
        }
        RuntimeException runtimeException = new RuntimeException("Failed to find method");
        runtimeException.initCause(last == null ? new NoSuchMethodException() : last);
        throw runtimeException;
    }

    public static boolean onFocusChange(Object object, boolean focused) {
        if (ClientProxy.INSTANCE != null) {
            ClientProxy.INSTANCE.onControlFocus(new VanillaTextFieldControl<Object>(object), focused, false);
        }
        return true;
    }

    public static boolean onFocusChange(GuiTextField object, boolean focused) {
        return onFocusChange((Object) object, focused);
    }
}
