package com.dhj.ingameime;

import com.dhj.ingameime.config.Config;
import com.dhj.ingameime.control.BookControl;
import com.dhj.ingameime.control.IControl;
import com.dhj.ingameime.control.NoControl;
import com.dhj.ingameime.control.SignControl;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiEditSign;

public enum IMStates implements IMEventHandler {
    Disabled {
        @Override
        public IMStates onScreenOpen(Object screen) {
            if (screen instanceof GuiEditSign || hasClassName(screen, "axy")) {
                setControl(new SignControl(screen), false);
                Internal.setActivated(true);
                return OpenedInternal;
            }
            if (screen instanceof GuiScreenBook || hasClassName(screen, "axf")) {
                setControl(new BookControl(screen), false);
                Internal.setActivated(true);
                return OpenedInternal;
            }
            return this;
        }

        @Override
        public IMStates onControlFocus(IControl control, boolean focused, boolean isOverlay) {
            if (focused && !ClientProxy.hasOpenScreen()) return this;
            if (focused) {
                setControl(control, isOverlay);
                IngameIME_Forge.logDebugInfo("Opened by control focus: {}", control.getClass().getSimpleName());
                Internal.setActivated(true);
                return OpenedAuto;
            }
            return this;
        }

        @Override
        public IMStates onToggleKey() {
            IngameIME_Forge.logDebugInfo("Turned on by toggle key");
            Internal.setActivated(true);
            return OpenedManual;
        }
    },
    OpenedInternal {
        @Override
        public void onLeaveState() {
            NoControl.NO_CONTROL.setCursorX(0);
            NoControl.NO_CONTROL.setCursorY(0);
            setControl(NoControl.NO_CONTROL, false);
        }
    },
    OpenedManual {
        @Override
        public IMStates onMouseMove() {
            if (!Config.TurnOffOnMouseMove) return this;
            Internal.setActivated(false);
            IngameIME_Forge.logDebugInfo("Turned off by mouse move");
            return Disabled;
        }
    },
    OpenedAuto {
        @Override
        public IMStates onControlFocus(IControl control, boolean focused, boolean isOverlay) {
            if (focused && !ClientProxy.hasOpenScreen()) return this;
            Object object = control.getControlObject();
            boolean changed = !isControlObject(object, isOverlay);
            if (!focused) {
                if (!changed) {
                    Internal.setActivated(false);
                    setControl(NoControl.NO_CONTROL, isOverlay);
                    if (IMStates.getActiveControl() != NoControl.NO_CONTROL) {
                        Internal.setActivated(true);
                        return this;
                    }
                    IngameIME_Forge.logDebugInfo("Turned off by losing control focus: {}", control.getClass().getSimpleName());
                    return Disabled;
                }
                return this;
            }

            if (changed) Internal.setActivated(false);
            setControl(control, isOverlay);
            if (changed) IngameIME_Forge.logDebugInfo("Opened by control focus: {}", control.getClass().getSimpleName());
            Internal.setActivated(true);
            ClientProxy.Screen.WInputMode.setActive(true);
            return this;
        }
    };

    @Override
    public IMStates onControlFocus(IControl control, boolean focused, boolean isOverlay) {
        if (focused && !ClientProxy.hasOpenScreen()) return this;
        if (focused) {
            setControl(control, isOverlay);
        } else if (isControlObject(control.getControlObject(), isOverlay)) {
            setControl(NoControl.NO_CONTROL, isOverlay);
        }
        return this;
    }

    @Override
    public IMStates onScreenClose() {
        Internal.setActivated(false);
        setControl(NoControl.NO_CONTROL, false);
        setControl(NoControl.NO_CONTROL, true);
        return Disabled;
    }

    @Override
    public IMStates onScreenOpen(Object screen) {
        return this;
    }

    @Override
    public IMStates onMouseMove() {
        return this;
    }

    @Override
    public IMStates onToggleKey() {
        IngameIME_Forge.logDebugInfo("Turned off by toggle key");
        Internal.setActivated(false);
        return Disabled;
    }

    @Override
    public void onLeaveState() {
    }

    @Override
    public void onGetState() {
    }

    private static IControl CommonControl = NoControl.NO_CONTROL;
    private static IControl OverlayControl = NoControl.NO_CONTROL;

    public static void setControl(IControl control, boolean isOverlay) {
        if (isOverlay) OverlayControl = control;
        else CommonControl = control;
    }

    public static boolean isControlObject(Object controlObject, boolean isOverlay) {
        return isOverlay ? OverlayControl.getControlObject() == controlObject : CommonControl.getControlObject() == controlObject;
    }

    private static boolean hasClassName(Object object, String name) {
        return object != null && name.equals(object.getClass().getName());
    }

    public static IControl getActiveControl() {
        IMEventHandler eventHandler = ClientProxy.getIMEventHandler();
        if (eventHandler == IMStates.OpenedManual) return NoControl.NO_CONTROL;
        return OverlayControl == NoControl.NO_CONTROL ? CommonControl : OverlayControl;
    }
}
