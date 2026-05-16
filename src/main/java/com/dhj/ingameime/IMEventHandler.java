package com.dhj.ingameime;

import com.dhj.ingameime.control.IControl;
public interface IMEventHandler {
    IMStates onScreenClose();

    IMStates onScreenOpen(Object screen);

    IMStates onControlFocus(IControl control, boolean focused, boolean isOverlay);

    IMStates onToggleKey();

    IMStates onMouseMove();

    void onLeaveState();

    void onGetState();
}
