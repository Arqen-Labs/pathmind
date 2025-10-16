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
 * Features a nested sidebar design with colored tabs like sticky notes.
 */
public class Sidebar {
    // Outer sidebar dimensions
    private static final int OUTER_SIDEBAR_WIDTH = 220;
    private static final int INNER_SIDEBAR_WIDTH = 40;
    private static final int TAB_SIZE = 12; // Original square tab size
    private static final int TAB_SPACING = 4; // More spacing between tabs
    private static final int TOP_PADDING = 4; // A tiny bit more padding above top category
    private static final int TAB_COLUMNS = 2;
    private static final int TAB_COLUMN_MARGIN = 8;
    private static final int TAB_COLUMN_SPACING = 8;
    
    // Node display dimensions
    private static final int NODE_HEIGHT = 18;
    private static final int PADDING = 4;
    private static final int CATEGORY_HEADER_HEIGHT = 20;
    
    // Colors
    private static final int DARK_GREY_ALT = 0xFF2A2A2A;
    private static final int DARKER_GREY = 0xFF1F1F1F;
    private static final int WHITE_MUTED = 0xFFE0E0E0;
    private static final int GREY_LINE = 0xFF666666;
    private static final int HOVER_COLOR = 0xFF404040;
    
    private final Map<NodeCategory, List<NodeType>> categoryNodes;
    private final Map<NodeCategory, Boolean> categoryExpanded;
    private NodeType hoveredNodeType = null;
    private NodeCategory hoveredCategory = null;
    private NodeCategory selectedCategory = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int currentSidebarHeight = 400; // Store current sidebar height
    
