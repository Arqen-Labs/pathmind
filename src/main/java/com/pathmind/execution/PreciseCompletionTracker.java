package com.pathmind.execution;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import baritone.api.process.IExploreProcess;
import baritone.api.process.IFarmProcess;
import baritone.api.process.IBaritoneProcess;
import baritone.api.pathing.goals.Goal;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Optional;

/**
 * Tracks Baritone processes precisely by monitoring their actual state changes.
 * This provides exact completion detection instead of timeouts or approximations.
 */
public class PreciseCompletionTracker {
    
    private static PreciseCompletionTracker instance;
    private final Map<String, CompletableFuture<Void>> pendingTasks = new ConcurrentHashMap<>();
    private final Map<String, ProcessState> processStates = new ConcurrentHashMap<>();
    private final Map<String, Long> taskStartTimes = new ConcurrentHashMap<>();
    private Timer monitoringTimer;
    
    // Task types
    public static final String TASK_GOTO = "goto";
    public static final String TASK_PATH = "path";
    public static final String TASK_GOAL = "goal";
    public static final String TASK_MINE = "mine";
    public static final String TASK_EXPLORE = "explore";
    public static final String TASK_FARM = "farm";
    
    // Maximum monitoring duration (in milliseconds) - safety fallback
    private static final long MAX_MONITORING_DURATION = 300000; // 5 minutes
    
    private enum ProcessState {
        STARTING,
        ACTIVE,
        COMPLETING,
        COMPLETED,
        FAILED
    }
    
    private PreciseCompletionTracker() {
        this.monitoringTimer = new Timer("PreciseCompletionTimer", true);
    }
    
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
    
    /**
     * Start monitoring a specific task
     */
    private void startMonitoringTask(String taskId) {
        // Schedule monitoring every 100ms for precise detection
        TimerTask monitoringTask = new TimerTask() {
            @Override
            public void run() {
                if (!pendingTasks.containsKey(taskId)) {
                    this.cancel();
                    return;
                }
                
                try {
                    if (checkTaskCompletion(taskId)) {
                        this.cancel();
                    }
                } catch (Exception e) {
                    System.err.println("Error monitoring task " + taskId + ": " + e.getMessage());
                    completeTaskWithError(taskId, "Monitoring error: " + e.getMessage());
                    this.cancel();
                }
            }
        };
        
        monitoringTimer.schedule(monitoringTask, 100, 100); // Start in 100ms, repeat every 100ms
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
        
        // Check if pathing has stopped and no goal is active
        boolean hasPath = pathingBehavior.hasPath();
        boolean isPathing = pathingBehavior.isPathing();
        boolean isActive = customGoalProcess.isActive();
        
        // Get current state
        ProcessState currentState = processStates.get(taskId);
        
        if (currentState == ProcessState.STARTING && isActive) {
            // Task has started
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE && !isActive && !hasPath && !isPathing) {
            // Task has completed - no longer active and no pathing happening
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
            completeTask(taskId);
            return true;
        } else if (currentState == ProcessState.ACTIVE && !isActive && hasPath) {
            // Task is finishing - no longer active but still has a path (might be reaching goal)
            processStates.put(taskId, ProcessState.COMPLETING);
            System.out.println("PreciseCompletionTracker: " + taskId + " is completing");
        } else if (currentState == ProcessState.COMPLETING && !hasPath && !isPathing) {
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
        
        ProcessState currentState = processStates.get(taskId);
        
        if (currentState == ProcessState.STARTING && mineProcess.isActive()) {
            // Mining has started
            processStates.put(taskId, ProcessState.ACTIVE);
            System.out.println("PreciseCompletionTracker: " + taskId + " is now active");
        } else if (currentState == ProcessState.ACTIVE && !mineProcess.isActive()) {
            // Mining has completed
            System.out.println("PreciseCompletionTracker: " + taskId + " completed - no longer active");
            completeTask(taskId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if exploration has completed
     */
    private boolean checkExplorationCompletion(IBaritone baritone, String taskId) {
        IExploreProcess exploreProcess = baritone.getExploreProcess();
        
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
        CompletableFuture<Void> future = pendingTasks.remove(taskId);
        processStates.remove(taskId);
        Long startTime = taskStartTimes.remove(taskId);
        
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
        CompletableFuture<Void> future = pendingTasks.remove(taskId);
        processStates.remove(taskId);
        taskStartTimes.remove(taskId);
        
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
        
        pendingTasks.clear();
        processStates.clear();
        taskStartTimes.clear();
    }
    
    /**
     * Get the Baritone instance
     */
    private IBaritone getBaritone() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            System.err.println("Failed to get Baritone instance: " + e.getMessage());
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
}
