package com.pathmind.nodes;

/**
 * Enum representing different modes for generalized nodes.
 * Each mode corresponds to a specific behavior within a generalized node type.
 */
public enum NodeMode {
    // GOTO modes
    GOTO_XYZ("Go to XYZ", "Go to specific X, Y, Z coordinates"),
    GOTO_XZ("Go to XZ", "Go to X, Z coordinates (Y defaults to surface)"),
    GOTO_Y("Go to Y", "Go to specific Y level"),
    GOTO_BLOCK("Go to Block", "Go to nearest block of specified type"),
    
    // GOAL modes
    GOAL_XYZ("Set Goal XYZ", "Set goal to specific X, Y, Z coordinates"),
    GOAL_XZ("Set Goal XZ", "Set goal to X, Z coordinates (Y defaults to surface)"),
    GOAL_Y("Set Goal Y", "Set goal to specific Y level"),
    GOAL_CURRENT("Set Goal Current", "Set goal to player's current position"),
    GOAL_CLEAR("Clear Goal", "Clear current goal"),
    
    // MINE modes
    MINE_SINGLE("Mine Single Block", "Mine a single block type"),
    MINE_MULTIPLE("Mine Multiple Blocks", "Mine multiple block types"),
    
    // BUILD modes
    BUILD_PLAYER("Build at Player", "Build schematic at player's location"),
    BUILD_XYZ("Build at XYZ", "Build schematic at specified coordinates"),
    
    // EXPLORE modes
    EXPLORE_CURRENT("Explore from Current", "Explore from current position"),
    EXPLORE_XYZ("Explore from XYZ", "Explore from specified coordinates"),
    EXPLORE_FILTER("Explore with Filter", "Explore using filter file"),
    
    // FOLLOW modes
    FOLLOW_PLAYER("Follow Player", "Follow specific player"),
    FOLLOW_PLAYERS("Follow Any Players", "Follow any players in range"),
    FOLLOW_ENTITIES("Follow Any Entities", "Follow any entities nearby"),
    FOLLOW_ENTITY_TYPE("Follow Entity Type", "Follow entities of specific type"),

    // CRAFT modes
    CRAFT_PLAYER_GUI("Player Inventory", "Craft using the player's 2x2 grid"),
    CRAFT_CRAFTING_TABLE("Crafting Table", "Craft using an open crafting table"),

    // Player GUI modes
    PLAYER_GUI_OPEN("Open Player GUI", "Open the player's inventory screen"),
    PLAYER_GUI_CLOSE("Close Player GUI", "Close the player's inventory screen"),

    // Screen control modes
    SCREEN_OPEN_CHAT("Open Chat", "Open the chat screen for typing"),
    SCREEN_CLOSE_CURRENT("Close Screen", "Close the currently open screen"),

    // FARM modes
    FARM_RANGE("Farm in Range", "Farm within specified range"),
    FARM_WAYPOINT("Farm at Waypoint", "Farm around specified waypoint"),
    
    // STOP modes
    STOP_NORMAL("Stop Process", "Stop current process"),
    STOP_CANCEL("Cancel Process", "Cancel current process"),
    STOP_FORCE("Force Cancel", "Force stop all processes");

    private final String displayName;
    private final String description;

    NodeMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
    
    /**
     * Get all modes for a specific node type
     */
    public static NodeMode[] getModesForNodeType(NodeType nodeType) {
        switch (nodeType) {
            case GOTO:
                return new NodeMode[]{
                    GOTO_XYZ, GOTO_XZ, GOTO_Y, GOTO_BLOCK
                };
            case GOAL:
                return new NodeMode[]{
                    GOAL_XYZ, GOAL_XZ, GOAL_Y, GOAL_CURRENT, GOAL_CLEAR
                };
            case MINE:
                return new NodeMode[]{
                    MINE_SINGLE, MINE_MULTIPLE
                };
            case BUILD:
                return new NodeMode[]{
                    BUILD_PLAYER, BUILD_XYZ
                };
            case EXPLORE:
                return new NodeMode[]{
                    EXPLORE_CURRENT, EXPLORE_XYZ, EXPLORE_FILTER
                };
            case FOLLOW:
                return new NodeMode[]{
                    FOLLOW_PLAYER, FOLLOW_PLAYERS, FOLLOW_ENTITIES, FOLLOW_ENTITY_TYPE
                };
            case CRAFT:
                return new NodeMode[]{
                    CRAFT_PLAYER_GUI, CRAFT_CRAFTING_TABLE
                };
            case SCREEN_CONTROL:
                return new NodeMode[]{
                    SCREEN_OPEN_CHAT, SCREEN_CLOSE_CURRENT
                };
            case FARM:
                return new NodeMode[]{
                    FARM_RANGE, FARM_WAYPOINT
                };
            case STOP:
                return new NodeMode[]{
                    STOP_NORMAL, STOP_CANCEL, STOP_FORCE
                };
            default:
                return new NodeMode[0];
        }
    }
    
    /**
     * Get the default mode for a node type
     */
    public static NodeMode getDefaultModeForNodeType(NodeType nodeType) {
        switch (nodeType) {
            case GOTO:
                return GOTO_XYZ;
            case GOAL:
                return GOAL_XYZ;
            case MINE:
                return MINE_SINGLE;
            case BUILD:
                return BUILD_PLAYER;
            case EXPLORE:
                return EXPLORE_CURRENT;
            case FOLLOW:
                return FOLLOW_PLAYER;
            case CRAFT:
                return CRAFT_PLAYER_GUI;
            case OPEN_INVENTORY:
                return PLAYER_GUI_OPEN;
            case CLOSE_INVENTORY:
                return PLAYER_GUI_CLOSE;
            case SCREEN_CONTROL:
                return SCREEN_OPEN_CHAT;
            case FARM:
                return FARM_RANGE;
            case STOP:
                return STOP_NORMAL;
            default:
                return null;
        }
    }
}