    public Sidebar() {
        this.categoryExpanded = new HashMap<>();
        this.categoryNodes = new HashMap<>();
        
        // Initialize categories as expanded by default
        for (NodeCategory category : NodeCategory.values()) {
            categoryExpanded.put(category, true);
        }

        // Organize nodes by category
        initializeCategoryNodes();
        calculateMaxScroll(400); // Default height for initialization
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
    
    private void calculateMaxScroll(int sidebarHeight) {
        int totalHeight = 0;
        
        // Add space for category header and nodes (content starts at top)
        if (selectedCategory != null) {
            totalHeight += CATEGORY_HEADER_HEIGHT;
            
            // Add space for nodes in selected category
            List<NodeType> nodes = categoryNodes.get(selectedCategory);
            if (nodes != null) {
                totalHeight += nodes.size() * NODE_HEIGHT;
            }
        }
        
        // Add padding
        totalHeight += PADDING * 2;
        
        // Calculate max scroll with proper room for scrolling
        maxScroll = Math.max(0, totalHeight - sidebarHeight + 100); // Extra 100px for better scrolling
    }
    
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int sidebarStartY, int sidebarHeight) {
        // Store current sidebar height and update max scroll
        this.currentSidebarHeight = sidebarHeight;
        calculateMaxScroll(sidebarHeight);
        
        int totalWidth = getWidth();
        
        // Outer sidebar background
        int outerColor = totalWidth > INNER_SIDEBAR_WIDTH ? DARK_GREY_ALT : DARKER_GREY;
        context.fill(0, sidebarStartY, totalWidth, sidebarStartY + sidebarHeight, outerColor);
        context.drawVerticalLine(totalWidth, sidebarStartY, sidebarStartY + sidebarHeight, GREY_LINE);
        
        // Inner sidebar background (for tabs)
        context.fill(0, sidebarStartY, INNER_SIDEBAR_WIDTH, sidebarStartY + sidebarHeight, DARKER_GREY);
        context.drawVerticalLine(INNER_SIDEBAR_WIDTH, sidebarStartY, sidebarStartY + sidebarHeight, GREY_LINE);
        
        // Tabs stay static (don't scroll with content)
        int currentY = sidebarStartY + TOP_PADDING;
        
        // Render colored tabs
        hoveredCategory = null;
        NodeCategory[] categories = NodeCategory.values();
        int visibleTabIndex = 0; // Track visible tabs separately from array index
        for (int i = 0; i < categories.length; i++) {
            NodeCategory category = categories[i];
            
            // Skip if category has no nodes
            if (categoryNodes.get(category).isEmpty()) {
                continue;
            }
            
            int row = visibleTabIndex / TAB_COLUMNS;
            int column = visibleTabIndex % TAB_COLUMNS;
            int tabY = currentY + row * (TAB_SIZE + TAB_SPACING);
            int tabX = TAB_COLUMN_MARGIN + column * (TAB_SIZE + TAB_COLUMN_SPACING);
            visibleTabIndex++; // Increment only for visible tabs
            
            // Check if tab is hovered
            boolean tabHovered = mouseX >= tabX && mouseX <= tabX + TAB_SIZE && 
                               mouseY >= tabY && mouseY < tabY + TAB_SIZE;
            
            // Check if tab is selected
            boolean tabSelected = category == selectedCategory;
            
            // Tab background color
            int baseColor = category.getColor();
            int tabColor = baseColor;
            if (tabSelected) {
                // Darken the category color for selected state
                tabColor = darkenColor(baseColor, 0.75f);
            } else if (tabHovered) {
                tabColor = lightenColor(baseColor, 1.2f);
            }
            
            // Render square tab
            context.fill(tabX, tabY, tabX + TAB_SIZE, tabY + TAB_SIZE, tabColor);
            
            // Tab outline slightly darker than base color
            int outlineColor = darkenColor(baseColor, 0.8f);
            context.drawBorder(tabX, tabY, TAB_SIZE, TAB_SIZE, outlineColor);
            
            // Render centered icon with bigger appearance
            String icon = category.getIcon();
            int iconX = tabX + (TAB_SIZE - textRenderer.getWidth(icon)) / 2;
            int iconY = tabY + (TAB_SIZE - textRenderer.fontHeight) / 2 + 1;
            
            context.drawTextWithShadow(textRenderer, icon, iconX, iconY, 0xFFFFFFFF);
            
            // Update hover state
            if (tabHovered) {
                hoveredCategory = category;
            }
        }
        
        // Render category name and nodes for selected category
        if (selectedCategory != null) {
            // Start content area at the very top of the sidebar, right after the title bar
            int contentY = sidebarStartY + PADDING - scrollOffset;
            
            // Category header
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(selectedCategory.getDisplayName()),
                INNER_SIDEBAR_WIDTH + 8,
                contentY + 4,
                selectedCategory.getColor()
            );
            
            contentY += CATEGORY_HEADER_HEIGHT;
            
            // Render nodes in selected category
            List<NodeType> nodes = categoryNodes.get(selectedCategory);
            if (nodes != null) {
                for (NodeType nodeType : nodes) {
                    if (contentY >= sidebarStartY + sidebarHeight) break; // Don't render beyond sidebar
                    
                    boolean nodeHovered = mouseX >= INNER_SIDEBAR_WIDTH && mouseX <= totalWidth && 
                                        mouseY >= contentY && mouseY < contentY + NODE_HEIGHT;
                    
                    if (nodeHovered) {
                        hoveredNodeType = nodeType;
                        context.fill(INNER_SIDEBAR_WIDTH, contentY, totalWidth, contentY + NODE_HEIGHT, HOVER_COLOR);
                    }
                    
                    // Node color indicator (using category color) - proper square/rectangle
                    int indicatorSize = 12;
                    int indicatorX = INNER_SIDEBAR_WIDTH + 8; // Align with category title
                    int indicatorY = contentY + 3;
                    context.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, nodeType.getColor());
                    context.drawBorder(indicatorX, indicatorY, indicatorSize, indicatorSize, 0xFF000000);
                    
                    // Node name
                    context.drawTextWithShadow(
                        textRenderer,
                        Text.literal(nodeType.getDisplayName()),
                        indicatorX + indicatorSize + 4, // Position after the indicator with some spacing
                        contentY + 4,
                        WHITE_MUTED
                    );
                    
                    contentY += NODE_HEIGHT;
                }
            }
        }
        
        // Reset hover states if mouse is not in sidebar
        if (mouseX < 0 || mouseX > getWidth()) {
            hoveredNodeType = null;
            hoveredCategory = null;
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < 0 || mouseX > getWidth()) {
            return false;
        }
        
        if (button == 0) { // Left click
            // Check tab clicks
            if (mouseX >= 0 && mouseX <= INNER_SIDEBAR_WIDTH && hoveredCategory != null) {
                if (selectedCategory != null && hoveredCategory == selectedCategory) {
                    selectedCategory = null;
                } else {
                    selectedCategory = hoveredCategory;
                }
                // Clear any hovered node when switching or collapsing categories
                hoveredNodeType = null;
                // Reset scroll to top when changing categories
                scrollOffset = 0;
                calculateMaxScroll(currentSidebarHeight);
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
        if (mouseX >= 0 && mouseX <= getWidth()) {
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
        return selectedCategory != null ? OUTER_SIDEBAR_WIDTH : INNER_SIDEBAR_WIDTH;
    }
    
    /**
     * Darkens a color by the specified factor
     */
    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Lightens a color by the specified factor
     */
    private int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
