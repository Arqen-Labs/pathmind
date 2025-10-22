package com.pathmind.execution;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.IMineProcess;
import baritone.api.process.IExploreProcess;
import baritone.api.process.IFarmProcess;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Tracks Baritone processes precisely by monitoring their actual state changes.
 * This provides exact completion detection instead of timeouts or approximations.
 */
public class PreciseCompletionTracker {
    
    private static PreciseCompletionTracker instance;
    private final Map<String, CompletableFuture<Void>> pendingTasks = new ConcurrentHashMap<>();
    private final Map<String, ProcessState> processStates = new ConcurrentHashMap<>();
    private final Map<String, Long> taskStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> nextCheckTimes = new ConcurrentHashMap<>();
    private MineTaskData mineTaskData;

    // Task types
    public static final String TASK_GOTO = "goto";
    public static final String TASK_PATH = "path";
    public static final String TASK_GOAL = "goal";
    public static final String TASK_MINE = "mine";
    public static final String TASK_EXPLORE = "explore";
    public static final String TASK_FARM = "farm";
    
    // Maximum monitoring duration (in milliseconds) - safety fallback
    private static final long MAX_MONITORING_DURATION = 300000; // 5 minutes
    private static final long MONITOR_INTERVAL_MS = 100; // 100ms between checks
    
    private enum ProcessState {
        STARTING,
        ACTIVE,
        COMPLETING,
        COMPLETED,
        FAILED
    }

    private static final class MineTaskData {
        final String blockId;
        final Item targetItem;
        final int targetAmount;
        final int startingCount;
        volatile boolean stopIssued;

        MineTaskData(String blockId, Item targetItem, int targetAmount, int startingCount) {
            this.blockId = blockId;
            this.targetItem = targetItem;
            this.targetAmount = targetAmount;
            this.startingCount = startingCount;
            this.stopIssued = false;
        }
    }
    
    private PreciseCompletionTracker() {}
    
    public static PreciseCompletionTracker getInstance() {
        if (instance == null) {
            instance = new PreciseCompletionTracker();
        }
        return instance;
    }
    
    /**
     * Start tracking a task with precise completion detection
     */
    public void startTrackingTask(String taskId, CompletableFuture<Void> future) {
        pendingTasks.put(taskId, future);
        processStates.put(taskId, ProcessState.STARTING);
        taskStartTimes.put(taskId, System.currentTimeMillis());

        System.out.println("PreciseCompletionTracker: Started tracking task: " + taskId);

        // Start monitoring this specific task
        startMonitoringTask(taskId);
    }

    public boolean startMineTask(String blockId, int targetAmount, CompletableFuture<Void> future) {
        if (blockId == null || blockId.isEmpty() || future == null || targetAmount <= 0) {
            return false;
        }

        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return false;
        }

        Block block = Registries.BLOCK.get(identifier);
        Item targetItem = block.asItem();
        if (targetItem == Items.AIR) {
            return false;
        }

        int startingCount = getCurrentInventoryCount(targetItem);
        mineTaskData = new MineTaskData(blockId, targetItem, targetAmount, startingCount);

