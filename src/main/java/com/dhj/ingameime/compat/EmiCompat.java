package com.dhj.ingameime.compat;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IMStates;
import com.dhj.ingameime.IngameIME_Fish;
import com.dhj.ingameime.control.IControl;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.widget.EmiSearchWidget;
import net.xiaoyu233.fml.FishModLoader;

public final class EmiCompat {


    public static boolean isEmiSearchWidget(Object screen) {
        if (!FishModLoader.hasMod("emi")) return false;
        try {
            return screen == EmiScreenManager.search;
        } catch (Throwable t) {
            return false;
        }
    }

    public static IControl wrapSearchControl() {
        return new EmiSearchControl(EmiScreenManager.search);
    }

    public static void tick() {
        if (!FishModLoader.hasMod("emi")) return;
        try {
            EmiSearchWidget search = EmiScreenManager.search;
            if (search == null) return;
            boolean focused = search.isActive();
            boolean isControl = IMStates.isControlObject(search, false);
            if (focused && !isControl) {
                if (ClientProxy.hasOpenScreen()) {
                    ClientProxy.INSTANCE.onScreenOpen(search);
                }
            } else if (!focused && isControl) {
                ClientProxy.INSTANCE.onControlFocus(wrapSearchControl(), false, false);
            }
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("EmiCompat.tick failed", t);
        }
    }
}
