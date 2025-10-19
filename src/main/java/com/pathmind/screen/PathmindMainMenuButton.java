package com.pathmind.screen;

import com.pathmind.PathmindMod;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A small icon button used on the title screen to open the Pathmind visual editor.
 */
public class PathmindMainMenuButton extends ButtonWidget {
    private static final Identifier ICON_TEXTURE = PathmindMod.id("textures/icon.png");
    private static final int ICON_TEXTURE_SIZE = 128;
    private static final int ICON_PADDING = 2;
    private static final Text OPEN_EDITOR_TEXT = Text.translatable("gui.pathmind.open_editor");

    public PathmindMainMenuButton(int x, int y, int size, PressAction pressAction) {
        super(x, y, size, size, Text.empty(), pressAction, DEFAULT_NARRATION_SUPPLIER);
        this.setTooltip(Tooltip.of(OPEN_EDITOR_TEXT));
        this.setMessage(Text.empty());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        int iconSize = this.width - ICON_PADDING * 2;
        int iconX = this.getX() + ICON_PADDING;
        int iconY = this.getY() + ICON_PADDING;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize,
                ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, OPEN_EDITOR_TEXT);
    }
}
