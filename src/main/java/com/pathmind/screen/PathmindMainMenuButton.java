package com.pathmind.screen;

import com.pathmind.PathmindMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A small icon button used on the title screen to open the Pathmind visual editor.
 */
public class PathmindMainMenuButton extends ButtonWidget {
    private static final Identifier ICON_TEXTURE = PathmindMod.id("textures/gui/icon.png");
    private static final int ICON_PADDING = 2;
    private static final int ICON_TEXTURE_SIZE = 128;

    public PathmindMainMenuButton(int x, int y, int size, PressAction pressAction) {
        super(x, y, size, size, Text.translatable("gui.pathmind.open_editor"), pressAction, DEFAULT_NARRATION_SUPPLIER);
        this.setTooltip(Tooltip.of(Text.translatable("gui.pathmind.open_editor")));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        int iconSize = this.width - ICON_PADDING * 2;
        int iconX = this.getX() + ICON_PADDING;
        int iconY = this.getY() + ICON_PADDING;

        context.drawTexture(ICON_TEXTURE, iconX, iconY, 0, 0, iconSize, iconSize, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
    }
}
