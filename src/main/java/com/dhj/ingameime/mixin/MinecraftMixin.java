package com.dhj.ingameime.mixin;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.Internal;
import net.minecraft.GuiScreen;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 界面切换、全屏切换与客户端 tick 末尾。
 *
 * 对应原 Forge 版 IngameIMETransformer.transformMinecraft() 注入的三处钩子，
 * 外加原先由 TickRegistry 提供的 CLIENT tickEnd。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    public GuiScreen currentScreen;
    @Unique
    private static boolean ingameime$activatedBeforeFullscreen;

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void ingameime$onGuiScreenClosing(GuiScreen screen, CallbackInfo ci) {
        if (ClientProxy.INSTANCE != null) ClientProxy.INSTANCE.onScreenClose();
    }

    @Inject(method = "displayGuiScreen", at = @At("RETURN"))
    private void ingameime$onGuiScreenDisplayed(GuiScreen screen, CallbackInfo ci) {
        if (ClientProxy.INSTANCE == null) return;
        // 不能用入参 screen：
        // 1. 原版会在方法体内把它重赋值为 GuiMainMenu/GuiGameOver（传 null 时）；
        // 2. MITE 额外加了一个提前 return（幽灵状态下打开 GuiInventory），
        //    那条路径上界面并未真正打开。
        // 读回写后的 currentScreen 同时避开这两个坑。
        if (this.currentScreen != null) ClientProxy.INSTANCE.onScreenOpen(this.currentScreen);
    }

    /**
     * 全屏切换会重建窗口，HWND 随之失效，必须销毁并重建 InputContext。
     */
    @Inject(method = "toggleFullscreen", at = @At("HEAD"))
    private void ingameime$beforeToggleFullscreen(CallbackInfo ci) {
        ingameime$activatedBeforeFullscreen = Internal.getActivated();
        Internal.destroyInputCtx();
    }

    @Inject(method = "toggleFullscreen", at = @At("RETURN"))
    private void ingameime$afterToggleFullscreen(CallbackInfo ci) {
        Internal.setActivated(ingameime$activatedBeforeFullscreen);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void ingameime$onClientTickEnd(CallbackInfo ci) {
        ClientProxy.onClientTickEnd();
    }
}
