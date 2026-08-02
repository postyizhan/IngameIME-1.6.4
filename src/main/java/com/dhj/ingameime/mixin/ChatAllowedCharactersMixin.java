package com.dhj.ingameime.mixin;

import com.dhj.ingameime.Internal;
import net.minecraft.ChatAllowedCharacters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 放宽原版的可输入字符白名单。
 *
 * 原版只允许 ASCII 里一小段可打印字符，中文会被整段丢掉；这里改成"除控制字符与 §
 * 之外都放行"。对应原 Forge 版 transformChatAllowedCharacters()（那边是整体重写方法体，
 * 这里用 cancellable 注入覆盖返回值，效果相同）。
 */
@Mixin(ChatAllowedCharacters.class)
public class ChatAllowedCharactersMixin {
    @Inject(method = "isAllowedCharacter", at = @At("HEAD"), cancellable = true)
    private static void ingameime$isAllowedCharacter(char character, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(character != 0 && character != 167 && !Character.isISOControl(character));
    }

    @Inject(method = "filerAllowedCharacters", at = @At("HEAD"), cancellable = true)
    private static void ingameime$filerAllowedCharacters(String text, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(Internal.filterAllowedCharacters(text));
    }
}
