package com.dhj.ingameime.control;

import java.awt.Point;
import java.io.IOException;

public interface IControl {
    Object getControlObject();

    void writeText(String text) throws IOException;

    boolean isVisible();

    Point getCursorPos();
}
