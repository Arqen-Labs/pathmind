package com.pathmind.nodes;

/**
 * Enum representing different categories of nodes for sidebar organization.
 */
public enum NodeCategory {
    NAVIGATION("Navigation", 0xFF00BCD4, "Movement and pathfinding commands", "→"),
    MINING_BUILDING("Mining & Building", 0xFFFF9800, "Mining, building, and crafting commands", "⛏"),
    EXPLORATION("Exploration", 0xFF9C27B0, "World exploration commands", "🗺"),
    UTILITY("Utility", 0xFF4CAF50, "Utility and configuration commands", "⚙"),
    SPECIAL("Special", 0xFFE91E63, "Special workflow nodes", "★");

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
