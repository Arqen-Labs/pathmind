package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeConnection;
import com.pathmind.nodes.NodeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the node graph for the Pathmind visual editor.
 * Handles node rendering, connections, and interactions.
 */
public class NodeGraph {
    private final List<Node> nodes;
    private final List<NodeConnection> connections;
    private Node selectedNode;
    private Node draggingNode;
    
    // Camera/viewport for infinite scrolling
    private int cameraX = 0;
    private int cameraY = 0;
    private boolean isPanning = false;
    private int panStartX, panStartY;
    private int panStartCameraX, panStartCameraY;
    
    // Connection dragging state
    private boolean isDraggingConnection = false;
    private Node connectionSourceNode;
    private int connectionSourceSocket;
    private boolean isOutputSocket; // true if dragging from output, false if from input
    private int connectionDragX, connectionDragY;
    private Node hoveredNode = null;
    private int hoveredSocket = -1;
    private boolean hoveredSocketIsInput = false;
    
    // Store the original connection that was disconnected
    private NodeConnection disconnectedConnection = null;
    
    // Socket hover state
    private Node hoveredSocketNode = null;
    private int hoveredSocketIndex = -1;

    public NodeGraph() {
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.selectedNode = null;
        this.draggingNode = null;
        
        // Add preset nodes similar to Blender's shader editor
        // Will be initialized with proper centering when screen dimensions are available
    }
    
    public void initializeWithScreenDimensions(int screenWidth, int screenHeight, int sidebarWidth, int titleBarHeight) {
        // Clear any existing nodes
        nodes.clear();
        connections.clear();
        
        // Calculate workspace area
        int workspaceStartX = sidebarWidth;
        int workspaceStartY = titleBarHeight;
        int workspaceWidth = screenWidth - sidebarWidth;
        int workspaceHeight = screenHeight - titleBarHeight;
        
        // Center nodes in the workspace
        int centerX = workspaceStartX + workspaceWidth / 2;
        int centerY = workspaceStartY + workspaceHeight / 2;
        
        // Position nodes with proper spacing, centered in workspace
        Node startNode = new Node(NodeType.START, centerX - 100, centerY - 50);
        nodes.add(startNode);
        
        Node middleNode = new Node(NodeType.MINE, centerX, centerY - 50);
        nodes.add(middleNode);
        
        Node endNode = new Node(NodeType.END, centerX + 100, centerY - 50);
        nodes.add(endNode);
        
        // Connect them
        connections.add(new NodeConnection(startNode, middleNode, 0, 0));
        connections.add(new NodeConnection(middleNode, endNode, 0, 0));
    }


    public void addNode(Node node) {
        nodes.add(node);
    }

    public void removeNode(Node node) {
        // Find connections involving this node before removing them
        List<NodeConnection> inputConnections = new ArrayList<>();
        List<NodeConnection> outputConnections = new ArrayList<>();
        
        for (NodeConnection conn : connections) {
            if (conn.getOutputNode().equals(node)) {
                outputConnections.add(conn);
            } else if (conn.getInputNode().equals(node)) {
                inputConnections.add(conn);
            }
        }
        
        // Auto-reconnect: connect each input source to each output target
        for (NodeConnection inputConn : inputConnections) {
            Node inputSource = inputConn.getOutputNode(); // Node that connects TO the deleted node
            int inputSocket = inputConn.getOutputSocket();
            
            for (NodeConnection outputConn : outputConnections) {
                Node outputTarget = outputConn.getInputNode(); // Node that the deleted node connects TO
                int outputSocket = outputConn.getInputSocket();
                
                // Create new connection between input source and output target
                NodeConnection newConnection = new NodeConnection(inputSource, outputTarget, inputSocket, outputSocket);
                connections.add(newConnection);
            }
        }
        
        // Remove all connections involving this node
        connections.removeIf(conn -> 
            conn.getOutputNode().equals(node) || conn.getInputNode().equals(node));
        nodes.remove(node);
        
        if (selectedNode == node) {
            selectedNode = null;
        }
        if (draggingNode == node) {
            draggingNode = null;
        }
    }

