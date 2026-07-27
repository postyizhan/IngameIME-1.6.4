package com.dhj.ingameime.mixin;

import com.dhj.ingameime.Internal;
import net.minecraft.GuiContainerCreative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模式搜索框：记录 ESC/Enter/Tab 这类控制键，供后续 keyTyped 判断是否为
 * 输入法送来的按键名序列。对应原 Forge 版 transformGuiContainerCreative()。
 */
@Mixin(GuiContainerCreative.class)
public class GuiContainerCreativeMixin {
    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void ingameime$onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        Internal.onGuiScreenKeyTyped(typedChar, keyCode);
    }
}
