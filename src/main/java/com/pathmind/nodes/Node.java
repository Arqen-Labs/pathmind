package com.pathmind.nodes;

import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import baritone.api.process.IExploreProcess;
import baritone.api.process.IFollowProcess;
import baritone.api.process.IFarmProcess;
import baritone.api.process.IElytraProcess;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BetterBlockPos;
import com.pathmind.execution.PreciseCompletionTracker;

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

    /**
     * Gets the Baritone instance for the current player
     * @return IBaritone instance or null if not available
     */
    private IBaritone getBaritone() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            System.err.println("Failed to get Baritone instance: " + e.getMessage());
            return null;
        }
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
        int x = 0, y = 64, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        System.out.println("Executing goto to: " + x + ", " + y + ", " + z);
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
            
            // Start the Baritone task
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            GoalBlock goal = new GoalBlock(x, y, z);
            customGoalProcess.setGoalAndPath(goal);
            
            // The future will be completed by the TaskCompletionManager when the path reaches the goal
        } else {
            System.err.println("Baritone not available for goto command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
    }
    
    private void executeMineCommand(CompletableFuture<Void> future) {
        String block = "stone";
        NodeParameter blockParam = getParameter("Block");
        if (blockParam != null) {
            block = blockParam.getStringValue();
        }
        
        System.out.println("Executing mine for: " + block);
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_MINE, future);
            
            // Start the Baritone mining task
            IMineProcess mineProcess = baritone.getMineProcess();
            mineProcess.mineByName(block);
            
            // The future will be completed by the PreciseCompletionTracker when mining actually finishes
        } else {
            System.err.println("Baritone not available for mine command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
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
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
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
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeBuildCommand(CompletableFuture<Void> future) {
        String schematic = "house.schematic";
        NodeParameter schematicParam = getParameter("Schematic");
        if (schematicParam != null) {
            schematic = schematicParam.getStringValue();
        }
        
        String command = String.format("#build %s", schematic);
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeExploreCommand(CompletableFuture<Void> future) {
        int x = 0, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        System.out.println("Executing explore at: " + x + ", " + z);
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_EXPLORE, future);
            
            // Start the Baritone exploration task
            IExploreProcess exploreProcess = baritone.getExploreProcess();
            exploreProcess.explore(x, z);
            
            // The future will be completed by the PreciseCompletionTracker when exploration actually finishes
        } else {
            System.err.println("Baritone not available for explore command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
    }
    
    private void executeFollowCommand(CompletableFuture<Void> future) {
        String player = "PlayerName";
        NodeParameter playerParam = getParameter("Player");
        if (playerParam != null) {
            player = playerParam.getStringValue();
        }
        
        System.out.println("Executing follow for: " + player);
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            IFollowProcess followProcess = baritone.getFollowProcess();
            // Note: Follow process doesn't have a direct method for player names
            // This would need to be implemented differently or use commands
            executeCommand("#follow " + player);
            future.complete(null); // Follow command completes immediately
        } else {
            System.err.println("Baritone not available for follow command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
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
        int x = 0, y = 64, z = 0;
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();
        
        System.out.println("Setting goal to: " + x + ", " + y + ", " + z);
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            GoalBlock goal = new GoalBlock(x, y, z);
            customGoalProcess.setGoal(goal);
            
            // Goal setting is immediate, no need to wait
            future.complete(null);
        } else {
            System.err.println("Baritone not available for goal command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
    }
    
    private void executePathCommand(CompletableFuture<Void> future) {
        System.out.println("Executing path command");
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_PATH, future);
            
            // Start the Baritone pathing task
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            customGoalProcess.path();
            
            // The future will be completed by the PreciseCompletionTracker when the path actually reaches the goal
        } else {
            System.err.println("Baritone not available for path command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
    }
    
    private void executeStopCommand(CompletableFuture<Void> future) {
        System.out.println("Executing stop command");
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Cancel all pending tasks first
            PreciseCompletionTracker.getInstance().cancelAllTasks();
            
            // Stop all Baritone processes
            baritone.getPathingBehavior().cancelEverything();
            
            // Complete immediately since stop is immediate
            future.complete(null);
        } else {
            System.err.println("Baritone not available for stop command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
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
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeSurfaceCommand(CompletableFuture<Void> future) {
        String command = "#surface";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeTunnelCommand(CompletableFuture<Void> future) {
        String command = "#tunnel";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeFarmCommand(CompletableFuture<Void> future) {
        System.out.println("Executing farm command");
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_FARM, future);
            
            // Start the Baritone farming task
            IFarmProcess farmProcess = baritone.getFarmProcess();
            farmProcess.farm();
            
            // The future will be completed by the PreciseCompletionTracker when farming actually finishes
        } else {
            System.err.println("Baritone not available for farm command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
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
    
    
}
