package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeCategory;
import com.pathmind.nodes.NodeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the sidebar with categorized draggable nodes.
 */
public class Sidebar {
    private static final int SIDEBAR_WIDTH = 140;
    private static final int CATEGORY_HEIGHT = 20;
    private static final int NODE_HEIGHT = 18;
    private static final int PADDING = 4;
    
    // Colors
    private static final int DARK_GREY_ALT = 0xFF2A2A2A;
    private static final int LIGHT_BLUE = 0xFF87CEEB;
    private static final int WHITE_MUTED = 0xFFE0E0E0;
    private static final int GREY_LINE = 0xFF666666;
    private static final int HOVER_COLOR = 0xFF404040;
    
    private final Map<NodeCategory, Boolean> categoryExpanded;
    private final Map<NodeCategory, List<NodeType>> categoryNodes;
    private NodeType hoveredNodeType = null;
    private NodeCategory hoveredCategory = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    public Sidebar() {
        this.categoryExpanded = new HashMap<>();
        this.categoryNodes = new HashMap<>();
        
        // Initialize categories as expanded by default
        for (NodeCategory category : NodeCategory.values()) {
            categoryExpanded.put(category, true);
        }
        
        // Organize nodes by category
        initializeCategoryNodes();
        calculateMaxScroll();
    }
    
    private void initializeCategoryNodes() {
        for (NodeCategory category : NodeCategory.values()) {
            List<NodeType> nodes = new ArrayList<>();
            
            for (NodeType nodeType : NodeType.values()) {
                if (nodeType.getCategory() == category && nodeType.isDraggableFromSidebar()) {
                    nodes.add(nodeType);
                }
            }
            
            categoryNodes.put(category, nodes);
        }
    }
    
    private void calculateMaxScroll() {
        int totalHeight = 0;
        
        for (NodeCategory category : NodeCategory.values()) {
            totalHeight += CATEGORY_HEIGHT; // Category header
            
            if (categoryExpanded.get(category)) {
                List<NodeType> nodes = categoryNodes.get(category);
                totalHeight += nodes.size() * NODE_HEIGHT;
            }
        }
        
        // Add padding
        totalHeight += PADDING * 4;
        
        // Calculate max scroll (assuming sidebar height of 400 for now)
        int sidebarHeight = 400;
        maxScroll = Math.max(0, totalHeight - sidebarHeight + 50);
    }
    
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int sidebarStartY, int sidebarHeight) {
        // Sidebar background
        context.fill(0, sidebarStartY, SIDEBAR_WIDTH, sidebarStartY + sidebarHeight, DARK_GREY_ALT);
        context.drawVerticalLine(SIDEBAR_WIDTH, sidebarStartY, sidebarStartY + sidebarHeight, GREY_LINE);
        
        // Render categories and nodes
        int currentY = sidebarStartY + 10 - scrollOffset;
        
        for (NodeCategory category : NodeCategory.values()) {
            // Skip if category has no nodes
            if (categoryNodes.get(category).isEmpty()) {
                continue;
            }
            
            // Render category header
            boolean categoryHovered = mouseX >= 0 && mouseX <= SIDEBAR_WIDTH && 
                                    mouseY >= currentY && mouseY < currentY + CATEGORY_HEIGHT;
            
            if (categoryHovered) {
                hoveredCategory = category;
                context.fill(0, currentY, SIDEBAR_WIDTH, currentY + CATEGORY_HEIGHT, HOVER_COLOR);
            }
            
            // Category expand/collapse indicator
            String indicator = categoryExpanded.get(category) ? "▼" : "▶";
            context.drawTextWithShadow(textRenderer, indicator, 8, currentY + 4, WHITE_MUTED);
            
            // Category name
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(category.getDisplayName()),
                20,
                currentY + 4,
                category.getColor()
            );
            
            currentY += CATEGORY_HEIGHT;
            
            // Render nodes if category is expanded
            if (categoryExpanded.get(category)) {
                List<NodeType> nodes = categoryNodes.get(category);
                for (NodeType nodeType : nodes) {
                    if (currentY >= sidebarStartY + sidebarHeight) break; // Don't render beyond sidebar
                    
                    boolean nodeHovered = mouseX >= 0 && mouseX <= SIDEBAR_WIDTH && 
                                        mouseY >= currentY && mouseY < currentY + NODE_HEIGHT;
                    
                    if (nodeHovered) {
                        hoveredNodeType = nodeType;
                        context.fill(0, currentY, SIDEBAR_WIDTH, currentY + NODE_HEIGHT, HOVER_COLOR);
                    }
                    
                    // Node color indicator
                    context.fill(4, currentY + 2, 14, currentY + 16, nodeType.getColor());
                    context.drawBorder(4, currentY + 2, 10, 14, 0xFF000000);
                    
                    // Node name
                    context.drawTextWithShadow(
                        textRenderer,
                        Text.literal(nodeType.getDisplayName()),
                        18,
                        currentY + 4,
                        WHITE_MUTED
                    );
                    
                    currentY += NODE_HEIGHT;
                }
            }
        }
        
        // Reset hover states if mouse is not in sidebar
        if (mouseX < 0 || mouseX > SIDEBAR_WIDTH) {
            hoveredNodeType = null;
            hoveredCategory = null;
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < 0 || mouseX > SIDEBAR_WIDTH) {
            return false;
        }
        
        if (button == 0) { // Left click
            // Check category header clicks
            if (hoveredCategory != null) {
                categoryExpanded.put(hoveredCategory, !categoryExpanded.get(hoveredCategory));
                calculateMaxScroll();
                return true;
            }
            
            // Check node clicks for dragging
            if (hoveredNodeType != null) {
                return true; // Signal that dragging should start
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= 0 && mouseX <= SIDEBAR_WIDTH) {
            scrollOffset += (int)(-amount * 20); // Scroll speed
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return false;
    }
    
    public NodeType getHoveredNodeType() {
        return hoveredNodeType;
    }
    
    public boolean isHoveringNode() {
        return hoveredNodeType != null;
    }
    
    public Node createNodeFromSidebar(int x, int y) {
        if (hoveredNodeType != null) {
            return new Node(hoveredNodeType, x, y);
        }
        return null;
    }
    
    public int getWidth() {
        return SIDEBAR_WIDTH;
    }
}
