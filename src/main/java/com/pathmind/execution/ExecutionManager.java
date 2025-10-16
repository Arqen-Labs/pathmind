package com.pathmind.execution;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeConnection;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeType;
import com.pathmind.nodes.ParameterType;
import com.pathmind.data.NodeGraphData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void executeGraph(List<Node> nodes, List<NodeConnection> connections) {
        if (nodes == null || connections == null) {
            System.out.println("ExecutionManager: Cannot execute graph - missing nodes or connections.");
            return;
        }

        Node startNode = findStartNode(nodes);
        if (startNode == null) {
            System.out.println("ExecutionManager: No START node found!");
            return;
        }

        this.lastExecutedGraph = createGraphSnapshot(nodes, connections);

        startExecution(startNode);
        executeNodesSequentially(startNode, new ArrayList<>(connections));
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
            for (NodeGraphData.ParameterData paramData : nodeData.getParameters()) {
                ParameterType paramType = ParameterType.valueOf(paramData.getType());
                NodeParameter param = new NodeParameter(paramData.getName(), paramType, paramData.getValue());
                node.getParameters().add(param);
            }
            node.recalculateDimensions();

            nodes.add(node);
            nodeMap.put(nodeData.getId(), node);
        }

        List<NodeConnection> connections = new ArrayList<>();
        for (NodeGraphData.ConnectionData connData : lastExecutedGraph.getConnections()) {
            Node outputNode = nodeMap.get(connData.getOutputNodeId());
            Node inputNode = nodeMap.get(connData.getInputNodeId());
            if (outputNode != null && inputNode != null) {
                connections.add(new NodeConnection(outputNode, inputNode, connData.getOutputSocket(), connData.getInputSocket()));
            }
        }

        if (nodes.isEmpty()) {
            System.out.println("ExecutionManager: No nodes available to replay.");
            return;
        }

        executeGraph(nodes, connections);
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

    private void executeNodesSequentially(Node currentNode, List<NodeConnection> connections) {
        setActiveNode(currentNode);

        currentNode.execute().thenRun(() -> {
            System.out.println("ExecutionManager: Node completed - " + currentNode.getType());

            if (currentNode.getType() == NodeType.END) {
                System.out.println("ExecutionManager: Node graph execution complete!");
                stopExecution();
                return;
            }

            int nextSocket = currentNode.consumeNextOutputSocket();
            Node nextNode = getNextConnectedNode(currentNode, connections, nextSocket);
            if (nextNode == null && nextSocket != 0) {
                nextNode = getNextConnectedNode(currentNode, connections, 0);
            }
            if (nextNode != null) {
                executeNodesSequentially(nextNode, connections);
            } else {
                System.out.println("ExecutionManager: No next node found - stopping execution");
                stopExecution();
            }
        }).exceptionally(throwable -> {
            System.err.println("ExecutionManager: Error executing node " + currentNode.getType() + ": " + throwable.getMessage());
            throwable.printStackTrace();
            stopExecution();
            return null;
        });
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

    private Node findStartNode(List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getType() == NodeType.START) {
                return node;
            }
        }
        return null;
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

            snapshot.getNodes().add(nodeData);
        }

        for (NodeConnection connection : connections) {
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
}
