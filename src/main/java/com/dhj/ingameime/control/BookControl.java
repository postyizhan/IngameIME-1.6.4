package com.dhj.ingameime.control;

import com.dhj.ingameime.IngameIME_Fish;
import net.minecraft.Minecraft;
import net.minecraft.FontRenderer;
import net.minecraft.GuiScreenBook;
import net.minecraft.NBTTagList;
import net.minecraft.NBTTagString;

import java.awt.Point;
import java.io.IOException;

/**
 * 成书编辑界面。
 *
 * width 来自 GuiScreen、NBTTagString.data 本来就是 public；其余成员(editingTitle/bookTitle/
 * bookModified/bookImageWidth/bookPages/currPage 与 func_74160_b/updateButtons)
 * 由 AccessWidener 放宽。func_74160_b 是原版的"向当前页追加文本"，MCP 未给出可读名。
 */
public class BookControl extends AbstractControl<Object> {
    public BookControl(Object control) {
        super(control);
    }

    private GuiScreenBook screen() {
        return (GuiScreenBook) controlObject;
    }

    @Override
    public void writeText(String text) throws IOException {
        if (text == null || text.length() == 0) return;
        try {
            GuiScreenBook screen = screen();
            if (screen.editingTitle) {
                writeTitle(screen, text);
            } else {
                screen.func_74160_b(text);
            }
        } catch (Throwable t) {
            IOException ioe = new IOException("Failed to write text to book");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private void writeTitle(GuiScreenBook screen, String text) {
        String old = screen.bookTitle;
        if (old == null) old = "";
        int room = 16 - old.length();
        if (room <= 0) return;
        String insert = text.length() > room ? text.substring(0, room) : text;
        screen.bookTitle = old + insert;
        screen.bookModified = true;
        screen.updateButtons();
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Point getCursorPos() {
        try {
            GuiScreenBook screen = screen();
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int left = (screen.width - screen.bookImageWidth) / 2;
            int top = 2;
            if (screen.editingTitle) {
                String title = screen.bookTitle;
                if (title == null) title = "";
                return new Point(left + 36 + 58 + font.getStringWidth(title) / 2, top + 48);
            }

            String page = getCurrentPageText(screen);
            String[] lines = page.split("\n", -1);
            String last = lines.length == 0 ? "" : lines[lines.length - 1];
            int line = Math.max(0, lines.length - 1);
            return new Point(left + 36 + font.getStringWidth(last) + 4, top + 32 + line * font.FONT_HEIGHT);
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("Failed to get book cursor position", t);
            return new Point(0, 0);
        }
    }

    private static String getCurrentPageText(GuiScreenBook screen) {
        NBTTagList pages = screen.bookPages;
        int currPage = screen.currPage;
        if (pages == null || currPage < 0 || currPage >= pages.tagCount()) return "";
        NBTTagString page = (NBTTagString) pages.tagAt(currPage);
        return page == null || page.data == null ? "" : page.data;
    }
}
