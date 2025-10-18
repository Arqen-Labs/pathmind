package com.pathmind.nodes;

/**
 * Enum representing different categories of nodes for sidebar organization.
 */
public enum NodeCategory {
    NAVIGATION("Navigation", 0xFF00BCD4, "Movement and pathfinding commands", "→"),
    MINING_BUILDING("Mining & Building", 0xFFFF9800, "Mining, building, and crafting commands", "⛏"),
    EXPLORATION("Exploration", 0xFF9C27B0, "World exploration commands", "🗺"),
    CONTROLS("Controls", 0xFFFFC107, "Flow control blocks", "⬚"),
    SENSORS("Sensors", 0xFF64B5F6, "Condition sensing blocks", "✦"),
    PLAYER_MOVEMENT("Player Movement", 0xFF26C6DA, "Player locomotion and view actions", "⇄"),
    PLAYER_COMBAT("Player Combat", 0xFFFF5252, "Offensive player actions", "⚔"),
    PLAYER_INTERACTION("Player Interaction", 0xFF7E57C2, "Block and entity interaction actions", "✋"),
    PLAYER_INVENTORY("Inventory", 0xFF8D6E63, "Inventory and hotbar management", "🎒"),
    PLAYER_EQUIPMENT("Equipment", 0xFF66BB6A, "Armor and held item management", "🛡"),
    WAYPOINTS("Waypoints", 0xFF9C27B0, "Waypoint management commands", "📍"),
    UTILITY("Utility", 0xFF4CAF50, "Utility and configuration commands", "⚙"),
    EVENTS("Events", 0xFFE91E63, "Event entry points and triggers", "★");

    private final String displayName;
    private final int color;
    private final String description;
    private final String icon;

    NodeCategory(String displayName, int color, String description, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
