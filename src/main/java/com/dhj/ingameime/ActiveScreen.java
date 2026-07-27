package com.dhj.ingameime;

import net.minecraft.GuiScreen;
import net.minecraft.Minecraft;

/**
 * 「当前是否有可输入界面」的唯一判定入口。
 *
 * 不能只看 Minecraft.currentScreen：MITE 的聊天走的是自己那套 imposed chat——
 * {@code Minecraft.openChat(GuiChat)} 只把界面塞进 {@code imposed_gui_chat} 字段并
 * 调 setWorldAndResolution，**完全不经过 displayGuiScreen，也不写 currentScreen**
 * （见 closeImposedChat / isChatImposed / getOpenChatGui）。
 *
 * 于是原来以 currentScreen != null 作判据的地方，在聊天打开时全部误判为"没有界面"：
 *   - hasOpenScreen() 返回 false，IMStates 的 onControlFocus 直接 return，输入法永不激活；
 *   - clientTickEnd() 每 tick 调 ensureInactiveForGameplay() 把输入法强制按死；
 *   - OverlayScreen.isActive() 返回 false，候选框/预编辑也不会画。
 * 表现就是"聊天框里只能打英文"，而创建世界名、告示牌、书与笔都正常。
 *
 * 这里统一取 currentScreen 与 imposed chat 的并集。
 */
public final class ActiveScreen {
    private ActiveScreen() {
    }

    /** 当前正在接受输入的界面，优先返回常规 currentScreen，其次是 MITE 的 imposed chat。 */
    public static GuiScreen get() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return null;
        if (mc.currentScreen != null) return mc.currentScreen;
        return mc.imposed_gui_chat;
    }

    public static boolean isOpen() {
        return get() != null;
    }
}
