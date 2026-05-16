package com.dhj.ingameime.core;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Forge;
import com.dhj.ingameime.Internal;
import com.dhj.ingameime.control.VanillaTextFieldControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public final class IngameIMEHooks {
    private static final long OVERLAY_LOG_INTERVAL_MS = 1000L;
    private static boolean imeActivatedBeforeFullscreen;
    private static long lastOverlayVerboseLog;

    private IngameIMEHooks() {
    }

    public static void onGuiTextFieldSetFocused(Object textField, boolean focused) {
        IngameIME_Forge.logVerboseInfo("GuiTextField focus hook: focused={}, class={}", Boolean.valueOf(focused), textField == null ? "null" : textField.getClass().getName());
        if (focused && !ClientProxy.hasOpenScreen()) {
            return;
        }
        VanillaTextFieldControl.onFocusChange(textField, focused);
    }

    public static void onGuiTextFieldSetFocused(GuiTextField textField, boolean focused) {
        onGuiTextFieldSetFocused((Object) textField, focused);
    }

    public static boolean shouldSuppressGuiTextFieldKeyTyped(Object textField, char typedChar, int keyCode) {
        return Internal.shouldSuppressGuiTextFieldKeyTyped(textField, typedChar, keyCode);
    }

    public static boolean shouldSuppressGuiTextFieldKeyTyped(GuiTextField textField, char typedChar, int keyCode) {
        return shouldSuppressGuiTextFieldKeyTyped((Object) textField, typedChar, keyCode);
    }

    public static void onGuiScreenKeyTyped(char typedChar, int keyCode) {
        Internal.onGuiScreenKeyTyped(typedChar, keyCode);
    }

    public static String filterAllowedCharacters(String text) {
        return Internal.filterAllowedCharacters(text);
    }

    public static void onGuiScreenClosing() {
        if (ClientProxy.INSTANCE != null) ClientProxy.INSTANCE.onScreenClose();
    }

    public static void onGuiScreenDisplayed(Object screen) {
        if (ClientProxy.INSTANCE != null && screen != null) ClientProxy.INSTANCE.onScreenOpen(screen);
    }

    public static void onGuiScreenDisplayed(GuiScreen screen) {
        onGuiScreenDisplayed((Object) screen);
    }

    public static void beforeToggleFullscreen() {
        imeActivatedBeforeFullscreen = Internal.getActivated();
        Internal.destroyInputCtx();
    }

    public static void afterToggleFullscreen() {
        Internal.setActivated(imeActivatedBeforeFullscreen);
    }

    public static void renderOverlay() {
        if (ClientProxy.INSTANCE != null) {
            if (ClientProxy.Screen.PreEdit.isActive()) {
                long now = System.currentTimeMillis();
                if (now - lastOverlayVerboseLog >= OVERLAY_LOG_INTERVAL_MS) {
                    lastOverlayVerboseLog = now;
                    Minecraft mc = Minecraft.getMinecraft();
                    IngameIME_Forge.logVerboseInfo("Overlay hook fired: preeditActive={}, activated={}, screen={}, content='{}', cursor={}",
                            Boolean.valueOf(ClientProxy.Screen.PreEdit.isActive()),
                            Boolean.valueOf(Internal.getActivated()),
                            mc == null || mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName(),
                            ClientProxy.Screen.PreEdit.getContentForDebug(),
                            Integer.valueOf(ClientProxy.Screen.PreEdit.getCursorForDebug()));
                }
            }
            ClientProxy.INSTANCE.drawOverlay();
        }
    }
}
