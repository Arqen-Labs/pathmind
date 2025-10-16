package com.pathmind.nodes;

/**
 * Enum representing different types of nodes in the Pathmind visual editor.
 * Similar to Blender's shader nodes, each type has specific properties and behaviors.
 */
public enum NodeType {
    // Special nodes
    START("Start", 0xFF4CAF50, "Begins the automation sequence"),
    END("End", 0xFFF44336, "Ends the automation sequence"),
    
    // Navigation Commands
    GOTO("Goto", 0xFF00BCD4, "Moves to specified coordinates"),
    GOAL("Goal", 0xFF2196F3, "Sets a goal at specified coordinates"),
    PATH("Path", 0xFF03DAC6, "Initiates pathfinding to the set goal"),
    STOP("Stop", 0xFFF44336, "Stops the current pathfinding task"),
    INVERT("Invert", 0xFFFF5722, "Inverts the current goal and path"),
    COME("Come", 0xFF9C27B0, "Moves towards the camera's direction"),
    SURFACE("Surface", 0xFF4CAF50, "Moves to the nearest surface"),
    
    // Mining and Building Commands
    MINE("Mine", 0xFF2196F3, "Mines specified block types"),
    BUILD("Build", 0xFFFF9800, "Constructs structures from schematic files"),
    TUNNEL("Tunnel", 0xFF795548, "Digs a 2x3 tunnel forward automatically"),
    FARM("Farm", 0xFF4CAF50, "Automates harvesting and replanting crops"),
    PLACE("Place", 0xFF9C27B0, "Places blocks at specified coordinates"),
    CRAFT("Craft", 0xFFFF9800, "Crafts items using available materials"),
    
    // Exploration Commands
    EXPLORE("Explore", 0xFF673AB7, "Explores the world from origin coordinates"),
    FOLLOW("Follow", 0xFF3F51B5, "Follows a specified player"),
    
    // Utility Commands
    WAIT("Wait", 0xFF607D8B, "Waits for specified duration"),
    MESSAGE("Message", 0xFF9E9E9E, "Sends a chat message"),
    SET("Set", 0xFF795548, "Sets a Baritone configuration option"),
    GET("Get", 0xFF795548, "Gets a Baritone configuration value"),

    // Control Flow Nodes
    IF_ELSE("If/Else", 0xFFFFC107, "Branches execution based on a condition"),
    FOREVER("Forever", 0xFFFFA000, "Repeats the connected sequence indefinitely");

    private final String displayName;
    private final String description;

    NodeType(String displayName, int color, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        // Special nodes keep their original colors
        if (this == START) {
            return 0xFF4CAF50; // Green
        } else if (this == END) {
            return 0xFFF44336; // Red
        }
        return getCategory().getColor();
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
        return true; // All nodes including START and END can be dragged from sidebar
    }
    
    /**
     * Get the category this node belongs to for sidebar organization
     */
    public NodeCategory getCategory() {
        switch (this) {
            case START:
            case END:
                return NodeCategory.SPECIAL;
            case GOTO:
            case GOAL:
            case PATH:
            case STOP:
            case INVERT:
            case COME:
            case SURFACE:
                return NodeCategory.NAVIGATION;
            case MINE:
            case BUILD:
            case TUNNEL:
            case FARM:
            case PLACE:
            case CRAFT:
                return NodeCategory.MINING_BUILDING;
            case EXPLORE:
            case FOLLOW:
                return NodeCategory.EXPLORATION;
            case WAIT:
            case MESSAGE:
            case SET:
            case GET:
            case IF_ELSE:
            case FOREVER:
                return NodeCategory.UTILITY;
            default:
                return NodeCategory.UTILITY;
        }
    }
    
    /**
     * Check if this node type requires parameters
     */
    public boolean hasParameters() {
        switch (this) {
            case GOTO:
            case GOAL:
            case MINE:
            case PLACE:
            case CRAFT:
            case BUILD:
            case EXPLORE:
            case FOLLOW:
            case WAIT:
            case MESSAGE:
            case SET:
            case GET:
            case IF_ELSE:
                return true;
            case PATH:
            case STOP:
            case INVERT:
            case COME:
            case SURFACE:
            case TUNNEL:
            case FARM:
            case START:
            case END:
            case FOREVER:
                return false;
            default:
                return false;
        }
    }
}
