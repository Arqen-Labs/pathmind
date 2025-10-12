package com.pathmind.nodes;

/**
 * Enum representing different types of parameters for nodes.
 */
public enum ParameterType {
    STRING("String"),
    INTEGER("Integer"),
    DOUBLE("Double"),
    BOOLEAN("Boolean");

    private final String displayName;

    ParameterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
