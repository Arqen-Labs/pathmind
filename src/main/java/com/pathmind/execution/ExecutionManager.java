package com.pathmind.execution;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IExploreProcess;
import baritone.api.process.IFarmProcess;
import baritone.api.process.IMineProcess;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeConnection;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeType;
import com.pathmind.nodes.ParameterType;
import com.pathmind.data.NodeGraphData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private NodeGraphData lastExecutedGraph;
    private List<Node> activeNodes;
    private List<NodeConnection> activeConnections;
    private final List<String> executingEvents;
    private volatile boolean cancelRequested;
    private final Map<Node, ChainController> activeChains;
    private boolean globalExecutionActive;

    private static class ChainController {
        final Node startNode;
        volatile boolean cancelRequested;

        ChainController(Node startNode) {
            this.startNode = startNode;
            this.cancelRequested = false;
        }
    }

    private ExecutionManager() {
        this.activeNode = null;
        this.isExecuting = false;
        this.executionStartTime = 0;
        this.executionEndTime = 0;
        this.activeNodes = new ArrayList<>();
        this.activeConnections = new ArrayList<>();
        this.executingEvents = new ArrayList<>();
        this.cancelRequested = false;
        this.activeChains = new ConcurrentHashMap<>();
        this.globalExecutionActive = false;
    }
    
    public static ExecutionManager getInstance() {
        if (instance == null) {
            instance = new ExecutionManager();
        }
        return instance;
    }

    public void executeGraph(List<Node> nodes, List<NodeConnection> connections) {
        if (nodes == null || connections == null) {
            System.out.println("ExecutionManager: Cannot execute graph - missing nodes or connections.");
            return;
        }

        List<Node> startNodes = findStartNodes(nodes);
        if (startNodes.isEmpty()) {
            System.out.println("ExecutionManager: No START nodes found!");
            return;
        }

        List<NodeConnection> filteredConnections = filterConnections(connections);

        this.lastExecutedGraph = createGraphSnapshot(nodes, filteredConnections);
        this.activeNodes = new ArrayList<>(nodes);
        this.activeConnections = new ArrayList<>(filteredConnections);
        this.cancelRequested = false;

        startExecution(startNodes, true);
        activeChains.clear();

        for (Node startNode : startNodes) {
            ChainController controller = new ChainController(startNode);
            activeChains.put(startNode, controller);
            CompletableFuture<Void> chainFuture = runChain(startNode, controller);
            chainFuture.whenComplete((ignored, throwable) -> handleChainCompletion(controller, throwable));
        }
    }

    public void replayLastGraph() {
        if (lastExecutedGraph == null) {
            System.out.println("ExecutionManager: No previously executed node graph to replay.");
            return;
        }

        Map<String, Node> nodeMap = new HashMap<>();
        List<Node> nodes = new ArrayList<>();

        for (NodeGraphData.NodeData nodeData : lastExecutedGraph.getNodes()) {
            Node node = new Node(nodeData.getType(), nodeData.getX(), nodeData.getY());

            try {
                java.lang.reflect.Field idField = Node.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(node, nodeData.getId());
            } catch (Exception e) {
                System.err.println("ExecutionManager: Failed to set node ID during replay - " + e.getMessage());
            }

            if (nodeData.getMode() != null) {
                node.setMode(nodeData.getMode());
            }

            node.getParameters().clear();
            if (nodeData.getParameters() != null) {
                for (NodeGraphData.ParameterData paramData : nodeData.getParameters()) {
                    ParameterType paramType = ParameterType.valueOf(paramData.getType());
                    NodeParameter param = new NodeParameter(paramData.getName(), paramType, paramData.getValue());
                    node.getParameters().add(param);
                }
            }
            node.recalculateDimensions();

            nodes.add(node);
            nodeMap.put(nodeData.getId(), node);
        }

        for (NodeGraphData.NodeData nodeData : lastExecutedGraph.getNodes()) {
            if (nodeData.getAttachedSensorId() != null) {
                Node control = nodeMap.get(nodeData.getId());
                Node sensor = nodeMap.get(nodeData.getAttachedSensorId());
                if (control != null && sensor != null) {
                    control.attachSensor(sensor);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : lastExecutedGraph.getNodes()) {
            if (nodeData.getParentControlId() != null) {
                Node sensor = nodeMap.get(nodeData.getId());
                Node control = nodeMap.get(nodeData.getParentControlId());
                if (sensor != null && control != null && sensor.isSensorNode()) {
                    control.attachSensor(sensor);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : lastExecutedGraph.getNodes()) {
            if (nodeData.getAttachedActionId() != null) {
                Node control = nodeMap.get(nodeData.getId());
                Node child = nodeMap.get(nodeData.getAttachedActionId());
                if (control != null && child != null) {
                    control.attachActionNode(child);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : lastExecutedGraph.getNodes()) {
            if (nodeData.getParentActionControlId() != null) {
                Node child = nodeMap.get(nodeData.getId());
                Node control = nodeMap.get(nodeData.getParentActionControlId());
                if (child != null && control != null && control.canAcceptActionNode(child)) {
                    control.attachActionNode(child);
                }
            }
        }

        List<NodeConnection> connections = new ArrayList<>();
        for (NodeGraphData.ConnectionData connData : lastExecutedGraph.getConnections()) {
            Node outputNode = nodeMap.get(connData.getOutputNodeId());
            Node inputNode = nodeMap.get(connData.getInputNodeId());
            if (outputNode != null && inputNode != null) {
                if (outputNode.isSensorNode() || inputNode.isSensorNode()) {
                    continue;
                }
                connections.add(new NodeConnection(outputNode, inputNode, connData.getOutputSocket(), connData.getInputSocket()));
            }
        }

        if (nodes.isEmpty()) {
            System.out.println("ExecutionManager: No nodes available to replay.");
            return;
        }

        executeGraph(nodes, connections);
    }

    public boolean executeBranch(Node startNode, List<Node> nodes, List<NodeConnection> connections) {
        if (startNode == null || startNode.getType() != NodeType.START) {
            System.out.println("ExecutionManager: Cannot execute branch - invalid START node.");
            return false;
        }
        if (nodes == null || connections == null) {
            System.out.println("ExecutionManager: Cannot execute branch - missing nodes or connections.");
            return false;
        }
        if (isChainActive(startNode)) {
            System.out.println("ExecutionManager: START node already executing, ignoring branch start request.");
            return false;
        }

        List<NodeConnection> filteredConnections = filterConnections(connections);

        this.lastExecutedGraph = createGraphSnapshot(nodes, filteredConnections);
        this.activeNodes = new ArrayList<>(nodes);
        this.activeConnections = new ArrayList<>(filteredConnections);
        this.cancelRequested = false;

        if (activeChains.isEmpty()) {
            startExecution(Collections.singletonList(startNode), false);
        } else {
            this.isExecuting = true;
        }

        ChainController controller = new ChainController(startNode);
        activeChains.put(startNode, controller);
        CompletableFuture<Void> chainFuture = runChain(startNode, controller);
        chainFuture.whenComplete((ignored, throwable) -> handleChainCompletion(controller, throwable));
        return true;
    }
    
    /**
     * Start execution with the given start node
     */
    private void startExecution(List<Node> startNodes, boolean markGlobal) {
        this.activeNode = startNodes.isEmpty() ? null : startNodes.get(0);
        this.isExecuting = true;
        this.globalExecutionActive = markGlobal;
        this.cancelRequested = false;
        this.executionStartTime = System.currentTimeMillis();
        this.executionEndTime = 0;
        if (!startNodes.isEmpty()) {
            System.out.println(
                    "ExecutionManager: Started execution with " + startNodes.size() +
                            " start node(s) at time " + this.executionStartTime);
        } else {
            System.out.println("ExecutionManager: Started execution without any root nodes at time " + this.executionStartTime);
        }
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
        this.globalExecutionActive = false;
        if (cancelRequested) {
            this.executionEndTime = 0;
            this.executionStartTime = 0;
            this.activeNode = null;
            cancelRequested = false;
        } else {
            this.executionEndTime = System.currentTimeMillis();
        }
        // Keep activeNode for minimum display duration
    }

    /**
     * Request that all executing node chains stop immediately.
     */
    public void requestStopAll() {
        cancelAllBaritoneCommands();

        if (!isExecuting && activeNode == null && activeChains.isEmpty()) {
            return;
        }

        System.out.println("ExecutionManager: Stop requested for all node trees at time " + System.currentTimeMillis());
        cancelRequested = true;
        for (ChainController controller : activeChains.values()) {
            controller.cancelRequested = true;
        }
        this.isExecuting = false;
        this.globalExecutionActive = false;
        this.activeNode = null;
        this.executionStartTime = 0;
        this.executionEndTime = 0;
        this.activeNodes.clear();
        this.activeConnections.clear();
        this.executingEvents.clear();
        this.activeChains.clear();
    }

    private void cancelAllBaritoneCommands() {
        PreciseCompletionTracker.getInstance().cancelAllTasks();

        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (baritone == null) {
                return;
            }

            IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
            if (pathingBehavior != null) {
                pathingBehavior.cancelEverything();
            }

            ICustomGoalProcess goalProcess = baritone.getCustomGoalProcess();
            if (goalProcess != null) {
                goalProcess.setGoal(null);
                goalProcess.onLostControl();
            }

            IMineProcess mineProcess = baritone.getMineProcess();
            if (mineProcess != null) {
                mineProcess.cancel();
            }

            IExploreProcess exploreProcess = baritone.getExploreProcess();
            if (exploreProcess != null && exploreProcess.isActive()) {
                exploreProcess.onLostControl();
            }

            IFarmProcess farmProcess = baritone.getFarmProcess();
            if (farmProcess != null && farmProcess.isActive()) {
                farmProcess.onLostControl();
            }
        } catch (Exception e) {
            System.err.println("ExecutionManager: Failed to cancel Baritone processes: " + e.getMessage());
        }
    }
    
    /**
     * Get the currently active node
     */
    public Node getActiveNode() {
        return activeNode;
    }

    public boolean requestStopForStart(Node startNode) {
        if (startNode == null) {
            return false;
        }

        ChainController controller = activeChains.get(startNode);
        if (controller == null) {
            System.out.println("ExecutionManager: No active chain found for requested START node stop.");
            return false;
        }

        controller.cancelRequested = true;
        System.out.println("ExecutionManager: Stop requested for START node " + startNode.getId() + " at time " + System.currentTimeMillis());
        return true;
    }

    public boolean isChainActive(Node startNode) {
        if (startNode == null) {
            return false;
        }
        ChainController controller = activeChains.get(startNode);
        return controller != null && !controller.cancelRequested;
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

    public boolean isGlobalExecutionActive() {
        return globalExecutionActive;
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

    private CompletableFuture<Void> runChain(Node currentNode, ChainController controller) {
        if (cancelRequested || controller == null || controller.cancelRequested) {
            return CompletableFuture.completedFuture(null);
        }

        setActiveNode(currentNode);

        if (cancelRequested || controller.cancelRequested) {
            return CompletableFuture.completedFuture(null);
        }

        return currentNode.execute()
            .thenCompose(ignored -> {
                if (cancelRequested || controller.cancelRequested) {
                    return CompletableFuture.completedFuture(null);
                }
                return handleEventCallIfNeeded(currentNode, controller);
            })
            .thenCompose(ignored -> {
                if (cancelRequested || controller.cancelRequested) {
                    return CompletableFuture.completedFuture(null);
                }
                int nextSocket = currentNode.consumeNextOutputSocket();
                if (nextSocket == Node.NO_OUTPUT) {
                    return CompletableFuture.completedFuture(null);
                }
                Node nextNode = getNextConnectedNode(currentNode, activeConnections, nextSocket);
                if (nextNode == null && nextSocket > 0) {
                    nextNode = getNextConnectedNode(currentNode, activeConnections, 0);
                }
                if (nextNode != null) {
                    return runChain(nextNode, controller);
                }
                return CompletableFuture.completedFuture(null);
            });
    }

    private CompletableFuture<Void> handleEventCallIfNeeded(Node node, ChainController controller) {
        if (cancelRequested || controller.cancelRequested || node.getType() != NodeType.EVENT_CALL) {
            return CompletableFuture.completedFuture(null);
        }

        NodeParameter nameParam = node.getParameter("Name");
        String eventName = normalizeEventName(nameParam != null ? nameParam.getStringValue() : null);
        if (eventName.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (executingEvents.contains(eventName)) {
            System.out.println("ExecutionManager: Skipping recursive event call for " + eventName);
            return CompletableFuture.completedFuture(null);
        }

        List<Node> handlers = new ArrayList<>();
        for (Node candidate : activeNodes) {
            if (candidate.getType() == NodeType.EVENT_FUNCTION) {
                NodeParameter candidateParam = candidate.getParameter("Name");
                String candidateName = normalizeEventName(candidateParam != null ? candidateParam.getStringValue() : null);
                if (!candidateName.isEmpty() && candidateName.equals(eventName)) {
                    handlers.add(candidate);
                }
            }
        }

        if (handlers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        executingEvents.add(eventName);
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (Node handler : handlers) {
            if (cancelRequested || controller.cancelRequested) {
                break;
            }
            chain = chain.thenCompose(ignored -> runChain(handler, controller));
        }
        return chain.whenComplete((ignored, throwable) -> executingEvents.remove(eventName));
    }

    private void handleChainCompletion(ChainController controller, Throwable throwable) {
        if (controller == null) {
            return;
        }

        if (throwable != null && !cancelRequested && !controller.cancelRequested) {
            System.err.println("ExecutionManager: Error during execution - " + throwable.getMessage());
            throwable.printStackTrace();
        }

        activeChains.remove(controller.startNode);

        if (activeChains.isEmpty() && isExecuting) {
            stopExecution();
            activeNodes.clear();
            activeConnections.clear();
            executingEvents.clear();
        }
    }

    private String normalizeEventName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private Node getNextConnectedNode(Node currentNode, List<NodeConnection> connections, int outputSocket) {
        for (NodeConnection connection : connections) {
            if (connection.getOutputNode() == currentNode) {
                if (connection.getOutputSocket() == outputSocket) {
                    return connection.getInputNode();
                }
            }
        }
        return null;
    }

    private List<Node> findStartNodes(List<Node> nodes) {
        List<Node> startNodes = new ArrayList<>();
        if (nodes == null) {
            return startNodes;
        }

        for (Node node : nodes) {
            if (node.getType() == NodeType.START) {
                startNodes.add(node);
            }
        }

        return startNodes;
    }

    private NodeGraphData createGraphSnapshot(List<Node> nodes, List<NodeConnection> connections) {
        NodeGraphData snapshot = new NodeGraphData();

        for (Node node : nodes) {
            NodeGraphData.NodeData nodeData = new NodeGraphData.NodeData();
            nodeData.setId(node.getId());
            nodeData.setType(node.getType());
            nodeData.setMode(node.getMode());
            nodeData.setX(node.getX());
            nodeData.setY(node.getY());

            List<NodeGraphData.ParameterData> parameterDataList = new ArrayList<>();
            for (NodeParameter parameter : node.getParameters()) {
                NodeGraphData.ParameterData parameterData = new NodeGraphData.ParameterData();
                parameterData.setName(parameter.getName());
                parameterData.setValue(parameter.getStringValue());
                parameterData.setType(parameter.getType().name());
                parameterDataList.add(parameterData);
            }
            nodeData.setParameters(parameterDataList);
            nodeData.setAttachedSensorId(node.getAttachedSensorId());
            nodeData.setParentControlId(node.getParentControlId());
            nodeData.setAttachedActionId(node.getAttachedActionId());
            nodeData.setParentActionControlId(node.getParentActionControlId());

            snapshot.getNodes().add(nodeData);
        }

        for (NodeConnection connection : filterConnections(connections)) {
            NodeGraphData.ConnectionData connectionData = new NodeGraphData.ConnectionData(
                    connection.getOutputNode().getId(),
                    connection.getInputNode().getId(),
                    connection.getOutputSocket(),
                    connection.getInputSocket()
            );
            snapshot.getConnections().add(connectionData);
        }

        return snapshot;
    }

    private List<NodeConnection> filterConnections(List<NodeConnection> connections) {
        List<NodeConnection> filtered = new ArrayList<>();
        if (connections == null) {
            return filtered;
        }
        for (NodeConnection connection : connections) {
            if (connection == null) {
                continue;
            }
            Node output = connection.getOutputNode();
            Node input = connection.getInputNode();
            if (output == null || input == null) {
                continue;
            }
            if (output.isSensorNode() || input.isSensorNode()) {
                continue;
            }
            if (connection.getOutputSocket() < 0 || connection.getOutputSocket() >= output.getOutputSocketCount()) {
                continue;
            }
            filtered.add(connection);
        }
        return filtered;
    }
}
