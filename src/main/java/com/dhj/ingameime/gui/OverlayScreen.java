package com.dhj.ingameime.gui;

import com.dhj.ingameime.Internal;
import com.dhj.ingameime.config.Config;
import net.minecraft.Minecraft;
import org.lwjgl.opengl.GL11;

public class OverlayScreen extends Widget {
    public WidgetPreEdit PreEdit = new WidgetPreEdit();
    public WidgetCandidateList CandidateList = new WidgetCandidateList();
    public WidgetInputMode WInputMode = new WidgetInputMode();

    @Override
    public boolean isActive() {
        return Minecraft.getMinecraft().currentScreen != null && Internal.getActivated();
    }

    @Override
    public void layout() {
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glTranslatef(0, 0, 300f);

            CandidateList.DrawInline = false;
            PreEdit.draw();
            if (shouldDrawCandidateList()) {
                CandidateList.draw();
            }
            WInputMode.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    public boolean shouldDrawCandidateList() {
        Minecraft mc = Minecraft.getMinecraft();
        return Config.UiLess_Windows || (mc != null && mc.isFullScreen());
    }

    public void setCaretPos(int x, int y, int height) {
        PreEdit.setCaretPos(x, y, height);
        CandidateList.setPos(x, y);
        WInputMode.setPos(x, y);
    }
}