    public Node getNodeAt(int x, int y) {
        // Convert screen coordinates to world coordinates
        int worldX = x + cameraX;
        int worldY = y + cameraY;
        
        for (Node node : nodes) {
            if (node.containsPoint(worldX, worldY)) {
                return node;
            }
        }
        return null;
    }

    public void selectNode(Node node) {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }
        selectedNode = node;
        if (node != null) {
            node.setSelected(true);
        }
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public void startDragging(Node node, int mouseX, int mouseY) {
        draggingNode = node;
        node.setDragging(true);
        node.setDragOffsetX(mouseX + cameraX - node.getX());
        node.setDragOffsetY(mouseY + cameraY - node.getY());
    }
    
    public void startDraggingConnection(Node node, int socketIndex, boolean isOutput, int mouseX, int mouseY) {
        isDraggingConnection = true;
        connectionSourceNode = node;
        connectionSourceSocket = socketIndex;
        isOutputSocket = isOutput;
        connectionDragX = mouseX + cameraX;
        connectionDragY = mouseY + cameraY;
        
        // Find and disconnect existing connection from this socket
        disconnectedConnection = null;
        if (isOutput) {
            // Dragging from output socket - find connection that starts from this socket
            for (NodeConnection conn : connections) {
                if (conn.getOutputNode().equals(node) && conn.getOutputSocket() == socketIndex) {
                    disconnectedConnection = conn;
                    connections.remove(conn);
                    break;
                }
            }
        } else {
            // Dragging from input socket - find connection that ends at this socket
            for (NodeConnection conn : connections) {
                if (conn.getInputNode().equals(node) && conn.getInputSocket() == socketIndex) {
                    disconnectedConnection = conn;
                    connections.remove(conn);
                    break;
                }
            }
        }
        
        System.out.println("Started dragging connection from " + (isOutput ? "output" : "input") + 
                         " socket " + socketIndex + " of node " + node.getType() + 
                         (disconnectedConnection != null ? " (disconnected existing connection)" : ""));
    }

    public void updateDrag(int mouseX, int mouseY) {
        if (draggingNode != null) {
            int newX = mouseX + cameraX - draggingNode.getDragOffsetX();
            int newY = mouseY + cameraY - draggingNode.getDragOffsetY();
            draggingNode.setPosition(newX, newY);
        }
        if (isDraggingConnection) {
            connectionDragX = mouseX + cameraX;
            connectionDragY = mouseY + cameraY;
            
            // Check for socket snapping
            hoveredNode = null;
            hoveredSocket = -1;
            
            for (Node node : nodes) {
                if (node == connectionSourceNode) continue;
                
                // Check input sockets if dragging from output
                if (isOutputSocket) {
                    for (int i = 0; i < node.getInputSocketCount(); i++) {
                        if (node.isSocketClicked(mouseX + cameraX, mouseY + cameraY, i, true)) {
                            hoveredNode = node;
                            hoveredSocket = i;
                            hoveredSocketIsInput = true;
                            break;
                        }
                    }
                } else {
                    // Check output sockets if dragging from input
                    for (int i = 0; i < node.getOutputSocketCount(); i++) {
                        if (node.isSocketClicked(mouseX + cameraX, mouseY + cameraY, i, false)) {
                            hoveredNode = node;
                            hoveredSocket = i;
                            hoveredSocketIsInput = false;
                            break;
                        }
                    }
                }
                
                if (hoveredNode != null) break;
            }
        }
    }
    
    public void updateMouseHover(int mouseX, int mouseY) {
        // Reset hover state
        hoveredSocketNode = null;
        hoveredSocketIndex = -1;
        
        // Don't check for socket hover if we're currently dragging a connection
        if (isDraggingConnection) {
            return;
        }
        
        // Check for socket hover
        for (Node node : nodes) {
            // Check input sockets
            for (int i = 0; i < node.getInputSocketCount(); i++) {
                if (node.isSocketClicked(mouseX, mouseY, i, true)) {
                    hoveredSocketNode = node;
                    hoveredSocketIndex = i;
                    hoveredSocketIsInput = true;
                    return;
                }
            }
            
            // Check output sockets
            for (int i = 0; i < node.getOutputSocketCount(); i++) {
                if (node.isSocketClicked(mouseX, mouseY, i, false)) {
                    hoveredSocketNode = node;
                    hoveredSocketIndex = i;
                    hoveredSocketIsInput = false;
                    return;
                }
            }
        }
    }

