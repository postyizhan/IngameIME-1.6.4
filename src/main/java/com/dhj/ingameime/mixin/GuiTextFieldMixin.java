package com.dhj.ingameime.mixin;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Fish;
import com.dhj.ingameime.Internal;
import com.dhj.ingameime.control.VanillaTextFieldControl;
import net.minecraft.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版文本框的焦点变化与按键过滤。
 *
 * 对应原 Forge 版 IngameIMETransformer.injectGuiTextFieldHooks()。
 */
@Mixin(GuiTextField.class)
public class GuiTextFieldMixin {
    @Inject(method = "setFocused", at = @At("HEAD"))
    private void ingameime$onSetFocused(boolean focused, CallbackInfo ci) {
        IngameIME_Fish.logVerboseInfo("GuiTextField focus hook: focused={}, class={}", Boolean.valueOf(focused), this.getClass().getName());
        if (focused && !ClientProxy.hasOpenScreen()) {
            return;
        }
        VanillaTextFieldControl.onFocusChange(this, focused);
    }

    /**
     * 部分输入法会把按键名（例如创造模式搜索框里的 ESC/CR）当成文本送进来，
     * 命中时直接返回 true 吞掉这次按键。
     */
    @Inject(method = "textboxKeyTyped", at = @At("HEAD"), cancellable = true)
    private void ingameime$filterKeyTyped(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        if (Internal.shouldSuppressGuiTextFieldKeyTyped(this, typedChar, keyCode)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
