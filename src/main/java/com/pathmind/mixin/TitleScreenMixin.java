package com.pathmind.mixin;

import com.pathmind.screen.PathmindVisualEditorScreen;
import com.pathmind.ui.PathmindIconButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private static final Identifier PATHMIND_ICON_TEXTURE = Identifier.of("pathmind", "icon.png");

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pathmind$addEditorButton(CallbackInfo ci) {
        int buttonSize = 20;
        int margin = 8;
        int x = this.width - buttonSize - margin;
        int y = margin;

        this.addDrawableChild(new PathmindIconButton(
            x,
            y,
            buttonSize,
            PATHMIND_ICON_TEXTURE,
            button -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (!(client.currentScreen instanceof PathmindVisualEditorScreen)) {
                    client.setScreen(new PathmindVisualEditorScreen());
                }
            },
            Text.translatable("gui.pathmind.open_editor")
        ));
    }
}
