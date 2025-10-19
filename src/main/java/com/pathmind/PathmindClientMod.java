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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client-side mod class for Pathmind.
 * This class initializes client-specific features and event handlers.
 */
public class PathmindClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathmind/Client");
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
        if (client.player == null) {
            return;
        }
        
        // Check if visual editor keybind was pressed
        while (PathmindKeybinds.OPEN_VISUAL_EDITOR.wasPressed()) {
            if (client.currentScreen == null) {
                // Only open if no screen is currently open
                client.setScreen(new PathmindVisualEditorScreen());
            }
            // If screen is already open, do nothing (don't close it)
        }

        while (PathmindKeybinds.PLAY_GRAPHS.wasPressed()) {
            ExecutionManager.getInstance().playAllGraphs();
        }
    }
}
