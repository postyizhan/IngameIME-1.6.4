package com.dhj.ingameime;

import com.dhj.ingameime.config.Config;
import com.dhj.ingameime.control.IControl;
import com.dhj.ingameime.gui.OverlayScreen;
import net.minecraft.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Point;

/**
 * 客户端状态机入口。
 *
 * 原 Forge 版实现 ITickHandler 并注册到 TickRegistry；FML 没有 tick 注册表，
 * 客户端 tick 末尾由 MinecraftMixin 注入 Minecraft.runTick() 的 RETURN 处直接调用
 * {@link #onClientTickEnd()}。
 */
public class ClientProxy implements IMEventHandler {
    private static final long OVERLAY_LOG_INTERVAL_MS = 1000L;

    public static ClientProxy INSTANCE = null;
    public static OverlayScreen Screen = new OverlayScreen();

    private static final int KEY_BIND_CODE = Keyboard.KEY_NONE;
    private static IMEventHandler IMEventHandler = IMStates.Disabled;
    private static boolean IsKeyDown = false;
    private long lastOverlayVerboseLog = 0L;

    public ClientProxy() {
        INSTANCE = this;
    }

    public static void init() {
        new ClientProxy();
        Internal.loadLibrary();
        // Display/HWND 在入口触发时还不存在（Minecraft 尚未构造），
        // InputContext 延迟到首次激活时创建。
    }

    public static IMEventHandler getIMEventHandler() {
        return IMEventHandler;
    }

    public void drawOverlay() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean preeditActive = Screen.PreEdit.isActive();
        boolean hasScreen = mc != null && mc.currentScreen != null;
        boolean updatedPreEditRect = updateNativePreEditRectFromActiveControl();
        boolean shouldDraw = (updatedPreEditRect && hasScreen) || (preeditActive && hasScreen);
        if (preeditActive) {
            long now = System.currentTimeMillis();
            if (now - lastOverlayVerboseLog >= OVERLAY_LOG_INTERVAL_MS) {
                lastOverlayVerboseLog = now;
                IngameIME_Fish.logVerboseInfo("Overlay draw check: shouldDraw={}, updatedRect={}, hasScreen={}, overlayActive={}, activated={}, screen={}, pos=({},{}), size={}x{}, content='{}', cursor={}",
                        Boolean.valueOf(shouldDraw), Boolean.valueOf(updatedPreEditRect), Boolean.valueOf(hasScreen),
                        Boolean.valueOf(Screen.isActive()), Boolean.valueOf(Internal.getActivated()),
                        hasScreen ? mc.currentScreen.getClass().getName() : "null",
                        Integer.valueOf(Screen.PreEdit.X), Integer.valueOf(Screen.PreEdit.Y),
                        Integer.valueOf(Screen.PreEdit.Width), Integer.valueOf(Screen.PreEdit.Height),
                        Screen.PreEdit.getContentForDebug(), Integer.valueOf(Screen.PreEdit.getCursorForDebug()));
            }
        }
        if (shouldDraw) {
            Screen.draw();
        }
    }

    private boolean updateNativePreEditRectFromActiveControl() {
        if (!Internal.getActivated()) return false;
        IControl control = IMStates.getActiveControl();
        if (control == null || !control.isVisible() || control.getControlObject() == null) return false;

        Point position = control.getCursorPos();
        if (position == null || position.x < 0 || position.y < 0) {
            IngameIME_Fish.logVerboseInfo("Skipped PreEditRect update for invalid active control cursor: {}", position);
            return false;
        }

        int caretHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        Screen.setCaretPos(position.x, position.y, caretHeight);
        Internal.updatePreEditRectFromGui(position.x, position.y, 1, caretHeight);
        return true;
    }

    /** 由 MinecraftMixin 在 runTick() 末尾调用，等价于原 Forge 版的 TickType.CLIENT tickEnd。 */
    public static void onClientTickEnd() {
        if (INSTANCE != null) INSTANCE.clientTickEnd();
    }

    private void clientTickEnd() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            if (Internal.getActivated()) {
                IngameIME_Fish.logDebugInfo("Force deactivating IME because currentScreen is null");
                onScreenClose();
            }
            Internal.ensureInactiveForGameplay();
        }
        updateNativePreEditRectFromActiveControl();
        Internal.drainCallbackQueue();
        Internal.drainCommitQueue();
        if (KEY_BIND_CODE != Keyboard.KEY_NONE && Keyboard.isKeyDown(KEY_BIND_CODE)) {
            IsKeyDown = true;
        } else if (IsKeyDown) {
            IsKeyDown = false;
            onToggleKey();
        }
        if (Mouse.getDX() != 0 || Mouse.getDY() != 0) {
            onMouseMove();
        }
    }

    @Override
    public IMStates onScreenClose() {
        IMEventHandler newEventHandler = IMEventHandler.onScreenClose();
        changeState(newEventHandler);
        return null;
    }

    @Override
    public IMStates onControlFocus(IControl control, boolean focused, boolean isOverlay) {
        IMEventHandler newEventHandler = IMEventHandler.onControlFocus(control, focused, isOverlay);
        changeState(newEventHandler);
        return null;
    }

    @Override
    public IMStates onScreenOpen(Object screen) {
        IngameIME_Fish.logDebugInfo("Screen opened {}", screen);
        IMEventHandler newEventHandler = IMEventHandler.onScreenOpen(screen);
        changeState(newEventHandler);
        return null;
    }

    @Override
    public IMStates onToggleKey() {
        IMEventHandler newEventHandler = IMEventHandler.onToggleKey();
        changeState(newEventHandler);
        return null;
    }

    @Override
    public IMStates onMouseMove() {
        IMEventHandler newEventHandler = IMEventHandler.onMouseMove();
        changeState(newEventHandler);
        return null;
    }

    @Override
    public void onLeaveState() {
    }

    @Override
    public void onGetState() {
    }

    public static boolean hasOpenScreen() {
        return Minecraft.getMinecraft().currentScreen != null;
    }

    private static void changeState(IMEventHandler newEventHandler) {
        if (newEventHandler != null && newEventHandler != IMEventHandler) {
            IMEventHandler.onLeaveState();
            IMEventHandler = newEventHandler;
            IMEventHandler.onGetState();
        }
    }
}
