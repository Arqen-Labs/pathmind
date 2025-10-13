package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * Overlay widget for editing node parameters.
 * Appears on top of the existing GUI without replacing it.
 */
public class NodeParameterOverlay {
    private final Node node;
    private final List<String> parameterValues;
    private final List<Boolean> fieldFocused;
    private final int popupWidth = 300;
    private final int popupHeight;
    private final int popupX;
    private final int popupY;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private final Runnable onClose;
    private boolean visible = false;
    private int focusedFieldIndex = -1;
    
    // Mode selection fields
    private NodeMode selectedMode;
    private final List<NodeMode> availableModes;
    private int modeDropdownOpen = -1; // -1 = closed, 0+ = open with selected index

    public NodeParameterOverlay(Node node, int screenWidth, int screenHeight, Runnable onClose) {
        this.node = node;
        this.onClose = onClose;
        this.parameterValues = new ArrayList<>();
        this.fieldFocused = new ArrayList<>();
        
        // Initialize mode selection
        this.availableModes = new ArrayList<>();
        NodeMode[] modes = NodeMode.getModesForNodeType(node.getType());
        if (modes != null) {
            for (NodeMode mode : modes) {
                this.availableModes.add(mode);
            }
        }
        this.selectedMode = node.getMode();
        
        // Calculate popup dimensions and position
        int parameterCount = node.getParameters().size();
        int modeHeight = hasModeSelection() ? 30 : 0; // Extra height for mode selector
        this.popupHeight = Math.max(150, parameterCount * 45 + 100 + modeHeight); // 45px per parameter + header/footer space + mode selector
        this.popupX = (screenWidth - popupWidth) / 2;
        this.popupY = (screenHeight - popupHeight) / 2;
    }

    public void init() {
        // Clear existing values
        parameterValues.clear();
        fieldFocused.clear();
        
        // Initialize parameter values
        for (NodeParameter param : node.getParameters()) {
            parameterValues.add(param.getStringValue());
            fieldFocused.add(false);
        }
        
        // Create buttons
        int buttonY = popupY + popupHeight - 40;
        this.saveButton = ButtonWidget.builder(
            Text.literal("Save"),
            button -> saveParameters()
        ).dimensions(popupX + 20, buttonY, 80, 20).build();
        
        this.cancelButton = ButtonWidget.builder(
            Text.literal("Cancel"),
            button -> close()
        ).dimensions(popupX + popupWidth - 100, buttonY, 80, 20).build();
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        // Render semi-transparent background overlay
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x80000000);
        
