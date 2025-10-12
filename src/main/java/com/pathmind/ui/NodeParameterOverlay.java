package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeParameter;
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

    public NodeParameterOverlay(Node node, int screenWidth, int screenHeight, Runnable onClose) {
        this.node = node;
        this.onClose = onClose;
        this.parameterValues = new ArrayList<>();
        this.fieldFocused = new ArrayList<>();
        
        // Calculate popup dimensions and position
        int parameterCount = node.getParameters().size();
        this.popupHeight = Math.max(150, parameterCount * 45 + 100); // 45px per parameter + header/footer space for better spacing
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
        
        // Render parameter labels and fields
        int labelY = popupY + 45;
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
        
        // Check field clicks
        int labelY = popupY + 45;
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
        
        // Close if clicking outside the popup
        if (mouseX < popupX || mouseX > popupX + popupWidth ||
            mouseY < popupY || mouseY > popupY + popupHeight) {
            close();
            return true;
        }
        
        return false;
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
}
