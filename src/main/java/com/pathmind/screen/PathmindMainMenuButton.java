package com.pathmind.screen;

import com.pathmind.PathmindMod;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A small icon button used on the title screen to open the Pathmind visual editor.
 */
public class PathmindMainMenuButton extends PressableWidget {
    private static final Identifier ICON_TEXTURE = Identifier.of(PathmindMod.MOD_ID, "icon.png");
    private static final int HOVER_BORDER_COLOR = 0xFFFFFFFF;
    private static final int HOVER_BACKGROUND_COLOR = 0x40FFFFFF;

    private final PressAction pressAction;

    public PathmindMainMenuButton(int x, int y, int size, PressAction pressAction) {
        super(x, y, size, size, Text.translatable("gui.pathmind.open_editor"));
        this.pressAction = pressAction;
        this.setTooltip(Tooltip.of(Text.translatable("gui.pathmind.open_editor")));
    }

    @Override
    public void onPress() {
        this.pressAction.onPress(this);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) {
            context.fill(getX(), getY(), getX() + this.width, getY() + this.height, HOVER_BACKGROUND_COLOR);
        }

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, getX(), getY(), this.width, this.height);

        if (this.isHovered()) {
            context.drawBorder(getX(), getY(), this.width, this.height, HOVER_BORDER_COLOR);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(PathmindMainMenuButton button);
    }
}
