package com.pathmind.nodes;

/**
 * Represents a parameter for a node in the Pathmind visual editor.
 * Each parameter has a name, value, and type.
 */
public class NodeParameter {
    private final String name;
    private final ParameterType type;
    private String stringValue;
    private int intValue;
    private double doubleValue;
    private boolean boolValue;
    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;
    private Node attachedNode;
    private int coordX;
    private int coordY;
    private int coordZ;

    public NodeParameter(String name, ParameterType type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.stringValue = defaultValue;
        this.intValue = 0;
        this.doubleValue = 0.0;
        this.boolValue = false;
        
        // Try to parse the default value based on type
        if (type == ParameterType.INTEGER) {
            try {
                this.intValue = Integer.parseInt(defaultValue);
            } catch (NumberFormatException e) {
                this.intValue = 0;
            }
        } else if (type == ParameterType.DOUBLE) {
            try {
                this.doubleValue = Double.parseDouble(defaultValue);
            } catch (NumberFormatException e) {
                this.doubleValue = 0.0;
            }
        } else if (type == ParameterType.BOOLEAN) {
            this.boolValue = Boolean.parseBoolean(defaultValue);
        } else if (type == ParameterType.COORDINATE) {
            parseCoordinate(defaultValue);
        }
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String value) {
        this.stringValue = value;
        
        // Update typed values
        if (type == ParameterType.INTEGER) {
            try {
                this.intValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Keep current intValue if parsing fails
            }
        } else if (type == ParameterType.DOUBLE) {
            try {
                this.doubleValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Keep current doubleValue if parsing fails
            }
        } else if (type == ParameterType.BOOLEAN) {
            this.boolValue = Boolean.parseBoolean(value);
        } else if (type == ParameterType.COORDINATE) {
            parseCoordinate(value);
        }
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int value) {
        this.intValue = value;
        this.stringValue = String.valueOf(value);
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double value) {
        this.doubleValue = value;
        this.stringValue = String.valueOf(value);
    }

    public boolean getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean value) {
        this.boolValue = value;
        this.stringValue = String.valueOf(value);
    }

    public void setCoordinateValue(int x, int y, int z) {
        this.coordX = x;
        this.coordY = y;
        this.coordZ = z;
        this.stringValue = x + "," + y + "," + z;
    }

    public int getCoordX() {
        return coordX;
    }

    public int getCoordY() {
        return coordY;
    }

    public int getCoordZ() {
        return coordZ;
    }

    public String getDisplayValue() {
        switch (type) {
            case INTEGER:
                return String.valueOf(intValue);
            case DOUBLE:
                return String.format("%.2f", doubleValue);
            case BOOLEAN:
                return boolValue ? "True" : "False";
            case COORDINATE:
                return "(" + coordX + ", " + coordY + ", " + coordZ + ")";
            case STRING:
            default:
                return stringValue;
        }
    }

    public boolean hasAttachedNode() {
        return attachedNode != null;
    }

    public Node getAttachedNode() {
        return attachedNode;
    }

    public void attachNode(Node node) {
        this.attachedNode = node;
    }

    public void detachNode() {
        this.attachedNode = null;
    }

    public void setRenderBounds(int x, int y, int width, int height) {
        this.boxX = x;
        this.boxY = y;
        this.boxWidth = width;
        this.boxHeight = height;
    }

    public boolean containsPoint(int px, int py) {
        return px >= boxX && px <= boxX + boxWidth && py >= boxY && py <= boxY + boxHeight;
    }

    private void parseCoordinate(String value) {
        if (value == null) {
            coordX = 0;
            coordY = 0;
            coordZ = 0;
            stringValue = "0,0,0";
            return;
        }
        String sanitized = value.trim().replace("(", "").replace(")", "");
        String[] parts = sanitized.split("[ ,]+");
        if (parts.length >= 3) {
            coordX = parseCoord(parts[0]);
            coordY = parseCoord(parts[1]);
            coordZ = parseCoord(parts[2]);
        } else {
            coordX = coordY = coordZ = 0;
        }
        this.stringValue = coordX + "," + coordY + "," + coordZ;
    }

    private int parseCoord(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
