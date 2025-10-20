package com.pathmind.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeConnection;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeType;
import com.pathmind.nodes.ParameterType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles saving and loading node graphs to/from disk.
 */
public class NodeGraphPersistence {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(NodeType.class, new NodeTypeAdapter())
            .create();

    private static class PendingParameterAttachment {
        final Node target;
        final NodeParameter parameter;
        final String attachedNodeId;

        PendingParameterAttachment(Node target, NodeParameter parameter, String attachedNodeId) {
            this.target = target;
            this.parameter = parameter;
            this.attachedNodeId = attachedNodeId;
        }
    }
    
    /**
     * Save the current node graph to disk
     */
    public static boolean saveNodeGraph(List<Node> nodes, List<NodeConnection> connections) {
        return saveNodeGraphForPreset(PresetManager.getActivePreset(), nodes, connections);
    }

    public static boolean saveNodeGraphForPreset(String presetName, List<Node> nodes, List<NodeConnection> connections) {
        return saveNodeGraphToPath(nodes, connections, PresetManager.getPresetPath(presetName));
    }

    public static boolean saveNodeGraphToPath(List<Node> nodes, List<NodeConnection> connections, Path savePath) {
        try {
            // Convert nodes and connections to serializable data
            NodeGraphData data = new NodeGraphData();

            // Convert nodes
            for (Node node : nodes) {
                NodeGraphData.NodeData nodeData = new NodeGraphData.NodeData();
                nodeData.setId(node.getId());
                nodeData.setType(node.getType());
                nodeData.setMode(node.getMode());
                nodeData.setX(node.getX());
                nodeData.setY(node.getY());

                // Convert parameters
                List<NodeGraphData.ParameterData> paramDataList = new ArrayList<>();
                for (NodeParameter param : node.getParameters()) {
                    NodeGraphData.ParameterData paramData = new NodeGraphData.ParameterData();
                    paramData.setName(param.getName());
                    paramData.setValue(param.getStringValue());
                    paramData.setType(param.getType().name());
                    if (param.hasAttachedNode()) {
                        paramData.setAttachedNodeId(param.getAttachedNode().getId());
                    }
                    paramDataList.add(paramData);
                }
                nodeData.setParameters(paramDataList);
                nodeData.setAttachedSensorId(node.getAttachedSensorId());
                nodeData.setParentControlId(node.getParentControlId());
                nodeData.setAttachedActionId(node.getAttachedActionId());
                nodeData.setParentActionControlId(node.getParentActionControlId());

                data.getNodes().add(nodeData);
            }

            // Convert connections
            for (NodeConnection connection : connections) {
                if (connection.getOutputNode().isSensorNode() || connection.getInputNode().isSensorNode()) {
                    continue;
                }
                NodeGraphData.ConnectionData connData = new NodeGraphData.ConnectionData();
                connData.setOutputNodeId(connection.getOutputNode().getId());
                connData.setInputNodeId(connection.getInputNode().getId());
                connData.setOutputSocket(connection.getOutputSocket());
                connData.setInputSocket(connection.getInputSocket());

                data.getConnections().add(connData);
            }

            if (savePath.getParent() != null) {
                Files.createDirectories(savePath.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                GSON.toJson(data, writer);
            }

            System.out.println("Node graph saved successfully to: " + savePath);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to save node graph: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load the node graph from disk
     */
    public static NodeGraphData loadNodeGraph() {
        return loadNodeGraphForPreset(PresetManager.getActivePreset());
    }

    public static NodeGraphData loadNodeGraphForPreset(String presetName) {
        return loadNodeGraphFromPath(PresetManager.getPresetPath(presetName));
    }

    public static NodeGraphData loadNodeGraphFromPath(Path savePath) {
        try {
            if (!Files.exists(savePath)) {
                System.out.println("No saved node graph found at: " + savePath);
                return null;
            }

            try (Reader reader = Files.newBufferedReader(savePath)) {
                NodeGraphData data = GSON.fromJson(reader, NodeGraphData.class);
                System.out.println("Node graph loaded successfully from: " + savePath);
                return data;
            }

        } catch (Exception e) {
            System.err.println("Failed to load node graph: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convert loaded data back to Node objects
     */
    public static List<Node> convertToNodes(NodeGraphData data) {
        List<Node> nodes = new ArrayList<>();
        Map<String, Node> nodeMap = new HashMap<>();
        List<PendingParameterAttachment> pendingAttachments = new ArrayList<>();
        
        // Create nodes
        for (NodeGraphData.NodeData nodeData : data.getNodes()) {
            Node node = new Node(nodeData.getType(), nodeData.getX(), nodeData.getY());
            
            // Set the same ID using reflection
            try {
                java.lang.reflect.Field idField = Node.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(node, nodeData.getId());
            } catch (Exception e) {
                System.err.println("Failed to set node ID: " + e.getMessage());
            }
            
            // Set the mode if it exists (this will reinitialize parameters)
            if (nodeData.getMode() != null) {
                node.setMode(nodeData.getMode());
            }
            
            // Restore parameters (overwrite the default parameters with saved ones)
            node.getParameters().clear();
            if (nodeData.getParameters() != null) {
                for (NodeGraphData.ParameterData paramData : nodeData.getParameters()) {
                    ParameterType paramType = ParameterType.valueOf(paramData.getType());
                    NodeParameter param = new NodeParameter(paramData.getName(), paramType, paramData.getValue());
                    node.getParameters().add(param);
                    if (paramData.getAttachedNodeId() != null) {
                        pendingAttachments.add(new PendingParameterAttachment(node, param, paramData.getAttachedNodeId()));
                    }
                }
            }
            node.recalculateDimensions();

            nodes.add(node);
            nodeMap.put(nodeData.getId(), node);
        }

        for (PendingParameterAttachment attachment : pendingAttachments) {
            Node provider = nodeMap.get(attachment.attachedNodeId);
            if (provider != null) {
                attachment.parameter.attachNode(provider);
                attachment.target.getParameter(attachment.parameter.getName());
            }
        }

        for (NodeGraphData.NodeData nodeData : data.getNodes()) {
            if (nodeData.getAttachedSensorId() != null) {
                Node control = nodeMap.get(nodeData.getId());
                Node sensor = nodeMap.get(nodeData.getAttachedSensorId());
                if (control != null && sensor != null) {
                    control.attachSensor(sensor);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : data.getNodes()) {
            if (nodeData.getParentControlId() != null) {
                Node sensor = nodeMap.get(nodeData.getId());
                Node control = nodeMap.get(nodeData.getParentControlId());
                if (sensor != null && control != null && sensor.isSensorNode()) {
                    control.attachSensor(sensor);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : data.getNodes()) {
            if (nodeData.getAttachedActionId() != null) {
                Node control = nodeMap.get(nodeData.getId());
                Node child = nodeMap.get(nodeData.getAttachedActionId());
                if (control != null && child != null) {
                    control.attachActionNode(child);
                }
            }
        }

        for (NodeGraphData.NodeData nodeData : data.getNodes()) {
            if (nodeData.getParentActionControlId() != null) {
                Node child = nodeMap.get(nodeData.getId());
                Node control = nodeMap.get(nodeData.getParentActionControlId());
                if (child != null && control != null && control.canAcceptActionNode(child)) {
                    control.attachActionNode(child);
                }
            }
        }

        return nodes;
    }
    
    /**
     * Convert loaded data back to Connection objects
     */
    public static List<NodeConnection> convertToConnections(NodeGraphData data, Map<String, Node> nodeMap) {
        List<NodeConnection> connections = new ArrayList<>();
        
        for (NodeGraphData.ConnectionData connData : data.getConnections()) {
            Node outputNode = nodeMap.get(connData.getOutputNodeId());
            Node inputNode = nodeMap.get(connData.getInputNodeId());
            
            if (outputNode != null && inputNode != null) {
                if (outputNode.isSensorNode() || inputNode.isSensorNode()) {
                    continue;
                }
                NodeConnection connection = new NodeConnection(
                    outputNode,
                    inputNode,
                    connData.getOutputSocket(),
                    connData.getInputSocket()
                );
                connections.add(connection);
            } else {
                System.err.println("Failed to restore connection: missing node(s)");
            }
        }
        
        return connections;
    }
    
    /**
     * Get the save file path in the Minecraft saves directory
     */
    public static Path getDefaultSavePath() {
        return PresetManager.getPresetPath(PresetManager.getActivePreset());
    }

    /**
     * Check if a saved node graph exists
     */
    public static boolean hasSavedNodeGraph() {
        return hasSavedNodeGraph(PresetManager.getActivePreset());
    }

    public static boolean hasSavedNodeGraph(String presetName) {
        return Files.exists(PresetManager.getPresetPath(presetName));
    }
}

/**
 * Custom adapter for NodeType enum serialization
 */
class NodeTypeAdapter extends com.google.gson.TypeAdapter<NodeType> {
    @Override
    public void write(com.google.gson.stream.JsonWriter out, NodeType value) throws IOException {
        out.value(value.name());
    }
    
    @Override
    public NodeType read(com.google.gson.stream.JsonReader in) throws IOException {
        String name = in.nextString();
        try {
            return NodeType.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Handle unknown node types gracefully
            System.err.println("Unknown node type: " + name + ", skipping...");
            return null;
        }
    }
}
