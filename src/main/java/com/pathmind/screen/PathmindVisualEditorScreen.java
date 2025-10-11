package com.pathmind.screen;

import com.pathmind.PathmindKeybinds;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeType;
import com.pathmind.ui.NodeGraph;
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
    private int sidebarOffsetY = TITLE_BAR_HEIGHT;
    
    public PathmindVisualEditorScreen() {
        super(Text.translatable("screen.pathmind.visual_editor.title"));
        this.nodeGraph = new NodeGraph();
    }

    @Override
    protected void init() {
        super.init();
        // No buttons needed - just the title bar
        
        // Initialize node graph with proper centering based on screen dimensions
        nodeGraph.initializeWithScreenDimensions(this.width, this.height, SIDEBAR_WIDTH, TITLE_BAR_HEIGHT);
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
        
        // Render node graph first
        renderNodeGraph(context, mouseX, mouseY, delta);
        
        // Always render sidebar after node graph to ensure sidebar line is visible
        renderSidebar(context, mouseX, mouseY, delta);
        
        // Render home button in bottom right
        renderHomeButton(context, mouseX, mouseY);
        
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
    
    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
        // Sidebar background
        context.fill(0, TITLE_BAR_HEIGHT, SIDEBAR_WIDTH, this.height, DARK_GREY_ALT);
        context.drawVerticalLine(SIDEBAR_WIDTH, TITLE_BAR_HEIGHT, this.height, GREY_LINE);
        
        // Sidebar title
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Node Palette"),
                10,
                TITLE_BAR_HEIGHT + 10,
                LIGHT_BLUE
        );
        
        // TODO: Add draggable node types here later
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Coming Soon..."),
                10,
                TITLE_BAR_HEIGHT + 40,
                WHITE_MUTED
        );
    }
    
    private void renderNodeGraph(DrawContext context, int mouseX, int mouseY, float delta) {
        // Node graph background
        context.fill(SIDEBAR_WIDTH, TITLE_BAR_HEIGHT, this.width, this.height, DARK_GREY);
        
        // Render grid pattern for better visual organization
        renderGrid(context);
        
        // Render nodes
        nodeGraph.render(context, this.textRenderer, mouseX, mouseY, delta);
    }
    
    private void renderGrid(DrawContext context) {
        int gridSize = 20;
        int startX = SIDEBAR_WIDTH;
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
        // Check if clicking home button
        if (isHomeButtonClicked((int)mouseX, (int)mouseY, button)) {
            nodeGraph.resetCamera();
            return true;
        }
        
        // Check if clicking in sidebar to add nodes
        if (mouseX < SIDEBAR_WIDTH && mouseY > TITLE_BAR_HEIGHT) {
            handleSidebarClick(mouseX, mouseY, button);
            return true;
        }
        
        // Check if clicking on nodes in the graph area
        if (mouseX >= SIDEBAR_WIDTH && mouseY > TITLE_BAR_HEIGHT) {
            // Handle right-click or middle-click for panning
            if (button == 1 || button == 2) { // Right click or middle click
                nodeGraph.startPanning((int)mouseX, (int)mouseY);
                return true;
            }
            return handleNodeGraphClick(mouseX, mouseY, button);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void handleSidebarClick(double mouseX, double mouseY, int button) {
        // TODO: Handle dragging nodes from sidebar
        // For now, just placeholder
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
            // Check if dragging node into sidebar for deletion (only if actually dragging)
            if (nodeGraph.getSelectedNode() != null && nodeGraph.getSelectedNode().isDragging()) {
                nodeGraph.deleteNodeIfInSidebar(nodeGraph.getSelectedNode(), (int)mouseX, SIDEBAR_WIDTH);
            }
            
            nodeGraph.stopDragging();
            nodeGraph.stopDraggingConnection();
        } else if (button == 1 || button == 2) {
            // Stop panning on right-click or middle-click release
            nodeGraph.stopPanning();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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