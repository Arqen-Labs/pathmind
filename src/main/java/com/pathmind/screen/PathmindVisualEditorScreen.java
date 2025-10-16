package com.pathmind.screen;

import com.pathmind.data.NodeGraphPersistence;
import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeType;
import com.pathmind.ui.NodeGraph;
import com.pathmind.ui.NodeParameterOverlay;
import com.pathmind.ui.Sidebar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main visual editor screen for Pathmind.
 * This screen provides the interface for creating and editing node-based workflows.
 */
public class PathmindVisualEditorScreen extends Screen {
    private static final int TITLE_BAR_HEIGHT = 20;
    
    // Dark mode color palette
    private static final int DARK_GREY = 0xFF1A1A1A;        // Very dark grey background
    private static final int DARK_GREY_ALT = 0xFF2A2A2A;     // Slightly lighter dark grey
    private static final int GREY_LINE = 0xFF666666;         // Grey line color
    private static final int WHITE = 0xFFFFFFFF;             // Pure white text
    private static final int ACCENT_COLOR = 0xFF87CEEB;
    private static final int SUCCESS_COLOR = 0xFF6DCB5A;
    private static final int ERROR_COLOR = 0xFFE57373;
    private static final int OVERLAY_BACKGROUND = 0xAA000000;
    private static final int BOTTOM_BUTTON_SIZE = 18;
    private static final int BOTTOM_BUTTON_MARGIN = 6;
    private static final int BOTTOM_BUTTON_SPACING = 6;
    
    private NodeGraph nodeGraph;
    private Sidebar sidebar;
    private NodeParameterOverlay parameterOverlay;
    
    // Drag and drop state
    private boolean isDraggingFromSidebar = false;
    private NodeType draggingNodeType = null;

    // Workspace dialogs
    private boolean clearPopupVisible = false;
    private boolean importExportPopupVisible = false;
    private boolean importExportFieldFocused = false;
    private String importExportPath = "";
    private String importExportStatus = "";
    private int importExportStatusColor = 0xFFCCCCCC;
    
    public PathmindVisualEditorScreen() {
        super(Text.translatable("screen.pathmind.visual_editor.title"));
        this.nodeGraph = new NodeGraph();
        this.sidebar = new Sidebar();
        Path defaultPath = NodeGraphPersistence.getDefaultSavePath();
        this.importExportPath = defaultPath != null ? defaultPath.toString() : "";
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
        nodeGraph.setSidebarWidth(sidebar.getWidth());
        
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
        
        // Render workspace controls in bottom right
        renderBottomButtons(context, mouseX, mouseY);

        // Render parameter overlay if visible
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            parameterOverlay.render(context, this.textRenderer, mouseX, mouseY, delta);
        }

        if (clearPopupVisible) {
            renderClearConfirmationPopup(context, mouseX, mouseY);
        }

