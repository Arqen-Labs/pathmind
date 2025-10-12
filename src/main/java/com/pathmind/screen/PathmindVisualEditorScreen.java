package com.pathmind.screen;

import com.pathmind.PathmindKeybinds;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeType;
import com.pathmind.ui.NodeGraph;
import com.pathmind.ui.NodeParameterOverlay;
import com.pathmind.ui.Sidebar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * The main visual editor screen for Pathmind.
 * This screen provides the interface for creating and editing node-based workflows.
 */
public class PathmindVisualEditorScreen extends Screen {
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int SIDEBAR_WIDTH = 120;
    
    // Dark mode color palette
    private static final int DARK_GREY = 0xFF1A1A1A;        // Very dark grey background
    private static final int DARK_GREY_ALT = 0xFF2A2A2A;     // Slightly lighter dark grey
    private static final int LIGHT_BLUE = 0xFF87CEEB;        // Light blue accent
    private static final int LIGHT_BLUE_DARK = 0xFF4682B4;   // Darker light blue
    private static final int GREY_LINE = 0xFF666666;         // Grey line color
    private static final int WHITE = 0xFFFFFFFF;             // Pure white text
    private static final int WHITE_MUTED = 0xFFE0E0E0;       // Muted white for secondary text
    
    private NodeGraph nodeGraph;
    private Sidebar sidebar;
    private NodeParameterOverlay parameterOverlay;
    
    // Drag and drop state
    private boolean isDraggingFromSidebar = false;
    private NodeType draggingNodeType = null;
    private int dragStartX, dragStartY;
    
    public PathmindVisualEditorScreen() {
        super(Text.translatable("screen.pathmind.visual_editor.title"));
        this.nodeGraph = new NodeGraph();
        this.sidebar = new Sidebar();
    }

    @Override
    protected void init() {
        super.init();
        // No buttons needed - just the title bar
        
        // Try to load saved node graph first
        if (nodeGraph.hasSavedGraph()) {
            System.out.println("Found saved node graph, loading...");
            if (nodeGraph.load()) {
                System.out.println("Successfully loaded saved node graph");
                return; // Don't initialize default nodes if we loaded a saved graph
            } else {
                System.out.println("Failed to load saved node graph, using default");
            }
        }
        
        // Initialize node graph with proper centering based on screen dimensions
        nodeGraph.initializeWithScreenDimensions(this.width, this.height, sidebar.getWidth(), TITLE_BAR_HEIGHT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fill background with dark grey theme
        context.fill(0, 0, this.width, this.height, DARK_GREY);
        
        // Render title bar at the top
        context.fill(0, 0, this.width, TITLE_BAR_HEIGHT, DARK_GREY_ALT);
        context.drawHorizontalLine(0, this.width, TITLE_BAR_HEIGHT, GREY_LINE);
        
        // Render title bar text
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Pathmind Node Editor"),
                this.width / 2,
                (TITLE_BAR_HEIGHT - this.textRenderer.fontHeight) / 2 + 1,
                WHITE
        );
        
        // Update mouse hover for socket highlighting
        nodeGraph.updateMouseHover(mouseX, mouseY);
        
        // Render node graph (stationary nodes only)
        renderNodeGraph(context, mouseX, mouseY, delta, false);
        
        // Always render sidebar after node graph to ensure sidebar line is visible
        sidebar.render(context, this.textRenderer, mouseX, mouseY, TITLE_BAR_HEIGHT, this.height - TITLE_BAR_HEIGHT);
        
        // Render dragged nodes above sidebar
        renderNodeGraph(context, mouseX, mouseY, delta, true);
        
        // Render dragging node from sidebar
        if (isDraggingFromSidebar && draggingNodeType != null) {
            renderDraggingNode(context, mouseX, mouseY);
        }
        
        // Render home button in bottom right
        renderHomeButton(context, mouseX, mouseY);
        
        // Render parameter overlay if visible
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            parameterOverlay.render(context, this.textRenderer, mouseX, mouseY, delta);
        }
        