        // Render popup background
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF2A2A2A);
        context.drawBorder(popupX, popupY, popupWidth, popupHeight, 0xFF666666); // Grey outline
        
        // Render title
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Edit Parameters: " + node.getType().getDisplayName()),
            popupX + 20,
            popupY + 15,
            0xFFFFFFFF
        );
        
        // Render mode selector if applicable
        int labelY = popupY + 45;
        if (hasModeSelection()) {
            // Mode selector label
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Mode:"),
                popupX + 20,
                labelY + 5,
                0xFFE0E0E0
            );
            
            // Mode dropdown button
            int modeButtonX = popupX + 20;
            int modeButtonY = labelY + 20;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = 20;
            
            // Check if mode dropdown is clicked
            boolean modeButtonHovered = mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                                      mouseY >= modeButtonY && mouseY <= modeButtonY + modeButtonHeight;
            
            int modeBgColor = modeButtonHovered ? 0xFF2A2A2A : 0xFF1A1A1A;
            int modeBorderColor = modeButtonHovered ? 0xFF87CEEB : 0xFF666666;
            
            context.fill(modeButtonX, modeButtonY, modeButtonX + modeButtonWidth, modeButtonY + modeButtonHeight, modeBgColor);
            context.drawBorder(modeButtonX, modeButtonY, modeButtonWidth, modeButtonHeight, modeBorderColor);
            
            // Render selected mode text
            String modeText = selectedMode != null ? selectedMode.getDisplayName() : "Select Mode";
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(modeText),
                modeButtonX + 4,
                modeButtonY + 6,
                0xFFFFFFFF
            );
            
            // Render dropdown arrow
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("▼"),
                modeButtonX + modeButtonWidth - 16,
                modeButtonY + 6,
                0xFFE0E0E0
            );
            
            // Note: Dropdown rendering moved to end of method for proper z-order
            
            labelY += 50; // Move down for mode selector
        }
        
        // Render parameter labels and fields
        for (int i = 0; i < node.getParameters().size(); i++) {
            NodeParameter param = node.getParameters().get(i);
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(param.getName() + " (" + param.getType().getDisplayName() + "):"),
                popupX + 20,
                labelY + 5, // Moved down from -5 to +5 for more space from previous field
                0xFFE0E0E0
            );
            
            // Render field background and border
            int fieldX = popupX + 20;
            int fieldY = labelY + 20; // Increased from +8 to +20 for more space between label and field
            int fieldWidth = popupWidth - 40;
            int fieldHeight = 20;
            
            boolean isFocused = i == focusedFieldIndex;
            int bgColor = isFocused ? 0xFF2A2A2A : 0xFF1A1A1A;
            int borderColor = isFocused ? 0xFF87CEEB : 0xFF666666;
            
            context.fill(fieldX, fieldY, fieldX + fieldWidth, fieldY + fieldHeight, bgColor);
            context.drawBorder(fieldX, fieldY, fieldWidth, fieldHeight, borderColor);
            
            // Render field text
            String text = parameterValues.get(i);
            if (text != null && !text.isEmpty()) {
                // Truncate text if it's too long to fit in the field
                String displayText = text;
                int maxChars = (fieldWidth - 8) / 6; // Approximate characters that fit
                if (text.length() > maxChars) {
                    displayText = text.substring(0, Math.max(0, maxChars - 3)) + "...";
                }
                
                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(displayText),
                    fieldX + 4,
                    fieldY + 6,
                    0xFFFFFFFF
                );
            }
            
            // Render cursor if focused
            if (isFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                String displayText = text != null ? text : "";
                int maxChars = (fieldWidth - 8) / 6;
                if (displayText.length() > maxChars) {
                    displayText = displayText.substring(0, Math.max(0, maxChars - 3)) + "...";
                }
                int cursorX = fieldX + 4 + (displayText.length() * 6);
                // Make sure cursor doesn't go outside the field
                if (cursorX < fieldX + fieldWidth - 2) {
                    context.fill(cursorX, fieldY + 4, cursorX + 1, fieldY + 16, 0xFFFFFFFF);
                }
            }
            
            labelY += 45;
        }
        
        // Render buttons
        renderButton(context, textRenderer, saveButton, mouseX, mouseY);
        renderButton(context, textRenderer, cancelButton, mouseX, mouseY);
        
        // Render mode dropdown on top of everything else (proper z-order)
        if (hasModeSelection() && modeDropdownOpen >= 0) {
            int dropdownLabelY = popupY + 45;
            int modeButtonX = popupX + 20;
            int modeButtonY = dropdownLabelY + 20;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = 20;
            
            int dropdownY = modeButtonY + modeButtonHeight;
            int dropdownHeight = availableModes.size() * 20;
            
            // Dropdown background with higher z-order
            context.fill(modeButtonX, dropdownY, modeButtonX + modeButtonWidth, dropdownY + dropdownHeight, 0xFF1A1A1A);
            context.drawBorder(modeButtonX, dropdownY, modeButtonWidth, dropdownHeight, 0xFF666666);
            
            // Render mode options
            for (int i = 0; i < availableModes.size(); i++) {
                NodeMode mode = availableModes.get(i);
                int optionY = dropdownY + i * 20;
                
                // Highlight selected option
                if (i == modeDropdownOpen) {
                    context.fill(modeButtonX, optionY, modeButtonX + modeButtonWidth, optionY + 20, 0xFF404040);
                }
                
                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(mode.getDisplayName()),
                    modeButtonX + 4,
                    optionY + 6,
                    0xFFFFFFFF
                );
            }
        }
    }

    private void renderButton(DrawContext context, TextRenderer textRenderer, ButtonWidget button, int mouseX, int mouseY) {
        boolean hovered = mouseX >= button.getX() && mouseX <= button.getX() + button.getWidth() &&
                         mouseY >= button.getY() && mouseY <= button.getY() + button.getHeight();
        
        int bgColor = hovered ? 0xFF505050 : 0xFF3A3A3A;
        context.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), bgColor);
        context.drawBorder(button.getX(), button.getY(), button.getWidth(), button.getHeight(), 0xFF666666);
        
        // Render button text
        context.drawCenteredTextWithShadow(
            textRenderer,
            button.getMessage(),
            button.getX() + button.getWidth() / 2,
            button.getY() + 6,
            0xFFFFFFFF
        );
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // Check button clicks
        if (saveButton.isMouseOver(mouseX, mouseY)) {
            saveButton.onPress();
            return true;
        }
        if (cancelButton.isMouseOver(mouseX, mouseY)) {
            cancelButton.onPress();
            return true;
        }
        
        // Check mode selector click
        int labelY = popupY + 45;
        if (hasModeSelection()) {
            int modeButtonX = popupX + 20;
            int modeButtonY = labelY + 20;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = 20;
            
            if (mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                mouseY >= modeButtonY && mouseY <= modeButtonY + modeButtonHeight) {
                // Toggle dropdown
                if (modeDropdownOpen >= 0) {
                    modeDropdownOpen = -1; // Close dropdown
                } else {
                    modeDropdownOpen = 0; // Open dropdown
                }
                return true;
            }
            
            // Check dropdown option clicks
            if (modeDropdownOpen >= 0) {
                int dropdownY = modeButtonY + modeButtonHeight;
                int dropdownHeight = availableModes.size() * 20;
                
                if (mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                    mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                    // Calculate which option was clicked
                    int optionIndex = (int) ((mouseY - dropdownY) / 20);
                    if (optionIndex >= 0 && optionIndex < availableModes.size()) {
                        selectedMode = availableModes.get(optionIndex);
                        // Update node mode and reinitialize parameters
                        node.setMode(selectedMode);
                        // Reinitialize parameter values
                        parameterValues.clear();
                        fieldFocused.clear();
                        for (NodeParameter param : node.getParameters()) {
                            parameterValues.add(param.getStringValue());
                            fieldFocused.add(false);
                        }
                    }
                    modeDropdownOpen = -1; // Close dropdown
                    return true;
                }
            }
            
            labelY += 50; // Move down for mode selector
        }
        
        // Check field clicks
        for (int i = 0; i < node.getParameters().size(); i++) {
            int fieldX = popupX + 20;
            int fieldY = labelY + 20; // Match the rendering position
            int fieldWidth = popupWidth - 40;
            int fieldHeight = 20;
            
            if (mouseX >= fieldX && mouseX <= fieldX + fieldWidth &&
                mouseY >= fieldY && mouseY <= fieldY + fieldHeight) {
                focusedFieldIndex = i;
                return true;
            }
            
            labelY += 45;
        }
        
        // Close dropdown if clicking outside of it
        if (hasModeSelection() && modeDropdownOpen >= 0) {
            int closeLabelY = popupY + 45;
            int modeButtonX = popupX + 20;
            int modeButtonY = closeLabelY + 20;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = 20;
            int dropdownY = modeButtonY + modeButtonHeight;
            int dropdownHeight = availableModes.size() * 20;
            
            // Check if click is outside dropdown area
            if (!(mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                  mouseY >= modeButtonY && mouseY <= dropdownY + dropdownHeight)) {
                modeDropdownOpen = -1; // Close dropdown
            }
        }
        
        // Close if clicking outside the popup
        if (mouseX < popupX || mouseX > popupX + popupWidth ||
            mouseY < popupY || mouseY > popupY + popupHeight) {
            close();
            return true;
        }
        
        // Always consume mouse events when popup is visible to prevent underlying UI interaction
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        
        // Handle text input for focused field
        if (focusedFieldIndex >= 0 && focusedFieldIndex < parameterValues.size()) {
            String currentText = parameterValues.get(focusedFieldIndex);
            
            // Handle backspace
            if (keyCode == 259 && currentText.length() > 0) { // Backspace key
                parameterValues.set(focusedFieldIndex, currentText.substring(0, currentText.length() - 1));
                return true;
            }
            
            // Handle tab to move to next field
            if (keyCode == 258) { // Tab key
                focusedFieldIndex = (focusedFieldIndex + 1) % node.getParameters().size();
                return true;
            }
        }
        
        // Close on Escape
        if (keyCode == 256) { // Escape key
            close();
            return true;
        }
        
        // Save on Enter
        if (keyCode == 257) { // Enter key
            saveParameters();
            return true;
        }
        
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible) return false;
        
        if (focusedFieldIndex >= 0 && focusedFieldIndex < parameterValues.size()) {
            String currentText = parameterValues.get(focusedFieldIndex);
            
            // Only allow printable characters and limit length to fit in the field
            int maxChars = (popupWidth - 44) / 6; // Calculate based on field width
            if (chr >= 32 && chr <= 126 && currentText.length() < maxChars) {
                parameterValues.set(focusedFieldIndex, currentText + chr);
                return true;
            }
        }
        
        return false;
    }

    private void saveParameters() {
        // Update node mode if applicable
        if (hasModeSelection() && selectedMode != null) {
            node.setMode(selectedMode);
        }
        
        // Update node parameters with field values
        List<NodeParameter> parameters = node.getParameters();
        for (int i = 0; i < parameters.size() && i < parameterValues.size(); i++) {
            NodeParameter param = parameters.get(i);
            String value = parameterValues.get(i);
            param.setStringValue(value);
        }
        
        close();
    }

    public void close() {
        visible = false;
        if (onClose != null) {
            onClose.run();
        }
    }

    public void show() {
        visible = true;
        focusedFieldIndex = -1;
    }

    public boolean isVisible() {
        return visible;
    }
    
    private boolean hasModeSelection() {
        return !availableModes.isEmpty();
    }
}
