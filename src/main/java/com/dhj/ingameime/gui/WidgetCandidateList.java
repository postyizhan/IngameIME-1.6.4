package com.dhj.ingameime.gui;

import com.dhj.ingameime.config.Config;
import net.minecraft.client.Minecraft;

import java.util.List;

public class WidgetCandidateList extends Widget {
    private final CandidateEntry drawItem = new CandidateEntry();
    private List<String> Candidates = null;
    private int Selected = -1;

    public WidgetCandidateList() {
        DrawInline = false;
    }

    @Override
    protected void updateThemeColors() {
        super.updateThemeColors();
        Padding = Config.CandidatePadding;
    }

    public void setContent(List<String> candidates, int selected) {
        Candidates = candidates;
        Selected = selected;
        isDirty = true;
    }

    @Override
    public boolean isActive() {
        return Candidates != null && !Candidates.isEmpty();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        updateThemeColors();
        Height = Width = 0;
        if (!isActive()) {
            isDirty = false;
            return;
        }
        Height = drawItem.getTotalHeight();
        int total = 0;
        int index = 1;
        for (String s : Candidates) {
            drawItem.setIndex(index++);
            drawItem.setText(s);
            total += drawItem.getTotalWidth();
        }
        Width = total;
        super.layout();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        if (isDirty) layout();
        super.draw();

        int drawX = X + Padding;
        int drawY = Y + Padding;
        int index = 1;
        for (String s : Candidates) {
            drawItem.setIndex(index++);
            drawItem.setText(s);
            int entryWidth = drawItem.getTotalWidth();
            boolean isSelected = (index - 1) == (Selected + 1);
            if (isSelected) {
                drawRect(drawX, Y, drawX + entryWidth, Y + Height + (Padding * 2), Config.SelectedBackgroundColor);
            }
            drawItem.draw(drawX, drawY, TextColor);
            drawX += entryWidth;
        }
    }

    private static final class CandidateEntry {
        private final Minecraft mc = Minecraft.getMinecraft();
        private String text = "";
        private int index = 0;
        /** 序号区宽度与字体无关索引变化，每帧重算 getStringWidth("00") 没意义。-1 表示待缓存。 */
        private int indexAreaWidth = -1;

        private int getIndexAreaWidth() {
            if (indexAreaWidth < 0) indexAreaWidth = mc.fontRenderer.getStringWidth("00") + 5;
            return indexAreaWidth;
        }

        void setText(String text) {
            this.text = text == null ? "" : text;
        }

        void setIndex(int index) {
            this.index = index;
        }

        int getTextWidth() {
            return mc.fontRenderer.getStringWidth(text);
        }

        int getTotalWidth() {
            return 2 + getIndexAreaWidth() + getTextWidth() + 2;
        }

        int getTotalHeight() {
            return mc.fontRenderer.FONT_HEIGHT;
        }

        void draw(int x, int y, int textColor) {
            int offsetX = x + 2;
            String idx = Integer.toString(index);
            int indexAreaW = getIndexAreaWidth();
            int idxTextW = mc.fontRenderer.getStringWidth(idx);
            int centeredX = offsetX + (indexAreaW - idxTextW) / 2;
            mc.fontRenderer.drawString(idx, centeredX, y, Config.IndexColor);
            offsetX += indexAreaW;
            mc.fontRenderer.drawString(text, offsetX, y, textColor);
        }
    }
}
