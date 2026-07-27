package com.dhj.ingameime.mixin;

import com.dhj.ingameime.ClientProxy;
import net.minecraft.GuiChat;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 的 imposed chat 生命周期。
 *
 * MITE 不用 displayGuiScreen 打开聊天，而是 {@code openChat(GuiChat)} 直接写
 * {@code imposed_gui_chat} 字段；关闭走 {@code closeImposedChat()}。因此
 * MinecraftMixin 里挂在 displayGuiScreen 上的那两个钩子对聊天完全不触发，
 * 状态机收不到「界面已打开」，输入法也就不会激活。
 *
 * 这里补上等价的两个通知。注意顺序：
 *   - openChat 用 RETURN——它内部会调 setWorldAndResolution -> initGui，
 *     GuiChat.initGui 里会 setFocused(true)，进而触发 GuiTextFieldMixin 的焦点钩子。
 *     必须等这些跑完再通知 onScreenOpen，否则 hasOpenScreen() 仍看不到 imposed 字段
 *     （该字段在方法入口处才刚写入，但 initGui 期间焦点钩子已经在跑了，
 *     所以字段先写入这一点很关键——实测 openChat 第 0-2 条指令就是 putfield）。
 *   - closeImposedChat 用 HEAD——此时字段还在，语义上等价于「界面即将关闭」。
 */
@Mixin(Minecraft.class)
public class MinecraftImposedChatMixin {
    @Inject(method = "openChat", at = @At("RETURN"))
    private void ingameime$onImposedChatOpened(GuiChat chat, CallbackInfo ci) {
        if (ClientProxy.INSTANCE != null && chat != null) ClientProxy.INSTANCE.onScreenOpen(chat);
    }

    @Inject(method = "closeImposedChat", at = @At("HEAD"))
    private void ingameime$onImposedChatClosing(CallbackInfo ci) {
        if (ClientProxy.INSTANCE != null) ClientProxy.INSTANCE.onScreenClose();
    }
}
