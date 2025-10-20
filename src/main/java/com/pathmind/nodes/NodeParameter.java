package com.pathmind.nodes;

import java.util.OptionalDouble;

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
    private Node attachedParameterNode;
    private String attachedParameterNodeId;

    public NodeParameter(String name, ParameterType type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.stringValue = defaultValue;
        this.intValue = 0;
        this.doubleValue = 0.0;
        this.boolValue = false;
        this.attachedParameterNode = null;
        this.attachedParameterNodeId = null;
        
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
        }
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getStringValue() {
        if (attachedParameterNode != null) {
            NodeType attachedType = attachedParameterNode.getType();
            switch (attachedType) {
                case PARAM_BLOCK_TYPE: {
                    NodeParameter blockParam = attachedParameterNode.getParameter("Block");
                    if (blockParam != null) {
                        return blockParam.getStringValue();
                    }
                    break;
                }
                case PARAM_ITEM_STACK: {
                    NodeParameter itemParam = attachedParameterNode.getParameter("Item");
                    if (itemParam != null) {
                        return itemParam.getStringValue();
                    }
                    break;
                }
                case PARAM_NUMBER: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getDisplayValue();
                    }
                    break;
                }
                case PARAM_BOOLEAN: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return Boolean.toString(valueParam.getBoolValue());
                    }
                    break;
                }
                case PARAM_STRING: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_PLAYER_NAME: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Player");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_ENTITY_TYPE: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Entity");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_WAYPOINT_NAME: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Waypoint");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_WAYPOINT_TAG: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Tag");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_SCHEMATIC: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Schematic");
                    if (valueParam != null) {
                        return valueParam.getStringValue();
                    }
                    break;
                }
                case PARAM_COORDINATE:
                case PARAM_ITEM_LOCATION: {
                    OptionalDouble component = attachedParameterNode.resolveCoordinateComponent(name);
                    if (component.isPresent()) {
                        double value = component.getAsDouble();
                        if (Math.abs(value - Math.rint(value)) < 1e-6) {
                            return Integer.toString((int) Math.round(value));
                        }
                        return Double.toString(value);
                    }
                    break;
                }
                default:
                    break;
            }
        }
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
        }
    }

    public int getIntValue() {
        if (attachedParameterNode != null) {
            NodeType attachedType = attachedParameterNode.getType();
            switch (attachedType) {
                case PARAM_COORDINATE:
                case PARAM_ITEM_LOCATION: {
                    OptionalDouble component = attachedParameterNode.resolveCoordinateComponent(name);
                    if (component.isPresent()) {
                        return (int) Math.round(component.getAsDouble());
                    }
                    break;
                }
                case PARAM_NUMBER: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return (int) Math.round(valueParam.getDoubleValue());
                    }
                    break;
                }
                case PARAM_BOOLEAN: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getBoolValue() ? 1 : 0;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return intValue;
    }

    public void setIntValue(int value) {
        this.intValue = value;
        this.stringValue = String.valueOf(value);
    }

    public double getDoubleValue() {
        if (attachedParameterNode != null) {
            NodeType attachedType = attachedParameterNode.getType();
            switch (attachedType) {
                case PARAM_COORDINATE:
                case PARAM_ITEM_LOCATION: {
                    OptionalDouble component = attachedParameterNode.resolveCoordinateComponent(name);
                    if (component.isPresent()) {
                        return component.getAsDouble();
                    }
                    break;
                }
                case PARAM_NUMBER: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getDoubleValue();
                    }
                    break;
                }
                case PARAM_BOOLEAN: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getBoolValue() ? 1.0 : 0.0;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return doubleValue;
    }

    public void setDoubleValue(double value) {
        this.doubleValue = value;
        this.stringValue = String.valueOf(value);
    }

    public boolean getBoolValue() {
        if (attachedParameterNode != null) {
            NodeType attachedType = attachedParameterNode.getType();
            switch (attachedType) {
                case PARAM_BOOLEAN: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getBoolValue();
                    }
                    break;
                }
                case PARAM_NUMBER: {
                    NodeParameter valueParam = attachedParameterNode.getParameter("Value");
                    if (valueParam != null) {
                        return valueParam.getDoubleValue() != 0.0;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return boolValue;
    }

    public void setBoolValue(boolean value) {
        this.boolValue = value;
        this.stringValue = String.valueOf(value);
    }

    public boolean hasAttachedParameterNode() {
        return attachedParameterNode != null;
    }

    public void attachParameterNode(Node node) {
        this.attachedParameterNode = node;
        this.attachedParameterNodeId = node != null ? node.getId() : null;
    }

    public void detachParameterNode() {
        if (attachedParameterNode != null) {
            attachedParameterNode.clearParameterParentAttachment();
        }
        this.attachedParameterNode = null;
        this.attachedParameterNodeId = null;
    }

    public Node getAttachedParameterNode() {
        return attachedParameterNode;
    }

    public String getAttachedParameterNodeId() {
        return attachedParameterNodeId;
    }

    public void setAttachedParameterNodeId(String nodeId) {
        this.attachedParameterNodeId = nodeId;
    }

    public void setAttachedParameterNode(Node node) {
        this.attachedParameterNode = node;
        this.attachedParameterNodeId = node != null ? node.getId() : null;
    }

    public String getStoredStringValue() {
        return stringValue;
    }

    public String getDisplayValue() {
        switch (type) {
            case INTEGER:
                return String.valueOf(intValue);
            case DOUBLE:
                return String.format("%.2f", doubleValue);
            case BOOLEAN:
                return boolValue ? "True" : "False";
            case STRING:
            default:
                return stringValue;
        }
    }
}
