package com.dhj.ingameime.control;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class VanillaTextFieldControl<T> extends AbstractControl<T> {
    private static final Class GUI_TEXT_FIELD_CLASS = GuiTextField.class;

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
            return ((Boolean) call(controlObject, new String[]{"getVisible", "func_73806_l"})).booleanValue();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public Point getCursorPos() {
        try {
            FontRenderer font = (FontRenderer) getField(controlObject, "fontRenderer", "field_73815_a");
            int x = ((Integer) getField(controlObject, "xPos", "field_73813_b")).intValue();
            int y = ((Integer) getField(controlObject, "yPos", "field_73814_c")).intValue();
            int width = ((Integer) getField(controlObject, "width", "field_73811_d")).intValue();
            int height = ((Integer) getField(controlObject, "height", "field_73812_e")).intValue();
            int lineScrollOffset = ((Integer) getField(controlObject, "lineScrollOffset", "field_73816_n")).intValue();
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

    private static Object getField(Object object, String deobfName, String srgName) throws Exception {
        try {
            Field field = GUI_TEXT_FIELD_CLASS.getDeclaredField(deobfName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException ignored) {
            Field field = GUI_TEXT_FIELD_CLASS.getDeclaredField(srgName);
            field.setAccessible(true);
            return field.get(object);
        }
    }

    private static void setField(Object object, String deobfName, String srgName, Object value) throws Exception {
        try {
            Field field = GUI_TEXT_FIELD_CLASS.getDeclaredField(deobfName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException ignored) {
            Field field = GUI_TEXT_FIELD_CLASS.getDeclaredField(srgName);
            field.setAccessible(true);
            field.set(object, value);
        }
    }

    private static Object call(Object object, String[] names, Class[] argTypes, Object[] args) throws Exception {
        Exception last = null;
        for (int i = 0; i < names.length; i++) {
            try {
                java.lang.reflect.Method method = GUI_TEXT_FIELD_CLASS.getDeclaredMethod(names[i], argTypes);
                method.setAccessible(true);
                return method.invoke(object, args);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last;
    }

    private static Object call(Object object, String[] names) throws Exception {
        return call(object, names, new Class[0], new Object[0]);
    }

    private static String getText(Object field) throws Exception {
        return (String) call(field, new String[]{"getText", "func_73781_b"});
    }

    private static int getCursorPosition(Object field) throws Exception {
        return ((Integer) call(field, new String[]{"getCursorPosition", "func_73799_h"})).intValue();
    }

    private static int getSelectionEnd(Object field) throws Exception {
        return ((Integer) call(field, new String[]{"getSelectionEnd", "func_73787_n"})).intValue();
    }

    private static int getMaxStringLength(Object field) throws Exception {
        return ((Integer) call(field, new String[]{"getMaxStringLength", "func_73808_g"})).intValue();
    }

    private static boolean getEnableBackgroundDrawing(Object field) throws Exception {
        return ((Boolean) call(field, new String[]{"getEnableBackgroundDrawing", "func_73783_i"})).booleanValue();
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

            setField(field, "text", "field_73809_f", newText);
            setField(field, "cursorPosition", "field_73817_o", Integer.valueOf(newCursor));
            // Keep both deobfuscated and SRG selection fields in sync before asking vanilla to recalc scroll.
            setField(field, "selectionEnd", "field_73826_p", Integer.valueOf(newCursor));
            call(field, new String[]{"setSelectionPos", "func_73800_i"}, new Class[]{Integer.TYPE}, new Object[]{Integer.valueOf(newCursor)});
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write raw text to GuiTextField");
            ioe.initCause(t);
            throw ioe;
        }
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
