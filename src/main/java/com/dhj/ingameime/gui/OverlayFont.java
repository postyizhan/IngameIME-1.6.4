package com.dhj.ingameime.gui;

import net.minecraft.FontRenderer;
import net.minecraft.Minecraft;

/**
 * 覆盖层文本的测量与绘制入口，统一走 unicode 字形路径。
 *
 * 为什么必须这样做：原版 FontRenderer 在 unicodeFlag=false 时，走的是
 * "在 ChatAllowedCharacters.allowedCharacters 里取字符下标，再用该下标索引仅 256 项的
 * charWidth[] / ascii.png 图集" 这条路（见 getCharWidth 与 renderCharAtPos）。
 * 原版 allowedCharacters 只有 ASCII，CJK 字符找不到下标（indexOf 返回 -1），会自动落到
 * renderUnicodeChar 分支，所以原版渲染中文不会出事。
 *
 * 但 FishModLoader 的 fix.AllowedCharFix 把 allowedCharacters 换成了 font.txt 里那份
 * 包含 CJK 的大字符集，于是 "中" 这类字符**能**查到下标（例如 16469），下标随即越界：
 *
 *   java.lang.ArrayIndexOutOfBoundsException: Index 16469 out of bounds for length 256
 *       at net.minecraft.FontRenderer.getCharWidth
 *
 * unicodeFlag=true 时这两处都直接走 renderUnicodeChar / glyphWidth[]，不再碰那个 256 项数组。
 * 游戏 jar 自带全部 222 张 unicode_page_*.png 与 glyph_sizes.bin，因此该路径能正常出字。
 *
 * 只在本模组自己的绘制期间临时置位并复原，不影响原版界面的字体外观。
 */
public final class OverlayFont {
    private OverlayFont() {
    }

    public static FontRenderer font() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc == null ? null : mc.fontRenderer;
    }

    public static int getStringWidth(String text) {
        FontRenderer font = font();
        if (font == null || text == null || text.length() == 0) return 0;
        boolean previous = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        try {
            return font.getStringWidth(text);
        } finally {
            font.setUnicodeFlag(previous);
        }
    }

    public static int drawString(String text, int x, int y, int color) {
        FontRenderer font = font();
        if (font == null || text == null) return x;
        boolean previous = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        try {
            return font.drawString(text, x, y, color);
        } finally {
            font.setUnicodeFlag(previous);
        }
    }

    public static int fontHeight() {
        FontRenderer font = font();
        return font == null ? 9 : font.FONT_HEIGHT;
    }

    /**
     * 按宽度裁剪字符串。与上面同理，必须在 unicodeFlag=true 下测量，
     * 否则 trimStringToWidth 内部调用 getCharWidth 时同样会越界。
     */
    public static String trimStringToWidth(FontRenderer font, String text, int width) {
        if (font == null || text == null) return text;
        boolean previous = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        try {
            return font.trimStringToWidth(text, width);
        } finally {
            font.setUnicodeFlag(previous);
        }
    }

    /** 供控件定位光标用：在 unicode 路径下测量宽度。 */
    public static int getStringWidth(FontRenderer font, String text) {
        if (font == null || text == null || text.length() == 0) return 0;
        boolean previous = font.getUnicodeFlag();
        font.setUnicodeFlag(true);
        try {
            return font.getStringWidth(text);
        } finally {
            font.setUnicodeFlag(previous);
        }
    }
}