    public void stopDragging() {
        if (draggingNode != null) {
            draggingNode.setDragging(false);
            draggingNode = null;
        }
    }
    
    public void stopDraggingConnection() {
        if (isDraggingConnection && connectionSourceNode != null) {
            // Try to create connection if hovering over valid socket
            if (hoveredNode != null && hoveredSocket != -1) {
                if (isOutputSocket && hoveredSocketIsInput) {
                    // Remove any existing incoming connection to the target socket
                    connections.removeIf(conn -> 
                        conn.getInputNode() == hoveredNode && conn.getInputSocket() == hoveredSocket
                    );
                    
                    // Connect output to input
                    NodeConnection newConnection = new NodeConnection(connectionSourceNode, hoveredNode, connectionSourceSocket, hoveredSocket);
                    connections.add(newConnection);
                    System.out.println("Created new connection from " + connectionSourceNode.getType() + " to " + hoveredNode.getType());
                } else if (!isOutputSocket && !hoveredSocketIsInput) {
                    // Remove any existing outgoing connection from the target socket
                    connections.removeIf(conn -> 
                        conn.getOutputNode() == hoveredNode && conn.getOutputSocket() == hoveredSocket
                    );
                    
                    // Connect input to output (reverse connection)
                    NodeConnection newConnection = new NodeConnection(hoveredNode, connectionSourceNode, hoveredSocket, connectionSourceSocket);
                    connections.add(newConnection);
                    System.out.println("Created new connection from " + hoveredNode.getType() + " to " + connectionSourceNode.getType());
                } else {
                    // Invalid connection - restore original
                    if (disconnectedConnection != null) {
                        connections.add(disconnectedConnection);
                        System.out.println("Restored original connection (invalid target)");
                    }
                }
            } else {
                // No valid target - restore original connection
                if (disconnectedConnection != null) {
                    connections.add(disconnectedConnection);
                    System.out.println("Restored original connection (no target)");
                }
            }
        }
        
        isDraggingConnection = false;
        connectionSourceNode = null;
        connectionSourceSocket = -1;
        hoveredNode = null;
        hoveredSocket = -1;
        disconnectedConnection = null;
    }
    
    public boolean isInSidebar(int mouseX, int sidebarWidth) {
        return mouseX < sidebarWidth;
    }
    
    public boolean isAnyNodeBeingDragged() {
        return draggingNode != null || isDraggingConnection;
    }
    
    public void startPanning(int mouseX, int mouseY) {
        isPanning = true;
        panStartX = mouseX;
        panStartY = mouseY;
        panStartCameraX = cameraX;
        panStartCameraY = cameraY;
    }
    
    public void updatePanning(int mouseX, int mouseY) {
        if (isPanning) {
            int deltaX = mouseX - panStartX;
            int deltaY = mouseY - panStartY;
            cameraX = panStartCameraX - deltaX; // Flip horizontal panning
            cameraY = panStartCameraY - deltaY; // Flip vertical panning
        }
    }
    
    public void stopPanning() {
        isPanning = false;
    }
    
    public boolean isPanning() {
        return isPanning;
    }
    
    public void resetCamera() {
        cameraX = 0;
        cameraY = 0;
    }
    
    // Convert screen coordinates to world coordinates
    public int screenToWorldX(int screenX) {
        return screenX + cameraX;
    }
    
    public int screenToWorldY(int screenY) {
        return screenY + cameraY;
    }
    
    // Convert world coordinates to screen coordinates
    public int worldToScreenX(int worldX) {
        return worldX - cameraX;
    }
    
    public int worldToScreenY(int worldY) {
        return worldY - cameraY;
    }
    
    public void deleteNodeIfInSidebar(Node node, int mouseX, int sidebarWidth) {
        if (isInSidebar(mouseX, sidebarWidth)) {
            removeNode(node);
        }
    }
    
