package com.pathmind.ui;

import java.util.function.Consumer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A small icon-only button that renders the Pathmind logo texture and exposes
 * standard hover and narration behaviour so it behaves like other title screen buttons.
 */
public class PathmindIconButton extends PressableWidget {
    private static final int ICON_TEXTURE_SIZE = 128;

    private final Identifier texture;
    private final Consumer<PathmindIconButton> onPress;

    public PathmindIconButton(int x, int y, int size, Identifier texture, Consumer<PathmindIconButton> onPress, Text tooltip) {
        super(x, y, size, size, tooltip);
        this.texture = texture;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onPress() {
        this.onPress.accept(this);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(
            this.texture,
            this.getX(),
            this.getY(),
            this.width,
            this.height,
            0,
            0,
            ICON_TEXTURE_SIZE,
            ICON_TEXTURE_SIZE,
            ICON_TEXTURE_SIZE,
            ICON_TEXTURE_SIZE
        );

        if (this.isHovered()) {
            int highlightColor = 0x40FFFFFF;
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, highlightColor);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }
}
