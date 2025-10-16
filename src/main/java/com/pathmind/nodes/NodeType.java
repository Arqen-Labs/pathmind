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
    
    // Control flow Commands
    CONTROL_REPEAT("Repeat", 0xFFFFC107, "Repeat enclosed nodes a set number of times"),
    CONTROL_REPEAT_UNTIL("Repeat Until", 0xFFFFC107, "Repeat until a condition becomes true"),
    CONTROL_FOREVER("Forever", 0xFFFFC107, "Loop enclosed nodes indefinitely"),
    CONTROL_IF("If", 0xFFFFC107, "Run inner nodes when a condition is true"),
    CONTROL_IF_ELSE("If Else", 0xFFFFC107, "Run one of two branches depending on a condition"),
    
    // Player movement commands
    LOOK("Look", 0xFF03A9F4, "Adjusts the player's view direction"),
    JUMP("Jump", 0xFF009688, "Makes the player jump"),
    CROUCH("Crouch", 0xFF607D8B, "Toggles crouching"),
    SPRINT("Sprint", 0xFFFFEB3B, "Toggles sprinting"),
    TURN("Turn", 0xFF4FC3F7, "Rotates the player by a relative angle"),
    
    // Player combat commands
    ATTACK("Attack", 0xFFE53935, "Attacks the targeted block or entity"),
    SWING("Swing", 0xFFFF7043, "Performs a hand swing without interaction"),
    
    // Player interaction commands
    USE_ITEM("Use Item", 0xFF8BC34A, "Uses the item in the selected hand"),
    INTERACT("Interact", 0xFF4DB6AC, "Interacts with the targeted block or entity"),
    PLACE_HAND("Place from Hand", 0xFFBA68C8, "Places a block from the selected hand"),
    
    // Consumable commands
    EAT("Eat Item", 0xFFFF8A65, "Consumes the edible item in the selected hand"),
    DRINK("Drink Item", 0xFF80DEEA, "Consumes the drinkable item in the selected hand"),
    
    // Inventory Commands
    HOTBAR("Hotbar Slot", 0xFFCDDC39, "Selects a hotbar slot"),
    DROP_ITEM("Drop Item", 0xFFFFAB91, "Drops the currently selected item"),
    DROP_SLOT("Drop Slot", 0xFFFF7043, "Drops items from a specific slot"),
    MOVE_ITEM("Move Item", 0xFFFFB74D, "Moves items between inventory slots"),
    SWAP_SLOTS("Swap Slots", 0xFFFFF176, "Swaps items between inventory slots"),
    CLEAR_SLOT("Clear Slot", 0xFFB0BEC5, "Clears a slot and optionally drops its contents"),
    
    // Equipment Commands
    EQUIP_ARMOR("Equip Armor", 0xFF7E57C2, "Equips armor from an inventory slot"),
    UNEQUIP_ARMOR("Unequip Armor", 0xFF9575CD, "Moves armor into an inventory slot"),
    EQUIP_HAND("Equip Hand", 0xFF5C6BC0, "Moves an inventory item into a hand"),
    UNEQUIP_HAND("Unequip Hand", 0xFF7986CB, "Moves a hand item into an inventory slot"),
    SWAP_HANDS("Swap Hands", 0xFFA1887F, "Swaps the items between hands"),
    
    // Sensor commands
    SENSOR_TOUCHING_BLOCK("Touching Block", 0xFF64B5F6, "Detect if player is touching a specific block"),
    SENSOR_TOUCHING_ENTITY("Touching Entity", 0xFF64B5F6, "Detect if player is touching an entity"),
    SENSOR_AT_COORDINATES("At Coordinates", 0xFF64B5F6, "Detect if player is at specific coordinates"),
    
    // Utility Commands
    WAIT("Wait", 0xFF607D8B, "Waits for specified duration"),
    MESSAGE("Message", 0xFF9E9E9E, "Sends a chat message"),
    SET("Set", 0xFF795548, "Sets a Baritone configuration option"),
    GET("Get", 0xFF795548, "Gets a Baritone configuration value");

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
            case CONTROL_REPEAT:
            case CONTROL_REPEAT_UNTIL:
            case CONTROL_FOREVER:
            case CONTROL_IF:
            case CONTROL_IF_ELSE:
                return NodeCategory.CONTROLS;
            case LOOK:
            case JUMP:
            case CROUCH:
            case SPRINT:
            case TURN:
                return NodeCategory.PLAYER_MOVEMENT;
            case ATTACK:
            case SWING:
                return NodeCategory.PLAYER_COMBAT;
            case USE_ITEM:
            case INTERACT:
            case PLACE_HAND:
                return NodeCategory.PLAYER_INTERACTION;
            case EAT:
            case DRINK:
                return NodeCategory.PLAYER_CONSUMABLES;
            case HOTBAR:
            case DROP_ITEM:
            case DROP_SLOT:
            case MOVE_ITEM:
            case SWAP_SLOTS:
            case CLEAR_SLOT:
                return NodeCategory.PLAYER_INVENTORY;
            case EQUIP_ARMOR:
            case UNEQUIP_ARMOR:
            case EQUIP_HAND:
            case UNEQUIP_HAND:
            case SWAP_HANDS:
                return NodeCategory.PLAYER_EQUIPMENT;
            case SENSOR_TOUCHING_BLOCK:
            case SENSOR_TOUCHING_ENTITY:
            case SENSOR_AT_COORDINATES:
                return NodeCategory.SENSORS;
            case WAIT:
            case MESSAGE:
            case SET:
            case GET:
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
            case HOTBAR:
            case DROP_ITEM:
            case USE_ITEM:
            case LOOK:
            case JUMP:
            case CROUCH:
            case SPRINT:
            case INTERACT:
            case ATTACK:
            case TURN:
            case SWING:
            case PLACE_HAND:
            case DROP_SLOT:
            case MOVE_ITEM:
            case SWAP_SLOTS:
            case CLEAR_SLOT:
            case EQUIP_ARMOR:
            case UNEQUIP_ARMOR:
            case EQUIP_HAND:
            case UNEQUIP_HAND:
            case EAT:
            case DRINK:
            case CONTROL_REPEAT:
            case CONTROL_REPEAT_UNTIL:
            case CONTROL_IF:
            case CONTROL_IF_ELSE:
            case SENSOR_TOUCHING_BLOCK:
            case SENSOR_TOUCHING_ENTITY:
            case SENSOR_AT_COORDINATES:
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
            case SWAP_HANDS:
                return false;
            default:
                return false;
        }
    }
}
