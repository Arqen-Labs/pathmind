package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.ParameterProfile;
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
    private static final int CONTENT_START_OFFSET = 32;
    private static final int LABEL_TO_FIELD_OFFSET = 18;
    private static final int FIELD_HEIGHT = 20;
    private static final int SECTION_SPACING = 12;
    private static final int BUTTON_TOP_MARGIN = 8;
    private static final int BOTTOM_PADDING = 12;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MIN_POPUP_HEIGHT = 140;
    private static final int DROPDOWN_OPTION_HEIGHT = 20;
    private static final int MIN_POPUP_WIDTH = 300;
    private static final int APPROX_CHAR_WIDTH = 6;
    private static final int POPUP_VERTICAL_MARGIN = 40;
    private static final int SCROLL_STEP = 18;
    private static final int SCROLLBAR_WIDTH = 6;

    private final Node node;
    private final List<String> parameterValues;
    private final List<Boolean> fieldFocused;
    private int popupWidth = MIN_POPUP_WIDTH;
    private final int screenWidth;
    private final int screenHeight;
    private final int topBarHeight;
    private int popupHeight;
    private int popupX;
    private int popupY;
    private int totalContentHeight;
    private int maxScroll;
    private int scrollOffset;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private final Runnable onClose;
    private boolean visible = false;
    private int focusedFieldIndex = -1;
    
    // Profile selection fields
    private ParameterProfile selectedProfile;
    private final List<ParameterProfile> availableProfiles;
    private boolean profileDropdownOpen = false;
    private int profileDropdownHoverIndex = -1;

    public NodeParameterOverlay(Node node, int screenWidth, int screenHeight, int topBarHeight, Runnable onClose) {
        this.node = node;
        this.onClose = onClose;
        this.parameterValues = new ArrayList<>();
        this.fieldFocused = new ArrayList<>();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.topBarHeight = topBarHeight;
        
        // Initialize profile selection
        this.availableProfiles = new ArrayList<>();
        this.selectedProfile = node.getParameterProfile();
        
        updatePopupDimensions();
    }

    public void init() {
        if (selectedProfile == null) {
            selectedProfile = node.getParameterProfile();
            if (selectedProfile == null) {
                selectedProfile = ParameterProfile.POSITION_XYZ;
                node.setParameterProfile(selectedProfile);
            }
        }
        resetParameterFields();
        updatePopupDimensions();
        recreateButtons();
        scrollOffset = Math.min(scrollOffset, maxScroll);
        updateButtonPositions();
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
            Text.literal("Edit Parameters: " + node.getDisplayName().getString()),
            popupX + 20,
            popupY + 15,
            0xFFFFFFFF
        );

        updateButtonPositions();

        int contentTop = getScrollAreaTop();
        int contentBottom = getScrollAreaBottom();
        int contentRight = popupX + popupWidth;

        context.enableScissor(popupX + 1, contentTop, contentRight - 1, contentBottom);

        int sectionY = contentTop - scrollOffset;
        if (hasProfileSelection()) {
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Profile:"),
                popupX + 20,
                sectionY + 4,
                0xFFE0E0E0
            );

            int profileButtonX = popupX + 20;
            int profileButtonY = sectionY + LABEL_TO_FIELD_OFFSET;
            int profileButtonWidth = popupWidth - 40;
            int profileButtonHeight = FIELD_HEIGHT;

            boolean profileButtonHovered = mouseX >= profileButtonX && mouseX <= profileButtonX + profileButtonWidth &&
                                      mouseY >= profileButtonY && mouseY <= profileButtonY + profileButtonHeight;

            int profileBgColor = profileButtonHovered ? adjustColorBrightness(0xFF1A1A1A, 1.1f) : 0xFF1A1A1A;
            int profileBorderColor = profileButtonHovered ? 0xFF87CEEB : 0xFF666666;

            context.fill(profileButtonX, profileButtonY, profileButtonX + profileButtonWidth, profileButtonY + profileButtonHeight, profileBgColor);
            context.drawBorder(profileButtonX, profileButtonY, profileButtonWidth, profileButtonHeight, profileBorderColor);

            String profileText = selectedProfile != null ? selectedProfile.getDisplayName() : "Select Profile";
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(profileText),
                profileButtonX + 4,
                profileButtonY + 6,
                0xFFFFFFFF
            );

            context.drawTextWithShadow(
                textRenderer,
                Text.literal("▼"),
                profileButtonX + profileButtonWidth - 16,
                profileButtonY + 6,
                0xFFE0E0E0
            );

            sectionY = profileButtonY + profileButtonHeight + SECTION_SPACING;
        }

        for (int i = 0; i < node.getParameters().size(); i++) {
            NodeParameter param = node.getParameters().get(i);
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(param.getName() + " (" + param.getType().getDisplayName() + "):"),
                popupX + 20,
                sectionY + 4,
                0xFFE0E0E0
            );

            int fieldX = popupX + 20;
            int fieldY = sectionY + LABEL_TO_FIELD_OFFSET;
            int fieldWidth = popupWidth - 40;
            int fieldHeight = FIELD_HEIGHT;

            boolean isFocused = i == focusedFieldIndex;
            int bgColor = isFocused ? 0xFF2A2A2A : 0xFF1A1A1A;
            int borderColor = isFocused ? 0xFF87CEEB : 0xFF666666;

            context.fill(fieldX, fieldY, fieldX + fieldWidth, fieldY + fieldHeight, bgColor);
            context.drawBorder(fieldX, fieldY, fieldWidth, fieldHeight, borderColor);

            String text = parameterValues.get(i);
            if (text != null && !text.isEmpty()) {
                int availableWidth = fieldWidth - 8;
                String displayText = textRenderer.trimToWidth(text, availableWidth);

                if (!displayText.equals(text)) {
                    int ellipsisWidth = textRenderer.getWidth("...");
                    int trimmedWidth = Math.max(0, availableWidth - ellipsisWidth);
                    String trimmed = textRenderer.trimToWidth(text, trimmedWidth);
                    displayText = trimmed + "...";
                }

                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(displayText),
                    fieldX + 4,
                    fieldY + 6,
                    0xFFFFFFFF
                );
            }

            if (isFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                String value = text != null ? text : "";
                int cursorX = fieldX + 4 + textRenderer.getWidth(value);
                cursorX = Math.min(cursorX, fieldX + fieldWidth - 2);
                context.fill(cursorX, fieldY + 4, cursorX + 1, fieldY + 16, 0xFFFFFFFF);
            }

            sectionY = fieldY + fieldHeight + SECTION_SPACING;
        }

        context.disableScissor();

        renderButton(context, textRenderer, saveButton, mouseX, mouseY);
        renderButton(context, textRenderer, cancelButton, mouseX, mouseY);

        if (hasProfileSelection() && profileDropdownOpen) {
            int profileButtonX = popupX + 20;
            int profileButtonY = popupY + CONTENT_START_OFFSET + LABEL_TO_FIELD_OFFSET - scrollOffset;
            int profileButtonWidth = popupWidth - 40;
            int profileButtonHeight = FIELD_HEIGHT;

            int dropdownY = profileButtonY + profileButtonHeight;
            int dropdownHeight = availableProfiles.size() * DROPDOWN_OPTION_HEIGHT;

            profileDropdownHoverIndex = -1;
            if (mouseX >= profileButtonX && mouseX <= profileButtonX + profileButtonWidth &&
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                int hoverIndex = (mouseY - dropdownY) / DROPDOWN_OPTION_HEIGHT;
                if (hoverIndex >= 0 && hoverIndex < availableProfiles.size()) {
                    profileDropdownHoverIndex = hoverIndex;
                }
            }

            context.fill(profileButtonX, dropdownY, profileButtonX + profileButtonWidth, dropdownY + dropdownHeight, 0xFF1A1A1A);
            context.drawBorder(profileButtonX, dropdownY, profileButtonWidth, dropdownHeight, 0xFF666666);

            for (int i = 0; i < availableProfiles.size(); i++) {
                ParameterProfile profile = availableProfiles.get(i);
                int optionY = dropdownY + i * DROPDOWN_OPTION_HEIGHT;

                boolean isSelected = selectedProfile == profile;
                boolean isHovered = i == profileDropdownHoverIndex;
                int optionColor = isSelected ? adjustColorBrightness(0xFF1A1A1A, 0.9f) : 0xFF1A1A1A;
                if (isHovered) {
                    optionColor = adjustColorBrightness(optionColor, 1.2f);
                }
                context.fill(profileButtonX, optionY, profileButtonX + profileButtonWidth, optionY + DROPDOWN_OPTION_HEIGHT, optionColor);

                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(profile.getDisplayName()),
                    profileButtonX + 4,
                    optionY + 6,
                    0xFFFFFFFF
                );
            }
        }

        renderScrollbar(context, contentTop, contentBottom);
    }

    private void renderButton(DrawContext context, TextRenderer textRenderer, ButtonWidget button, int mouseX, int mouseY) {
        if (button == null) {
            return;
        }

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

    private void renderScrollbar(DrawContext context, int contentTop, int contentBottom) {
        if (maxScroll <= 0) {
            return;
        }

        int trackLeft = popupX + popupWidth - 12;
        int trackRight = trackLeft + SCROLLBAR_WIDTH;
        int trackTop = contentTop;
        int trackBottom = contentBottom;
        int trackHeight = Math.max(1, trackBottom - trackTop);

        context.fill(trackLeft, trackTop, trackRight, trackBottom, 0xFF1A1A1A);
        context.drawBorder(trackLeft, trackTop, SCROLLBAR_WIDTH, trackHeight, 0xFF444444);

        int visibleScrollableHeight = Math.max(1, contentBottom - contentTop);
        int totalScrollableHeight = Math.max(visibleScrollableHeight, visibleScrollableHeight + maxScroll);
        int knobHeight = Math.max(20, (int) ((float) visibleScrollableHeight / totalScrollableHeight * trackHeight));
        int maxKnobTravel = Math.max(0, trackHeight - knobHeight);
        int knobOffset = maxKnobTravel <= 0 ? 0 : (int) ((float) scrollOffset / (float) maxScroll * maxKnobTravel);
        int knobTop = trackTop + knobOffset;

        context.fill(trackLeft + 1, knobTop, trackRight - 1, knobTop + knobHeight, 0xFF777777);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        updateButtonPositions();

        // Prepare scrollable bounds for subsequent hit checks
        int contentTop = getScrollAreaTop();
        int contentBottom = getScrollAreaBottom();
        int labelY = contentTop - scrollOffset;
        if (hasProfileSelection()) {
            int profileButtonX = popupX + 20;
            int profileButtonY = labelY + LABEL_TO_FIELD_OFFSET;
            int profileButtonWidth = popupWidth - 40;
            int profileButtonHeight = FIELD_HEIGHT;

            boolean profileVisible = profileButtonY <= contentBottom && profileButtonY + profileButtonHeight >= contentTop;

            if (profileDropdownOpen) {
                int dropdownY = profileButtonY + profileButtonHeight;
                int dropdownHeight = availableProfiles.size() * DROPDOWN_OPTION_HEIGHT;

                if (mouseX >= profileButtonX && mouseX <= profileButtonX + profileButtonWidth &&
                    mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                    int optionIndex = (int) ((mouseY - dropdownY) / DROPDOWN_OPTION_HEIGHT);
                    if (optionIndex >= 0 && optionIndex < availableProfiles.size()) {
                        selectedProfile = availableProfiles.get(optionIndex);
                        node.setParameterProfile(selectedProfile);
                        resetParameterFields();
                        updatePopupDimensions();
                        recreateButtons();
                        updateButtonPositions();
                    }
                    profileDropdownOpen = false;
                    profileDropdownHoverIndex = -1;
                    return true;
                }
            }

            if (profileVisible && mouseX >= profileButtonX && mouseX <= profileButtonX + profileButtonWidth &&
                mouseY >= Math.max(profileButtonY, contentTop) && mouseY <= Math.min(profileButtonY + profileButtonHeight, contentBottom)) {
                profileDropdownOpen = !profileDropdownOpen;
                profileDropdownHoverIndex = -1;
                return true;
            }

            labelY = profileButtonY + profileButtonHeight + SECTION_SPACING;
        }

        // Check button clicks after handling dropdown interactions so dropdown selections aren't swallowed by buttons beneath
        if (saveButton != null && saveButton.isMouseOver(mouseX, mouseY)) {
            saveButton.onPress();
            return true;
        }
        if (cancelButton != null && cancelButton.isMouseOver(mouseX, mouseY)) {
            cancelButton.onPress();
            return true;
        }

        // Check field clicks
        for (int i = 0; i < node.getParameters().size(); i++) {
            int fieldX = popupX + 20;
            int fieldY = labelY + LABEL_TO_FIELD_OFFSET; // Match the rendering position
            int fieldWidth = popupWidth - 40;
            int fieldHeight = FIELD_HEIGHT;

            if (mouseX >= fieldX && mouseX <= fieldX + fieldWidth &&
                mouseY >= Math.max(fieldY, contentTop) && mouseY <= Math.min(fieldY + fieldHeight, contentBottom)) {
                focusedFieldIndex = i;
                return true;
            }

            labelY = fieldY + fieldHeight + SECTION_SPACING;
        }

        // Close dropdown if clicking outside of it
        if (hasProfileSelection() && profileDropdownOpen) {
            int profileButtonX = popupX + 20;
            int profileButtonY = popupY + CONTENT_START_OFFSET + LABEL_TO_FIELD_OFFSET - scrollOffset;
            int profileButtonWidth = popupWidth - 40;
            int profileButtonHeight = FIELD_HEIGHT;
            int dropdownY = profileButtonY + profileButtonHeight;
            int dropdownHeight = availableProfiles.size() * DROPDOWN_OPTION_HEIGHT;

            if (!(mouseX >= profileButtonX && mouseX <= profileButtonX + profileButtonWidth &&
                  mouseY >= profileButtonY && mouseY <= dropdownY + dropdownHeight)) {
                profileDropdownOpen = false;
                profileDropdownHoverIndex = -1;
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

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!visible) return false;

        if (mouseX < popupX || mouseX > popupX + popupWidth || mouseY < popupY || mouseY > popupY + popupHeight) {
            return true;
        }

        if (maxScroll <= 0) {
            return true;
        }

        double newOffset = scrollOffset - verticalAmount * SCROLL_STEP;
        int clampedOffset = (int) Math.round(newOffset);
        clampedOffset = Math.max(0, Math.min(maxScroll, clampedOffset));
        if (clampedOffset != scrollOffset) {
            scrollOffset = clampedOffset;
            updateButtonPositions();
        }

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
        if (hasProfileSelection() && selectedProfile != null) {
            node.setParameterProfile(selectedProfile);
        }

        // Update node parameters with field values
        List<NodeParameter> parameters = node.getParameters();
        for (int i = 0; i < parameters.size() && i < parameterValues.size(); i++) {
            NodeParameter param = parameters.get(i);
            String value = parameterValues.get(i);
            param.setStringValue(value);
        }

        node.recalculateDimensions();
        if (node.getParameterConsumer() != null) {
            node.getParameterConsumer().onAttachedParameterChanged();
        }

        close();
    }

    public void close() {
        visible = false;
        profileDropdownOpen = false;
        profileDropdownHoverIndex = -1;
        if (onClose != null) {
            onClose.run();
        }
    }

    public void show() {
        visible = true;
        focusedFieldIndex = -1;
        profileDropdownOpen = false;
        profileDropdownHoverIndex = -1;
        scrollOffset = 0;
        updateButtonPositions();
    }

    public boolean isVisible() {
        return visible;
    }
    
    private void resetParameterFields() {
        parameterValues.clear();
        fieldFocused.clear();
        
        for (NodeParameter param : node.getParameters()) {
            parameterValues.add(param.getStringValue());
            fieldFocused.add(false);
        }
    }
    
    private void updatePopupDimensions() {
        int longestLineLength = ("Edit Parameters: " + node.getDisplayName().getString()).length();

        if (hasProfileSelection()) {
            longestLineLength = Math.max(longestLineLength, "Profile:".length());
            String profileText = selectedProfile != null ? selectedProfile.getDisplayName() : "Select Profile";
            longestLineLength = Math.max(longestLineLength, profileText.length());
        }

        for (NodeParameter param : node.getParameters()) {
            String label = param.getName() + " (" + param.getType().getDisplayName() + "):";
            longestLineLength = Math.max(longestLineLength, label.length());
            String value = param.getStringValue();
            if (value != null) {
                longestLineLength = Math.max(longestLineLength, value.length());
            }
        }

        for (String value : parameterValues) {
            if (value != null) {
                longestLineLength = Math.max(longestLineLength, value.length());
            }
        }

        int computedWidth = longestLineLength * APPROX_CHAR_WIDTH + 64; // Padding for margins and borders
        int maxAllowedWidth = Math.max(MIN_POPUP_WIDTH, screenWidth - 40);
        this.popupWidth = Math.min(Math.max(MIN_POPUP_WIDTH, computedWidth), maxAllowedWidth);

        int contentHeight = CONTENT_START_OFFSET;
        if (hasProfileSelection()) {
            contentHeight += LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT;
            if (!node.getParameters().isEmpty()) {
                contentHeight += SECTION_SPACING;
            }
        }

        int paramCount = node.getParameters().size();
        for (int i = 0; i < paramCount; i++) {
            contentHeight += LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT;
            if (i < paramCount - 1) {
                contentHeight += SECTION_SPACING;
            }
        }

        contentHeight += BUTTON_TOP_MARGIN + BUTTON_HEIGHT + BOTTOM_PADDING;

        this.totalContentHeight = contentHeight;

        int maxPopupHeight = Math.max(MIN_POPUP_HEIGHT, screenHeight - topBarHeight - POPUP_VERTICAL_MARGIN);
        this.popupHeight = Math.min(Math.max(MIN_POPUP_HEIGHT, contentHeight), maxPopupHeight);
        this.popupX = Math.max(0, (screenWidth - popupWidth) / 2);
        int availableHeight = Math.max(0, screenHeight - topBarHeight);
        int centeredOffset = Math.max(0, (availableHeight - popupHeight) / 2);
        this.popupY = topBarHeight + centeredOffset;

        this.maxScroll = Math.max(0, totalContentHeight - popupHeight);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }
    
    private void recreateButtons() {
        this.saveButton = ButtonWidget.builder(
            Text.literal("Save"),
            b -> saveParameters()
        ).dimensions(popupX + 20, computeVisibleButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT).build();

        this.cancelButton = ButtonWidget.builder(
            Text.literal("Cancel"),
            b -> close()
        ).dimensions(popupX + popupWidth - (BUTTON_WIDTH + 20), computeVisibleButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT).build();

        updateButtonPositions();
    }
    
    private boolean hasProfileSelection() {
        return false;
    }

    private int computeButtonY() {
        int offset = CONTENT_START_OFFSET;
        if (hasProfileSelection()) {
            offset += LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT + SECTION_SPACING;
        }
        offset += node.getParameters().size() * (LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT + SECTION_SPACING);
        return popupY + offset + BUTTON_TOP_MARGIN;
    }

    private void updateButtonPositions() {
        int adjustedY = computeVisibleButtonY();

        if (saveButton != null) {
            saveButton.setX(popupX + 20);
            saveButton.setY(adjustedY);
        }

        if (cancelButton != null) {
            cancelButton.setX(popupX + popupWidth - (BUTTON_WIDTH + 20));
            cancelButton.setY(adjustedY);
        }
    }

    private int computeVisibleButtonY() {
        int base = computeButtonY();
        int bottomLimit = popupY + popupHeight - BOTTOM_PADDING - BUTTON_HEIGHT;
        int topLimit = popupY + CONTENT_START_OFFSET;
        int clamped = Math.min(base, bottomLimit);
        return Math.max(clamped, topLimit);
    }

    private int adjustColorBrightness(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, Math.max(0, Math.round(((color >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((color >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((color & 0xFF) * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int getScrollAreaTop() {
        return popupY + CONTENT_START_OFFSET;
    }

    private int getScrollAreaBottom() {
        int top = getScrollAreaTop();
        int baseBottom = popupY + popupHeight - BOTTOM_PADDING;
        int buttonTop = saveButton != null ? saveButton.getY() : computeVisibleButtonY();
        int limitedBottom = buttonTop - 4;
        int bottom = Math.min(baseBottom, limitedBottom);
        if (bottom <= top) {
            bottom = Math.max(top + 1, baseBottom);
        }
        return bottom;
    }
}