        // Re-render title bar on top of everything to ensure it's always visible
        context.fill(0, 0, this.width, TITLE_BAR_HEIGHT, DARK_GREY_ALT);
        context.drawHorizontalLine(0, this.width, TITLE_BAR_HEIGHT, GREY_LINE);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Pathmind Node Editor"),
                this.width / 2,
                (TITLE_BAR_HEIGHT - this.textRenderer.fontHeight) / 2 + 1,
                WHITE
        );
    }
    
    private void renderDraggingNode(DrawContext context, int mouseX, int mouseY) {
        if (draggingNodeType == null) return;
        
        // Create a temporary node for rendering
        Node tempNode = new Node(draggingNodeType, 0, 0);
        tempNode.setDragging(true);
        
        // Calculate proper centering based on node dimensions
        int width = tempNode.getWidth();
        int height = tempNode.getHeight();
        int x = mouseX - width / 2;
        int y = mouseY - height / 2;
        
        // Update temp node position for rendering
        tempNode.setPosition(x, y);
        
        // Render the node with a slight transparency
        int alpha = 0x80;
        int nodeColor = (draggingNodeType.getColor() & 0x00FFFFFF) | alpha;
        
        // Node background with transparency
        context.fill(x, y, x + width, y + height, 0x802A2A2A);
        // Draw grey outline for dragging state
        context.drawBorder(x, y, width, height, 0xFFAAAAAA);
        
        // Node header
        if (draggingNodeType != NodeType.START && draggingNodeType != NodeType.END) {
            context.fill(x + 1, y + 1, x + width - 1, y + 14, nodeColor);
            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(draggingNodeType.getDisplayName()),
                x + 4,
                y + 4,
                0xFFFFFFFF
            );
        }
    }
    
    private void renderNodeGraph(DrawContext context, int mouseX, int mouseY, float delta, boolean onlyDragged) {
        if (!onlyDragged) {
            // Node graph background
            context.fill(sidebar.getWidth(), TITLE_BAR_HEIGHT, this.width, this.height, DARK_GREY);
            
            // Render grid pattern for better visual organization
            renderGrid(context);
        }
        
        // Render nodes
        nodeGraph.render(context, this.textRenderer, mouseX, mouseY, delta, onlyDragged);
    }
    
    private void renderGrid(DrawContext context) {
        int gridSize = 20;
        int startX = sidebar.getWidth();
        int startY = TITLE_BAR_HEIGHT;
        
        // Get camera offset from node graph
        int cameraX = nodeGraph.getCameraX();
        int cameraY = nodeGraph.getCameraY();
        
        // Calculate grid offset based on camera position
        int gridOffsetX = cameraX % gridSize;
        int gridOffsetY = cameraY % gridSize;
        
        // Adjust starting positions to account for camera offset
        int adjustedStartX = startX - gridOffsetX;
        int adjustedStartY = startY - gridOffsetY;
        
        // Vertical lines
        for (int x = adjustedStartX; x < this.width; x += gridSize) {
            context.drawVerticalLine(x, startY, this.height, 0x40333333);
        }
        
        // Horizontal lines
        for (int y = adjustedStartY; y < this.height; y += gridSize) {
            context.drawHorizontalLine(startX, this.width, y, 0x40333333);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle parameter overlay clicks first
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            if (parameterOverlay.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Check if clicking home button
        if (isHomeButtonClicked((int)mouseX, (int)mouseY, button)) {
            nodeGraph.resetCamera();
            return true;
        }
        
        // Check if clicking in sidebar to add nodes
        if (mouseX < sidebar.getWidth() && mouseY > TITLE_BAR_HEIGHT) {
            if (sidebar.mouseClicked(mouseX, mouseY, button)) {
                // Check if we should start dragging a node from sidebar
                if (sidebar.isHoveringNode()) {
                    isDraggingFromSidebar = true;
                    draggingNodeType = sidebar.getHoveredNodeType();
                    dragStartX = (int)mouseX;
                    dragStartY = (int)mouseY;
                }
                return true;
            }
        }
        
        // Check if clicking on nodes in the graph area
        if (mouseX >= sidebar.getWidth() && mouseY > TITLE_BAR_HEIGHT) {
            // Handle right-click or middle-click for panning
            if (button == 1 || button == 2) { // Right click or middle click
                nodeGraph.startPanning((int)mouseX, (int)mouseY);
                return true;
            }
            
            // Check if clicking START button
            if (button == 0 && nodeGraph.isHoveringStartButton()) { // Left click on START button
                if (nodeGraph.handleStartButtonClick()) {
                    // Close the GUI after execution starts
                    this.close();
                    return true;
                }
            }
            
            return handleNodeGraphClick(mouseX, mouseY, button);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    
    private boolean handleNodeGraphClick(double mouseX, double mouseY, int button) {
        // FIRST check if clicking on ANY socket (before checking node body)
        for (Node node : nodeGraph.getNodes()) {
            // Check input sockets
            for (int i = 0; i < node.getInputSocketCount(); i++) {
                if (node.isSocketClicked((int)mouseX, (int)mouseY, i, true)) {
                    if (button == 0) { // Left click - start dragging connection from input
                        nodeGraph.startDraggingConnection(node, i, false, (int)mouseX, (int)mouseY);
                        return true;
                    }
                }
            }
            
            // Check output sockets
            for (int i = 0; i < node.getOutputSocketCount(); i++) {
                if (node.isSocketClicked((int)mouseX, (int)mouseY, i, false)) {
                    if (button == 0) { // Left click - start dragging connection from output
                        nodeGraph.startDraggingConnection(node, i, true, (int)mouseX, (int)mouseY);
                        return true;
                    }
                }
            }
        }
        
        // THEN check if clicking on node body
        Node clickedNode = nodeGraph.getNodeAt((int)mouseX, (int)mouseY);
        
        if (clickedNode != null) {
            // Node body clicked (not socket)
            if (button == 0) { // Left click - select node or start dragging
                // Check for double-click to open parameter editor
                if (nodeGraph.handleNodeClick(clickedNode, (int)mouseX, (int)mouseY) && 
                    clickedNode.hasParameters()) {
                    // Open parameter overlay
                    parameterOverlay = new NodeParameterOverlay(
                        clickedNode, 
                        this.width, 
                        this.height, 
                        () -> parameterOverlay = null // Clear reference on close
                    );
                    parameterOverlay.init();
                    parameterOverlay.show();
                    return true;
                }
                
                nodeGraph.selectNode(clickedNode);
                nodeGraph.startDragging(clickedNode, (int)mouseX, (int)mouseY);
                return true;
            }
        } else {
            // Check if clicking on a connection to delete it
            var connection = nodeGraph.getConnectionAt((int)mouseX, (int)mouseY);
            if (connection != null && button == 1) {
                nodeGraph.getConnections().remove(connection);
                return true;
            }
            
            // Clicked on empty space - deselect and stop dragging
            nodeGraph.selectNode(null);
            nodeGraph.stopDraggingConnection();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Handle dragging from sidebar
        if (isDraggingFromSidebar && button == 0) {
            return true; // Continue dragging
        }
        
        // Handle node dragging and connection dragging
        if (button == 0) {
            nodeGraph.updateDrag((int)mouseX, (int)mouseY);
            return true;
        }
        
        // Handle panning with right-click or middle-click
        if ((button == 1 || button == 2) && nodeGraph.isPanning()) {
            nodeGraph.updatePanning((int)mouseX, (int)mouseY);
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Handle dropping node from sidebar
            if (isDraggingFromSidebar) {
                if (mouseX >= sidebar.getWidth() && mouseY > TITLE_BAR_HEIGHT) {
                    // Drop in graph area - create new node with proper centering
                    Node tempNode = new Node(draggingNodeType, 0, 0);
                    int width = tempNode.getWidth();
                    int height = tempNode.getHeight();
                    int nodeX = (int)mouseX - width / 2;
                    int nodeY = (int)mouseY - height / 2;
                    
                    Node newNode = new Node(draggingNodeType, nodeX, nodeY);
                    nodeGraph.addNode(newNode);
                    nodeGraph.selectNode(newNode);
                }
                // Reset drag state
                isDraggingFromSidebar = false;
                draggingNodeType = null;
            } else {
                // Check if dragging node into sidebar for deletion (only if actually dragging)
                if (nodeGraph.getSelectedNode() != null && nodeGraph.getSelectedNode().isDragging()) {
                    nodeGraph.deleteNodeIfInSidebar(nodeGraph.getSelectedNode(), (int)mouseX, sidebar.getWidth());
                }
                
                nodeGraph.stopDragging();
                nodeGraph.stopDraggingConnection();
            }
        } else if (button == 1 || button == 2) {
            // Stop panning on right-click or middle-click release
            nodeGraph.stopPanning();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle parameter overlay key presses first
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            if (parameterOverlay.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        // Close screen with Escape key
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        
        // Delete selected node with Delete key
        if (keyCode == GLFW.GLFW_KEY_DELETE && nodeGraph.getSelectedNode() != null) {
            nodeGraph.removeNode(nodeGraph.getSelectedNode());
            return true;
        }
        
        // Don't handle the opening keybind - let it be ignored
        // This prevents the screen from closing when the same key is pressed
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Handle parameter overlay character typing first
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            if (parameterOverlay.charTyped(chr, modifiers)) {
                return true;
            }
        }
        
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Handle sidebar scrolling
        if (mouseX >= 0 && mouseX <= sidebar.getWidth()) {
            if (sidebar.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public void close() {
        // Auto-save the node graph when closing
        if (nodeGraph.save()) {
            System.out.println("Node graph auto-saved successfully");
        } else {
            System.err.println("Failed to auto-save node graph");
        }
        
        super.close();
    }
    
    private void renderHomeButton(DrawContext context, int mouseX, int mouseY) {
        int buttonSize = 18;
        int buttonX = this.width - buttonSize - 6;
        int buttonY = this.height - buttonSize - 6;
        
        // Check if mouse is hovering over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonSize && 
                           mouseY >= buttonY && mouseY <= buttonY + buttonSize;
        
        // Button background with rounded appearance
        int bgColor = isHovered ? 0xFF505050 : 0xFF3A3A3A;
        context.fill(buttonX + 1, buttonY + 1, buttonX + buttonSize - 1, buttonY + buttonSize - 1, bgColor);
        
        // Button border (clean outline)
        int borderColor = isHovered ? 0xFF87CEEB : 0xFF666666;
        // Draw clean border outline
        context.drawHorizontalLine(buttonX, buttonX + buttonSize - 1, buttonY, borderColor); // Top
        context.drawHorizontalLine(buttonX, buttonX + buttonSize - 1, buttonY + buttonSize - 1, borderColor); // Bottom
        context.drawVerticalLine(buttonX, buttonY, buttonY + buttonSize - 1, borderColor); // Left
        context.drawVerticalLine(buttonX + buttonSize - 1, buttonY, buttonY + buttonSize - 1, borderColor); // Right
        
        // Home icon (arrow pointing up-left)
        int iconColor = isHovered ? 0xFF87CEEB : WHITE;
        int centerX = buttonX + buttonSize / 2;
        int centerY = buttonY + buttonSize / 2;
        
        // Draw arrow pointing up-left (↖)
        // Main arrow line
        context.drawHorizontalLine(centerX - 4, centerX + 2, centerY, iconColor);
        context.drawVerticalLine(centerX - 4, centerY - 4, centerY + 2, iconColor);
        
        // Arrow head
        context.drawHorizontalLine(centerX - 2, centerX, centerY - 2, iconColor);
        context.drawHorizontalLine(centerX - 3, centerX - 1, centerY - 1, iconColor);
        context.drawVerticalLine(centerX - 2, centerY - 2, centerY, iconColor);
        context.drawVerticalLine(centerX - 3, centerY - 3, centerY - 1, iconColor);
    }
    
    private boolean isHomeButtonClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false; // Only left click
        
        int buttonSize = 18;
        int buttonX = this.width - buttonSize - 6;
        int buttonY = this.height - buttonSize - 6;
        
        return mouseX >= buttonX && mouseX <= buttonX + buttonSize && 
               mouseY >= buttonY && mouseY <= buttonY + buttonSize;
    }

}