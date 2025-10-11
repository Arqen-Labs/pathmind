package com.pathmind.nodes;

/**
 * Enum representing different types of nodes in the Pathmind visual editor.
 * Similar to Blender's shader nodes, each type has specific properties and behaviors.
 */
public enum NodeType {
    START("Start", 0xFF4CAF50, "Begins the automation sequence"),
    MINE("Mine", 0xFF2196F3, "Mines blocks at specified coordinates"),
    CRAFT("Craft", 0xFFFF9800, "Crafts items using available materials"),
    PLACE("Place", 0xFF9C27B0, "Places blocks at specified coordinates"),
    MOVE("Move", 0xFF00BCD4, "Moves to specified coordinates"),
    WAIT("Wait", 0xFF607D8B, "Waits for specified duration"),
    END("End", 0xFFF44336, "Ends the automation sequence");

    private final String displayName;
    private final int color;
    private final String description;

    NodeType(String displayName, int color, String description) {
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

    public boolean isInputNode() {
        return this == START;
    }

    public boolean isOutputNode() {
        return this == END;
    }

    public boolean isDraggableFromSidebar() {
        return this != START && this != END; // Start and End are special nodes
    }
}