    public boolean isNodeOverSidebar(Node node, int sidebarWidth) {
        // Check if node is more than halfway over the sidebar area (for deletion)
        // Use world coordinates (without camera offset) for this check
        return node.getX() + node.getWidth() / 2 < sidebarWidth;
    }
    
    public boolean isNodeOverSidebar(Node node, int sidebarWidth, int screenX, int screenWidth) {
        // Check if node is more than halfway over the sidebar area (for deletion)
        // Use screen coordinates (with camera offset) for this check
        return screenX + screenWidth / 2 < sidebarWidth;
    }
    
    public boolean tryConnectToSocket(Node targetNode, int targetSocket, boolean isInput) {
        if (isDraggingConnection && connectionSourceNode != null) {
            // Validate connection (output can only connect to input)
            if (isInput && connectionSourceNode != targetNode) {
                // Create new connection
                connections.add(new NodeConnection(connectionSourceNode, targetNode, connectionSourceSocket, targetSocket));
                stopDraggingConnection();
                return true;
            }
        }
        return false;
    }
    
    public NodeConnection getConnectionAt(int mouseX, int mouseY) {
        for (NodeConnection connection : connections) {
            // Simple check - could be improved with better line collision detection
            Node outputNode = connection.getOutputNode();
            Node inputNode = connection.getInputNode();
            
            int outputX = outputNode.getSocketX(false);
            int outputY = outputNode.getSocketY(connection.getOutputSocket(), false);
            int inputX = inputNode.getSocketX(true);
            int inputY = inputNode.getSocketY(connection.getInputSocket(), true);
            
            // Check if mouse is near the connection line (simplified)
            if (Math.abs(mouseY - (outputY + inputY) / 2) < 10) {
                int minX = Math.min(outputX, inputX);
                int maxX = Math.max(outputX, inputX);
                if (mouseX >= minX && mouseX <= maxX) {
                    return connection;
                }
            }
        }
        return null;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        // Render connections first (behind nodes)
        renderConnections(context);
        
        // Render nodes
        for (Node node : nodes) {
            renderNode(context, textRenderer, node, mouseX, mouseY, delta);
        }
    }

    private void renderNode(DrawContext context, TextRenderer textRenderer, Node node, int mouseX, int mouseY, float delta) {
        int x = node.getX() - cameraX;
        int y = node.getY() - cameraY;
        int width = node.getWidth();
        int height = node.getHeight();

        // Check if node is being dragged over sidebar (grey-out effect)
        // Use screen coordinates (with camera offset) for this check
        boolean isOverSidebar = node.isDragging() && isNodeOverSidebar(node, 120, x, width); // 120 is sidebar width

        // Node background
        int bgColor = node.isSelected() ? 0xFF404040 : 0xFF2A2A2A;
        if (isOverSidebar) {
            bgColor = 0xFF333333; // Grey when over sidebar for deletion
        }
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Node border - use light blue for selection, node type color otherwise
        int borderColor = node.isSelected() ? 0xFF87CEEB : node.getType().getColor(); // Light blue selection
        if (isOverSidebar) {
            borderColor = 0xFF555555; // Darker grey border when over sidebar
        }
        context.drawBorder(x, y, width, height, borderColor);
        
        // Node header
        int headerColor = node.getType().getColor() & 0x80FFFFFF;
        if (isOverSidebar) {
            headerColor = 0x80555555; // Grey header when over sidebar
        }
        context.fill(x + 1, y + 1, x + width - 1, y + 14, headerColor);
        
        // Node title
        int titleColor = isOverSidebar ? 0xFF888888 : 0xFFFFFFFF; // Grey text when over sidebar
        context.drawTextWithShadow(
            textRenderer,
            node.getDisplayName(),
            x + 4,
            y + 4,
            titleColor
        );
        
        // Render input sockets
        for (int i = 0; i < node.getInputSocketCount(); i++) {
            boolean isHovered = (hoveredSocketNode == node && hoveredSocketIndex == i && hoveredSocketIsInput);
            int socketColor = isHovered ? 0xFF87CEEB : node.getType().getColor(); // Light blue when hovered
            if (isOverSidebar) {
                socketColor = 0xFF666666; // Grey sockets when over sidebar
            }
            renderSocket(context, node.getSocketX(true) - cameraX, node.getSocketY(i, true) - cameraY, true, socketColor);
        }
        
        // Render output sockets
        for (int i = 0; i < node.getOutputSocketCount(); i++) {
            boolean isHovered = (hoveredSocketNode == node && hoveredSocketIndex == i && !hoveredSocketIsInput);
            int socketColor = isHovered ? 0xFF87CEEB : node.getType().getColor(); // Light blue when hovered
            if (isOverSidebar) {
                socketColor = 0xFF666666; // Grey sockets when over sidebar
            }
            renderSocket(context, node.getSocketX(false) - cameraX, node.getSocketY(i, false) - cameraY, false, socketColor);
        }
        
        // Render node parameters area (placeholder for now)
        int paramBgColor = isOverSidebar ? 0xFF2A2A2A : 0xFF1A1A1A; // Grey when over sidebar
        context.fill(x + 3, y + 16, x + width - 3, y + height - 3, paramBgColor);
        
        int paramTextColor = isOverSidebar ? 0xFF888888 : 0xFFE0E0E0; // Grey text when over sidebar
        context.drawTextWithShadow(
            textRenderer,
            "Parameters",
            x + 5,
            y + 18,
            paramTextColor
        );
    }

