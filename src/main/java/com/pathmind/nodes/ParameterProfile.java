package com.pathmind.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Parameter profiles define reusable collections of {@link ParameterField}s that can be
 * attached to command nodes. Each profile can optionally force a specific {@link NodeMode}
 * when used with a node type.
 */
public enum ParameterProfile {
    POSITION_XYZ(
        "position_xyz",
        "XYZ Coordinates",
        "Specifies the exact X, Y and Z block coordinates.",
        new ParameterField[]{ParameterField.X, ParameterField.Y, ParameterField.Z},
        usage(NodeType.GOTO, NodeMode.GOTO_XYZ),
        usage(NodeType.GOAL, NodeMode.GOAL_XYZ),
        usage(NodeType.SENSOR_AT_COORDINATES, null)
    ),
    POSITION_XZ(
        "position_xz",
        "XZ Plane Coordinates",
        "Specifies horizontal coordinates while letting the Y level float.",
        new ParameterField[]{ParameterField.X, ParameterField.Z},
        usage(NodeType.GOTO, NodeMode.GOTO_XZ),
        usage(NodeType.GOAL, NodeMode.GOAL_XZ),
        usage(NodeType.EXPLORE, NodeMode.EXPLORE_XYZ)
    ),
    POSITION_Y(
        "position_y",
        "Y Level",
        "Specifies only the vertical Y level for movement commands.",
        new ParameterField[]{ParameterField.Y},
        usage(NodeType.GOTO, NodeMode.GOTO_Y),
        usage(NodeType.GOAL, NodeMode.GOAL_Y)
    ),
    TARGET_BLOCK(
        "target_block",
        "Target Block",
        "Targets a single block type.",
        new ParameterField[]{ParameterField.BLOCK},
        usage(NodeType.GOTO, NodeMode.GOTO_BLOCK),
        usage(NodeType.MINE, NodeMode.MINE_SINGLE),
        usage(NodeType.SENSOR_TOUCHING_BLOCK, null),
        usage(NodeType.SENSOR_BLOCK_AHEAD, null),
        usage(NodeType.SENSOR_BLOCK_BELOW, null)
    ),
    PLACE_BLOCK_POSITION(
        "place_block_position",
        "Place Block",
        "Places a specific block at explicit coordinates.",
        new ParameterField[]{ParameterField.BLOCK, ParameterField.X, ParameterField.Y, ParameterField.Z},
        usage(NodeType.PLACE, null)
    ),
    TARGET_BLOCK_LIST(
        "target_block_list",
        "Block List",
        "Targets a comma separated list of blocks.",
        new ParameterField[]{ParameterField.BLOCKS},
        usage(NodeType.MINE, NodeMode.MINE_MULTIPLE)
    ),
    TARGET_ENTITY(
        "target_entity",
        "Target Entity",
        "Targets a specific entity type.",
        new ParameterField[]{ParameterField.ENTITY},
        usage(NodeType.SENSOR_TOUCHING_ENTITY, null)
    ),
    SCHEMATIC_PLAYER(
        "schematic_player",
        "Schematic at Player",
        "Builds the schematic at the player's current position.",
        new ParameterField[]{ParameterField.SCHEMATIC},
        usage(NodeType.BUILD, NodeMode.BUILD_PLAYER)
    ),
    SCHEMATIC_AT_POSITION(
        "schematic_at_position",
        "Schematic at Coordinates",
        "Builds a schematic at explicit coordinates.",
        new ParameterField[]{ParameterField.SCHEMATIC, ParameterField.X, ParameterField.Y, ParameterField.Z},
        usage(NodeType.BUILD, NodeMode.BUILD_XYZ)
    ),
    EXPLORE_CURRENT_ORIGIN(
        "explore_current_origin",
        "Explore from Current",
        "Explores outward from the player's current position.",
        new ParameterField[0],
        usage(NodeType.EXPLORE, NodeMode.EXPLORE_CURRENT)
    ),
    EXPLORE_FILTER(
        "explore_filter",
        "Explore Filter",
        "Explores using a Baritone filter file.",
        new ParameterField[]{ParameterField.FILTER},
        usage(NodeType.EXPLORE, NodeMode.EXPLORE_FILTER)
    ),
    FOLLOW_SPECIFIC_PLAYER(
        "follow_specific_player",
        "Follow Player",
        "Follows a player with the provided name.",
        new ParameterField[]{ParameterField.PLAYER},
        usage(NodeType.FOLLOW, NodeMode.FOLLOW_PLAYER)
    ),
    FOLLOW_ANY_PLAYER(
        "follow_any_player",
        "Follow Players",
        "Follows any nearby players.",
        new ParameterField[0],
        usage(NodeType.FOLLOW, NodeMode.FOLLOW_PLAYERS)
    ),
    FOLLOW_ANY_ENTITY(
        "follow_any_entity",
        "Follow Entities",
        "Follows any nearby entity.",
        new ParameterField[0],
        usage(NodeType.FOLLOW, NodeMode.FOLLOW_ENTITIES)
    ),
    FOLLOW_ENTITY_TYPE(
        "follow_entity_type",
        "Follow Entity Type",
        "Follows entities of the specified type.",
        new ParameterField[]{ParameterField.ENTITY},
        usage(NodeType.FOLLOW, NodeMode.FOLLOW_ENTITY_TYPE)
    ),
    ITEM_TARGET(
        "item_target",
        "Item Target",
        "Describes a single target item for sensors or navigation.",
        new ParameterField[]{ParameterField.ITEM},
        usage(NodeType.GOTO, null),
        usage(NodeType.SENSOR_ITEM_IN_INVENTORY, null)
    ),
    CRAFT_PLAYER_INVENTORY(
        "craft_player_inventory",
        "Craft (Inventory)",
        "Crafts an item using the player's inventory grid.",
        new ParameterField[]{ParameterField.ITEM, ParameterField.QUANTITY},
        usage(NodeType.CRAFT, NodeMode.CRAFT_PLAYER_GUI)
    ),
    CRAFT_CRAFTING_TABLE(
        "craft_crafting_table",
        "Craft (Table)",
        "Crafts an item using a crafting table GUI.",
        new ParameterField[]{ParameterField.ITEM, ParameterField.QUANTITY},
        usage(NodeType.CRAFT, NodeMode.CRAFT_CRAFTING_TABLE)
    ),
    FARM_RANGE(
        "farm_range",
        "Farm Range",
        "Farms crops within the provided radius.",
        new ParameterField[]{ParameterField.RANGE},
        usage(NodeType.FARM, NodeMode.FARM_RANGE)
    ),
    FARM_WAYPOINT(
        "farm_waypoint",
        "Farm Waypoint",
        "Farms around a waypoint with the provided range.",
        new ParameterField[]{ParameterField.WAYPOINT, ParameterField.WAYPOINT_RANGE},
        usage(NodeType.FARM, NodeMode.FARM_WAYPOINT)
    ),
    GOAL_CURRENT(
        "goal_current",
        "Goal Current Position",
        "Sets the goal to the player's current position.",
        new ParameterField[0],
        usage(NodeType.GOAL, NodeMode.GOAL_CURRENT)
    ),
    GOAL_CLEAR(
        "goal_clear",
        "Clear Goal",
        "Clears the currently active goal.",
        new ParameterField[0],
        usage(NodeType.GOAL, NodeMode.GOAL_CLEAR)
    ),
    WAIT_DURATION(
        "wait_duration",
        "Wait Duration",
        "Controls wait time including minimum duration and random variance.",
        new ParameterField[]{ParameterField.DURATION, ParameterField.MINIMUM_DURATION_SECONDS, ParameterField.RANDOM_VARIANCE_SECONDS},
        usage(NodeType.WAIT, null)
    ),
    MESSAGE_TEXT(
        "message_text",
        "Chat Message",
        "Sends the provided chat message.",
        new ParameterField[]{ParameterField.TEXT},
        usage(NodeType.MESSAGE, null)
    ),
    HOTBAR_SLOT(
        "hotbar_slot",
        "Hotbar Slot",
        "Selects the given hotbar slot.",
        new ParameterField[]{ParameterField.SLOT},
        usage(NodeType.HOTBAR, null)
    ),
    DROP_ITEM(
        "drop_item",
        "Drop Current Item",
        "Drops the currently selected item with optional repetition.",
        new ParameterField[]{ParameterField.ALL, ParameterField.COUNT, ParameterField.INTERVAL_SECONDS},
        usage(NodeType.DROP_ITEM, null)
    ),
    DROP_SLOT(
        "drop_slot",
        "Drop Slot",
        "Drops items from a specific slot.",
        new ParameterField[]{ParameterField.SLOT, ParameterField.COUNT, ParameterField.ENTIRE_STACK},
        usage(NodeType.DROP_SLOT, null)
    ),
    MOVE_ITEM(
        "move_item",
        "Move Item",
        "Moves items between two slots.",
        new ParameterField[]{ParameterField.SOURCE_SLOT, ParameterField.TARGET_SLOT, ParameterField.COUNT},
        usage(NodeType.MOVE_ITEM, null)
    ),
    SWAP_SLOTS(
        "swap_slots",
        "Swap Slots",
        "Swaps items between two inventory slots.",
        new ParameterField[]{ParameterField.FIRST_SLOT, ParameterField.SECOND_SLOT},
        usage(NodeType.SWAP_SLOTS, null)
    ),
    CLEAR_SLOT(
        "clear_slot",
        "Clear Slot",
        "Clears an inventory slot and optionally drops contents.",
        new ParameterField[]{ParameterField.SLOT, ParameterField.DROP_ITEMS},
        usage(NodeType.CLEAR_SLOT, null)
    ),
    EQUIP_ARMOR(
        "equip_armor",
        "Equip Armor",
        "Equips armor from a slot.",
        new ParameterField[]{ParameterField.SOURCE_SLOT, ParameterField.ARMOR_SLOT},
        usage(NodeType.EQUIP_ARMOR, null)
    ),
    UNEQUIP_ARMOR(
        "unequip_armor",
        "Unequip Armor",
        "Moves armor into an inventory slot.",
        new ParameterField[]{ParameterField.ARMOR_SLOT, ParameterField.TARGET_SLOT, ParameterField.DROP_IF_FULL},
        usage(NodeType.UNEQUIP_ARMOR, null)
    ),
    EQUIP_HAND(
        "equip_hand",
        "Equip Hand",
        "Moves an item into a hand.",
        new ParameterField[]{ParameterField.SOURCE_SLOT, ParameterField.HAND},
        usage(NodeType.EQUIP_HAND, null)
    ),
    UNEQUIP_HAND(
        "unequip_hand",
        "Unequip Hand",
        "Moves a hand item into an inventory slot.",
        new ParameterField[]{ParameterField.HAND, ParameterField.TARGET_SLOT, ParameterField.DROP_IF_FULL},
        usage(NodeType.UNEQUIP_HAND, null)
    ),
    USE_OPTIONS(
        "use_options",
        "Use Item Options",
        "Configures how the selected hand is used repeatedly.",
        new ParameterField[]{
            ParameterField.HAND,
            ParameterField.USE_DURATION_SECONDS,
            ParameterField.REPEAT_COUNT,
            ParameterField.USE_INTERVAL_SECONDS,
            ParameterField.STOP_IF_UNAVAILABLE,
            ParameterField.USE_UNTIL_EMPTY,
            ParameterField.ALLOW_BLOCK_INTERACTION,
            ParameterField.ALLOW_ENTITY_INTERACTION,
            ParameterField.SWING_AFTER_USE,
            ParameterField.SNEAK_WHILE_USING,
            ParameterField.RESTORE_SNEAK_STATE
        },
        usage(NodeType.USE, null)
    ),
    INTERACT_OPTIONS(
        "interact_options",
        "Interact Options",
        "Controls how interaction prioritizes blocks or entities.",
        new ParameterField[]{
            ParameterField.HAND,
            ParameterField.PREFER_ENTITY,
            ParameterField.PREFER_BLOCK,
            ParameterField.FALLBACK_TO_ITEM_USE,
            ParameterField.SWING_ON_SUCCESS,
            ParameterField.SNEAK_WHILE_INTERACTING,
            ParameterField.RESTORE_SNEAK_STATE
        },
        usage(NodeType.INTERACT, null)
    ),
    PLACE_FROM_HAND(
        "place_from_hand",
        "Place From Hand",
        "Configures how blocks are placed from the player's hand.",
        new ParameterField[]{
            ParameterField.HAND,
            ParameterField.SNEAK_WHILE_PLACING,
            ParameterField.SWING_ON_PLACE,
            ParameterField.REQUIRE_BLOCK_HIT,
            ParameterField.RESTORE_SNEAK_STATE
        },
        usage(NodeType.PLACE_HAND, null)
    ),
    SWING_OPTIONS(
        "swing_options",
        "Swing Options",
        "Configures swinging a hand repeatedly.",
        new ParameterField[]{ParameterField.HAND, ParameterField.COUNT, ParameterField.INTERVAL_SECONDS},
        usage(NodeType.SWING, null)
    ),
    ATTACK_OPTIONS(
        "attack_options",
        "Attack Options",
        "Configures attack behavior against blocks or entities.",
        new ParameterField[]{
            ParameterField.HAND,
            ParameterField.SWING_ONLY,
            ParameterField.ATTACK_ENTITIES,
            ParameterField.ATTACK_BLOCKS,
            ParameterField.REPEAT_COUNT,
            ParameterField.ATTACK_INTERVAL_SECONDS,
            ParameterField.SNEAK_WHILE_ATTACKING,
            ParameterField.RESTORE_SNEAK_STATE
        },
        usage(NodeType.ATTACK, null)
    ),
    LOOK_ROTATION(
        "look_rotation",
        "Look Rotation",
        "Adjusts the player's yaw and pitch.",
        new ParameterField[]{ParameterField.YAW, ParameterField.PITCH},
        usage(NodeType.LOOK, null)
    ),
    TURN_OFFSET(
        "turn_offset",
        "Turn Offset",
        "Applies relative yaw and pitch offsets.",
        new ParameterField[]{ParameterField.YAW_OFFSET, ParameterField.PITCH_OFFSET},
        usage(NodeType.TURN, null)
    ),
    JUMP_OPTIONS(
        "jump_options",
        "Jump Options",
        "Controls repeated jumping behavior.",
        new ParameterField[]{ParameterField.COUNT, ParameterField.INTERVAL_SECONDS},
        usage(NodeType.JUMP, null)
    ),
    CROUCH_STATE(
        "crouch_state",
        "Crouch State",
        "Toggles crouching on or off.",
        new ParameterField[]{ParameterField.ACTIVE, ParameterField.TOGGLE_KEY},
        usage(NodeType.CROUCH, null)
    ),
    SPRINT_STATE(
        "sprint_state",
        "Sprint State",
        "Toggles sprinting and optional flying.",
        new ParameterField[]{ParameterField.ACTIVE, ParameterField.ALLOW_FLYING},
        usage(NodeType.SPRINT, null)
    ),
    CONTROL_REPEAT_COUNT(
        "control_repeat_count",
        "Repeat Count",
        "Controls how many times to repeat the enclosed nodes.",
        new ParameterField[]{ParameterField.COUNT},
        usage(NodeType.CONTROL_REPEAT, null)
    ),
    STOP_NORMAL(
        "stop_normal",
        "Stop",
        "Stops the current process.",
        new ParameterField[0],
        usage(NodeType.STOP, NodeMode.STOP_NORMAL)
    ),
    STOP_CANCEL(
        "stop_cancel",
        "Cancel",
        "Cancels the current process if possible.",
        new ParameterField[0],
        usage(NodeType.STOP, NodeMode.STOP_CANCEL)
    ),
    STOP_FORCE(
        "stop_force",
        "Force Stop",
        "Forcefully cancels all processes.",
        new ParameterField[0],
        usage(NodeType.STOP, NodeMode.STOP_FORCE)
    ),
    SCREEN_OPEN_CHAT(
        "screen_open_chat",
        "Open Chat",
        "Opens the chat screen.",
        new ParameterField[0],
        usage(NodeType.SCREEN_CONTROL, NodeMode.SCREEN_OPEN_CHAT)
    ),
    SCREEN_CLOSE(
        "screen_close",
        "Close Screen",
        "Closes the currently open screen.",
        new ParameterField[0],
        usage(NodeType.SCREEN_CONTROL, NodeMode.SCREEN_CLOSE_CURRENT)
    ),
    PLAYER_GUI_OPEN(
        "player_gui_open",
        "Open Inventory",
        "Opens the player's inventory screen.",
        new ParameterField[0],
        usage(NodeType.PLAYER_GUI, NodeMode.PLAYER_GUI_OPEN)
    ),
    PLAYER_GUI_CLOSE(
        "player_gui_close",
        "Close Inventory",
        "Closes the player's inventory screen.",
        new ParameterField[0],
        usage(NodeType.PLAYER_GUI, NodeMode.PLAYER_GUI_CLOSE)
    ),
    FUNCTION_NAME(
        "function_name",
        "Function Name",
        "Defines or references a function name.",
        new ParameterField[]{ParameterField.NAME},
        usage(NodeType.EVENT_FUNCTION, null)
    ),
    FUNCTION_CALL(
        "function_call",
        "Call Function",
        "Calls a function by name.",
        new ParameterField[]{ParameterField.NAME},
        usage(NodeType.EVENT_CALL, null)
    ),
    SENSOR_LIGHT_LEVEL(
        "sensor_light_level",
        "Light Threshold",
        "Detects when light drops below a threshold.",
        new ParameterField[]{ParameterField.THRESHOLD},
        usage(NodeType.SENSOR_LIGHT_LEVEL_BELOW, null)
    ),
    SENSOR_HEALTH_AMOUNT(
        "sensor_health_amount",
        "Health Threshold",
        "Triggers when health drops below a threshold.",
        new ParameterField[]{ParameterField.AMOUNT_DOUBLE},
        usage(NodeType.SENSOR_HEALTH_BELOW, null)
    ),
    SENSOR_HUNGER_AMOUNT(
        "sensor_hunger_amount",
        "Hunger Threshold",
        "Triggers when hunger drops below a threshold.",
        new ParameterField[]{ParameterField.AMOUNT_INTEGER},
        usage(NodeType.SENSOR_HUNGER_BELOW, null)
    ),
    SENSOR_ENTITY_RANGE(
        "sensor_entity_range",
        "Entity Range",
        "Detects entities within range.",
        new ParameterField[]{ParameterField.ENTITY, ParameterField.RANGE},
        usage(NodeType.SENSOR_ENTITY_NEARBY, null)
    ),
    SENSOR_FALL_DISTANCE(
        "sensor_fall_distance",
        "Fall Distance",
        "Detects falling beyond a distance.",
        new ParameterField[]{ParameterField.DISTANCE},
        usage(NodeType.SENSOR_IS_FALLING, null)
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final ParameterField[] fields;
    private final EnumSet<NodeType> supportedTypes;
    private final EnumMap<NodeType, NodeMode> modeOverrides;

    ParameterProfile(String id, String displayName, String description, ParameterField[] fields, ParameterUsage... usages) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.fields = fields;
        this.supportedTypes = EnumSet.noneOf(NodeType.class);
        this.modeOverrides = new EnumMap<>(NodeType.class);
        for (ParameterUsage usage : usages) {
            if (usage == null || usage.type == null) {
                continue;
            }
            this.supportedTypes.add(usage.type);
            if (usage.mode != null) {
                this.modeOverrides.put(usage.type, usage.mode);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<NodeParameter> instantiateParameters() {
        if (fields.length == 0) {
            return Collections.emptyList();
        }
        List<NodeParameter> params = new ArrayList<>(fields.length);
        for (ParameterField field : fields) {
            params.add(field.createParameter());
        }
        return params;
    }

    public boolean supports(NodeType type) {
        return supportedTypes.contains(type);
    }

    public Optional<NodeMode> resolveMode(NodeType type) {
        return Optional.ofNullable(modeOverrides.get(type));
    }

    public ParameterField[] getFields() {
        return fields;
    }

    public String getSummary() {
        if (fields.length == 0) {
            return description;
        }
        List<String> names = new ArrayList<>(fields.length);
        for (ParameterField field : fields) {
            names.add(field.getParameterName());
        }
        return String.join(", ", names);
    }

    public static Optional<ParameterProfile> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (ParameterProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public String getTechnicalName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static ParameterUsage usage(NodeType type, NodeMode mode) {
        return new ParameterUsage(type, mode);
    }

    private static ParameterUsage usage(NodeType type) {
        return new ParameterUsage(type, null);
    }

    private static class ParameterUsage {
        private final NodeType type;
        private final NodeMode mode;

        private ParameterUsage(NodeType type, NodeMode mode) {
            this.type = type;
            this.mode = mode;
        }
    }
}
