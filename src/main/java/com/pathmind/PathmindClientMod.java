package com.pathmind;

import com.pathmind.data.PresetManager;
import com.pathmind.execution.ExecutionManager;
import com.pathmind.screen.PathmindVisualEditorScreen;
import com.pathmind.ui.ActiveNodeOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client-side mod class for Pathmind.
 * This class initializes client-specific features and event handlers.
 */
public class PathmindClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathmind/Client");
    private ActiveNodeOverlay activeNodeOverlay;
    private boolean titleScreenShortcutHeld;

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

    private void handleKeybinds(MinecraftClient client) {
        // Check if visual editor keybind was pressed
        while (PathmindKeybinds.OPEN_VISUAL_EDITOR.wasPressed()) {
            if (!(client.currentScreen instanceof PathmindVisualEditorScreen)
                    && (client.currentScreen == null || client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen)) {
                client.setScreen(new PathmindVisualEditorScreen());
            }
        }

        boolean onTitleScreen = client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen;
        if (onTitleScreen && client.getWindow() != null) {
            int boundKeyCode = PathmindKeybinds.OPEN_VISUAL_EDITOR.getBoundKey().getCode();
            boolean isPressed = boundKeyCode != InputUtil.UNKNOWN_KEY.getCode()
                    && InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKeyCode);
            if (isPressed && !this.titleScreenShortcutHeld && !(client.currentScreen instanceof PathmindVisualEditorScreen)) {
                client.setScreen(new PathmindVisualEditorScreen());
            }
            this.titleScreenShortcutHeld = isPressed;
        } else if (!onTitleScreen) {
            this.titleScreenShortcutHeld = false;
        }

        if (client.player == null) {
            return;
        }

        while (PathmindKeybinds.PLAY_GRAPHS.wasPressed()) {
            ExecutionManager.getInstance().playAllGraphs();
        }
    }
}
