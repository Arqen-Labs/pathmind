package com.pathmind.ui;

import com.pathmind.nodes.Node;
import com.pathmind.nodes.NodeParameter;
import com.pathmind.nodes.NodeMode;
import com.pathmind.nodes.NodeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private static final int COORDINATE_FIELD_SPACING = 12;

    private final Node node;
    private final List<String> parameterValues;
    private final List<Boolean> fieldFocused;
    private final List<FieldBounds> fieldBounds;
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
    
    // Mode selection fields
    private NodeMode selectedMode;
    private final List<NodeMode> availableModes;
    private boolean modeDropdownOpen = false;
    private int modeDropdownHoverIndex = -1;

    public NodeParameterOverlay(Node node, int screenWidth, int screenHeight, int topBarHeight, Runnable onClose) {
        this.node = node;
        this.onClose = onClose;
        this.parameterValues = new ArrayList<>();
        this.fieldFocused = new ArrayList<>();
        this.fieldBounds = new ArrayList<>();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.topBarHeight = topBarHeight;
        
        // Initialize mode selection
        this.availableModes = new ArrayList<>();
        NodeMode[] modes = NodeMode.getModesForNodeType(node.getType());
        if (modes != null) {
            for (NodeMode mode : modes) {
                this.availableModes.add(mode);
            }
        }
        this.selectedMode = node.getMode();
        
        updatePopupDimensions();
    }

    public void init() {
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
            Text.literal("Edit Parameters: " + node.getType().getDisplayName()),
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
        List<NodeParameter> parameters = node.getParameters();
        prepareFieldBounds(parameters.size());
        boolean[] rendered = new boolean[parameters.size()];
        boolean isPlaceNode = node.getType() == NodeType.PLACE;
        int placeFieldSections = isPlaceNode ? countPlaceFieldSections(parameters) : parameters.size();
        boolean hasSectionsAfterMode = placeFieldSections > 0;

        if (hasModeSelection()) {
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("Mode:"),
                popupX + 20,
                sectionY + 4,
                0xFFE0E0E0
            );

            int modeButtonX = popupX + 20;
            int modeButtonY = sectionY + LABEL_TO_FIELD_OFFSET;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = FIELD_HEIGHT;

            boolean modeButtonHovered = mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                                      mouseY >= modeButtonY && mouseY <= modeButtonY + modeButtonHeight;

            int modeBgColor = modeButtonHovered ? adjustColorBrightness(0xFF1A1A1A, 1.1f) : 0xFF1A1A1A;
            int modeBorderColor = modeButtonHovered ? 0xFF87CEEB : 0xFF666666;

            context.fill(modeButtonX, modeButtonY, modeButtonX + modeButtonWidth, modeButtonY + modeButtonHeight, modeBgColor);
            context.drawBorder(modeButtonX, modeButtonY, modeButtonWidth, modeButtonHeight, modeBorderColor);

            String modeText = selectedMode != null ? selectedMode.getDisplayName() : "Select Mode";
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(modeText),
                modeButtonX + 4,
                modeButtonY + 6,
                0xFFFFFFFF
            );

            context.drawTextWithShadow(
                textRenderer,
                Text.literal("▼"),
                modeButtonX + modeButtonWidth - 16,
                modeButtonY + 6,
                0xFFE0E0E0
            );

            sectionY = modeButtonY + modeButtonHeight;
            if (hasSectionsAfterMode) {
                sectionY += SECTION_SPACING;
            }
        }

        int blockIndex = isPlaceNode ? findParameterIndex(parameters, "Block") : -1;
        int xIndex = isPlaceNode ? findParameterIndex(parameters, "X") : -1;
        int yIndex = isPlaceNode ? findParameterIndex(parameters, "Y") : -1;
        int zIndex = isPlaceNode ? findParameterIndex(parameters, "Z") : -1;

        if (isPlaceNode && xIndex >= 0 && yIndex >= 0 && zIndex >= 0) {
            sectionY = renderPlaceCoordinateRow(context, textRenderer, sectionY, xIndex, yIndex, zIndex);
            rendered[xIndex] = true;
            rendered[yIndex] = true;
            rendered[zIndex] = true;
        }

        for (int i = 0; i < parameters.size(); i++) {
            if (isPlaceNode && i == blockIndex) {
                rendered[i] = true;
                continue;
            }

            if (!rendered[i]) {
                sectionY = renderParameterField(context, textRenderer, parameters.get(i), i, sectionY);
                rendered[i] = true;
            }
        }

        context.disableScissor();

        renderButton(context, textRenderer, saveButton, mouseX, mouseY);
        renderButton(context, textRenderer, cancelButton, mouseX, mouseY);

        if (hasModeSelection() && modeDropdownOpen) {
            int modeButtonX = popupX + 20;
            int modeButtonY = popupY + CONTENT_START_OFFSET + LABEL_TO_FIELD_OFFSET - scrollOffset;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = FIELD_HEIGHT;

            int dropdownY = modeButtonY + modeButtonHeight;
            int dropdownHeight = availableModes.size() * DROPDOWN_OPTION_HEIGHT;

            modeDropdownHoverIndex = -1;
            if (mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                int hoverIndex = (mouseY - dropdownY) / DROPDOWN_OPTION_HEIGHT;
                if (hoverIndex >= 0 && hoverIndex < availableModes.size()) {
                    modeDropdownHoverIndex = hoverIndex;
                }
            }

            context.fill(modeButtonX, dropdownY, modeButtonX + modeButtonWidth, dropdownY + dropdownHeight, 0xFF1A1A1A);
            context.drawBorder(modeButtonX, dropdownY, modeButtonWidth, dropdownHeight, 0xFF666666);

            for (int i = 0; i < availableModes.size(); i++) {
                NodeMode mode = availableModes.get(i);
                int optionY = dropdownY + i * DROPDOWN_OPTION_HEIGHT;

                boolean isSelected = selectedMode == mode;
                boolean isHovered = i == modeDropdownHoverIndex;
                int optionColor = isSelected ? adjustColorBrightness(0xFF1A1A1A, 0.9f) : 0xFF1A1A1A;
                if (isHovered) {
                    optionColor = adjustColorBrightness(optionColor, 1.2f);
                }
                context.fill(modeButtonX, optionY, modeButtonX + modeButtonWidth, optionY + DROPDOWN_OPTION_HEIGHT, optionColor);

                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(mode.getDisplayName()),
                    modeButtonX + 4,
                    optionY + 6,
                    0xFFFFFFFF
                );
            }
        }

        renderScrollbar(context, contentTop, contentBottom);
    }

    private void prepareFieldBounds(int size) {
        fieldBounds.clear();
        for (int i = 0; i < size; i++) {
            fieldBounds.add(null);
        }
    }

    private void setFieldBounds(int index, int x, int y, int width, int height) {
        if (index < 0 || index >= fieldBounds.size()) {
            return;
        }
        fieldBounds.set(index, new FieldBounds(x, y, width, height));
    }

    private FieldBounds getFieldBounds(int index) {
        if (index < 0 || index >= fieldBounds.size()) {
            return null;
        }
        return fieldBounds.get(index);
    }

    private int getNextFocusableFieldIndex(int currentIndex) {
        if (fieldBounds.isEmpty()) {
            return -1;
        }

        int size = fieldBounds.size();
        int fallback = -1;
        if (currentIndex >= 0 && currentIndex < size && fieldBounds.get(currentIndex) != null) {
            fallback = currentIndex;
        }

        for (int offset = 1; offset <= size; offset++) {
            int nextIndex = currentIndex + offset;
            if (nextIndex < 0) {
                nextIndex = (nextIndex % size + size) % size;
            } else {
                nextIndex %= size;
            }

            FieldBounds bounds = fieldBounds.get(nextIndex);
            if (bounds != null) {
                return nextIndex;
            }
        }

        return fallback;
    }

    private int findParameterIndex(List<NodeParameter> parameters, String name) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private int countPlaceFieldSections(List<NodeParameter> parameters) {
        if (parameters.isEmpty()) {
            return 0;
        }

        int sections = 0;
        int xIndex = findParameterIndex(parameters, "X");
        int yIndex = findParameterIndex(parameters, "Y");
        int zIndex = findParameterIndex(parameters, "Z");

        boolean allAxesPresent = xIndex >= 0 && yIndex >= 0 && zIndex >= 0;
        if (allAxesPresent) {
            sections++;
        } else {
            if (xIndex >= 0) sections++;
            if (yIndex >= 0) sections++;
            if (zIndex >= 0) sections++;
        }

        for (NodeParameter parameter : parameters) {
            String name = parameter.getName();
            if (!"Block".equals(name) && !"X".equals(name) && !"Y".equals(name) && !"Z".equals(name)) {
                sections++;
            }
        }

        return sections;
    }

    private int renderParameterField(DrawContext context, TextRenderer textRenderer, NodeParameter param, int index, int sectionY) {
        if (param == null) {
            return sectionY;
        }

        int labelX = popupX + 20;
        int labelY = sectionY + 4;
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(param.getName() + " (" + param.getType().getDisplayName() + "):"),
            labelX,
            labelY,
            0xFFE0E0E0
        );

        int fieldX = labelX;
        int fieldY = sectionY + LABEL_TO_FIELD_OFFSET;
        int fieldWidth = popupWidth - 40;
        int fieldHeight = FIELD_HEIGHT;

        boolean isFocused = index == focusedFieldIndex;
        int bgColor = isFocused ? 0xFF2A2A2A : 0xFF1A1A1A;
        int borderColor = isFocused ? 0xFF87CEEB : 0xFF666666;

        context.fill(fieldX, fieldY, fieldX + fieldWidth, fieldY + fieldHeight, bgColor);
        context.drawBorder(fieldX, fieldY, fieldWidth, fieldHeight, borderColor);

        String text = index < parameterValues.size() ? parameterValues.get(index) : "";
        if (text != null && !text.isEmpty()) {
            String displayText = trimWithEllipsis(text, fieldWidth - 8, textRenderer);
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

        setFieldBounds(index, fieldX, fieldY, fieldWidth, fieldHeight);
        return fieldY + fieldHeight + SECTION_SPACING;
    }

    private int renderPlaceCoordinateRow(DrawContext context, TextRenderer textRenderer, int sectionY, int xIndex, int yIndex, int zIndex) {
        int baseX = popupX + 20;
        int labelY = sectionY + 4;
        int fieldY = sectionY + LABEL_TO_FIELD_OFFSET;
        int totalWidth = popupWidth - 40;
        int spacing = COORDINATE_FIELD_SPACING;
        int fieldWidth = (totalWidth - spacing * 2) / 3;
        fieldWidth = Math.max(60, fieldWidth);

        renderAxisField(context, textRenderer, "X", xIndex, baseX, labelY, fieldY, fieldWidth);
        renderAxisField(context, textRenderer, "Y", yIndex, baseX + fieldWidth + spacing, labelY, fieldY, fieldWidth);
        renderAxisField(context, textRenderer, "Z", zIndex, baseX + 2 * (fieldWidth + spacing), labelY, fieldY, fieldWidth);

        return fieldY + FIELD_HEIGHT + SECTION_SPACING;
    }

    private void renderAxisField(DrawContext context, TextRenderer textRenderer, String axis, int index, int fieldX, int labelY, int fieldY, int fieldWidth) {
        if (index < 0 || index >= parameterValues.size()) {
            return;
        }

        context.drawTextWithShadow(
            textRenderer,
            Text.literal(axis + ":"),
            fieldX,
            labelY,
            0xFFE0E0E0
        );

        boolean isFocused = index == focusedFieldIndex;
        int bgColor = isFocused ? 0xFF2A2A2A : 0xFF1A1A1A;
        int borderColor = isFocused ? 0xFF87CEEB : 0xFF666666;

        context.fill(fieldX, fieldY, fieldX + fieldWidth, fieldY + FIELD_HEIGHT, bgColor);
        context.drawBorder(fieldX, fieldY, fieldWidth, FIELD_HEIGHT, borderColor);

        String value = parameterValues.get(index);
        if (value != null && !value.isEmpty()) {
            String displayText = trimWithEllipsis(value, fieldWidth - 8, textRenderer);
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(displayText),
                fieldX + 4,
                fieldY + 6,
                0xFFFFFFFF
            );
        }

        if (isFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            String displayValue = value != null ? value : "";
            int cursorX = fieldX + 4 + textRenderer.getWidth(displayValue);
            cursorX = Math.min(cursorX, fieldX + fieldWidth - 2);
            context.fill(cursorX, fieldY + 4, cursorX + 1, fieldY + 16, 0xFFFFFFFF);
        }

        setFieldBounds(index, fieldX, fieldY, fieldWidth, FIELD_HEIGHT);
    }

    private String trimWithEllipsis(String text, int maxWidth, TextRenderer textRenderer) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return text;
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = textRenderer.getWidth("...");
        int available = Math.max(0, maxWidth - ellipsisWidth);
        String trimmed = textRenderer.trimToWidth(text, available);
        return trimmed + "...";
    }

    private static class FieldBounds {
        final int x;
        final int y;
        final int width;
        final int height;

        FieldBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
        }
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
        if (hasModeSelection()) {
            int modeButtonX = popupX + 20;
            int modeButtonY = labelY + LABEL_TO_FIELD_OFFSET;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = FIELD_HEIGHT;

            boolean modeVisible = modeButtonY <= contentBottom && modeButtonY + modeButtonHeight >= contentTop;

            if (modeDropdownOpen) {
                int dropdownY = modeButtonY + modeButtonHeight;
                int dropdownHeight = availableModes.size() * DROPDOWN_OPTION_HEIGHT;

                if (mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                    mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                    int optionIndex = (int) ((mouseY - dropdownY) / DROPDOWN_OPTION_HEIGHT);
                    if (optionIndex >= 0 && optionIndex < availableModes.size()) {
                        selectedMode = availableModes.get(optionIndex);
                        node.setMode(selectedMode);
                        resetParameterFields();
                        updatePopupDimensions();
                        recreateButtons();
                        updateButtonPositions();
                    }
                    modeDropdownOpen = false;
                    modeDropdownHoverIndex = -1;
                    return true;
                }
            }

            if (modeVisible && mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                mouseY >= Math.max(modeButtonY, contentTop) && mouseY <= Math.min(modeButtonY + modeButtonHeight, contentBottom)) {
                modeDropdownOpen = !modeDropdownOpen;
                modeDropdownHoverIndex = -1;
                return true;
            }

            labelY = modeButtonY + modeButtonHeight + SECTION_SPACING;
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
        for (int i = 0; i < fieldBounds.size(); i++) {
            FieldBounds bounds = fieldBounds.get(i);
            if (bounds == null) {
                continue;
            }

            if (mouseX >= bounds.x && mouseX <= bounds.x + bounds.width &&
                mouseY >= Math.max(bounds.y, contentTop) && mouseY <= Math.min(bounds.y + bounds.height, contentBottom)) {
                focusedFieldIndex = i;
                return true;
            }
        }

        // Close dropdown if clicking outside of it
        if (hasModeSelection() && modeDropdownOpen) {
            int modeButtonX = popupX + 20;
            int modeButtonY = popupY + CONTENT_START_OFFSET + LABEL_TO_FIELD_OFFSET - scrollOffset;
            int modeButtonWidth = popupWidth - 40;
            int modeButtonHeight = FIELD_HEIGHT;
            int dropdownY = modeButtonY + modeButtonHeight;
            int dropdownHeight = availableModes.size() * DROPDOWN_OPTION_HEIGHT;

            // Check if click is outside dropdown area
            if (!(mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                  mouseY >= modeButtonY && mouseY <= dropdownY + dropdownHeight)) {
                modeDropdownOpen = false; // Close dropdown
                modeDropdownHoverIndex = -1;
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

        if (keyCode == 258) { // Tab key
            int nextIndex = getNextFocusableFieldIndex(focusedFieldIndex);
            if (nextIndex != -1) {
                focusedFieldIndex = nextIndex;
            }
            return true;
        }

        // Handle text input for focused field
        if (focusedFieldIndex >= 0 && focusedFieldIndex < parameterValues.size()) {
            String currentText = parameterValues.get(focusedFieldIndex);

            // Handle backspace
            if (keyCode == 259 && currentText.length() > 0) { // Backspace key
                parameterValues.set(focusedFieldIndex, currentText.substring(0, currentText.length() - 1));
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
            int availableWidth = popupWidth - 44;
            FieldBounds bounds = getFieldBounds(focusedFieldIndex);
            if (bounds != null) {
                availableWidth = Math.max(1, bounds.width - 8);
            }
            int maxChars = Math.max(1, availableWidth / 6); // Calculate based on field width
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

        if (node.isParameterNode() && node.getParentParameterHost() != null) {
            Node host = node.getParentParameterHost();
            host.attachParameter(node);
        }

        node.recalculateDimensions();
        synchronizeAttachedPlaceParameter();

        close();
    }

    public void close() {
        visible = false;
        modeDropdownOpen = false;
        modeDropdownHoverIndex = -1;
        if (onClose != null) {
            onClose.run();
        }
    }

    public void show() {
        visible = true;
        focusedFieldIndex = -1;
        modeDropdownOpen = false;
        modeDropdownHoverIndex = -1;
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

    private void synchronizeAttachedPlaceParameter() {
        if (node.getType() != NodeType.PLACE || !node.hasAttachedParameter()) {
            return;
        }

        Node attached = node.getAttachedParameter();
        if (attached == null || attached.getType() != NodeType.PARAM_PLACE_TARGET) {
            return;
        }

        boolean updated = false;
        updated |= copyParameterValue(node, attached, "Block");
        updated |= copyParameterValue(node, attached, "X");
        updated |= copyParameterValue(node, attached, "Y");
        updated |= copyParameterValue(node, attached, "Z");

        if (updated) {
            attached.recalculateDimensions();
            node.updateAttachedParameterPosition();
        }
    }

    private boolean copyParameterValue(Node source, Node target, String parameterName) {
        NodeParameter sourceParam = source.getParameter(parameterName);
        NodeParameter targetParam = target.getParameter(parameterName);
        if (sourceParam == null || targetParam == null) {
            return false;
        }

        String sourceValue = sourceParam.getStringValue();
        if (sourceValue == null) {
            sourceValue = "";
        }

        if (Objects.equals(sourceValue, targetParam.getStringValue())) {
            return false;
        }

        targetParam.setStringValue(sourceValue);
        return true;
    }
    
    private void updatePopupDimensions() {
        int longestLineLength = ("Edit Parameters: " + node.getType().getDisplayName()).length();
        List<NodeParameter> parameters = node.getParameters();
        boolean isPlaceNode = node.getType() == NodeType.PLACE;
        int placeFieldSections = isPlaceNode ? countPlaceFieldSections(parameters) : parameters.size();

        if (hasModeSelection()) {
            longestLineLength = Math.max(longestLineLength, "Mode:".length());
            String modeText = selectedMode != null ? selectedMode.getDisplayName() : "Select Mode";
            longestLineLength = Math.max(longestLineLength, modeText.length());
        }

        for (NodeParameter param : parameters) {
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
        boolean hasFields = placeFieldSections > 0;

        if (hasModeSelection()) {
            contentHeight += LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT;
            if (hasFields) {
                contentHeight += SECTION_SPACING;
            }
        }

        int fieldSections = isPlaceNode ? placeFieldSections : parameters.size();
        if (fieldSections > 0) {
            contentHeight += fieldSections * (LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT);
            contentHeight += Math.max(0, fieldSections - 1) * SECTION_SPACING;
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
    
    private boolean hasModeSelection() {
        return !availableModes.isEmpty();
    }

    private int computeButtonY() {
        int offset = CONTENT_START_OFFSET;
        List<NodeParameter> parameters = node.getParameters();
        boolean isPlaceNode = node.getType() == NodeType.PLACE;
        int fieldSections = isPlaceNode ? countPlaceFieldSections(parameters) : parameters.size();
        boolean hasFields = fieldSections > 0;

        if (hasModeSelection()) {
            offset += LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT;
            if (hasFields) {
                offset += SECTION_SPACING;
            }
        }

        if (fieldSections > 0) {
            offset += fieldSections * (LABEL_TO_FIELD_OFFSET + FIELD_HEIGHT);
            offset += Math.max(0, fieldSections - 1) * SECTION_SPACING;
        }

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
