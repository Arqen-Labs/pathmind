package com.pathmind.nodes;

import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private final List<NodeParameter> parameters;

    public Node(NodeType type, int x, int y) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.x = x;
        this.y = y;
        this.parameters = new ArrayList<>();
        initializeParameters();
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
        // START and END nodes are square, others are rectangular
        if (type == NodeType.START || type == NodeType.END) {
            return 30; // Even smaller square size
        }
        return width;
    }

    public int getHeight() {
        // START and END nodes are square, others are rectangular
        if (type == NodeType.START || type == NodeType.END) {
            return 30; // Even smaller square size
        }
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
        return pointX >= x && pointX <= x + getWidth() && pointY >= y && pointY <= y + getHeight();
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
            case GOTO:
            case GOAL:
            case BUILD:
            case EXPLORE:
            case FOLLOW:
            case WAIT:
            case MESSAGE:
            case SET:
            case GET:
                return 1;
            case PATH:
            case STOP:
            case INVERT:
            case COME:
            case SURFACE:
            case TUNNEL:
            case FARM:
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
            case GOTO:
            case GOAL:
            case BUILD:
            case EXPLORE:
            case FOLLOW:
            case WAIT:
            case MESSAGE:
            case SET:
            case GET:
            case PATH:
            case STOP:
            case INVERT:
            case COME:
            case SURFACE:
            case TUNNEL:
            case FARM:
                return 1;
            case END:
                return 0;
            default:
                return 1;
        }
    }

    public int getSocketY(int socketIndex, boolean isInput) {
        int socketHeight = 12;
        if (type == NodeType.START || type == NodeType.END) {
            // For START and END nodes, center the socket vertically
            return y + getHeight() / 2;
        } else {
            int headerHeight = 14;
            int contentStartY = y + headerHeight + 6; // Start sockets below header with some padding
            return contentStartY + socketIndex * socketHeight;
        }
    }
    
    public int getSocketX(boolean isInput) {
        return isInput ? x - 4 : x + getWidth() + 4;
    }
    
    public boolean isSocketClicked(int mouseX, int mouseY, int socketIndex, boolean isInput) {
        int socketX = getSocketX(isInput);
        int socketY = getSocketY(socketIndex, isInput);
        int socketRadius = 6; // Smaller size for more space
        
        return Math.abs(mouseX - socketX) <= socketRadius && Math.abs(mouseY - socketY) <= socketRadius;
    }

    /**
     * Initialize default parameters for each node type
     */
    private void initializeParameters() {
        switch (type) {
            // Navigation Commands
            case GOTO:
            case GOAL:
                parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                break;
            
            // Mining and Building Commands
            case MINE:
                parameters.add(new NodeParameter("Block", ParameterType.STRING, "stone"));
                break;
            case CRAFT:
                parameters.add(new NodeParameter("Item", ParameterType.STRING, "stick"));
                parameters.add(new NodeParameter("Quantity", ParameterType.INTEGER, "1"));
                break;
            case PLACE:
                parameters.add(new NodeParameter("Block", ParameterType.STRING, "stone"));
                parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                break;
            case BUILD:
                parameters.add(new NodeParameter("Schematic", ParameterType.STRING, "house.schematic"));
                break;
            
            // Exploration Commands
            case EXPLORE:
                parameters.add(new NodeParameter("Radius", ParameterType.INTEGER, "100"));
                parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                break;
            case FOLLOW:
                parameters.add(new NodeParameter("Player", ParameterType.STRING, "PlayerName"));
                break;
            
            // Utility Commands
            case WAIT:
                parameters.add(new NodeParameter("Duration", ParameterType.DOUBLE, "1.0"));
                parameters.add(new NodeParameter("Unit", ParameterType.STRING, "seconds"));
                break;
            case MESSAGE:
                parameters.add(new NodeParameter("Text", ParameterType.STRING, "Hello World"));
                break;
            case SET:
                parameters.add(new NodeParameter("Setting", ParameterType.STRING, "allowBreak"));
                parameters.add(new NodeParameter("Value", ParameterType.STRING, "true"));
                break;
            case GET:
                parameters.add(new NodeParameter("Setting", ParameterType.STRING, "allowBreak"));
                break;
            
            // No parameters needed
            case PATH:
            case STOP:
            case INVERT:
            case COME:
            case SURFACE:
            case TUNNEL:
            case FARM:
            case START:
            case END:
            default:
                // These nodes have no parameters
                break;
        }
    }

    /**
     * Get all parameters for this node
     */
    public List<NodeParameter> getParameters() {
        return parameters;
    }

    /**
     * Get a specific parameter by name
     */
    public NodeParameter getParameter(String name) {
        for (NodeParameter param : parameters) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Check if this node has parameters (Start and End nodes don't)
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Get the height needed to display parameters
     */
    public int getParameterDisplayHeight() {
        if (!hasParameters()) {
            return 0;
        }
        return parameters.size() * 12 + 8; // 12px per parameter + padding
    }

    /**
     * Execute this node asynchronously.
     * Returns a CompletableFuture that completes when the node's command is finished.
     */
    public CompletableFuture<Void> execute() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Execute on the main Minecraft thread
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                try {
                    executeNodeCommand(future);
                } catch (Exception e) {
                    System.err.println("Error executing node " + type + ": " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } else {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
        }
        
        return future;
    }
    
    /**
     * Execute the actual command for this node type.
     * This method should be overridden by specific node implementations if needed.
     */
    private void executeNodeCommand(CompletableFuture<Void> future) {
        switch (type) {
            case START:
                // START node doesn't execute any command, just passes through
                System.out.println("START node - passing through");
                future.complete(null);
                break;
                
            case END:
                // END node stops execution
                System.out.println("END node - execution complete");
                future.complete(null);
                break;
                
            case GOTO:
                executeGotoCommand(future);
                break;
            case MINE:
                executeMineCommand(future);
                break;
            case CRAFT:
                executeCraftCommand(future);
                break;
            case PLACE:
                executePlaceCommand(future);
                break;
            case BUILD:
                executeBuildCommand(future);
                break;
            case EXPLORE:
                executeExploreCommand(future);
                break;
            case FOLLOW:
                executeFollowCommand(future);
                break;
            case WAIT:
                executeWaitCommand(future);
                break;
            case MESSAGE:
                executeMessageCommand(future);
                break;
            case SET:
                executeSetCommand(future);
                break;
            case GET:
                executeGetCommand(future);
                break;
            case GOAL:
                executeGoalCommand(future);
                break;
            case PATH:
                executePathCommand(future);
                break;
            case STOP:
                executeStopCommand(future);
                break;
            case INVERT:
                executeInvertCommand(future);
                break;
            case COME:
                executeComeCommand(future);
                break;
            case SURFACE:
                executeSurfaceCommand(future);
                break;
            case TUNNEL:
                executeTunnelCommand(future);
                break;
            case FARM:
                executeFarmCommand(future);
                break;
                
            default:
                System.out.println("Unknown node type: " + type);
                future.complete(null);
                break;
        }
    }
    
    // Command execution methods that wait for Baritone completion
    private void executeGotoCommand(CompletableFuture<Void> future) {
        // Get position parameters
        int x = 0, y = 0, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        String command = String.format("#goto %d %d %d", x, y, z);
        System.out.println("Executing command: " + command);
        
        // Send command to Baritone
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeMineCommand(CompletableFuture<Void> future) {
        String block = "stone";
        NodeParameter blockParam = getParameter("Block");
        if (blockParam != null) {
            block = blockParam.getStringValue();
        }
        
        String command = String.format("#mine %s", block);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeCraftCommand(CompletableFuture<Void> future) {
        String item = "stick";
        int quantity = 1;
        
        NodeParameter itemParam = getParameter("Item");
        NodeParameter quantityParam = getParameter("Quantity");
        
        if (itemParam != null) item = itemParam.getStringValue();
        if (quantityParam != null) quantity = quantityParam.getIntValue();
        
        String command = String.format("#craft %s %d", item, quantity);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executePlaceCommand(CompletableFuture<Void> future) {
        String block = "stone";
        int x = 0, y = 0, z = 0;
        
        NodeParameter blockParam = getParameter("Block");
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        
        if (blockParam != null) block = blockParam.getStringValue();
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        String command = String.format("#place %s %d %d %d", block, x, y, z);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeBuildCommand(CompletableFuture<Void> future) {
        String schematic = "house.schematic";
        NodeParameter schematicParam = getParameter("Schematic");
        if (schematicParam != null) {
            schematic = schematicParam.getStringValue();
        }
        
        String command = String.format("#build %s", schematic);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeExploreCommand(CompletableFuture<Void> future) {
        int x = 0, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        String command = String.format("#explore %d %d", x, z);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeFollowCommand(CompletableFuture<Void> future) {
        String player = "PlayerName";
        NodeParameter playerParam = getParameter("Player");
        if (playerParam != null) {
            player = playerParam.getStringValue();
        }
        
        String command = String.format("#follow %s", player);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeWaitCommand(CompletableFuture<Void> future) {
        double durationValue = 1.0;
        NodeParameter durationParam = getParameter("Duration");
        if (durationParam != null) {
            durationValue = durationParam.getDoubleValue();
        }
        
        final double duration = durationValue; // Make final for lambda
        System.out.println("Waiting for " + duration + " seconds...");
        
        // Use a separate thread for waiting to avoid blocking the main thread
        new Thread(() -> {
            try {
                Thread.sleep((long)(duration * 1000));
                future.complete(null);
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        }).start();
    }
    
    private void executeMessageCommand(CompletableFuture<Void> future) {
        String text = "Hello World";
        NodeParameter textParam = getParameter("Text");
        if (textParam != null) {
            text = textParam.getStringValue();
        }
        
        String command = String.format("#message %s", text);
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Message commands complete immediately
    }
    
    private void executeSetCommand(CompletableFuture<Void> future) {
        String setting = "allowBreak";
        String value = "true";
        
        NodeParameter settingParam = getParameter("Setting");
        NodeParameter valueParam = getParameter("Value");
        
        if (settingParam != null) setting = settingParam.getStringValue();
        if (valueParam != null) value = valueParam.getStringValue();
        
        String command = String.format("#set %s %s", setting, value);
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Set commands complete immediately
    }
    
    private void executeGetCommand(CompletableFuture<Void> future) {
        String setting = "allowBreak";
        NodeParameter settingParam = getParameter("Setting");
        if (settingParam != null) {
            setting = settingParam.getStringValue();
        }
        
        String command = String.format("#get %s", setting);
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Get commands complete immediately
    }
    
    private void executeGoalCommand(CompletableFuture<Void> future) {
        int x = 0, y = 0, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        String command = String.format("#goal %d %d %d", x, y, z);
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executePathCommand(CompletableFuture<Void> future) {
        String command = "#path";
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeStopCommand(CompletableFuture<Void> future) {
        String command = "#stop";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Stop commands complete immediately
    }
    
    private void executeInvertCommand(CompletableFuture<Void> future) {
        String command = "#invert";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Invert commands complete immediately
    }
    
    private void executeComeCommand(CompletableFuture<Void> future) {
        String command = "#come";
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeSurfaceCommand(CompletableFuture<Void> future) {
        String command = "#surface";
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeTunnelCommand(CompletableFuture<Void> future) {
        String command = "#tunnel";
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeFarmCommand(CompletableFuture<Void> future) {
        String command = "#farm";
        System.out.println("Executing command: " + command);
        
        executeCommandAndWaitForCompletion(command, future);
    }
    
    private void executeCommand(String command) {
        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.networkHandler.sendChatMessage(command);
                System.out.println("Sent command to Minecraft: " + command);
            } else {
                System.out.println("Cannot execute command - client or player is null");
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void executeCommandAndWaitForCompletion(String command, CompletableFuture<Void> future) {
        // Send the command
        executeCommand(command);
        
        // Get appropriate delay based on command type
        long delayMs = getCommandDelay(this.getType());
        System.out.println("Waiting " + delayMs + "ms for " + this.getType() + " command to complete...");
        
        // Start a thread to wait for the command to complete
        new Thread(() -> {
            try {
                // Wait for the appropriate duration for this command type
                Thread.sleep(delayMs);
                System.out.println("Baritone command completed: " + command);
                future.complete(null);
            } catch (InterruptedException e) {
                System.err.println("Error waiting for Baritone completion: " + e.getMessage());
                future.completeExceptionally(e);
            }
        }).start();
    }
    
    private long getCommandDelay(NodeType nodeType) {
        switch (nodeType) {
            case GOTO:
                return 8000; // 8 seconds for goto commands (enough time to pathfind)
            case MINE:
                return 5000; // 5 seconds for mining
            case CRAFT:
                return 6000; // 6 seconds for crafting
            case PLACE:
                return 3000; // 3 seconds for placing blocks
            case BUILD:
                return 15000; // 15 seconds for building
            case EXPLORE:
                return 12000; // 12 seconds for exploration
            case FARM:
                return 8000; // 8 seconds for farming
            case GOAL:
                return 2000; // 2 seconds for goal commands
            case PATH:
                return 8000; // 8 seconds for path commands
            case SURFACE:
                return 6000; // 6 seconds for surface commands
            case TUNNEL:
                return 10000; // 10 seconds for tunneling
            case FOLLOW:
                return 8000; // 8 seconds for following
            case STOP:
            case INVERT:
            case COME:
                return 1000; // 1 second for simple commands
            default:
                return 3000; // Default 3 seconds
        }
    }
}
