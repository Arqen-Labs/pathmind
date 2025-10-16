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
import baritone.api.pathing.goals.GoalBlock;
import com.pathmind.execution.PreciseCompletionTracker;

/**
 * Represents a single node in the Pathmind visual editor.
 * Similar to Blender's shader nodes, each node has inputs, outputs, and parameters.
 */
public class Node {
    private final String id;
    private final NodeType type;
    private NodeMode mode;
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
        this.mode = NodeMode.getDefaultModeForNodeType(type);
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
    
    public NodeMode getMode() {
        return mode;
    }
    
    public void setMode(NodeMode mode) {
        this.mode = mode;
        // Reinitialize parameters when mode changes
        parameters.clear();
        initializeParameters();
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
        return type == NodeType.START ? 0 : 1;
    }

    public int getOutputSocketCount() {
        return type == NodeType.END ? 0 : 1;
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
     * Initialize default parameters for each node type and mode
     */
    private void initializeParameters() {
        // Handle generalized nodes with modes
        if (mode != null) {
            switch (mode) {
                // GOTO modes
                case GOTO_XYZ:
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                case GOTO_XZ:
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                case GOTO_Y:
                    parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "64"));
                    break;
                case GOTO_BLOCK:
                    parameters.add(new NodeParameter("Block", ParameterType.STRING, "stone"));
                    break;
                case GOTO_PORTAL:
                case GOTO_ENDER_CHEST:
                    // No parameters needed
                    break;
                    
                // GOAL modes
                case GOAL_XYZ:
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                case GOAL_XZ:
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                case GOAL_Y:
                    parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "64"));
                    break;
                case GOAL_CURRENT:
                case GOAL_CLEAR:
                    // No parameters needed
                    break;
                    
                // MINE modes
                case MINE_SINGLE:
                    parameters.add(new NodeParameter("Block", ParameterType.STRING, "stone"));
                    break;
                case MINE_MULTIPLE:
                    parameters.add(new NodeParameter("Blocks", ParameterType.STRING, "stone,dirt"));
                    break;
                    
                // BUILD modes
                case BUILD_PLAYER:
                    parameters.add(new NodeParameter("Schematic", ParameterType.STRING, "house.schematic"));
                    break;
                case BUILD_XYZ:
                    parameters.add(new NodeParameter("Schematic", ParameterType.STRING, "house.schematic"));
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                    
                // EXPLORE modes
                case EXPLORE_CURRENT:
                    // No parameters needed
                    break;
                case EXPLORE_XYZ:
                    parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                    parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                    break;
                case EXPLORE_FILTER:
                    parameters.add(new NodeParameter("Filter", ParameterType.STRING, "explore.txt"));
                    break;
                    
                // FOLLOW modes
                case FOLLOW_PLAYER:
                    parameters.add(new NodeParameter("Player", ParameterType.STRING, "PlayerName"));
                    break;
                case FOLLOW_PLAYERS:
                case FOLLOW_ENTITIES:
                    // No parameters needed
                    break;
                case FOLLOW_ENTITY_TYPE:
                    parameters.add(new NodeParameter("Entity", ParameterType.STRING, "cow"));
                    break;
                    
                // FARM modes
                case FARM_RANGE:
                    parameters.add(new NodeParameter("Range", ParameterType.INTEGER, "10"));
                    break;
                case FARM_WAYPOINT:
                    parameters.add(new NodeParameter("Waypoint", ParameterType.STRING, "farm"));
                    parameters.add(new NodeParameter("Range", ParameterType.INTEGER, "10"));
                    break;
                    
                // STOP modes
                case STOP_NORMAL:
                case STOP_CANCEL:
                case STOP_FORCE:
                    // No parameters needed
                    break;
                    
