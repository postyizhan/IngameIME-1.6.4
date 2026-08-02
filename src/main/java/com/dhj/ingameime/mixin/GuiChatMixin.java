package com.dhj.ingameime.mixin;

import com.dhj.ingameime.ClientProxy;
import net.minecraft.GuiChat;
import net.xiaoyu233.fml.util.ReflectHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class GuiChatMixin {
	@Inject(method = "<init>()V", at = @At("TAIL"))
	private void compatVanillaChatActionOpen(CallbackInfo ci) {
		ClientProxy.INSTANCE.onScreenOpen(ReflectHelper.dyCast(this));
	}

	@Inject(method = "<init>(Ljava/lang/String;)V", at = @At("TAIL"))
	private void compatVanillaChatActionOpenWithString(CallbackInfo ci) {
		ClientProxy.INSTANCE.onScreenOpen(ReflectHelper.dyCast(this));
	}

	@Inject(method = "onGuiClosed", at = @At("TAIL"))
	private void compatVanillaChatActionClose(CallbackInfo ci) {
		ClientProxy.INSTANCE.onScreenClose();
	}
}
