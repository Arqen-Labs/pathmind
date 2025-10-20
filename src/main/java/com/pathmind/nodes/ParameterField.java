package com.pathmind.nodes;

import java.util.Locale;

/**
 * Represents a single configurable field that can exist on a parameter profile.
 * Each field corresponds to a legacy node parameter with its default value and type.
 */
public enum ParameterField {
    X("X", ParameterType.INTEGER, "0"),
    Y("Y", ParameterType.INTEGER, "0"),
    Z("Z", ParameterType.INTEGER, "0"),
    BLOCK("Block", ParameterType.BLOCK_TYPE, "minecraft:stone"),
    BLOCKS("Blocks", ParameterType.STRING, "stone,dirt"),
    SCHEMATIC("Schematic", ParameterType.SCHEMATIC, "house.schematic"),
    FILTER("Filter", ParameterType.STRING, "explore.txt"),
    PLAYER("Player", ParameterType.PLAYER_NAME, "PlayerName"),
    ENTITY("Entity", ParameterType.ENTITY_TYPE, "minecraft:zombie"),
    ITEM("Item", ParameterType.ITEM, "minecraft:stone"),
    QUANTITY("Quantity", ParameterType.INTEGER, "1"),
    RANGE("Range", ParameterType.INTEGER, "10"),
    WAYPOINT("Waypoint", ParameterType.WAYPOINT_NAME, "farm"),
    DURATION("Duration", ParameterType.DOUBLE, "1.0"),
    MINIMUM_DURATION_SECONDS("MinimumDurationSeconds", ParameterType.DOUBLE, "0.0"),
    RANDOM_VARIANCE_SECONDS("RandomVarianceSeconds", ParameterType.DOUBLE, "0.0"),
    TEXT("Text", ParameterType.STRING, "Hello World"),
    SLOT("Slot", ParameterType.INTEGER, "0"),
    ALL("All", ParameterType.BOOLEAN, "false"),
    COUNT("Count", ParameterType.INTEGER, "1"),
    INTERVAL_SECONDS("IntervalSeconds", ParameterType.DOUBLE, "0.0"),
    ENTIRE_STACK("EntireStack", ParameterType.BOOLEAN, "true"),
    SOURCE_SLOT("SourceSlot", ParameterType.INTEGER, "0"),
    TARGET_SLOT("TargetSlot", ParameterType.INTEGER, "9"),
    FIRST_SLOT("FirstSlot", ParameterType.INTEGER, "0"),
    SECOND_SLOT("SecondSlot", ParameterType.INTEGER, "9"),
    DROP_ITEMS("DropItems", ParameterType.BOOLEAN, "false"),
    DROP_IF_FULL("DropIfFull", ParameterType.BOOLEAN, "true"),
    ARMOR_SLOT("ArmorSlot", ParameterType.STRING, "head"),
    HAND("Hand", ParameterType.STRING, "main"),
    USE_DURATION_SECONDS("UseDurationSeconds", ParameterType.DOUBLE, "0.0"),
    REPEAT_COUNT("RepeatCount", ParameterType.INTEGER, "1"),
    USE_INTERVAL_SECONDS("UseIntervalSeconds", ParameterType.DOUBLE, "0.0"),
    STOP_IF_UNAVAILABLE("StopIfUnavailable", ParameterType.BOOLEAN, "true"),
    USE_UNTIL_EMPTY("UseUntilEmpty", ParameterType.BOOLEAN, "false"),
    ALLOW_BLOCK_INTERACTION("AllowBlockInteraction", ParameterType.BOOLEAN, "true"),
    ALLOW_ENTITY_INTERACTION("AllowEntityInteraction", ParameterType.BOOLEAN, "true"),
    SWING_AFTER_USE("SwingAfterUse", ParameterType.BOOLEAN, "true"),
    SNEAK_WHILE_USING("SneakWhileUsing", ParameterType.BOOLEAN, "false"),
    RESTORE_SNEAK_STATE("RestoreSneakState", ParameterType.BOOLEAN, "true"),
    PREFER_ENTITY("PreferEntity", ParameterType.BOOLEAN, "true"),
    PREFER_BLOCK("PreferBlock", ParameterType.BOOLEAN, "true"),
    FALLBACK_TO_ITEM_USE("FallbackToItemUse", ParameterType.BOOLEAN, "true"),
    SWING_ON_SUCCESS("SwingOnSuccess", ParameterType.BOOLEAN, "true"),
    SNEAK_WHILE_INTERACTING("SneakWhileInteracting", ParameterType.BOOLEAN, "false"),
    SNEAK_WHILE_PLACING("SneakWhilePlacing", ParameterType.BOOLEAN, "false"),
    SWING_ON_PLACE("SwingOnPlace", ParameterType.BOOLEAN, "true"),
    REQUIRE_BLOCK_HIT("RequireBlockHit", ParameterType.BOOLEAN, "true"),
    SWING_ONLY("SwingOnly", ParameterType.BOOLEAN, "false"),
    ATTACK_ENTITIES("AttackEntities", ParameterType.BOOLEAN, "true"),
    ATTACK_BLOCKS("AttackBlocks", ParameterType.BOOLEAN, "true"),
    ATTACK_INTERVAL_SECONDS("AttackIntervalSeconds", ParameterType.DOUBLE, "0.0"),
    SNEAK_WHILE_ATTACKING("SneakWhileAttacking", ParameterType.BOOLEAN, "false"),
    YAW("Yaw", ParameterType.DOUBLE, "0.0"),
    PITCH("Pitch", ParameterType.DOUBLE, "0.0"),
    YAW_OFFSET("YawOffset", ParameterType.DOUBLE, "0.0"),
    PITCH_OFFSET("PitchOffset", ParameterType.DOUBLE, "0.0"),
    ACTIVE("Active", ParameterType.BOOLEAN, "true"),
    TOGGLE_KEY("ToggleKey", ParameterType.BOOLEAN, "false"),
    ALLOW_FLYING("AllowFlying", ParameterType.BOOLEAN, "false"),
    NAME("Name", ParameterType.STRING, "function"),
    THRESHOLD("Threshold", ParameterType.INTEGER, "7"),
    AMOUNT_DOUBLE("Amount", ParameterType.DOUBLE, "10.0"),
    AMOUNT_INTEGER("Amount", ParameterType.INTEGER, "10"),
    DISTANCE("Distance", ParameterType.DOUBLE, "2.0"),
    WAYPOINT_RANGE("Range", ParameterType.INTEGER, "10");

    private final String parameterName;
    private final ParameterType type;
    private final String defaultValue;

    ParameterField(String parameterName, ParameterType type, String defaultValue) {
        this.parameterName = parameterName;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public ParameterType getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public NodeParameter createParameter() {
        return new NodeParameter(parameterName, type, defaultValue);
    }

    public String getDisplayLabel() {
        return parameterName.replace('_', ' ');
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
