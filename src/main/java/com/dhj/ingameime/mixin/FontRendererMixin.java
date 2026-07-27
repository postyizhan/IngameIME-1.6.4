package com.dhj.ingameime.mixin;

import net.minecraft.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修掉 FishModLoader 扩展字符集与原版 ascii 图集之间的越界冲突。
 *
 * 原版 FontRenderer 在 unicodeFlag=false 时这样画字：
 *   idx = ChatAllowedCharacters.allowedCharacters.indexOf(ch)
 *   idx >= 0  ->  用 charWidth[idx + 32] / ascii.png 里第 idx 格图元
 *   idx <  0  ->  落到 renderUnicodeChar / glyphWidth[]（unicode_page_XX.png）
 *
 * 原版 font.txt 恰好 144 个字符，正好铺满 ascii.png，CJK 一律 indexOf==-1 走 unicode 分支，
 * 所以原版渲染中文没问题。
 *
 * 但 FML 的 fix.AllowedCharFix 把 allowedCharacters 换成了它自己的 font.txt——
 * 前 144 个字符与原版完全一致，之后追加了 28013 个 CJK 字符。于是：
 *
 *   - 下标 144..223：不越界但取错图元，中文被画成 ascii.png 里的乱码字形；
 *   - 下标 >= 224：charWidth[idx+32] 直接越界，
 *     java.lang.ArrayIndexOutOfBoundsException: Index 16469 out of bounds for length 256
 *     （"中" 在 FML font.txt 里的下标是 16437，+32 = 16469）
 *
 * 崩溃点遍布 getCharWidth / renderStringAtPos，既能在本模组的候选框上触发，
 * 也能在纯原版界面上触发（语言列表、资源包列表、创建世界界面等），与本模组无关时同样会崩。
 *
 * 这里把两处 indexOf 重定向：下标 >= 144（即落在原版 ascii 图集之外）时返回 -1，
 * 让它走 unicode 字形路径。游戏 jar 自带全部 222 张 unicode_page_*.png 与
 * glyph_sizes.bin，因此 CJK 能正常出字。下标 < 144 的 ASCII/Latin-1 行为完全不变。
 */
@Mixin(FontRenderer.class)
public class FontRendererMixin {
    /** 原版 ascii.png 图集容量：font.txt 的原版长度，也是 charWidth[] 的有效上界(144+32=176 < 256)。 */
    private static final int ingameime$ASCII_ATLAS_SIZE = 144;

    @Redirect(
            method = "getCharWidth",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I")
    )
    private int ingameime$clampCharWidthIndex(String allowedCharacters, int ch) {
        return ingameime$asciiAtlasIndex(allowedCharacters, ch);
    }

    @Redirect(
            method = "renderStringAtPos",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;indexOf(I)I")
    )
    private int ingameime$clampRenderIndex(String allowedCharacters, int ch) {
        return ingameime$asciiAtlasIndex(allowedCharacters, ch);
    }

    private static int ingameime$asciiAtlasIndex(String allowedCharacters, int ch) {
        int index = allowedCharacters.indexOf(ch);
        return index >= ingameime$ASCII_ATLAS_SIZE ? -1 : index;
    }
}
