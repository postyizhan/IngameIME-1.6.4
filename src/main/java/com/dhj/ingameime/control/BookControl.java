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
    private static final Class GUI_SCREEN_BOOK_CLASS = GuiScreenBook.class;
    private static final Field EDITING_TITLE = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"editingTitle", "field_74172_m", "p"});
    private static final Field BOOK_TITLE = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"bookTitle", "field_74176_t", "w"});
    private static final Field BOOK_MODIFIED = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"bookModified", "field_74166_d", "e"});
    private static final Field WIDTH = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"width", "field_73880_f", "g"});
    private static final Field BOOK_IMAGE_WIDTH = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"bookImageWidth", "field_74171_o", "r"});
    private static final Field BOOK_PAGES = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"bookPages", "field_74177_s", "v"});
    private static final Field CURR_PAGE = findField(GUI_SCREEN_BOOK_CLASS, new String[]{"currPage", "field_74179_q", "t"});
    private static final Field TAG_STRING_DATA = findField(NBTTagString.class, new String[]{"data", "field_74751_a", "a"});
    private static final Method APPEND_TEXT = findMethod(GUI_SCREEN_BOOK_CLASS, new String[]{"func_74160_b", "func_74160_b", "b"}, new Class[]{String.class});
    private static final Method UPDATE_BUTTONS = findMethod(GUI_SCREEN_BOOK_CLASS, new String[]{"updateButtons", "func_74161_g", "h"}, new Class[0]);

    public BookControl(Object control) {
        super(control);
    }

    @Override
    public void writeText(String text) throws IOException {
        if (text == null || text.length() == 0) return;
        try {
            if (((Boolean) getField(controlObject, EDITING_TITLE)).booleanValue()) {
                writeTitle(text);
            } else {
                APPEND_TEXT.invoke(controlObject, new Object[]{text});
            }
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write text to book");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private void writeTitle(String text) throws Exception {
        String old = (String) getField(controlObject, BOOK_TITLE);
        if (old == null) old = "";
        int room = 16 - old.length();
        if (room <= 0) return;
        String insert = text.length() > room ? text.substring(0, room) : text;
        setField(controlObject, BOOK_TITLE, old + insert);
        setField(controlObject, BOOK_MODIFIED, Boolean.TRUE);
        UPDATE_BUTTONS.invoke(controlObject, new Object[0]);
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
            int screenWidth = ((Integer) getField(controlObject, WIDTH)).intValue();
            int left = (screenWidth - ((Integer) getField(controlObject, BOOK_IMAGE_WIDTH)).intValue()) / 2;
            int top = 2;
            if (((Boolean) getField(controlObject, EDITING_TITLE)).booleanValue()) {
                String title = (String) getField(controlObject, BOOK_TITLE);
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
        NBTTagList pages = (NBTTagList) getField(controlObject, BOOK_PAGES);
        int currPage = ((Integer) getField(controlObject, CURR_PAGE)).intValue();
        if (pages == null || currPage < 0 || currPage >= pages.tagCount()) return "";
        NBTTagString page = (NBTTagString) pages.tagAt(currPage);
        return getTagStringData(page);
    }

    private static String getTagStringData(NBTTagString tag) throws Exception {
        if (tag == null) return "";
        String value = (String) getField(tag, TAG_STRING_DATA);
        return value == null ? "" : value;
    }

    private static Object getField(Object object, Field field) throws Exception {
        return field.get(object);
    }

    private static void setField(Object object, Field field, Object value) throws Exception {
        field.set(object, value);
    }

    private static Field findField(Class cls, String[] names) {
        NoSuchFieldException last = null;
        for (int i = 0; i < names.length; i++) {
            try {
                Field field = findField(cls, names[i]);
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

    private static Method findMethod(Class cls, String[] names, Class[] argTypes) {
        Class current = cls;
        Exception last = null;
        while (current != null) {
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
        RuntimeException runtimeException = new RuntimeException("Failed to find method");
        runtimeException.initCause(last == null ? new NoSuchMethodException() : last);
        throw runtimeException;
    }
}
