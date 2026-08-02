package com.dhj.ingameime.mixin;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Fish;
import com.dhj.ingameime.Internal;
import net.minecraft.EntityRenderer;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 候选窗/预编辑覆盖层的绘制点。
 *
 * 原 Forge 版注入在 Minecraft.runGameLoop() 里 checkGLError("Post render") 之前，
 * 但那个位置在 Display.update()（缓冲交换）之后。这里改注入 updateCameraAndRender()
 * 的 RETURN：该方法尾部刚画完 currentScreen，且仍在交换缓冲之前，覆盖层必然可见。
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Unique
    private static final long ingameime$OVERLAY_LOG_INTERVAL_MS = 1000L;
    @Unique
    private static long ingameime$lastOverlayVerboseLog;

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void ingameime$renderOverlay(float partialTicks, CallbackInfo ci) {
        if (ClientProxy.INSTANCE == null) return;
        if (ClientProxy.Screen.PreEdit.isActive()) {
            long now = System.currentTimeMillis();
            if (now - ingameime$lastOverlayVerboseLog >= ingameime$OVERLAY_LOG_INTERVAL_MS) {
                ingameime$lastOverlayVerboseLog = now;
                Minecraft mc = Minecraft.getMinecraft();
                IngameIME_Fish.logVerboseInfo("Overlay hook fired: preeditActive={}, activated={}, screen={}, content='{}', cursor={}",
                        Boolean.valueOf(ClientProxy.Screen.PreEdit.isActive()),
                        Boolean.valueOf(Internal.getActivated()),
                        mc == null || mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName(),
                        ClientProxy.Screen.PreEdit.getContentForDebug(),
		                ClientProxy.Screen.PreEdit.getCursorForDebug());
            }
        }
        ClientProxy.INSTANCE.drawOverlay();
    }
}
