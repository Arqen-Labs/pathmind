package com.pathmind.nodes;

/**
 * Enum representing different categories of nodes for sidebar organization.
 */
public enum NodeCategory {
    SPECIAL("Special", 0xFF666666, "Special workflow nodes"),
    NAVIGATION("Navigation", 0xFF00BCD4, "Movement and pathfinding commands"),
    MINING_BUILDING("Mining & Building", 0xFF2196F3, "Mining, building, and crafting commands"),
    EXPLORATION("Exploration", 0xFF673AB7, "World exploration commands"),
    UTILITY("Utility", 0xFF607D8B, "Utility and configuration commands");

    private final String displayName;
    private final int color;
    private final String description;

    NodeCategory(String displayName, int color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
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
}
