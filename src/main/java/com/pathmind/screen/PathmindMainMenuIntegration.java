package com.pathmind.screen;

import com.pathmind.mixin.ScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Hooks into the main menu to add the Pathmind visual editor button and key handling.
 */
public final class PathmindMainMenuIntegration {
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_MARGIN = 8;

    private PathmindMainMenuIntegration() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                addButton(screen, scaledWidth, scaledHeight);
                registerKeyHandler(client, screen);
            }
        });
    }

    private static void addButton(Screen screen, int width, int height) {
        int x = width - BUTTON_SIZE - BUTTON_MARGIN;
        int y = BUTTON_MARGIN;

        ((ScreenAccessor) screen).pathmind$addDrawableChild(new PathmindMainMenuButton(x, y, BUTTON_SIZE, button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && !(client.currentScreen instanceof PathmindVisualEditorScreen)) {
                client.setScreen(new PathmindVisualEditorScreen());
            }
        }));
    }

    private static void registerKeyHandler(MinecraftClient client, Screen screen) {
        ScreenKeyboardEvents.afterKeyPress(screen).register((currentScreen, keyCode, scanCode, modifiers) -> {
            if (!(currentScreen instanceof TitleScreen)) {
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT && !(client.currentScreen instanceof PathmindVisualEditorScreen)) {
                client.setScreen(new PathmindVisualEditorScreen());
            }
        });
    }
}