    private void renderSocket(DrawContext context, int x, int y, boolean isInput, int color) {
        // Socket circle
        context.fill(x - 3, y - 3, x + 3, y + 3, color);
        context.drawBorder(x - 3, y - 3, 6, 6, 0xFF000000);
        
        // Socket highlight
        context.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
    }

    private void renderConnections(DrawContext context) {
        for (NodeConnection connection : connections) {
            Node outputNode = connection.getOutputNode();
            Node inputNode = connection.getInputNode();
            
            int outputX = outputNode.getSocketX(false) - cameraX;
            int outputY = outputNode.getSocketY(connection.getOutputSocket(), false) - cameraY;
            int inputX = inputNode.getSocketX(true) - cameraX;
            int inputY = inputNode.getSocketY(connection.getInputSocket(), true) - cameraY;
            
            // Simple bezier-like curve
            renderConnectionCurve(context, outputX, outputY, inputX, inputY, outputNode.getType().getColor());
        }
        
        // Render dragging connection if active
        if (isDraggingConnection && connectionSourceNode != null) {
            int sourceX = connectionSourceNode.getSocketX(!isOutputSocket) - cameraX;
            int sourceY = connectionSourceNode.getSocketY(connectionSourceSocket, !isOutputSocket) - cameraY;
            int targetX = connectionDragX - cameraX;
            int targetY = connectionDragY - cameraY;
            
            
            // Snap to hovered socket if available
            if (hoveredNode != null && hoveredSocket != -1) {
                targetX = hoveredNode.getSocketX(hoveredSocketIsInput) - cameraX;
                targetY = hoveredNode.getSocketY(hoveredSocket, hoveredSocketIsInput) - cameraY;
                
                // Highlight the target socket
                renderSocket(context, targetX, targetY, hoveredSocketIsInput, 0xFF87CEEB); // Light blue highlight
            }
            
            // Render the dragging connection using the source node's color
            renderConnectionCurve(context, sourceX, sourceY, targetX, targetY, connectionSourceNode.getType().getColor());
        }
    }

    private void renderConnectionCurve(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Draw a simple L-shaped connection line
        int midX = x1 + (x2 - x1) / 2;
        
        // Horizontal line from source to middle
        context.drawHorizontalLine(Math.min(x1, midX), Math.max(x1, midX), y1, color);
        
        // Vertical line from middle to target
        context.drawVerticalLine(midX, Math.min(y1, y2), Math.max(y1, y2), color);
        
        // Horizontal line from middle to target
        context.drawHorizontalLine(Math.min(midX, x2), Math.max(midX, x2), y2, color);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<NodeConnection> getConnections() {
        return connections;
    }
    
    public int getCameraX() {
        return cameraX;
    }
    
    public int getCameraY() {
        return cameraY;
    }
}
