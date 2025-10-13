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
    THISWAY("Thisway", 0xFF00BCD4, "Moves forward in the direction the player is facing for the given number of blocks"),
    GOAL_XYZ("Goal XYZ", 0xFF2196F3, "Sets a goal coordinate for Baritone to path toward"),
    GOAL_XZ("Goal XZ", 0xFF2196F3, "Sets a goal to a coordinate using only x and z (y defaults to surface)"),
    GOAL_Y("Goal Y", 0xFF2196F3, "Sets a goal only for y level"),
    GOAL_CURRENT("Goal Current", 0xFF2196F3, "Sets the goal to the player's current feet position"),
    GOAL_CLEAR("Goal Clear", 0xFF2196F3, "Clears the current goal"),
    GOTO_XYZ("Goto XYZ", 0xFF00BCD4, "Immediately starts moving toward the given coordinate"),
    GOTO_XZ("Goto XZ", 0xFF00BCD4, "Goes toward coordinate x,z"),
    GOTO_Y("Goto Y", 0xFF00BCD4, "Moves to a specific y-level"),
    GOTO_BLOCK("Goto Block", 0xFF00BCD4, "Finds and moves toward the nearest specified block type"),
    GOTO_PORTAL("Goto Portal", 0xFF00BCD4, "Goes to the nearest portal"),
    GOTO_ENDER_CHEST("Goto Ender Chest", 0xFF00BCD4, "Goes to the nearest ender chest"),
    PATH("Path", 0xFF03DAC6, "Starts pathing to the current goal"),
    STOP("Stop", 0xFFF44336, "Stops current process"),
    CANCEL("Cancel", 0xFFF44336, "Same as stop"),
    FORCE_CANCEL("Force Cancel", 0xFFF44336, "Forces all Baritone processes to stop immediately, ignoring checks"),
    INVERT("Invert", 0xFFFF5722, "Inverts the current goal; moves away instead of toward it"),
    COME("Come", 0xFF9C27B0, "Moves toward the location under your camera"),
    SURFACE("Surface", 0xFF4CAF50, "Moves to the nearest open air space or top surface"),
    TOP("Top", 0xFF4CAF50, "Same as surface"),
    
    // Mining and Building Commands
    MINE_BLOCK("Mine Block", 0xFF2196F3, "Mines the specified block or blocks, optionally a certain number of them"),
    MINE_MULTIPLE("Mine Multiple", 0xFF2196F3, "Can include multiple block types to mine at once"),
    BUILD_SCHEMATIC("Build Schematic", 0xFFFF9800, "Loads and builds the schematic file from the schematics folder, using player's feet as origin"),
    BUILD_SCHEMATIC_XYZ("Build Schematic XYZ", 0xFFFF9800, "Loads and builds schematic at given coordinates (can use ~ for relative positions)"),
    SCHEMATICA("Schematica", 0xFFFF9800, "Builds the schematic currently open in the Schematica mod"),
    TUNNEL("Tunnel", 0xFF795548, "Digs a tunnel of given dimensions"),
    FARM_RANGE("Farm Range", 0xFF4CAF50, "Harvests and replants crops within given range"),
    FARM_WAYPOINT("Farm Waypoint", 0xFF4CAF50, "Harvests and replants crops within range around specified waypoint"),
    
    // Exploration Commands
    EXPLORE("Explore", 0xFF673AB7, "Explores unseen chunks starting from the player's current position"),
    EXPLORE_XYZ("Explore XYZ", 0xFF673AB7, "Explores unseen chunks starting from given coordinates"),
    EXPLORE_FILTER("Explore Filter", 0xFF673AB7, "Loads list of chunks to explore or avoid from a file"),
    FOLLOW_PLAYER("Follow Player", 0xFF3F51B5, "Follows a specific player"),
    FOLLOW_PLAYERS("Follow Players", 0xFF3F51B5, "Follows any players in range"),
    FOLLOW_ENTITIES("Follow Entities", 0xFF3F51B5, "Follows any entities nearby"),
    FOLLOW_ENTITY_TYPE("Follow Entity Type", 0xFF3F51B5, "Follows entities of a specific type"),
    AXIS("Axis", 0xFF673AB7, "Travels to the nearest axis or diagonal axis at default y height (120 unless changed)"),
    
    // Waypoint Commands
    WP_SAVE("WP Save", 0xFF9C27B0, "Saves a waypoint under a tag, for example \"user\" or \"home\""),
    WP_GOAL("WP Goal", 0xFF9C27B0, "Sets a waypoint as the current goal"),
    WP_LIST("WP List", 0xFF9C27B0, "Lists available waypoints"),
    WP_GOAL_DEATH("WP Goal Death", 0xFF9C27B0, "Sets goal to last death waypoint"),
    
    // Utility Commands
    CLICK("Click", 0xFF607D8B, "Clicks a destination block on screen; right click paths on top, left click paths into it, left drag selects an area"),
    BLACKLIST("Blacklist", 0xFF607D8B, "Stops Baritone from targeting or going to the current block"),
    ETA("ETA", 0xFF607D8B, "Prints an estimated time to reach next segment and the goal"),
    PROC("Proc", 0xFF607D8B, "Shows information about the current active Baritone process"),
    REPACK("Repack", 0xFF607D8B, "Recaches chunks around the player"),
    GC("GC", 0xFF607D8B, "Runs garbage collection to free memory"),
    RENDER("Render", 0xFF607D8B, "Fixes glitched chunk rendering"),
    RELOAD_ALL("Reload All", 0xFF607D8B, "Reloads Baritone's world cache"),
    SAVE_ALL("Save All", 0xFF607D8B, "Saves Baritone's world cache"),
    FIND("Find", 0xFF607D8B, "Searches Baritone's cache for a given block and prints coordinates if found");

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
            case THISWAY:
            case GOAL_XYZ:
            case GOAL_XZ:
            case GOAL_Y:
            case GOAL_CURRENT:
            case GOAL_CLEAR:
            case GOTO_XYZ:
            case GOTO_XZ:
            case GOTO_Y:
            case GOTO_BLOCK:
            case GOTO_PORTAL:
            case GOTO_ENDER_CHEST:
            case PATH:
            case STOP:
            case CANCEL:
            case FORCE_CANCEL:
            case INVERT:
            case COME:
            case SURFACE:
            case TOP:
                return NodeCategory.NAVIGATION;
            case MINE_BLOCK:
            case MINE_MULTIPLE:
            case BUILD_SCHEMATIC:
            case BUILD_SCHEMATIC_XYZ:
            case SCHEMATICA:
            case TUNNEL:
            case FARM_RANGE:
            case FARM_WAYPOINT:
                return NodeCategory.MINING_BUILDING;
            case EXPLORE:
            case EXPLORE_XYZ:
            case EXPLORE_FILTER:
            case FOLLOW_PLAYER:
            case FOLLOW_PLAYERS:
            case FOLLOW_ENTITIES:
            case FOLLOW_ENTITY_TYPE:
            case AXIS:
                return NodeCategory.EXPLORATION;
            case WP_SAVE:
            case WP_GOAL:
            case WP_LIST:
            case WP_GOAL_DEATH:
                return NodeCategory.WAYPOINTS;
            case CLICK:
            case BLACKLIST:
            case ETA:
            case PROC:
            case REPACK:
            case GC:
            case RENDER:
            case RELOAD_ALL:
            case SAVE_ALL:
            case FIND:
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
            case THISWAY:
            case GOAL_XYZ:
            case GOAL_XZ:
            case GOAL_Y:
            case GOTO_XYZ:
            case GOTO_XZ:
            case GOTO_Y:
            case GOTO_BLOCK:
            case MINE_BLOCK:
            case MINE_MULTIPLE:
            case BUILD_SCHEMATIC:
            case BUILD_SCHEMATIC_XYZ:
            case TUNNEL:
            case FARM_RANGE:
            case FARM_WAYPOINT:
            case EXPLORE_XYZ:
            case EXPLORE_FILTER:
            case FOLLOW_PLAYER:
            case FOLLOW_ENTITY_TYPE:
            case WP_SAVE:
            case WP_GOAL:
            case FIND:
                return true;
            case GOAL_CURRENT:
            case GOAL_CLEAR:
            case GOTO_PORTAL:
            case GOTO_ENDER_CHEST:
            case PATH:
            case STOP:
            case CANCEL:
            case FORCE_CANCEL:
            case INVERT:
            case COME:
            case SURFACE:
            case TOP:
            case SCHEMATICA:
            case EXPLORE:
            case FOLLOW_PLAYERS:
            case FOLLOW_ENTITIES:
            case AXIS:
            case WP_LIST:
            case WP_GOAL_DEATH:
            case CLICK:
            case BLACKLIST:
            case ETA:
            case PROC:
            case REPACK:
            case GC:
            case RENDER:
            case RELOAD_ALL:
            case SAVE_ALL:
            case START:
            case END:
                return false;
            default:
                return false;
        }
    }
}
