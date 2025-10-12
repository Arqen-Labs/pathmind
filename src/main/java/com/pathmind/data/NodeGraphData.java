package com.pathmind.data;

import com.pathmind.nodes.NodeType;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable data structure for saving and loading node graphs.
 */
public class NodeGraphData {
    private List<NodeData> nodes;
    private List<ConnectionData> connections;
    
    public NodeGraphData() {
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
    }
    
    public NodeGraphData(List<NodeData> nodes, List<ConnectionData> connections) {
        this.nodes = nodes;
        this.connections = connections;
    }
    
    public List<NodeData> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<NodeData> nodes) {
        this.nodes = nodes;
    }
    
    public List<ConnectionData> getConnections() {
        return connections;
    }
    
    public void setConnections(List<ConnectionData> connections) {
        this.connections = connections;
    }
    
    /**
     * Data structure for a single node
     */
    public static class NodeData {
        private String id;
        private NodeType type;
        private int x, y;
        private List<ParameterData> parameters;
        
        public NodeData() {
            this.parameters = new ArrayList<>();
        }
        
        public NodeData(String id, NodeType type, int x, int y, List<ParameterData> parameters) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.parameters = parameters;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public NodeType getType() { return type; }
        public void setType(NodeType type) { this.type = type; }
        
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        
        public List<ParameterData> getParameters() { return parameters; }
        public void setParameters(List<ParameterData> parameters) { this.parameters = parameters; }
    }
    
    /**
     * Data structure for a connection between nodes
     */
    public static class ConnectionData {
        private String outputNodeId;
        private String inputNodeId;
        private int outputSocket;
        private int inputSocket;
        
        public ConnectionData() {}
        
        public ConnectionData(String outputNodeId, String inputNodeId, int outputSocket, int inputSocket) {
            this.outputNodeId = outputNodeId;
            this.inputNodeId = inputNodeId;
            this.outputSocket = outputSocket;
            this.inputSocket = inputSocket;
        }
        
        // Getters and setters
        public String getOutputNodeId() { return outputNodeId; }
        public void setOutputNodeId(String outputNodeId) { this.outputNodeId = outputNodeId; }
        
        public String getInputNodeId() { return inputNodeId; }
        public void setInputNodeId(String inputNodeId) { this.inputNodeId = inputNodeId; }
        
        public int getOutputSocket() { return outputSocket; }
        public void setOutputSocket(int outputSocket) { this.outputSocket = outputSocket; }
        
        public int getInputSocket() { return inputSocket; }
        public void setInputSocket(int inputSocket) { this.inputSocket = inputSocket; }
    }
    
    /**
     * Data structure for a node parameter
     */
    public static class ParameterData {
        private String name;
        private String value;
        private String type;
        
        public ParameterData() {}
        
        public ParameterData(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
