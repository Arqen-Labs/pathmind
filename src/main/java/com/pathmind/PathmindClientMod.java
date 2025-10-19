package com.pathmind;

import com.pathmind.data.PresetManager;
import com.pathmind.execution.ExecutionManager;
import com.pathmind.mixin.ScreenAccessor;
import com.pathmind.screen.PathmindVisualEditorScreen;
import com.pathmind.ui.ActiveNodeOverlay;
import com.pathmind.ui.PathmindIconButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client-side mod class for Pathmind.
 * This class initializes client-specific features and event handlers.
 */
public class PathmindClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathmind/Client");
    private static final Identifier PATHMIND_ICON_TEXTURE = Identifier.of("pathmind", "icon.png");
    private ActiveNodeOverlay activeNodeOverlay;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Pathmind client mod");

        PresetManager.initialize();

        // Initialize the active node overlay
        this.activeNodeOverlay = new ActiveNodeOverlay();
        
        // Register keybindings
        PathmindKeybinds.registerKeybinds();
        KeyBindingHelper.registerKeyBinding(PathmindKeybinds.OPEN_VISUAL_EDITOR);
        KeyBindingHelper.registerKeyBinding(PathmindKeybinds.PLAY_GRAPHS);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen titleScreen) {
                addTitleScreenButton(client, titleScreen, scaledWidth, scaledHeight);
            }
        });
        
        // Register client tick events for keybind handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds(client);
        });
        
        // Register HUD render callback for the active node overlay
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.textRenderer != null) {
                activeNodeOverlay.render(drawContext, client.textRenderer, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            }
        });
        
        LOGGER.info("Pathmind client mod initialized successfully");
    }

    private void addTitleScreenButton(MinecraftClient client, TitleScreen screen, int scaledWidth, int scaledHeight) {
        int buttonSize = 20;
        int margin = 8;
        int x = scaledWidth - buttonSize - margin;
        int y = margin;

        PathmindIconButton openEditorButton = new PathmindIconButton(
            x,
            y,
            buttonSize,
            PATHMIND_ICON_TEXTURE,
            button -> {
                if (!(client.currentScreen instanceof PathmindVisualEditorScreen)) {
                    client.setScreen(new PathmindVisualEditorScreen());
                }
            },
            Text.translatable("gui.pathmind.open_editor")
        );

        ((ScreenAccessor) screen).pathmind$addDrawableChild(openEditorButton);
    }

    private void handleKeybinds(MinecraftClient client) {
        // Check if visual editor keybind was pressed
        while (PathmindKeybinds.OPEN_VISUAL_EDITOR.wasPressed()) {
            if (!(client.currentScreen instanceof PathmindVisualEditorScreen)
                    && (client.currentScreen == null || client.currentScreen instanceof TitleScreen)) {
                client.setScreen(new PathmindVisualEditorScreen());
            }
        }

        if (client.player == null) {
            return;
        }

        while (PathmindKeybinds.PLAY_GRAPHS.wasPressed()) {
            ExecutionManager.getInstance().playAllGraphs();
        }
    }
}