        System.out.println("PreciseCompletionTracker: tracking \"" + blockId + "\" until " + targetAmount + " blocks are mined");
        startTrackingTask(TASK_MINE, future);
        return true;
    }

    public void clearMineTracking() {
        mineTaskData = null;
    }
    
    /**
     * Start monitoring a specific task
     */
    private void startMonitoringTask(String taskId) {
        cancelMonitoringTask(taskId);
        nextCheckTimes.put(taskId, 0L);
    }

    public void tick() {
        if (pendingTasks.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (String taskId : pendingTasks.keySet()) {
            long nextCheck = nextCheckTimes.getOrDefault(taskId, 0L);
            if (now < nextCheck) {
                continue;
            }

            if (!pendingTasks.containsKey(taskId)) {
                cancelMonitoringTask(taskId);
                continue;
            }

            try {
                boolean completed = checkTaskCompletion(taskId);
                if (!completed) {
                    scheduleNextCheck(taskId, now);
                }
            } catch (Exception e) {
                System.err.println("PreciseCompletionTracker: Error monitoring task " + taskId + ": " + e.getMessage());
                completeTaskWithError(taskId, "Monitoring error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if a specific task has completed
     */
    private boolean checkTaskCompletion(String taskId) {
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            completeTaskWithError(taskId, "Baritone not available");
            return true;
        }
        
        // Check for timeout
        Long startTime = taskStartTimes.get(taskId);
        if (startTime != null && System.currentTimeMillis() - startTime > MAX_MONITORING_DURATION) {
            completeTaskWithError(taskId, "Task timed out after " + (MAX_MONITORING_DURATION / 1000) + " seconds");
            return true;
        }
        
        ProcessState currentState = processStates.get(taskId);
        if (currentState == ProcessState.COMPLETED || currentState == ProcessState.FAILED) {
            return true; // Already handled
        }
        
        boolean completed = false;
        ProcessState newState = currentState;
        
        switch (taskId) {
            case TASK_GOTO:
            case TASK_PATH:
                completed = checkPathingCompletion(baritone, taskId);
                break;
                
            case TASK_GOAL:
                completed = checkGoalCompletion(baritone, taskId);
                break;
                
            case TASK_MINE:
                completed = checkMiningCompletion(baritone, taskId);
                break;
                
            case TASK_EXPLORE:
                completed = checkExplorationCompletion(baritone, taskId);
                break;
                
            case TASK_FARM:
                completed = checkFarmingCompletion(baritone, taskId);
                break;
                
            default:
                System.err.println("Unknown task type: " + taskId);
                completed = true;
                break;
        }
        
        if (completed) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if pathing tasks (goto/path) have completed
     */
    private boolean checkPathingCompletion(IBaritone baritone, String taskId) {
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        IGetToBlockProcess getToBlockProcess = baritone.getGetToBlockProcess();

        if (pathingBehavior == null || customGoalProcess == null) {
            completeTaskWithError(taskId, "Pathing behavior unavailable");
            return true;
        }

        // Check if pathing has stopped and no goal is active
        boolean hasPath = pathingBehavior.hasPath();
        boolean isPathing = pathingBehavior.isPathing();
        boolean isActive = customGoalProcess.isActive();
        boolean getToBlockActive = getToBlockProcess != null && getToBlockProcess.isActive();
        
        // Get current state
        ProcessState currentState = processStates.get(taskId);
        
        if (currentState == ProcessState.STARTING && (isActive || getToBlockActive)) {
            // Task has started
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE && !isActive && !getToBlockActive && !hasPath && !isPathing) {
            // Task has completed - no longer active and no pathing happening
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
            completeTask(taskId);
            return true;
        } else if (currentState == ProcessState.ACTIVE && !isActive && !getToBlockActive && hasPath) {
            // Task is finishing - no longer active but still has a path (might be reaching goal)
            processStates.put(taskId, ProcessState.COMPLETING);
            System.out.println("PreciseCompletionTracker: " + taskId + " is completing");
        } else if (currentState == ProcessState.COMPLETING && !hasPath && !isPathing && !getToBlockActive) {
            // Path finished - task completed
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - path finished");
            completeTask(taskId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if goal setting has completed
     */
    private boolean checkGoalCompletion(IBaritone baritone, String taskId) {
        // Goal setting is immediate, so complete right away
        completeTask(taskId);
        return true;
    }
    
    /**
     * Check if mining has completed
     */
    private boolean checkMiningCompletion(IBaritone baritone, String taskId) {
        IMineProcess mineProcess = baritone.getMineProcess();
        if (mineProcess == null) {
            completeTaskWithError(taskId, "Mine process unavailable");
            return true;
        }

        ProcessState currentState = processStates.get(taskId);

        if (currentState == ProcessState.STARTING && mineProcess.isActive()) {
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE) {
            if (mineTaskData != null) {
                int mined = Math.max(0, getCurrentInventoryCount(mineTaskData.targetItem) - mineTaskData.startingCount);
                if (mined >= mineTaskData.targetAmount && !mineTaskData.stopIssued) {
                    System.out.println("PreciseCompletionTracker: mine target reached (" + mined + "/" + mineTaskData.targetAmount + ")");
                    issueStopCommand();
                    mineTaskData.stopIssued = true;
                }
            }

            if (!mineProcess.isActive()) {
                System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
                completeTask(taskId);
                return true;
            }
        }

        return false;
    }

    private int getCurrentInventoryCount(Item targetItem) {
        if (targetItem == null) {
            return 0;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }

        if (client.isOnThread()) {
            return countItemInInventory(client.player.getInventory(), targetItem);
        }

        try {
            return client.submit(() -> {
                PlayerInventory inventory = client.player != null ? client.player.getInventory() : null;
                return countItemInInventory(inventory, targetItem);
            }).get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            System.err.println("PreciseCompletionTracker: Failed to query inventory count: " + e.getMessage());
        }

        return 0;
    }

    private int countItemInInventory(PlayerInventory inventory, Item targetItem) {
        if (inventory == null || targetItem == null) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void issueStopCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null && client.player.networkHandler != null) {
                client.player.networkHandler.sendChatMessage("#stop");
            }
        });
    }
    
    /**
     * Check if exploration has completed
     */
    private boolean checkExplorationCompletion(IBaritone baritone, String taskId) {
        IExploreProcess exploreProcess = baritone.getExploreProcess();

        if (exploreProcess == null) {
            completeTaskWithError(taskId, "Explore process unavailable");
            return true;
        }

        ProcessState currentState = processStates.get(taskId);

        if (currentState == ProcessState.STARTING && exploreProcess.isActive()) {
            // Exploration has started
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE && !exploreProcess.isActive()) {
            // Exploration has completed
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
            completeTask(taskId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if farming has completed
     */
    private boolean checkFarmingCompletion(IBaritone baritone, String taskId) {
        IFarmProcess farmProcess = baritone.getFarmProcess();

        if (farmProcess == null) {
            completeTaskWithError(taskId, "Farm process unavailable");
            return true;
        }

        ProcessState currentState = processStates.get(taskId);

        if (currentState == ProcessState.STARTING && farmProcess.isActive()) {
            // Farming has started
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE && !farmProcess.isActive()) {
            // Farming has completed
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
            completeTask(taskId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Complete a task successfully
     */
    private void completeTask(String taskId) {
        cancelMonitoringTask(taskId);
        CompletableFuture<Void> future = pendingTasks.remove(taskId);
        processStates.remove(taskId);
        Long startTime = taskStartTimes.remove(taskId);
        
        if (TASK_MINE.equals(taskId)) {
            mineTaskData = null;
        }

        if (future != null && !future.isDone()) {
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            System.out.println("PreciseCompletionTracker: Completing task " + taskId + " (duration: " + duration + "ms)");
            processStates.put(taskId, ProcessState.COMPLETED);
            future.complete(null);
        }
    }
    
    /**
     * Complete a task with an error
     */
    private void completeTaskWithError(String taskId, String reason) {
        cancelMonitoringTask(taskId);
        CompletableFuture<Void> future = pendingTasks.remove(taskId);
        processStates.remove(taskId);
        taskStartTimes.remove(taskId);
        
        if (TASK_MINE.equals(taskId)) {
            mineTaskData = null;
        }

        if (future != null && !future.isDone()) {
            System.out.println("PreciseCompletionTracker: Completing task " + taskId + " with error: " + reason);
            processStates.put(taskId, ProcessState.FAILED);
            future.completeExceptionally(new RuntimeException(reason));
        }
    }
    
    /**
     * Cancel all pending tasks
     */
    public void cancelAllTasks() {
        System.out.println("PreciseCompletionTracker: Canceling all pending tasks (" + pendingTasks.size() + " tasks)");

        for (String taskId : pendingTasks.keySet()) {
            CompletableFuture<Void> future = pendingTasks.get(taskId);
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new RuntimeException("All tasks cancelled"));
            }
        }

        mineTaskData = null;

        pendingTasks.clear();
        processStates.clear();
        taskStartTimes.clear();
        nextCheckTimes.clear();
    }
    
    /**
     * Get the Baritone instance
     */
    private IBaritone getBaritone() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            System.err.println("PreciseCompletionTracker: Failed to get Baritone instance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the number of pending tasks
     */
    public int getPendingTaskCount() {
        return pendingTasks.size();
    }
    
    /**
     * Check if a task is still pending
     */
    public boolean isTaskPending(String taskId) {
        return pendingTasks.containsKey(taskId);
    }

    private void cancelMonitoringTask(String taskId) {
        nextCheckTimes.remove(taskId);
    }

    private void scheduleNextCheck(String taskId, long now) {
        nextCheckTimes.put(taskId, now + MONITOR_INTERVAL_MS);
    }
}
