package com.pathmind.ui;

import com.pathmind.data.NodeGraphData;
import com.pathmind.data.NodeGraphPersistence;
import com.pathmind.data.PresetManager;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeCategory;
import com.pathmind.nodes.NodeConnection;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeType;
import com.pathmind.nodes.ParameterType;
import com.pathmind.execution.ExecutionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the node graph for the Pathmind visual editor.
 * Handles node rendering, connections, and interactions.
 */
public class NodeGraph {
    private static final int CONNECTION_DOT_SPACING = 12;
    private static final int CONNECTION_DOT_LENGTH = 4;
    private static final int CONNECTION_ANIMATION_STEP_MS = 50;

    private final List<Node> nodes;
    private final List<NodeConnection> connections;
    private Node selectedNode;
    private Node draggingNode;
    private int draggingNodeStartX;
    private int draggingNodeStartY;
    private boolean draggingNodeDetached;
    
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

    // Start button hover state
    private boolean hoveringStartButton = false;
    private Node hoveredStartNode = null;
    private boolean lastStartButtonTriggeredExecution = false;

    private Node sensorDropTarget = null;
    private Node actionDropTarget = null;
    private Node parameterDropTarget = null;
    
    // Double-click detection
    private long lastClickTime = 0;
    private Node lastClickedNode = null;
    private static final long DOUBLE_CLICK_THRESHOLD = 300; // milliseconds
    private int sidebarWidthForRendering = 180;

    private String activePreset;
    private final Set<Node> cascadeDeletionPreviewNodes;

    public NodeGraph() {
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.selectedNode = null;
        this.draggingNode = null;
        this.draggingNodeStartX = 0;
        this.draggingNodeStartY = 0;
        this.draggingNodeDetached = false;
        this.activePreset = PresetManager.getActivePreset();
        this.cascadeDeletionPreviewNodes = new HashSet<>();

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
        
        Node middleNode = new Node(NodeType.GOTO, centerX, centerY - 50);
        nodes.add(middleNode);
        
        // Connect them
        connections.add(new NodeConnection(startNode, middleNode, 0, 0));
    }


    public void addNode(Node node) {
        nodes.add(node);
    }

    public void removeNode(Node node) {
        removeNodeInternal(node, true, true);
    }

    private void removeNodeInternal(Node node, boolean autoReconnect, boolean repositionDetachments) {
        if (node == null) {
            return;
        }

        if (node.hasAttachedSensor()) {
            Node attached = node.getAttachedSensor();
            node.detachSensor();
            if (repositionDetachments && attached != null) {
                attached.setPosition(node.getX() + node.getWidth() + 12, node.getY());
            }
        }

        if (node.hasAttachedActionNode()) {
            Node attached = node.getAttachedActionNode();
            node.detachActionNode();
            if (repositionDetachments && attached != null) {
                attached.setPosition(node.getX() + node.getWidth() + 12, node.getY());
            }
        }

        if (node.hasAttachedParameter()) {
            Node parameter = node.getAttachedParameter();
            node.detachParameter();
            if (repositionDetachments && parameter != null) {
                parameter.setPosition(node.getX() + node.getWidth() + 12, node.getY());
            }
        }

        if (node.isParameterNode() && node.isAttachedToParameterConsumer()) {
            Node consumer = node.getParentParameterConsumer();
            if (consumer != null) {
                consumer.detachParameter();
            }
        }

        if (node.isSensorNode() && node.isAttachedToControl()) {
            Node parent = node.getParentControl();
            if (parent != null) {
                parent.detachSensor();
            }
        }

        if (node.isAttachedToActionControl()) {
            Node parent = node.getParentActionControl();
            if (parent != null) {
                parent.detachActionNode();
            }
        }

        if (sensorDropTarget == node) {
            sensorDropTarget = null;
            actionDropTarget = null;
        }

        if (actionDropTarget == node) {
            actionDropTarget = null;
        }

        if (parameterDropTarget == node) {
            parameterDropTarget = null;
        }

        if (autoReconnect) {
            List<NodeConnection> inputConnections = new ArrayList<>();
            List<NodeConnection> outputConnections = new ArrayList<>();

            for (NodeConnection conn : connections) {
                if (conn.getOutputNode().equals(node)) {
                    outputConnections.add(conn);
                } else if (conn.getInputNode().equals(node)) {
                    inputConnections.add(conn);
                }
            }

            for (NodeConnection inputConn : inputConnections) {
                Node inputSource = inputConn.getOutputNode();
                int inputSocket = inputConn.getOutputSocket();

                for (NodeConnection outputConn : outputConnections) {
                    Node outputTarget = outputConn.getInputNode();
                    int outputSocket = outputConn.getInputSocket();

                    connections.add(new NodeConnection(inputSource, outputTarget, inputSocket, outputSocket));
                }
            }
        }

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
            if (node.isSensorNode() && node.containsPoint(worldX, worldY)) {
                return node;
            }
        }

        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            if (node.isSensorNode()) {
                continue;
            }
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
        sensorDropTarget = null;
        actionDropTarget = null;
        parameterDropTarget = null;