        if (importExportPopupVisible) {
            renderImportExportPopup(context, mouseX, mouseY);
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
        if (draggingNodeType != NodeType.START && draggingNodeType != NodeType.EVENT_FUNCTION) {
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
        if (clearPopupVisible) {
            if (handleClearPopupClick(mouseX, mouseY, button)) {
                return true;
            }
            return true;
        }

        if (importExportPopupVisible) {
            if (handleImportExportPopupClick(mouseX, mouseY, button)) {
                return true;
            }
            if (button == 0) {
                importExportFieldFocused = false;
            }
            return true;
        }

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

        if (isClearButtonClicked((int)mouseX, (int)mouseY, button)) {
            openClearPopup();
            return true;
        }

        if (isImportExportButtonClicked((int)mouseX, (int)mouseY, button)) {
            openImportExportPopup();
            return true;
        }

        // Check if clicking in sidebar to add nodes
        if (mouseX < sidebar.getWidth() && mouseY > TITLE_BAR_HEIGHT) {
            if (sidebar.mouseClicked(mouseX, mouseY, button)) {
                // Check if we should start dragging a node from sidebar
                if (sidebar.isHoveringNode()) {
                    isDraggingFromSidebar = true;
                    draggingNodeType = sidebar.getHoveredNodeType();
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
        int worldMouseX = nodeGraph.screenToWorldX((int) mouseX);
        int worldMouseY = nodeGraph.screenToWorldY((int) mouseY);
        // FIRST check if clicking on ANY socket (before checking node body)
        for (Node node : nodeGraph.getNodes()) {
            if (!node.shouldRenderSockets()) {
                continue;
            }
            // Check input sockets
            for (int i = 0; i < node.getInputSocketCount(); i++) {
                if (node.isSocketClicked(worldMouseX, worldMouseY, i, true)) {
                    if (button == 0) { // Left click - start dragging connection from input
                        nodeGraph.startDraggingConnection(node, i, false, (int)mouseX, (int)mouseY);
                        return true;
                    }
                }
            }

            // Check output sockets
            for (int i = 0; i < node.getOutputSocketCount(); i++) {
                if (node.isSocketClicked(worldMouseX, worldMouseY, i, false)) {
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
                        TITLE_BAR_HEIGHT,
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
        if (clearPopupVisible || importExportPopupVisible) {
            return true;
        }

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
        if (clearPopupVisible || importExportPopupVisible) {
            return true;
        }

        if (button == 0) {
            // Handle dropping node from sidebar
            if (isDraggingFromSidebar) {
                if (mouseX >= sidebar.getWidth() && mouseY > TITLE_BAR_HEIGHT) {
                    // Drop in graph area - create new node with proper centering
                    Node tempNode = new Node(draggingNodeType, 0, 0);
                    int width = tempNode.getWidth();
                    int height = tempNode.getHeight();
                    int worldMouseX = nodeGraph.screenToWorldX((int) mouseX);
                    int worldMouseY = nodeGraph.screenToWorldY((int) mouseY);
                    int nodeX = worldMouseX - width / 2;
                    int nodeY = worldMouseY - height / 2;

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
        if (clearPopupVisible) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                clearPopupVisible = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirmClearWorkspace();
                return true;
            }
            return true;
        }

        if (importExportPopupVisible) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeImportExportPopup();
                return true;
            }

            if (importExportFieldFocused) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    if (importExportPath != null && !importExportPath.isEmpty()) {
                        importExportPath = importExportPath.substring(0, importExportPath.length() - 1);
                        clearImportExportStatus();
                    }
                    return true;
                }

                if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
                    String clipboard = this.client.keyboard.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        appendToImportExportPath(clipboard);
                        clearImportExportStatus();
                    }
                    return true;
                }
            }

            if (keyCode == GLFW.GLFW_KEY_TAB) {
                importExportFieldFocused = !importExportFieldFocused;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                attemptImport();
                return true;
            }

            return true;
        }

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
        if (clearPopupVisible) {
            return true;
        }

        if (importExportPopupVisible) {
            if (importExportFieldFocused) {
                if (chr >= 32 && chr <= 126) {
                    appendToImportExportPath(Character.toString(chr));
                    clearImportExportStatus();
                    return true;
                }
            }
            return true;
        }

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
        if (clearPopupVisible || importExportPopupVisible) {
            return true;
        }

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

    private void renderClearConfirmationPopup(DrawContext context, int mouseX, int mouseY) {
        context.fill(0, 0, this.width, this.height, OVERLAY_BACKGROUND);

        int popupWidth = 280;
        int popupHeight = 150;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, DARK_GREY_ALT);
        context.drawBorder(popupX, popupY, popupWidth, popupHeight, GREY_LINE);

        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Clear workspace?"),
            popupX + popupWidth / 2,
            popupY + 14,
            WHITE
        );

        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("This will remove all nodes from the workspace."),
            popupX + 20,
            popupY + 48,
            0xFFCCCCCC
        );

        int buttonWidth = 90;
        int buttonHeight = 20;
        int buttonY = popupY + popupHeight - buttonHeight - 16;
        int cancelX = popupX + 20;
        int confirmX = popupX + popupWidth - buttonWidth - 20;

        boolean cancelHovered = isPointInRect(mouseX, mouseY, cancelX, buttonY, buttonWidth, buttonHeight);
        boolean confirmHovered = isPointInRect(mouseX, mouseY, confirmX, buttonY, buttonWidth, buttonHeight);

        drawPopupButton(context, cancelX, buttonY, buttonWidth, buttonHeight, cancelHovered, Text.literal("Cancel"), false);
        drawPopupButton(context, confirmX, buttonY, buttonWidth, buttonHeight, confirmHovered, Text.literal("Clear"), true);
    }

