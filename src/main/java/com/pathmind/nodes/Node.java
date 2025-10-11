package com.pathmind.nodes;

import net.minecraft.text.Text;

/**
 * Represents a single node in the Pathmind visual editor.
 * Similar to Blender's shader nodes, each node has inputs, outputs, and parameters.
 */
public class Node {
    private final String id;
    private final NodeType type;
    private int x, y;
    private final int width = 80;
    private final int height = 100;
    private boolean selected = false;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    public Node(NodeType type, int x, int y) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public int getDragOffsetX() {
        return dragOffsetX;
    }

    public void setDragOffsetX(int dragOffsetX) {
        this.dragOffsetX = dragOffsetX;
    }

    public int getDragOffsetY() {
        return dragOffsetY;
    }

    public void setDragOffsetY(int dragOffsetY) {
        this.dragOffsetY = dragOffsetY;
    }

    public boolean containsPoint(int pointX, int pointY) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }

    public Text getDisplayName() {
        return Text.literal(type.getDisplayName());
    }

    public int getInputSocketCount() {
        switch (type) {
            case START:
                return 0;
            case END:
                return 1;
            case MINE:
            case CRAFT:
            case PLACE:
            case MOVE:
            case WAIT:
            default:
                return 1;
        }
    }

    public int getOutputSocketCount() {
        switch (type) {
            case START:
            case MINE:
            case CRAFT:
            case PLACE:
            case MOVE:
            case WAIT:
                return 1;
            case END:
                return 0;
            default:
                return 1;
        }
    }

    public int getSocketY(int socketIndex, boolean isInput) {
        int socketHeight = 12;
        int headerHeight = 14;
        int contentStartY = y + headerHeight + 6; // Start sockets below header with some padding
        return contentStartY + socketIndex * socketHeight;
    }
    
    public int getSocketX(boolean isInput) {
        return isInput ? x - 4 : x + width + 4;
    }
    
    public boolean isSocketClicked(int mouseX, int mouseY, int socketIndex, boolean isInput) {
        int socketX = getSocketX(isInput);
        int socketY = getSocketY(socketIndex, isInput);
        int socketRadius = 6; // Smaller size for more space
        
        return Math.abs(mouseX - socketX) <= socketRadius && Math.abs(mouseY - socketY) <= socketRadius;
    }
}