                default:
                    // No parameters needed
                    break;
            }
            return;
        }
        
        // Handle node types that don't use modes
        switch (type) {
            case PLACE:
                parameters.add(new NodeParameter("Block", ParameterType.BLOCK_TYPE, "stone"));
                parameters.add(new NodeParameter("X", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Y", ParameterType.INTEGER, "0"));
                parameters.add(new NodeParameter("Z", ParameterType.INTEGER, "0"));
                break;
            case CRAFT:
                parameters.add(new NodeParameter("Item", ParameterType.STRING, "stick"));
                parameters.add(new NodeParameter("Quantity", ParameterType.INTEGER, "1"));
                break;
            case WAIT:
                parameters.add(new NodeParameter("Duration", ParameterType.DOUBLE, "1.0"));
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
            default:
                // No parameters needed
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
                
            // Generalized nodes
            case GOTO:
                executeGotoCommand(future);
                break;
            case GOAL:
                executeGoalCommand(future);
                break;
            case MINE:
                executeMineCommand(future);
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
            case FARM:
                executeFarmCommand(future);
                break;
            case STOP:
                executeStopCommand(future);
                break;
            case PLACE:
                executePlaceCommand(future);
                break;
            case CRAFT:
                executeCraftCommand(future);
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
                
            // Legacy nodes
            case PATH:
                executePathCommand(future);
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
                
            default:
                System.out.println("Unknown node type: " + type);
                future.complete(null);
                break;
        }
    }
    
    // Command execution methods that wait for Baritone completion
    private void executeGotoCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for GOTO node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for goto command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        
        switch (mode) {
            case GOTO_XYZ:
                int x = 0, y = 64, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Executing goto to: " + x + ", " + y + ", " + z);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                GoalBlock goal = new GoalBlock(x, y, z);
                customGoalProcess.setGoalAndPath(goal);
                break;
                
            case GOTO_XZ:
                int x2 = 0, z2 = 0;
                NodeParameter xParam2 = getParameter("X");
                NodeParameter zParam2 = getParameter("Z");
                
                if (xParam2 != null) x2 = xParam2.getIntValue();
                if (zParam2 != null) z2 = zParam2.getIntValue();
                
                System.out.println("Executing goto to: " + x2 + ", " + z2);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                GoalBlock goal2 = new GoalBlock(x2, 0, z2); // Y will be determined by pathfinding
                customGoalProcess.setGoalAndPath(goal2);
                break;
                
            case GOTO_Y:
                int y3 = 64;
                NodeParameter yParam3 = getParameter("Y");
                if (yParam3 != null) y3 = yParam3.getIntValue();
                
                System.out.println("Executing goto to Y level: " + y3);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                // For Y-only movement, we need to get current X,Z and set goal there
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    int currentX = (int) client.player.getX();
                    int currentZ = (int) client.player.getZ();
                    GoalBlock goal3 = new GoalBlock(currentX, y3, currentZ);
                    customGoalProcess.setGoalAndPath(goal3);
                }
                break;
                
            case GOTO_BLOCK:
                String block = "stone";
                NodeParameter blockParam = getParameter("Block");
                if (blockParam != null) {
                    block = blockParam.getStringValue();
                }
                
                System.out.println("Executing goto to block: " + block);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                // Use Baritone's block goal - need to find the block first
                executeCommand("#goto " + block);
                break;
                
            case GOTO_PORTAL:
                System.out.println("Executing goto to nearest portal");
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                // Use Baritone's portal goal
                executeCommand("#goto portal");
                break;
                
            case GOTO_ENDER_CHEST:
                System.out.println("Executing goto to nearest ender chest");
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                // Use Baritone's ender chest goal
                executeCommand("#goto ender_chest");
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown GOTO mode: " + mode));
                break;
        }
    }
    
    private void executeMineCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for MINE node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for mine command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IMineProcess mineProcess = baritone.getMineProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_MINE, future);
        
        switch (mode) {
            case MINE_SINGLE:
                String block = "stone";
                NodeParameter blockParam = getParameter("Block");
                if (blockParam != null) {
                    block = blockParam.getStringValue();
                }
                
                System.out.println("Executing mine for: " + block);
                mineProcess.mineByName(block);
                break;
                
            case MINE_MULTIPLE:
                String blocks = "stone,dirt";
                NodeParameter blocksParam = getParameter("Blocks");
                if (blocksParam != null) {
                    blocks = blocksParam.getStringValue();
                }
                
                System.out.println("Executing mine for blocks: " + blocks);
                // Split the comma-separated block names and mine them
                String[] blockNames = blocks.split(",");
                for (String blockName : blockNames) {
                    mineProcess.mineByName(blockName.trim());
                }
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown MINE mode: " + mode));
                break;
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
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for BUILD node"));
            return;
        }
        
        String schematic = "house.schematic";
        NodeParameter schematicParam = getParameter("Schematic");
        if (schematicParam != null) {
            schematic = schematicParam.getStringValue();
        }
        
        String command;
        switch (mode) {
            case BUILD_PLAYER:
                command = String.format("#build %s", schematic);
                System.out.println("Executing build at player location: " + command);
                break;
                
            case BUILD_XYZ:
                int x = 0, y = 0, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                command = String.format("#build %s %d %d %d", schematic, x, y, z);
                System.out.println("Executing build at coordinates: " + command);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown BUILD mode: " + mode));
                return;
        }
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeExploreCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for EXPLORE node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for explore command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IExploreProcess exploreProcess = baritone.getExploreProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_EXPLORE, future);
        
        switch (mode) {
            case EXPLORE_CURRENT:
                System.out.println("Executing explore from current position");
                exploreProcess.explore(0, 0); // 0,0 means from current position
                break;
                
            case EXPLORE_XYZ:
                int x = 0, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Executing explore at: " + x + ", " + z);
                exploreProcess.explore(x, z);
                break;
                
            case EXPLORE_FILTER:
                String filter = "explore.txt";
                NodeParameter filterParam = getParameter("Filter");
                if (filterParam != null) {
                    filter = filterParam.getStringValue();
                }
                
                System.out.println("Executing explore with filter: " + filter);
                // For filter-based exploration, we need to use a different approach
                executeCommand("#explore " + filter);
                future.complete(null); // Command-based exploration completes immediately
                return;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown EXPLORE mode: " + mode));
                return;
        }
    }
    
    private void executeFollowCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for FOLLOW node"));
            return;
        }
        
        String command;
        switch (mode) {
            case FOLLOW_PLAYER:
                String player = "PlayerName";
                NodeParameter playerParam = getParameter("Player");
                if (playerParam != null) {
                    player = playerParam.getStringValue();
                }
                
                command = "#follow " + player;
                System.out.println("Executing follow player: " + command);
                break;
                
            case FOLLOW_PLAYERS:
                command = "#follow players";
                System.out.println("Executing follow any players: " + command);
                break;
                
            case FOLLOW_ENTITIES:
                command = "#follow entities";
                System.out.println("Executing follow any entities: " + command);
                break;
                
            case FOLLOW_ENTITY_TYPE:
                String entity = "cow";
                NodeParameter entityParam = getParameter("Entity");
                if (entityParam != null) {
                    entity = entityParam.getStringValue();
                }
                
                command = "#follow " + entity;
                System.out.println("Executing follow entity type: " + command);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown FOLLOW mode: " + mode));
                return;
        }
        
        executeCommand(command);
        future.complete(null); // Follow commands complete immediately
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
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for GOAL node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for goal command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        
        switch (mode) {
            case GOAL_XYZ:
                int x = 0, y = 64, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Setting goal to: " + x + ", " + y + ", " + z);
                GoalBlock goal = new GoalBlock(x, y, z);
                customGoalProcess.setGoal(goal);
                break;
                
            case GOAL_XZ:
                int x2 = 0, z2 = 0;
                NodeParameter xParam2 = getParameter("X");
                NodeParameter zParam2 = getParameter("Z");
                
                if (xParam2 != null) x2 = xParam2.getIntValue();
                if (zParam2 != null) z2 = zParam2.getIntValue();
                
                System.out.println("Setting goal to: " + x2 + ", " + z2);
                GoalBlock goal2 = new GoalBlock(x2, 0, z2); // Y will be determined by pathfinding
                customGoalProcess.setGoal(goal2);
                break;
                
            case GOAL_Y:
                int y3 = 64;
                NodeParameter yParam3 = getParameter("Y");
                if (yParam3 != null) y3 = yParam3.getIntValue();
                
                System.out.println("Setting goal to Y level: " + y3);
                // For Y-only goal, we need to get current X,Z and set goal there
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    int currentX = (int) client.player.getX();
                    int currentZ = (int) client.player.getZ();
                    GoalBlock goal3 = new GoalBlock(currentX, y3, currentZ);
                    customGoalProcess.setGoal(goal3);
                }
                break;
                
            case GOAL_CURRENT:
                System.out.println("Setting goal to current position");
                net.minecraft.client.MinecraftClient client2 = net.minecraft.client.MinecraftClient.getInstance();
                if (client2 != null && client2.player != null) {
                    int currentX = (int) client2.player.getX();
                    int currentY = (int) client2.player.getY();
                    int currentZ = (int) client2.player.getZ();
                    GoalBlock goal4 = new GoalBlock(currentX, currentY, currentZ);
                    customGoalProcess.setGoal(goal4);
                }
                break;
                
            case GOAL_CLEAR:
                System.out.println("Clearing current goal");
                customGoalProcess.setGoal(null);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown GOAL mode: " + mode));
                return;
        }
        
        // Goal setting is immediate, no need to wait
        future.complete(null);
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
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for STOP node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for stop command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        switch (mode) {
            case STOP_NORMAL:
                System.out.println("Executing stop command");
                // Cancel all pending tasks first
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            case STOP_CANCEL:
                System.out.println("Executing cancel command");
                // Cancel all pending tasks first
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            case STOP_FORCE:
                System.out.println("Executing force cancel command");
                // Force cancel all tasks
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Force stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown STOP mode: " + mode));
                return;
        }
        
        // Complete immediately since stop is immediate
        future.complete(null);
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
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for FARM node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for farm command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IFarmProcess farmProcess = baritone.getFarmProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_FARM, future);
        
        switch (mode) {
            case FARM_RANGE:
                int range = 10;
                NodeParameter rangeParam = getParameter("Range");
                if (rangeParam != null) {
                    range = rangeParam.getIntValue();
                }
                
                System.out.println("Executing farm within range: " + range);
                farmProcess.farm(range);
                break;
                
            case FARM_WAYPOINT:
                String waypoint = "farm";
                int waypointRange = 10;
                NodeParameter waypointParam = getParameter("Waypoint");
                NodeParameter waypointRangeParam = getParameter("Range");
                
                if (waypointParam != null) {
                    waypoint = waypointParam.getStringValue();
                }
                if (waypointRangeParam != null) {
                    waypointRange = waypointRangeParam.getIntValue();
                }
                
                System.out.println("Executing farm around waypoint: " + waypoint + " with range: " + waypointRange);
                // For waypoint-based farming, we need to use a different approach
                executeCommand("#farm " + waypoint + " " + waypointRange);
                future.complete(null); // Command-based farming completes immediately
                return;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown FARM mode: " + mode));
                return;
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