    private void renderImportExportPopup(DrawContext context, int mouseX, int mouseY) {
        context.fill(0, 0, this.width, this.height, OVERLAY_BACKGROUND);

        int popupWidth = 360;
        int popupHeight = 210;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, DARK_GREY_ALT);
        context.drawBorder(popupX, popupY, popupWidth, popupHeight, GREY_LINE);

        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Import / Export Workspace"),
            popupX + popupWidth / 2,
            popupY + 14,
            WHITE
        );

        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Choose a file path to import or export the current layout."),
            popupX + 20,
            popupY + 44,
            0xFFCCCCCC
        );

        Path defaultPath = NodeGraphPersistence.getDefaultSavePath();
        if (defaultPath != null) {
            String defaultLabel = "Default: " + defaultPath.toString();
            String trimmedDefault = this.textRenderer.trimToWidth(defaultLabel, popupWidth - 40);
            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(trimmedDefault),
                popupX + 20,
                popupY + 60,
                0xFF888888
            );
        }

        int fieldX = popupX + 20;
        int fieldY = popupY + 82;
        int fieldWidth = popupWidth - 40;
        int fieldHeight = 18;
        boolean fieldHovered = isPointInRect(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight);

        context.fill(fieldX, fieldY, fieldX + fieldWidth, fieldY + fieldHeight, 0xFF1F1F1F);
        int borderColor = importExportFieldFocused ? ACCENT_COLOR : (fieldHovered ? 0xFF888888 : 0xFF555555);
        context.drawBorder(fieldX, fieldY, fieldWidth, fieldHeight, borderColor);

        String displayPath = importExportPath != null ? importExportPath : "";
        String trimmedPath = this.textRenderer.trimToWidth(displayPath, fieldWidth - 8);
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal(trimmedPath),
            fieldX + 4,
            fieldY + 4,
            WHITE
        );

        if (!importExportStatus.isEmpty()) {
            String statusText = this.textRenderer.trimToWidth(importExportStatus, fieldWidth);
            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(statusText),
                fieldX,
                popupY + popupHeight - 56,
                importExportStatusColor
            );
        }

        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = popupY + popupHeight - buttonHeight - 16;
        int importX = popupX + 20;
        int exportX = importX + buttonWidth + 8;
        int cancelX = popupX + popupWidth - buttonWidth - 20;

        boolean importHovered = isPointInRect(mouseX, mouseY, importX, buttonY, buttonWidth, buttonHeight);
        boolean exportHovered = isPointInRect(mouseX, mouseY, exportX, buttonY, buttonWidth, buttonHeight);
        boolean cancelHovered = isPointInRect(mouseX, mouseY, cancelX, buttonY, buttonWidth, buttonHeight);

        drawPopupButton(context, importX, buttonY, buttonWidth, buttonHeight, importHovered, Text.literal("Import"), true);
        drawPopupButton(context, exportX, buttonY, buttonWidth, buttonHeight, exportHovered, Text.literal("Export"), false);
        drawPopupButton(context, cancelX, buttonY, buttonWidth, buttonHeight, cancelHovered, Text.literal("Close"), false);
    }

    private boolean handleClearPopupClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }

        int popupWidth = 280;
        int popupHeight = 150;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;
        int buttonWidth = 90;
        int buttonHeight = 20;
        int buttonY = popupY + popupHeight - buttonHeight - 16;
        int cancelX = popupX + 20;
        int confirmX = popupX + popupWidth - buttonWidth - 20;

        int mouseXi = (int) mouseX;
        int mouseYi = (int) mouseY;

        if (isPointInRect(mouseXi, mouseYi, confirmX, buttonY, buttonWidth, buttonHeight)) {
            confirmClearWorkspace();
            return true;
        }

        if (isPointInRect(mouseXi, mouseYi, cancelX, buttonY, buttonWidth, buttonHeight)) {
            clearPopupVisible = false;
            return true;
        }

        return true;
    }

    private boolean handleImportExportPopupClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }

        int popupWidth = 360;
        int popupHeight = 210;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;
        int fieldX = popupX + 20;
        int fieldY = popupY + 82;
        int fieldWidth = popupWidth - 40;
        int fieldHeight = 18;

        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = popupY + popupHeight - buttonHeight - 16;
        int importX = popupX + 20;
        int exportX = importX + buttonWidth + 8;
        int cancelX = popupX + popupWidth - buttonWidth - 20;

        int mouseXi = (int) mouseX;
        int mouseYi = (int) mouseY;

        if (isPointInRect(mouseXi, mouseYi, importX, buttonY, buttonWidth, buttonHeight)) {
            attemptImport();
            return true;
        }

        if (isPointInRect(mouseXi, mouseYi, exportX, buttonY, buttonWidth, buttonHeight)) {
            attemptExport();
            return true;
        }

        if (isPointInRect(mouseXi, mouseYi, cancelX, buttonY, buttonWidth, buttonHeight)) {
            closeImportExportPopup();
            return true;
        }

        if (isPointInRect(mouseXi, mouseYi, fieldX, fieldY, fieldWidth, fieldHeight)) {
            importExportFieldFocused = true;
            return true;
        }

        importExportFieldFocused = false;
        return true;
    }

    private void drawPopupButton(DrawContext context, int x, int y, int width, int height, boolean hovered, Text label, boolean primary) {
        int bgColor = primary ? (hovered ? 0xFF3B6A7D : 0xFF2F4F5C) : (hovered ? 0xFF505050 : 0xFF3A3A3A);
        int borderColor = hovered || primary ? ACCENT_COLOR : 0xFF666666;
        context.fill(x, y, x + width, y + height, bgColor);
        context.drawBorder(x, y, width, height, borderColor);
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            label,
            x + width / 2,
            y + (height - this.textRenderer.fontHeight) / 2 + 1,
            WHITE
        );
    }

    private void openClearPopup() {
        dismissParameterOverlay();
        importExportPopupVisible = false;
        importExportFieldFocused = false;
        clearPopupVisible = true;
    }

    private void confirmClearWorkspace() {
        nodeGraph.clearWorkspace();
        clearPopupVisible = false;
    }

    private void openImportExportPopup() {
        dismissParameterOverlay();
        clearPopupVisible = false;
        importExportPopupVisible = true;
        importExportFieldFocused = true;
        clearImportExportStatus();
        if (importExportPath == null || importExportPath.isEmpty()) {
            Path defaultPath = NodeGraphPersistence.getDefaultSavePath();
            importExportPath = defaultPath != null ? defaultPath.toString() : "";
        }
    }

    private void closeImportExportPopup() {
        importExportPopupVisible = false;
        importExportFieldFocused = false;
    }

    private void attemptImport() {
        if (importExportPath == null || importExportPath.trim().isEmpty()) {
            setImportExportStatus("Enter a file path to import.", ERROR_COLOR);
            return;
        }

        try {
            Path path = Paths.get(importExportPath.trim());
            boolean success = nodeGraph.importFromPath(path);
            if (success) {
                Path fileName = path.getFileName();
                setImportExportStatus("Imported workspace from " + (fileName != null ? fileName.toString() : path.toString()), SUCCESS_COLOR);
            } else {
                setImportExportStatus("Failed to import workspace from file.", ERROR_COLOR);
            }
        } catch (InvalidPathException ex) {
            setImportExportStatus("Invalid file path.", ERROR_COLOR);
        }
    }

    private void attemptExport() {
        if (importExportPath == null || importExportPath.trim().isEmpty()) {
            setImportExportStatus("Enter a file path to export.", ERROR_COLOR);
            return;
        }

        try {
            Path path = Paths.get(importExportPath.trim());
            boolean success = nodeGraph.exportToPath(path);
            if (success) {
                Path fileName = path.getFileName();
                setImportExportStatus("Exported workspace to " + (fileName != null ? fileName.toString() : path.toString()), SUCCESS_COLOR);
            } else {
                setImportExportStatus("Failed to export workspace.", ERROR_COLOR);
            }
        } catch (InvalidPathException ex) {
            setImportExportStatus("Invalid file path.", ERROR_COLOR);
        }
    }

    private void setImportExportStatus(String message, int color) {
        importExportStatus = message != null ? message : "";
        importExportStatusColor = color;
    }

    private void clearImportExportStatus() {
        importExportStatus = "";
        importExportStatusColor = 0xFFCCCCCC;
    }

    private void appendToImportExportPath(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String current = importExportPath != null ? importExportPath : "";
        String combined = current + text;
        if (combined.length() > 260) {
            combined = combined.substring(0, 260);
        }
        importExportPath = combined;
    }

    private void dismissParameterOverlay() {
        if (parameterOverlay != null && parameterOverlay.isVisible()) {
            parameterOverlay.close();
        }
        parameterOverlay = null;
    }

    private void renderBottomButtons(DrawContext context, int mouseX, int mouseY) {
        int buttonY = getBottomButtonY();
        renderImportExportButton(context, mouseX, mouseY, buttonY);
        renderClearButton(context, mouseX, mouseY, buttonY);
        renderHomeButton(context, mouseX, mouseY, buttonY);
    }

    private void renderHomeButton(DrawContext context, int mouseX, int mouseY, int buttonY) {
        int buttonX = getHomeButtonX();
        boolean hovered = renderButtonBackground(context, buttonX, buttonY, mouseX, mouseY, false);
        int iconColor = hovered ? ACCENT_COLOR : WHITE;
        int centerX = buttonX + BOTTOM_BUTTON_SIZE / 2;
        int centerY = buttonY + BOTTOM_BUTTON_SIZE / 2;

        context.drawHorizontalLine(centerX - 4, centerX + 2, centerY, iconColor);
        context.drawVerticalLine(centerX - 4, centerY - 4, centerY + 2, iconColor);
        context.drawHorizontalLine(centerX - 2, centerX, centerY - 2, iconColor);
        context.drawHorizontalLine(centerX - 3, centerX - 1, centerY - 1, iconColor);
        context.drawVerticalLine(centerX - 2, centerY - 2, centerY, iconColor);
        context.drawVerticalLine(centerX - 3, centerY - 3, centerY - 1, iconColor);
    }

    private void renderClearButton(DrawContext context, int mouseX, int mouseY, int buttonY) {
        int buttonX = getClearButtonX();
        boolean hovered = renderButtonBackground(context, buttonX, buttonY, mouseX, mouseY, clearPopupVisible);
        int iconColor = (hovered || clearPopupVisible) ? ACCENT_COLOR : WHITE;
        int centerX = buttonX + BOTTOM_BUTTON_SIZE / 2;
        int top = buttonY + 4;
        int bottom = buttonY + BOTTOM_BUTTON_SIZE - 4;

        context.drawHorizontalLine(centerX - 5, centerX + 4, top, iconColor);
        context.drawVerticalLine(centerX - 5, top, top + 2, iconColor);
        context.drawVerticalLine(centerX + 4, top, top + 2, iconColor);
        context.drawHorizontalLine(centerX - 4, centerX + 3, top + 2, iconColor);
        context.drawVerticalLine(centerX - 3, top + 2, bottom, iconColor);
        context.drawVerticalLine(centerX + 2, top + 2, bottom, iconColor);
        context.drawHorizontalLine(centerX - 3, centerX + 2, bottom, iconColor);
    }

    private void renderImportExportButton(DrawContext context, int mouseX, int mouseY, int buttonY) {
        int buttonX = getImportExportButtonX();
        boolean hovered = renderButtonBackground(context, buttonX, buttonY, mouseX, mouseY, importExportPopupVisible);
        int iconColor = (hovered || importExportPopupVisible) ? ACCENT_COLOR : WHITE;
        int centerX = buttonX + BOTTOM_BUTTON_SIZE / 2;
        int centerY = buttonY + BOTTOM_BUTTON_SIZE / 2;

        // Up arrow
        context.drawVerticalLine(centerX - 4, centerY - 5, centerY, iconColor);
        context.drawHorizontalLine(centerX - 6, centerX - 2, centerY - 5, iconColor);
        context.drawHorizontalLine(centerX - 5, centerX - 3, centerY - 4, iconColor);

        // Down arrow
        context.drawVerticalLine(centerX + 3, centerY, centerY + 5, iconColor);
        context.drawHorizontalLine(centerX + 1, centerX + 5, centerY + 5, iconColor);
        context.drawHorizontalLine(centerX + 2, centerX + 4, centerY + 4, iconColor);

        // Connector line
        context.drawHorizontalLine(centerX - 4, centerX + 3, centerY, iconColor);
    }

    private boolean renderButtonBackground(DrawContext context, int buttonX, int buttonY, int mouseX, int mouseY, boolean active) {
        boolean hovered = isPointInRect(mouseX, mouseY, buttonX, buttonY, BOTTOM_BUTTON_SIZE, BOTTOM_BUTTON_SIZE);
        boolean highlight = hovered || active;

        int bgColor = highlight ? 0xFF505050 : 0xFF3A3A3A;
        context.fill(buttonX + 1, buttonY + 1, buttonX + BOTTOM_BUTTON_SIZE - 1, buttonY + BOTTOM_BUTTON_SIZE - 1, bgColor);

        int borderColor = highlight ? ACCENT_COLOR : 0xFF666666;
        context.drawHorizontalLine(buttonX, buttonX + BOTTOM_BUTTON_SIZE - 1, buttonY, borderColor);
        context.drawHorizontalLine(buttonX, buttonX + BOTTOM_BUTTON_SIZE - 1, buttonY + BOTTOM_BUTTON_SIZE - 1, borderColor);
        context.drawVerticalLine(buttonX, buttonY, buttonY + BOTTOM_BUTTON_SIZE - 1, borderColor);
        context.drawVerticalLine(buttonX + BOTTOM_BUTTON_SIZE - 1, buttonY, buttonY + BOTTOM_BUTTON_SIZE - 1, borderColor);

        return hovered;
    }

    private int getBottomButtonY() {
        return this.height - BOTTOM_BUTTON_SIZE - BOTTOM_BUTTON_MARGIN;
    }

    private int getHomeButtonX() {
        return this.width - BOTTOM_BUTTON_SIZE - BOTTOM_BUTTON_MARGIN;
    }

    private int getClearButtonX() {
        return getHomeButtonX() - BOTTOM_BUTTON_SPACING - BOTTOM_BUTTON_SIZE;
    }

    private int getImportExportButtonX() {
        return getClearButtonX() - BOTTOM_BUTTON_SPACING - BOTTOM_BUTTON_SIZE;
    }

    private boolean isHomeButtonClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        int buttonX = getHomeButtonX();
        int buttonY = getBottomButtonY();
        return isPointInRect(mouseX, mouseY, buttonX, buttonY, BOTTOM_BUTTON_SIZE, BOTTOM_BUTTON_SIZE);
    }

    private boolean isClearButtonClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        int buttonX = getClearButtonX();
        int buttonY = getBottomButtonY();
        return isPointInRect(mouseX, mouseY, buttonX, buttonY, BOTTOM_BUTTON_SIZE, BOTTOM_BUTTON_SIZE);
    }

    private boolean isImportExportButtonClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        int buttonX = getImportExportButtonX();
        int buttonY = getBottomButtonY();
        return isPointInRect(mouseX, mouseY, buttonX, buttonY, BOTTOM_BUTTON_SIZE, BOTTOM_BUTTON_SIZE);
    }

    private boolean isPointInRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

}
