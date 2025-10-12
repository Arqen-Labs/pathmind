package com.pathmind.execution;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeType;

/**
 * Manages the execution state of the node graph.
 * Tracks which node is currently active and provides state information for overlays.
 */
public class ExecutionManager {
    private static ExecutionManager instance;
    private Node activeNode;
    private boolean isExecuting;
    private long executionStartTime;
    private long executionEndTime;
    private static final long MINIMUM_DISPLAY_DURATION = 3000; // 3 seconds minimum display
    
    private ExecutionManager() {
        this.activeNode = null;
        this.isExecuting = false;
        this.executionStartTime = 0;
        this.executionEndTime = 0;
    }
    
    public static ExecutionManager getInstance() {
        if (instance == null) {
            instance = new ExecutionManager();
        }
        return instance;
    }
    
    /**
     * Start execution with the given start node
     */
    public void startExecution(Node startNode) {
        this.activeNode = startNode;
        this.isExecuting = true;
        this.executionStartTime = System.currentTimeMillis();
        System.out.println("ExecutionManager: Started execution with node " + startNode.getType() + " at time " + this.executionStartTime);
    }
    
    /**
     * Set the currently active node
     */
    public void setActiveNode(Node node) {
        this.activeNode = node;
        System.out.println("ExecutionManager: Set active node to " + (node != null ? node.getType() : "null") + " at time " + System.currentTimeMillis());
    }
    
    /**
     * Stop execution
     */
    public void stopExecution() {
        System.out.println("ExecutionManager: Stopping execution at time " + System.currentTimeMillis());
        this.isExecuting = false;
        this.executionEndTime = System.currentTimeMillis();
        // Keep activeNode for minimum display duration
    }
    
    /**
     * Get the currently active node
     */
    public Node getActiveNode() {
        return activeNode;
    }
    
    /**
     * Check if execution is currently running or should still be displayed
     */
    public boolean isExecuting() {
        if (isExecuting) {
            return true;
        }
        
        // Show overlay for minimum duration after execution ends
        if (executionEndTime > 0 && activeNode != null) {
            long timeSinceEnd = System.currentTimeMillis() - executionEndTime;
            if (timeSinceEnd < MINIMUM_DISPLAY_DURATION) {
                return true;
            } else {
                // Clear the active node after minimum display duration
                this.activeNode = null;
                this.executionEndTime = 0;
            }
        }
        
        return false;
    }
    
    /**
     * Get the execution start time
     */
    public long getExecutionStartTime() {
        return executionStartTime;
    }
    
    /**
     * Get the current execution duration in milliseconds
     */
    public long getExecutionDuration() {
        if (executionStartTime == 0) {
            return 0;
        }
        
        if (isExecuting) {
            return System.currentTimeMillis() - executionStartTime;
        } else if (executionEndTime > 0) {
            return executionEndTime - executionStartTime;
        }
        
        return 0;
    }
}
