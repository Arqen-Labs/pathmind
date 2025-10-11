package com.pathmind;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main mod class for Pathmind.
 * This class initializes the mod and sets up event handlers.
 */
public class PathmindMod implements ModInitializer {
    public static final String MOD_ID = "pathmind";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Pathmind mod");
        
        // Register server tick events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Add server-side logic here
        });
        
        LOGGER.info("Pathmind mod initialized successfully");
    }
}
