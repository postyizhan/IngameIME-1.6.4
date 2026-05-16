package com.dhj.ingameime.control;

import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class BookControl extends AbstractControl<Object> {
    public BookControl(Object control) {
        super(control);
    }

    @Override
    public void writeText(String text) throws IOException {
        if (text == null || text.length() == 0) return;
        try {
            if (((Boolean) getField(controlObject, "editingTitle", "field_74172_m", "p")).booleanValue()) {
                writeTitle(text);
            } else {
                Method append = getMethod(controlObject, "func_74160_b", "func_74160_b", "b", new Class[]{String.class});
                append.invoke(controlObject, new Object[]{text});
            }
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write text to book");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private void writeTitle(String text) throws Exception {
        String old = (String) getField(controlObject, "bookTitle", "field_74176_t", "w");
        if (old == null) old = "";
        int room = 16 - old.length();
        if (room <= 0) return;
        String insert = text.length() > room ? text.substring(0, room) : text;
        setField(controlObject, "bookTitle", "field_74176_t", "w", old + insert);
        setField(controlObject, "bookModified", "field_74166_d", "e", Boolean.TRUE);
        Method updateButtons = getMethod(controlObject, "updateButtons", "func_74161_g", "h", new Class[0]);
        updateButtons.invoke(controlObject, new Object[0]);
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Point getCursorPos() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            FontRenderer font = mc.fontRenderer;
            int screenWidth = ((Integer) getField(controlObject, "width", "field_73880_f", "g")).intValue();
            int left = (screenWidth - ((Integer) getField(controlObject, "bookImageWidth", "field_74171_o", "r")).intValue()) / 2;
            int top = 2;
            if (((Boolean) getField(controlObject, "editingTitle", "field_74172_m", "p")).booleanValue()) {
                String title = (String) getField(controlObject, "bookTitle", "field_74176_t", "w");
                if (title == null) title = "";
                return new Point(left + 36 + 58 + font.getStringWidth(title) / 2, top + 48);
            }

            String page = getCurrentPageText();
            String[] lines = page.split("\n", -1);
            String last = lines.length == 0 ? "" : lines[lines.length - 1];
            int line = Math.max(0, lines.length - 1);
            return new Point(left + 36 + font.getStringWidth(last) + 4, top + 32 + line * font.FONT_HEIGHT);
        } catch (Throwable t) {
            IngameIME_Forge.LOG.log(Level.WARNING, "Failed to get book cursor position", t);
            return new Point(0, 0);
        }
    }

    private String getCurrentPageText() throws Exception {
        NBTTagList pages = (NBTTagList) getField(controlObject, "bookPages", "field_74177_s", "v");
        int currPage = ((Integer) getField(controlObject, "currPage", "field_74179_q", "t")).intValue();
        if (pages == null || currPage < 0 || currPage >= pages.tagCount()) return "";
        NBTTagString page = (NBTTagString) pages.tagAt(currPage);
        return getTagStringData(page);
    }

    private static String getTagStringData(NBTTagString tag) throws Exception {
        if (tag == null) return "";
        String value = (String) getField(tag, "data", "field_74751_a", "a");
        return value == null ? "" : value;
    }

    private static Object getField(Object object, String deobfName, String srgName, String obfName) throws Exception {
        Field field = findField(object.getClass(), new String[]{deobfName, srgName, obfName});
        field.setAccessible(true);
        return field.get(object);
    }

    private static void setField(Object object, String deobfName, String srgName, String obfName, Object value) throws Exception {
        Field field = findField(object.getClass(), new String[]{deobfName, srgName, obfName});
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Field findField(Class cls, String[] names) throws NoSuchFieldException {
        NoSuchFieldException last = null;
        for (int i = 0; i < names.length; i++) {
            try {
                return findField(cls, names[i]);
            } catch (NoSuchFieldException e) {
                last = e;
            }
        }
        throw last == null ? new NoSuchFieldException() : last;
    }

    private static Field findField(Class cls, String name) throws NoSuchFieldException {
        Class current = cls;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method getMethod(Object object, String deobfName, String srgName, String obfName, Class[] argTypes) throws Exception {
        Class current = object.getClass();
        Exception last = null;
        while (current != null) {
            String[] names = new String[]{deobfName, srgName, obfName};
            for (int i = 0; i < names.length; i++) {
                try {
                    Method method = current.getDeclaredMethod(names[i], argTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    last = e;
                }
            }
            current = current.getSuperclass();
        }
        throw last == null ? new NoSuchMethodException() : last;
    }
}
