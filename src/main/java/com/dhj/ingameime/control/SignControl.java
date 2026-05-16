package com.dhj.ingameime.control;

import com.dhj.ingameime.IngameIME_Forge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class SignControl extends AbstractControl<Object> {
    private static final Class GUI_EDIT_SIGN_CLASS = GuiEditSign.class;
    private static final Field ENTITY_SIGN = findField(GUI_EDIT_SIGN_CLASS, new String[]{"entitySign", "field_73982_c"});
    private static final Field EDIT_LINE = findField(GUI_EDIT_SIGN_CLASS, new String[]{"editLine", "field_73984_m"});
    private static final Field WIDTH = findField(GUI_EDIT_SIGN_CLASS, new String[]{"width", "field_73880_f"});
    private static final Field HEIGHT = findField(GUI_EDIT_SIGN_CLASS, new String[]{"height", "field_73881_g"});

    public SignControl(Object control) {
        super(control);
    }

    @Override
    public void writeText(String text) throws IOException {
        if (text == null || text.length() == 0) return;
        try {
            TileEntitySign sign = (TileEntitySign) getField(controlObject, ENTITY_SIGN);
            int line = ((Integer) getField(controlObject, EDIT_LINE)).intValue();
            if (sign == null || sign.signText == null || line < 0 || line >= sign.signText.length) return;

            String old = sign.signText[line];
            if (old == null) old = "";
            int room = 15 - old.length();
            if (room <= 0) return;
            String insert = text.length() > room ? text.substring(0, room) : text;
            sign.signText[line] = old + insert;
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write text to sign");
            ioe.initCause(t);
            throw ioe;
        }
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Point getCursorPos() {
        try {
            TileEntitySign sign = (TileEntitySign) getField(controlObject, ENTITY_SIGN);
            int line = ((Integer) getField(controlObject, EDIT_LINE)).intValue();
            String text = sign != null && sign.signText != null && line >= 0 && line < sign.signText.length && sign.signText[line] != null
                    ? sign.signText[line] : "";
            Minecraft mc = Minecraft.getMinecraft();
            FontRenderer font = mc.fontRenderer;
            int screenWidth = ((Integer) getField(controlObject, WIDTH)).intValue();
            int screenHeight = ((Integer) getField(controlObject, HEIGHT)).intValue();
            int x = screenWidth / 2 + font.getStringWidth(text) / 2 + 8;
            int y = screenHeight / 4 + 58 + line * 10;
            return new Point(x, y);
        } catch (Throwable t) {
            IngameIME_Forge.LOG.log(Level.WARNING, "Failed to get sign cursor position", t);
            Minecraft mc = Minecraft.getMinecraft();
            return new Point(mc.currentScreen != null ? mc.currentScreen.width / 2 : 0,
                    mc.currentScreen != null ? mc.currentScreen.height / 2 : 0);
        }
    }

    private static Object getField(Object object, Field field) throws Exception {
        return field.get(object);
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
}
