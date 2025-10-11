package com.pathmind;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Manages all keybindings for the Pathmind mod.
 */
public class PathmindKeybinds {
    public static KeyBinding OPEN_VISUAL_EDITOR;
    
    public static void registerKeybinds() {
        OPEN_VISUAL_EDITOR = new KeyBinding(
                "key.pathmind.open_visual_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SUPER, // Right Command/Ctrl key
                "category.pathmind.general"
        );
    }
}