        draggingNode = node;
        draggingNodeStartX = node.getX();
        draggingNodeStartY = node.getY();
        draggingNodeDetached = false;
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
        int worldMouseX = mouseX + cameraX;
        int worldMouseY = mouseY + cameraY;

        if (draggingNode != null) {
            int newX = worldMouseX - draggingNode.getDragOffsetX();
            int newY = worldMouseY - draggingNode.getDragOffsetY();

            if (!draggingNodeDetached) {
                if (newX != draggingNodeStartX || newY != draggingNodeStartY) {
                    detachDraggingNodeFromParents();
                }
            }

            if (draggingNodeDetached) {
                draggingNode.setPosition(newX, newY);

                boolean hideSockets = false;
                if (draggingNode.isSensorNode()) {
                    sensorDropTarget = null;
                    actionDropTarget = null;
                    parameterDropTarget = null;
                    for (Node node : nodes) {
                        if (!node.canAcceptSensor() || node == draggingNode) {
                            continue;
                        }
                        if (node.isPointInsideSensorSlot(worldMouseX, worldMouseY)) {
                            sensorDropTarget = node;
                            hideSockets = true;
                            break;
                        }
                    }
                } else if (draggingNode.isParameterNode()) {
                    sensorDropTarget = null;
                    actionDropTarget = null;
                    parameterDropTarget = null;
                    for (Node node : nodes) {
                        if (!node.hasParameterSlot() || node == draggingNode) {
                            continue;
                        }
                        if (node.isPointInsideParameterSlot(worldMouseX, worldMouseY)) {
                            parameterDropTarget = node;
                            hideSockets = true;
                            break;
                        }
                    }
                } else {
                    sensorDropTarget = null;
                    actionDropTarget = null;
                    parameterDropTarget = null;
                    for (Node node : nodes) {
                        if (!node.canAcceptActionNode() || node == draggingNode) {
                            continue;
                        }
                        if (!node.canAcceptActionNode(draggingNode)) {
                            continue;
                        }
                        if (node.isPointInsideActionSlot(worldMouseX, worldMouseY)) {
                            actionDropTarget = node;
                            hideSockets = true;
                            break;
                        }
                    }
                }
                draggingNode.setSocketsHidden(hideSockets);
            }
        }
        if (isDraggingConnection) {
            connectionDragX = worldMouseX;
            connectionDragY = worldMouseY;

            // Check for socket snapping
            hoveredNode = null;
            hoveredSocket = -1;

            for (Node node : nodes) {
                if (node == connectionSourceNode) continue;
                if (!node.shouldRenderSockets()) continue;

                // Check input sockets if dragging from output
                if (isOutputSocket) {
                    for (int i = 0; i < node.getInputSocketCount(); i++) {
                        if (node.isSocketClicked(worldMouseX, worldMouseY, i, true)) {
                            hoveredNode = node;
                            hoveredSocket = i;
                            hoveredSocketIsInput = true;
                            break;
                        }
                    }
                } else {
                    // Check output sockets if dragging from input
                    for (int i = 0; i < node.getOutputSocketCount(); i++) {
                        if (node.isSocketClicked(worldMouseX, worldMouseY, i, false)) {
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
        hoveringStartButton = false;
        hoveredStartNode = null;

        // Check for start button hover
        for (Node node : nodes) {
            if (node.getType() == NodeType.START && isMouseOverStartButton(node, mouseX, mouseY)) {
                hoveringStartButton = true;
                hoveredStartNode = node;
                break;
            }
        }
        
        // Don't check for socket hover if we're currently dragging a connection
        if (isDraggingConnection) {
            return;
        }

        int worldMouseX = mouseX + cameraX;
        int worldMouseY = mouseY + cameraY;

        // Check for socket hover
        for (Node node : nodes) {
            if (!node.shouldRenderSockets()) {
                continue;
            }
            // Check input sockets
            for (int i = 0; i < node.getInputSocketCount(); i++) {
                if (node.isSocketClicked(worldMouseX, worldMouseY, i, true)) {
                    hoveredSocketNode = node;
                    hoveredSocketIndex = i;
                    hoveredSocketIsInput = true;
                    return;
                }
            }

            // Check output sockets
            for (int i = 0; i < node.getOutputSocketCount(); i++) {
                if (node.isSocketClicked(worldMouseX, worldMouseY, i, false)) {
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
            Node node = draggingNode;
            if (node.isSensorNode() && sensorDropTarget != null) {
                Node target = sensorDropTarget;
                node.setDragging(false);
                if (!target.attachSensor(node)) {
                    node.setSocketsHidden(false);
                }
            } else if (node.isParameterNode() && parameterDropTarget != null) {
                Node target = parameterDropTarget;
                node.setDragging(false);
                if (!target.attachParameter(node)) {
                    node.setSocketsHidden(false);
                }
            } else if (!node.isSensorNode() && actionDropTarget != null) {
                Node target = actionDropTarget;
                node.setDragging(false);
                if (!target.attachActionNode(node)) {
                    node.setSocketsHidden(false);
                }
            } else {
                node.setDragging(false);
                node.setSocketsHidden(false);
            }
            draggingNode = null;
        }
        draggingNodeDetached = false;
        sensorDropTarget = null;
        actionDropTarget = null;
        parameterDropTarget = null;
    }

    private void detachDraggingNodeFromParents() {
        if (draggingNode == null || draggingNodeDetached) {
            return;
        }

        if (draggingNode.isSensorNode() && draggingNode.isAttachedToControl()) {
            Node parent = draggingNode.getParentControl();
            if (parent != null) {
                parent.detachSensor();
            }
        }

        if (draggingNode.isAttachedToActionControl()) {
            Node parent = draggingNode.getParentActionControl();
            if (parent != null) {
                parent.detachActionNode();
            }
        }

        if (draggingNode.isParameterNode() && draggingNode.isAttachedToParameterConsumer()) {
            Node parent = draggingNode.getParentParameterConsumer();
            if (parent != null) {
                parent.detachParameter();
            }
        }

        draggingNodeDetached = true;
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

                    // Ensure only one outgoing connection per source socket
                    connections.removeIf(conn ->
                        conn.getOutputNode() == connectionSourceNode && conn.getOutputSocket() == connectionSourceSocket
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
        // Use the same logic as the grey-out function - more than halfway over the sidebar
        // Calculate the node's screen position (same as in renderNode)
        int nodeScreenX = node.getX() - cameraX;
        if (isNodeOverSidebar(node, sidebarWidth, nodeScreenX, node.getWidth())) {
            if (node.getType().getCategory() == NodeCategory.LOGIC) {
                removeNodeCascade(node);
            } else {
                removeNode(node);
            }
        }
    }

    private void removeNodeCascade(Node node) {
        List<Node> removalOrder = new ArrayList<>();
        collectNodesForCascade(node, removalOrder, new HashSet<>());
        for (Node toRemove : removalOrder) {
            boolean shouldReconnect = toRemove == node;
            removeNodeInternal(toRemove, shouldReconnect, false);
        }
    }

    private void collectNodesForCascade(Node node, List<Node> order, Set<Node> visited) {
        if (node == null || !visited.add(node)) {
            return;
        }

        if (node.hasAttachedSensor()) {
            collectNodesForCascade(node.getAttachedSensor(), order, visited);
        }
        if (node.hasAttachedActionNode()) {
            collectNodesForCascade(node.getAttachedActionNode(), order, visited);
        }

        order.add(node);
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
            if (!targetNode.shouldRenderSockets()) {
                return false;
            }
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
        int worldX = screenToWorldX(mouseX);
        int worldY = screenToWorldY(mouseY);
        for (NodeConnection connection : connections) {
            // Simple check - could be improved with better line collision detection
            Node outputNode = connection.getOutputNode();
            Node inputNode = connection.getInputNode();

            int outputX = outputNode.getSocketX(false);
            int outputY = outputNode.getSocketY(connection.getOutputSocket(), false);
            int inputX = inputNode.getSocketX(true);
            int inputY = inputNode.getSocketY(connection.getInputSocket(), true);

            // Check if mouse is near the connection line (simplified)
            if (Math.abs(worldY - (outputY + inputY) / 2) < 10) {
                int minX = Math.min(outputX, inputX);
                int maxX = Math.max(outputX, inputX);
                if (worldX >= minX && worldX <= maxX) {
                    return connection;
                }
            }
        }
        return null;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, boolean onlyDragged) {
        if (!onlyDragged) {
            updateCascadeDeletionPreview();
            // Render connections first (behind nodes) - only for stationary rendering
            renderConnections(context);
        }

        for (Node node : nodes) {
            if (node.isSensorNode() || node.isAttachedToActionControl()) {
                continue;
            }
            boolean shouldRender = onlyDragged ? node.isDragging() : !node.isDragging();
            if (shouldRender) {
                renderNode(context, textRenderer, node, mouseX, mouseY, delta);
            }
        }

        for (Node node : nodes) {
            if (!node.isSensorNode()) {
                continue;
            }
            boolean parentDragging = node.isAttachedToControl() && node.getParentControl() != null && node.getParentControl().isDragging();
            boolean shouldRender = onlyDragged ? (node.isDragging() || parentDragging) : !node.isDragging();
            if (shouldRender) {
                renderNode(context, textRenderer, node, mouseX, mouseY, delta);
            }
        }

        for (Node node : nodes) {
            if (!node.isAttachedToActionControl()) {
                continue;
            }
            boolean parentDragging = node.getParentActionControl() != null && node.getParentActionControl().isDragging();
            boolean shouldRender = onlyDragged ? (node.isDragging() || parentDragging) : !node.isDragging();
            if (shouldRender) {
                renderNode(context, textRenderer, node, mouseX, mouseY, delta);
            }
        }
    }

    private void renderNode(DrawContext context, TextRenderer textRenderer, Node node, int mouseX, int mouseY, float delta) {
        int x = node.getX() - cameraX;
        int y = node.getY() - cameraY;
        int width = node.getWidth();
        int height = node.getHeight();

        // Check if node is being dragged over sidebar (grey-out effect)
        // Use screen coordinates (with camera offset) for this check
        boolean isOverSidebar = node.isDragging() && isNodeOverSidebar(node, sidebarWidthForRendering, x, width);
        if (!isOverSidebar && cascadeDeletionPreviewNodes.contains(node)) {
            isOverSidebar = true;
        }

        // Node background
        int bgColor = node.isSelected() ? 0xFF404040 : 0xFF2A2A2A;
        if (isOverSidebar) {
            bgColor = 0xFF333333; // Grey when over sidebar for deletion
        }
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Node border - use light blue for selection, grey for dragging, darker node type color for START/events, node type color otherwise
        int borderColor;
        if (node.isDragging()) {
            borderColor = 0xFFAAAAAA; // Medium grey outline when dragging
        } else if (node.isSelected()) {
            borderColor = 0xFF87CEEB; // Light blue selection
        } else if (node.getType() == NodeType.START) {
            borderColor = isOverSidebar ? 0xFF2D4A2D : 0xFF2E7D32; // Darker green for START
        } else {
            borderColor = node.getType().getColor(); // Regular node type color
        }
        if (isOverSidebar && node.getType() != NodeType.START && !node.isDragging()) {
            borderColor = 0xFF555555; // Darker grey border when over sidebar (for regular nodes)
        }
        context.drawBorder(x, y, width, height, borderColor);

        // Node header (only for non-START nodes)
        if (node.getType() != NodeType.START) {
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
        }
        
        if (node.shouldRenderSockets()) {
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
                int socketColor = isHovered ? 0xFF87CEEB : node.getOutputSocketColor(i);
                if (isOverSidebar) {
                    socketColor = 0xFF666666; // Grey sockets when over sidebar
                }
                renderSocket(context, node.getSocketX(false) - cameraX, node.getSocketY(i, false) - cameraY, false, socketColor);
            }
        }

        // Render node content based on type
        if (node.getType() == NodeType.START) {
            // START node - green square with play button
            int greenColor = isOverSidebar ? 0xFF4A5D23 : 0xFF4CAF50; // Darker green when over sidebar
            context.fill(x + 1, y + 1, x + width - 1, y + height - 1, greenColor);

            // Draw play button (triangle pointing right) - with hover effect
            int playColor;
            if (hoveringStartButton) {
                playColor = isOverSidebar ? 0xFFCCCCCC : 0xFFE0E0E0; // Darker when hovered
            } else {
                playColor = isOverSidebar ? 0xFFE0E0E0 : 0xFFFFFFFF; // Normal white
            }
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            
            // Play triangle (pointing right) - bigger and cleaner
            int triangleSize = 10; // Bigger triangle
            int offset = 1; // Slight right offset for centering
            
            // Draw triangle using a cleaner algorithm
            for (int i = 0; i < triangleSize; i++) {
                int lineWidth = i + 1; // Each line gets progressively wider
                int startX = centerX - triangleSize/2 + offset;
                int lineY = centerY - triangleSize/2 + i;
                
                if (lineY >= y + 2 && lineY <= y + height - 3) {
                    context.drawHorizontalLine(startX, startX + lineWidth, lineY, playColor);
                }
            }
            
        } else {
            if (node.isParameterNode()) {
                renderParameterNodeDetails(context, textRenderer, node, isOverSidebar);
            } else if (node.hasParameterSlot()) {
                renderParameterSlot(context, textRenderer, node, isOverSidebar);
            }

            if (node.hasSensorSlot()) {
                renderSensorSlot(context, textRenderer, node, isOverSidebar);
            }
            if (node.hasActionSlot()) {
                renderActionSlot(context, textRenderer, node, isOverSidebar);
            }
        }
    }

    private void renderParameterNodeDetails(DrawContext context, TextRenderer textRenderer, Node node, boolean isOverSidebar) {
        int x = node.getX() - cameraX;
        int y = node.getY() - cameraY;
        int width = node.getWidth();
        int height = node.getHeight();

        int paramBgColor = isOverSidebar ? 0xFF2A2A2A : 0xFF1A1A1A;
        context.fill(x + 3, y + 16, x + width - 3, y + height - 3, paramBgColor);

        int paramY = y + 18;
        int textColor = isOverSidebar ? 0xFF888888 : 0xFFE0E0E0;

        for (NodeParameter param : node.getParameters()) {
            String displayText = node.getParameterLabel(param);
            displayText = trimTextToWidth(displayText, textRenderer, width - 10);
            context.drawTextWithShadow(
                textRenderer,
                displayText,
                x + 5,
                paramY,
                textColor
            );
            paramY += 10;
        }
    }

    private void renderParameterSlot(DrawContext context, TextRenderer textRenderer, Node node, boolean isOverSidebar) {
        int slotLeft = node.getParameterSlotLeft() - cameraX;
        int slotRight = node.getParameterSlotRight() - cameraX;
        int slotTop = node.getParameterSlotTop() - cameraY;
        int slotBottom = node.getParameterSlotBottom() - cameraY;

        int backgroundColor = isOverSidebar ? 0xFF2A2A2A : 0xFF1A1A1A;
        context.fill(slotLeft, slotTop, slotRight, slotBottom, backgroundColor);

        int borderColor = parameterDropTarget == node ? 0xFF87CEEB : 0xFF666666;
        context.drawBorder(slotLeft, slotTop, slotRight - slotLeft, slotBottom - slotTop, borderColor);

        String displayText;
        int textColor;
        if (node.hasAttachedParameter()) {
            displayText = buildParameterSummary(node.getAttachedParameter());
            textColor = isOverSidebar ? 0xFFB0E0E6 : 0xFFE0E0E0;
        } else {
            displayText = "drag parameter here";
            textColor = isOverSidebar ? 0xFF666666 : 0xFF999999;
        }

        displayText = trimTextToWidth(displayText, textRenderer, (slotRight - slotLeft) - 8);
        int textY = slotTop + ((slotBottom - slotTop) - textRenderer.fontHeight) / 2 + 1;
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(displayText),
            slotLeft + 4,
            textY,
            textColor
        );
    }

    private String buildParameterSummary(Node parameterNode) {
        if (parameterNode == null) {
            return "";
        }
        List<NodeParameter> params = parameterNode.getParameters();
        if (params.isEmpty()) {
            return parameterNode.getType().getDisplayName();
        }
        List<String> pieces = new ArrayList<>();
        for (int i = 0; i < Math.min(2, params.size()); i++) {
            NodeParameter param = params.get(i);
            pieces.add(param.getName() + "=" + param.getDisplayValue());
        }
        return parameterNode.getType().getDisplayName() + " – " + String.join(", ", pieces);
    }

    private void renderSensorSlot(DrawContext context, TextRenderer textRenderer, Node node, boolean isOverSidebar) {
        int slotX = node.getSensorSlotLeft() - cameraX;
        int slotY = node.getSensorSlotTop() - cameraY;
        int slotWidth = node.getSensorSlotWidth();
        int slotHeight = node.getSensorSlotHeight();

        int backgroundColor = node.hasAttachedSensor() ? 0xFF262626 : 0xFF1E1E1E;
        if (isOverSidebar) {
            backgroundColor = 0xFF2E2E2E;
        }

        int borderColor = node.hasAttachedSensor() ? 0xFF666666 : 0xFF444444;
        if (sensorDropTarget == node) {
            backgroundColor = 0xFF21303E;
            borderColor = 0xFF87CEEB;
        }

        context.fill(slotX, slotY, slotX + slotWidth, slotY + slotHeight, backgroundColor);
        context.drawBorder(slotX, slotY, slotWidth, slotHeight, borderColor);

        if (!node.hasAttachedSensor()) {
            String placeholder = "Drag a sensor here";
            String display = trimTextToWidth(placeholder, textRenderer, slotWidth - 8);
            int textWidth = textRenderer.getWidth(display);
            int textX = slotX + Math.max(4, (slotWidth - textWidth) / 2);
            int textY = slotY + (slotHeight - textRenderer.fontHeight) / 2;
            int textColor = sensorDropTarget == node ? 0xFF87CEEB : 0xFF888888;
            context.drawTextWithShadow(textRenderer, Text.literal(display), textX, textY, textColor);
        }
    }

    private void renderActionSlot(DrawContext context, TextRenderer textRenderer, Node node, boolean isOverSidebar) {
        int slotX = node.getActionSlotLeft() - cameraX;
        int slotY = node.getActionSlotTop() - cameraY;
        int slotWidth = node.getActionSlotWidth();
        int slotHeight = node.getActionSlotHeight();

        int backgroundColor = node.hasAttachedActionNode() ? 0xFF262626 : 0xFF1E1E1E;
        if (isOverSidebar) {
            backgroundColor = 0xFF2E2E2E;
        }

        int borderColor = node.hasAttachedActionNode() ? 0xFF666666 : 0xFF444444;
        if (actionDropTarget == node) {
            backgroundColor = 0xFF2E3221;
            borderColor = 0xFF8BC34A;
        }

        context.fill(slotX, slotY, slotX + slotWidth, slotY + slotHeight, backgroundColor);
        context.drawBorder(slotX, slotY, slotWidth, slotHeight, borderColor);

        if (!node.hasAttachedActionNode()) {
            String placeholder = "Drag a node here";
            String display = trimTextToWidth(placeholder, textRenderer, slotWidth - 8);
            int textWidth = textRenderer.getWidth(display);
            int textX = slotX + Math.max(4, (slotWidth - textWidth) / 2);
            int textY = slotY + (slotHeight - textRenderer.fontHeight) / 2;
            int textColor = actionDropTarget == node ? 0xFF8BC34A : 0xFF888888;
            context.drawTextWithShadow(textRenderer, Text.literal(display), textX, textY, textColor);
        }
    }

    private String trimTextToWidth(String text, TextRenderer renderer, int maxWidth) {
        if (renderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = renderer.getWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return ellipsis;
        }

        String baseText = text;
        if (baseText.endsWith(ellipsis)) {
            baseText = baseText.substring(0, baseText.length() - ellipsis.length());
        }

        StringBuilder builder = new StringBuilder(baseText);
        while (builder.length() > 0 && renderer.getWidth(builder.toString() + ellipsis) > maxWidth) {
            builder.setLength(builder.length() - 1);
        }
        return builder.append(ellipsis).toString();
    }

    private void renderSocket(DrawContext context, int x, int y, boolean isInput, int color) {
        // Socket circle
        context.fill(x - 3, y - 3, x + 3, y + 3, color);
        context.drawBorder(x - 3, y - 3, 6, 6, 0xFF000000);
        
        // Socket highlight
        context.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
    }

    private void renderConnections(DrawContext context) {
        ExecutionManager manager = ExecutionManager.getInstance();
        boolean animateConnections = manager.isExecuting();
        long animationTimestamp = System.currentTimeMillis();

        for (NodeConnection connection : connections) {
            Node outputNode = connection.getOutputNode();
            Node inputNode = connection.getInputNode();

            if (!outputNode.shouldRenderSockets() || !inputNode.shouldRenderSockets()) {
                continue;
            }

            int outputX = outputNode.getSocketX(false) - cameraX;
            int outputY = outputNode.getSocketY(connection.getOutputSocket(), false) - cameraY;
            int inputX = inputNode.getSocketX(true) - cameraX;
            int inputY = inputNode.getSocketY(connection.getInputSocket(), true) - cameraY;
            
            // Simple bezier-like curve
            if (animateConnections && manager.shouldAnimateConnection(connection)) {
                renderAnimatedConnectionCurve(context, outputX, outputY, inputX, inputY,
                        outputNode.getOutputSocketColor(connection.getOutputSocket()), animationTimestamp);
            } else {
                renderConnectionCurve(context, outputX, outputY, inputX, inputY,
                        outputNode.getOutputSocketColor(connection.getOutputSocket()));
            }
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
            if (animateConnections) {
                renderAnimatedConnectionCurve(context, sourceX, sourceY, targetX, targetY,
                        connectionSourceNode.getOutputSocketColor(connectionSourceSocket), animationTimestamp);
            } else {
                renderConnectionCurve(context, sourceX, sourceY, targetX, targetY,
                        connectionSourceNode.getOutputSocketColor(connectionSourceSocket));
            }
        }
    }

    private void renderAnimatedConnectionCurve(DrawContext context, int x1, int y1, int x2, int y2, int color, long timestamp) {
        int midX = x1 + (x2 - x1) / 2;

        int firstSegmentLength = Math.abs(midX - x1);
        int secondSegmentLength = Math.abs(y2 - y1);

        int animationOffset = (int) ((timestamp / CONNECTION_ANIMATION_STEP_MS) % CONNECTION_DOT_SPACING);

        drawAnimatedSegment(context, x1, y1, midX, y1, true, color, animationOffset, 0);
        drawAnimatedSegment(context, midX, y1, midX, y2, false, color, animationOffset, firstSegmentLength);
        drawAnimatedSegment(context, midX, y2, x2, y2, true, color, animationOffset,
                firstSegmentLength + secondSegmentLength);
    }

    private void drawAnimatedSegment(DrawContext context, int x1, int y1, int x2, int y2, boolean horizontal,
                                     int color, int animationOffset, int distanceOffset) {
        int length = horizontal ? Math.abs(x2 - x1) : Math.abs(y2 - y1);
        if (length == 0) {
            return;
        }

        int direction = horizontal ? Integer.compare(x2, x1) : Integer.compare(y2, y1);
        int start = horizontal ? x1 : y1;
        int staticCoord = horizontal ? y1 : x1;

        int initialOffset = mod(distanceOffset - animationOffset, CONNECTION_DOT_SPACING);
        int stepStart = (CONNECTION_DOT_SPACING - initialOffset) % CONNECTION_DOT_SPACING;

        int position = stepStart;
        while (position > 0) {
            position -= CONNECTION_DOT_SPACING;
        }

        boolean drewSegment = false;

        for (; position <= length; position += CONNECTION_DOT_SPACING) {
            int minDistance = Math.max(position, 0);
            int maxDistance = Math.min(position + CONNECTION_DOT_LENGTH - 1, length);
            if (maxDistance < 0 || minDistance > length || minDistance > maxDistance) {
                continue;
            }

            drewSegment = true;

            int startPos = start + minDistance * direction;
            int endPos = start + maxDistance * direction;

            if (horizontal) {
                int minX = Math.min(startPos, endPos);
                int maxX = Math.max(startPos, endPos);
                context.drawHorizontalLine(minX, maxX, staticCoord, color);
            } else {
                int minY = Math.min(startPos, endPos);
                int maxY = Math.max(startPos, endPos);
                context.drawVerticalLine(staticCoord, minY, maxY, color);
            }
        }

        if (!drewSegment) {
            int fallbackLength = Math.min(CONNECTION_DOT_LENGTH, length);
            int minDistance = Math.max(0, length - fallbackLength);
            int maxDistance = length;
            int startPos = start + minDistance * direction;
            int endPos = start + maxDistance * direction;

            if (horizontal) {
                int minX = Math.min(startPos, endPos);
                int maxX = Math.max(startPos, endPos);
                context.drawHorizontalLine(minX, maxX, staticCoord, color);
            } else {
                int minY = Math.min(startPos, endPos);
                int maxY = Math.max(startPos, endPos);
                context.drawVerticalLine(staticCoord, minY, maxY, color);
            }
        }
    }

    private int mod(int value, int mod) {
        int result = value % mod;
        return result < 0 ? result + mod : result;
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
    
    public void setSidebarWidth(int sidebarWidth) {
        this.sidebarWidthForRendering = sidebarWidth;
    }
    
    /**
     * Handle node click and detect double-clicks for parameter editing
     * Returns true if a double-click was detected and the popup should open
     */
    public boolean handleNodeClick(Node clickedNode, int mouseX, int mouseY) {
        long currentTime = System.currentTimeMillis();
        boolean isDoubleClick = false;
        
        if (clickedNode == lastClickedNode && 
            (currentTime - lastClickTime) < DOUBLE_CLICK_THRESHOLD) {
            isDoubleClick = true;
        }
        
        lastClickTime = currentTime;
        lastClickedNode = clickedNode;
        
        return isDoubleClick;
    }
    
    private boolean isMouseOverStartButton(Node startNode, int mouseX, int mouseY) {
        int x = startNode.getX() - cameraX;
        int y = startNode.getY() - cameraY;
        int centerX = x + startNode.getWidth() / 2;
        int centerY = y + startNode.getHeight() / 2;
        
        // Check if mouse is within the triangle area
        int triangleSize = 10;
        int offset = 1;
        int startX = centerX - triangleSize/2 + offset;
        
        // Simple bounding box check for the triangle
        return mouseX >= startX && mouseX <= startX + triangleSize &&
               mouseY >= centerY - triangleSize/2 && mouseY <= centerY + triangleSize/2;
    }
    
    public boolean isHoveringStartButton() {
        return hoveringStartButton;
    }

    public boolean handleStartButtonClick(int mouseX, int mouseY) {
        lastStartButtonTriggeredExecution = false;
        Node startNode = findStartNodeAt(mouseX, mouseY);
        if (startNode == null) {
            return false;
        }

        hoveredStartNode = startNode;

        ExecutionManager manager = ExecutionManager.getInstance();
        if (manager.isChainActive(startNode)) {
            return manager.requestStopForStart(startNode);
        }

        boolean started = manager.executeBranch(startNode, nodes, connections);
        if (started) {
            lastStartButtonTriggeredExecution = true;
        }
        return started;
    }

    private Node findStartNodeAt(int mouseX, int mouseY) {
        for (Node node : nodes) {
            if (node.getType() == NodeType.START && isMouseOverStartButton(node, mouseX, mouseY)) {
                return node;
            }
        }
        return null;
    }
    
    
    /**
     * Check if a node should show parameters (Start and End nodes don't)
     */
    public boolean shouldShowParameters(Node node) {
        return node.isParameterNode();
    }

    public boolean didLastStartButtonTriggerExecution() {
        return lastStartButtonTriggeredExecution;
    }

    private void updateCascadeDeletionPreview() {
        cascadeDeletionPreviewNodes.clear();
        for (Node node : nodes) {
            if (node.getType().getCategory() != NodeCategory.LOGIC) {
                continue;
            }
            if (!node.isDragging()) {
                continue;
            }
            int screenX = node.getX() - cameraX;
            if (!isNodeOverSidebar(node, sidebarWidthForRendering, screenX, node.getWidth())) {
                continue;
            }
            List<Node> removalOrder = new ArrayList<>();
            collectNodesForCascade(node, removalOrder, new HashSet<>());
            cascadeDeletionPreviewNodes.addAll(removalOrder);
        }
    }
    
    /**
     * Save the current node graph to disk
     */
    public boolean save() {
        return NodeGraphPersistence.saveNodeGraphForPreset(activePreset, nodes, connections);
    }

    /**
     * Load a node graph from disk, replacing the current one
     */
    public boolean load() {
        NodeGraphData data = NodeGraphPersistence.loadNodeGraphForPreset(activePreset);
        if (data != null) {
            return applyLoadedData(data);
        }
        return false;
    }

    public boolean importFromPath(Path savePath) {
        NodeGraphData data = NodeGraphPersistence.loadNodeGraphFromPath(savePath);
        if (data != null) {
            return applyLoadedData(data);
        }
        return false;
    }

    public boolean exportToPath(Path savePath) {
        return NodeGraphPersistence.saveNodeGraphToPath(nodes, connections, savePath);
    }

    public void clearWorkspace() {
        for (Node node : new ArrayList<>(nodes)) {
            if (node.hasAttachedSensor()) {
                node.detachSensor();
            }
            if (node.hasAttachedActionNode()) {
                node.detachActionNode();
            }
            if (node.isSensorNode() && node.isAttachedToControl()) {
                Node parent = node.getParentControl();
                if (parent != null) {
                    parent.detachSensor();
                }
            }
            if (node.isAttachedToActionControl()) {
                Node parent = node.getParentActionControl();
                if (parent != null) {
                    parent.detachActionNode();
                }
            }
            node.setDragging(false);
            node.setSelected(false);
        }

        nodes.clear();
        connections.clear();
        selectedNode = null;
        draggingNode = null;
        hoveredNode = null;
        hoveredSocketNode = null;
        hoveredSocketIndex = -1;
        hoveredSocket = -1;
        hoveredSocketIsInput = false;
        hoveringStartButton = false;
        hoveredStartNode = null;
        isDraggingConnection = false;
        connectionSourceNode = null;
        disconnectedConnection = null;
        sensorDropTarget = null;
        actionDropTarget = null;
        lastClickedNode = null;
        lastClickTime = 0;
        cascadeDeletionPreviewNodes.clear();
    }

    private boolean applyLoadedData(NodeGraphData data) {
        nodes.clear();
        connections.clear();
        selectedNode = null;
        draggingNode = null;

        // Load nodes and create node map for connections
        java.util.Map<String, Node> nodeMap = new java.util.HashMap<>();
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
                }
            }
            node.recalculateDimensions();

            nodes.add(node);
            nodeMap.put(nodeData.getId(), node);
        }

        // Restore sensor attachments
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

        // Load connections
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

        sensorDropTarget = null;
        actionDropTarget = null;
        hoveredNode = null;
        hoveredSocketNode = null;
        hoveredSocketIndex = -1;
        hoveredSocket = -1;
        hoveredSocketIsInput = false;
        hoveringStartButton = false;
        hoveredStartNode = null;
        isDraggingConnection = false;
        connectionSourceNode = null;
        disconnectedConnection = null;
        lastClickedNode = null;
        lastClickTime = 0;
        cascadeDeletionPreviewNodes.clear();

        System.out.println("Loaded " + nodes.size() + " nodes and " + connections.size() + " connections");
        return true;
    }
    
    /**
     * Check if there's a saved node graph available
     */
    public boolean hasSavedGraph() {
        return NodeGraphPersistence.hasSavedNodeGraph(activePreset);
    }

    public void setActivePreset(String presetName) {
        this.activePreset = presetName;
    }

    public String getActivePreset() {
        return activePreset;
    }
}
