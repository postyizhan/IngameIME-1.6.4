package com.dhj.ingameime.control;

import com.dhj.ingameime.ClientProxy;
import com.dhj.ingameime.IngameIME_Fish;
import net.minecraft.GuiTextField;

import java.awt.Point;

/**
 * GuiTextField 的光标定位。
 *
 * 这里不再有反射：需要的方法(getVisible/getText/getCursorPosition/...)原版就是 public，
 * 私有字段(fontRenderer/xPos/yPos/width/height/lineScrollOffset)由 AccessWidener 放宽,
 * 见 src/main/resources/ingameime.accesswidener。
 *
 * 注意用 field.width 而不是 getWidth()：后者在启用背景绘制时返回 width-8，
 * 与 getCursorPos 期望的裸宽度不符。
 */
public class VanillaTextFieldControl<T> extends AbstractControl<T> {
    public VanillaTextFieldControl(T control) {
        super(control);
    }

    private GuiTextField textField() {
        return (GuiTextField) controlObject;
    }

    @Override
    public boolean isVisible() {
        try {
            return textField().getVisible();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public Point getCursorPos() {
        try {
            GuiTextField field = textField();
            return AbstractControl.getCursorPos(
                    field.fontRenderer, field.getText(), field.xPos, field.yPos,
                    field.width, field.height, field.lineScrollOffset,
                    field.getCursorPosition(), field.getSelectionEnd(),
                    field.getEnableBackgroundDrawing()
            );
        } catch (Throwable t) {
            IngameIME_Fish.LOG.warn("Failed to get GuiTextField cursor position", t);
            return new Point(0, 0);
        }
    }

    public static boolean onFocusChange(Object object, boolean focused) {
        if (ClientProxy.INSTANCE != null) {
            ClientProxy.INSTANCE.onControlFocus(new VanillaTextFieldControl<Object>(object), focused, false);
        }
        return true;
    }

    public static boolean onFocusChange(GuiTextField object, boolean focused) {
        return onFocusChange((Object) object, focused);
    }
}
