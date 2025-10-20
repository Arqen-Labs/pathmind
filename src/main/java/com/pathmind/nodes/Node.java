package com.pathmind.nodes;

import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import baritone.api.process.IExploreProcess;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.IFarmProcess;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BlockOptionalMeta;
import com.pathmind.execution.PreciseCompletionTracker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.recipe.ServerRecipeManager;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.lang.reflect.Field;

/**
 * Represents a single node in the Pathmind visual editor.
 * Similar to Blender's shader nodes, each node has inputs, outputs, and parameters.
 */
public class Node {
    public static final int NO_OUTPUT = -1;
    private final String id;
    private final NodeType type;
    private NodeMode mode;
    private int x, y;
    private static final int MIN_WIDTH = 92;
    private static final int MIN_HEIGHT = 44;
    private static final int CHAR_PIXEL_WIDTH = 6;
    private static final int HEADER_HEIGHT = 18;
    private static final int PARAM_LINE_HEIGHT = 10;
    private static final int PARAM_PADDING_TOP = 2;
    private static final int PARAM_PADDING_BOTTOM = 4;
    private static final int MAX_PARAMETER_LABEL_LENGTH = 20;
    private static final int BODY_PADDING_NO_PARAMS = 10;
    private static final int START_END_SIZE = 36;
    private static final String ERROR_MESSAGE_PREFIX = "\u00A7f⚙ \u00A74[\u00A7cPathmind\u00A74] \u00A77";
    private static final long CRAFTING_ACTION_DELAY_MS = 75L;
    private static final int CRAFTING_OUTPUT_POLL_LIMIT = 5;
    private static final int SENSOR_SLOT_MARGIN_HORIZONTAL = 8;
    private static final int SENSOR_SLOT_INNER_PADDING = 4;
    private static final int SENSOR_SLOT_MIN_CONTENT_WIDTH = 60;
    private static final int SENSOR_SLOT_MIN_CONTENT_HEIGHT = 28;
    private static final int ACTION_SLOT_MARGIN_HORIZONTAL = 8;
    private static final int ACTION_SLOT_INNER_PADDING = 4;
    private static final int ACTION_SLOT_MIN_CONTENT_WIDTH = 80;
    private static final int ACTION_SLOT_MIN_CONTENT_HEIGHT = 32;
    private static final int PARAMETER_SLOT_MARGIN_HORIZONTAL = 8;
    private static final int PARAMETER_SLOT_INNER_PADDING = 4;
    private static final int PARAMETER_SLOT_MIN_CONTENT_WIDTH = 96;
    private static final int PARAMETER_SLOT_MIN_CONTENT_HEIGHT = 32;
    private static final int SLOT_AREA_PADDING_TOP = 0;
    private static final int SLOT_AREA_PADDING_BOTTOM = 6;
    private static final int SLOT_VERTICAL_SPACING = 6;
    private int width;
    private int height;
    private int nextOutputSocket = 0;
    private int repeatRemainingIterations = 0;
    private boolean repeatActive = false;
    private boolean lastSensorResult = false;
    private boolean selected = false;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private final List<NodeParameter> parameters;
    private Node attachedSensor;
    private Node parentControl;
    private Node attachedActionNode;
    private Node parentActionControl;
    private Node attachedParameter;
    private Node parameterConsumer;
    private ParameterProfile parameterProfile;
    private ParameterProfile activeParameterProfile;
    private boolean socketsHidden;

    public Node(NodeType type, int x, int y) {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.mode = NodeMode.getDefaultModeForNodeType(type);
        this.x = x;
        this.y = y;
        this.parameters = new ArrayList<>();
        this.attachedSensor = null;
        this.parentControl = null;
        this.attachedActionNode = null;
        this.parentActionControl = null;
        this.socketsHidden = false;
        this.attachedParameter = null;
        this.parameterConsumer = null;
        this.activeParameterProfile = null;
        if (type == NodeType.PARAMETER) {
            this.parameterProfile = ParameterProfile.POSITION_XYZ;
        } else {
            this.parameterProfile = null;
        }
        initializeParameters();
        recalculateDimensions();
        resetControlState();
    }

    private void sendNodeErrorMessage(net.minecraft.client.MinecraftClient client, String message) {
        if (client == null || message == null || message.isEmpty()) {
            return;
        }

        client.execute(() -> sendNodeErrorMessageOnClientThread(client, message));
    }

    private void sendNodeErrorMessageOnClientThread(net.minecraft.client.MinecraftClient client, String message) {
        if (client == null || client.player == null || message == null || message.isEmpty()) {
            return;
        }

        client.player.sendMessage(Text.literal(ERROR_MESSAGE_PREFIX + message), false);
    }

    /**
     * Gets the Baritone instance for the current player
     * @return IBaritone instance or null if not available
     */
    private IBaritone getBaritone() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            System.err.println("Failed to get Baritone instance: " + e.getMessage());
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public boolean isParameterNode() {
        return type == NodeType.PARAMETER;
    }

    public NodeMode getMode() {
        return mode;
    }
    
    public void setMode(NodeMode mode) {
        this.mode = mode;
        // Reinitialize parameters when mode changes
        parameters.clear();
        initializeParameters();
        recalculateDimensions();
        resetControlState();
    }

    public ParameterProfile getParameterProfile() {
        return parameterProfile;
    }

    public void setParameterProfile(ParameterProfile profile) {
        if (!isParameterNode()) {
            return;
        }
        ParameterProfile newProfile = profile != null ? profile : ParameterProfile.POSITION_XYZ;
        if (this.parameterProfile == newProfile && !parameters.isEmpty()) {
            return;
        }
        this.parameterProfile = newProfile;
        parameters.clear();
        parameters.addAll(newProfile.instantiateParameters());
        recalculateDimensions();
        notifyParameterConsumerOfChange();
    }

    private void notifyParameterConsumerOfChange() {
        if (parameterConsumer != null) {
            parameterConsumer.onAttachedParameterChanged();
        }
    }

    public void onAttachedParameterChanged() {
        if (attachedParameter == null) {
            return;
        }
        ParameterProfile profile = attachedParameter.getParameterProfile();
        if (profile == null || !profile.supports(this.type)) {
            detachParameter();
            return;
        }
        this.activeParameterProfile = profile;
        Optional<NodeMode> override = profile.resolveMode(this.type);
        if (override.isPresent()) {
            NodeMode desired = override.get();
            if (this.mode != desired) {
                setMode(desired);
            }
        } else if (NodeMode.getModesForNodeType(type).length == 0) {
            // Node doesn't rely on modes, ensure geometry updates
            recalculateDimensions();
        } else {
            this.mode = null;
            recalculateDimensions();
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        setPositionSilently(x, y);
        if (attachedSensor != null) {
            updateAttachedSensorPosition();
        }
        if (attachedActionNode != null) {
            updateAttachedActionPosition();
        }
        if (attachedParameter != null) {
            updateAttachedParameterPosition();
        }
    }

    private void setPositionSilently(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public int getDragOffsetX() {
        return dragOffsetX;
    }

    public void setDragOffsetX(int dragOffsetX) {
        this.dragOffsetX = dragOffsetX;
    }

    public int getDragOffsetY() {
        return dragOffsetY;
    }

    public void setDragOffsetY(int dragOffsetY) {
        this.dragOffsetY = dragOffsetY;
    }

    public boolean containsPoint(int pointX, int pointY) {
        return pointX >= x && pointX <= x + getWidth() && pointY >= y && pointY <= y + getHeight();
    }

    private String getHeaderTitle() {
        if (isParameterNode() && parameterProfile != null) {
            return parameterProfile.getDisplayName();
        }
        return type.getDisplayName();
    }

    public Text getDisplayName() {
        return Text.literal(getHeaderTitle());
    }

    public boolean isSensorNode() {
        switch (type) {
            case SENSOR_TOUCHING_BLOCK:
            case SENSOR_TOUCHING_ENTITY:
            case SENSOR_AT_COORDINATES:
            case SENSOR_BLOCK_AHEAD:
            case SENSOR_BLOCK_BELOW:
            case SENSOR_LIGHT_LEVEL_BELOW:
            case SENSOR_IS_DAYTIME:
            case SENSOR_IS_RAINING:
            case SENSOR_HEALTH_BELOW:
            case SENSOR_HUNGER_BELOW:
            case SENSOR_ENTITY_NEARBY:
            case SENSOR_ITEM_IN_INVENTORY:
            case SENSOR_IS_SWIMMING:
            case SENSOR_IS_IN_LAVA:
            case SENSOR_IS_UNDERWATER:
            case SENSOR_IS_FALLING:
                return true;
            default:
                return false;
        }
    }

    public boolean canAcceptSensor() {
        switch (type) {
            case CONTROL_IF:
            case CONTROL_IF_ELSE:
            case CONTROL_REPEAT_UNTIL:
                return true;
            default:
                return false;
        }
    }

    public boolean hasSensorSlot() {
        return canAcceptSensor();
    }

    public boolean canAcceptActionNode() {
        switch (type) {
            case CONTROL_REPEAT:
            case CONTROL_REPEAT_UNTIL:
            case CONTROL_FOREVER:
                return true;
            default:
                return false;
        }
    }

    public boolean hasActionSlot() {
        return canAcceptActionNode();
    }

    public boolean hasParameterSlot() {
        return !isParameterNode() && type != NodeType.START;
    }

    public boolean canAcceptParameter() {
        return hasParameterSlot();
    }

    public boolean hasAttachedParameter() {
        return attachedParameter != null;
    }

    public Node getAttachedParameter() {
        return attachedParameter;
    }

    public Node getParameterConsumer() {
        return parameterConsumer;
    }

    public String getAttachedParameterId() {
        return attachedParameter != null ? attachedParameter.getId() : null;
    }

    public String getParameterConsumerId() {
        return parameterConsumer != null ? parameterConsumer.getId() : null;
    }

    public boolean hasAttachedSensor() {
        return attachedSensor != null;
    }

    public Node getAttachedSensor() {
        return attachedSensor;
    }

    public boolean isAttachedToControl() {
        return parentControl != null;
    }

    public Node getParentControl() {
        return parentControl;
    }

    public String getAttachedSensorId() {
        return attachedSensor != null ? attachedSensor.getId() : null;
    }

    public String getParentControlId() {
        return parentControl != null ? parentControl.getId() : null;
    }

    public boolean hasAttachedActionNode() {
        return attachedActionNode != null;
    }

    public Node getAttachedActionNode() {
        return attachedActionNode;
    }

    public boolean isAttachedToActionControl() {
        return parentActionControl != null;
    }

    public Node getParentActionControl() {
        return parentActionControl;
    }

    public String getAttachedActionId() {
        return attachedActionNode != null ? attachedActionNode.getId() : null;
    }

    public String getParentActionControlId() {
        return parentActionControl != null ? parentActionControl.getId() : null;
    }

    public int getInputSocketCount() {
        if (type == NodeType.START || type == NodeType.EVENT_FUNCTION || isSensorNode() || isParameterNode()) {
            return 0;
        }
        return 1;
    }

    public int getOutputSocketCount() {
        if (isSensorNode() || isParameterNode()) {
            return 0;
        }
        if (type == NodeType.CONTROL_FOREVER) {
            return 0;
        }
        if (type == NodeType.CONTROL_IF_ELSE) {
            return 2;
        }
        return 1;
    }

    public int getOutputSocketColor(int socketIndex) {
        if (type == NodeType.CONTROL_IF_ELSE) {
            if (socketIndex == 0) {
                return 0xFF4CAF50; // Green for true branch
            } else if (socketIndex == 1) {
                return 0xFFF44336; // Red for false branch
            }
        }
        return getType().getColor();
    }

    public int getSocketY(int socketIndex, boolean isInput) {
        int socketHeight = 12;
        if (type == NodeType.START || type == NodeType.EVENT_FUNCTION) {
            // For START and END nodes, center the socket vertically
            return y + getHeight() / 2;
        } else {
            int headerHeight = 14;
            int contentStartY = y + headerHeight + 6; // Start sockets below header with some padding
            return contentStartY + socketIndex * socketHeight;
        }
    }
    
    public int getSocketX(boolean isInput) {
        return isInput ? x - 4 : x + getWidth() + 4;
    }
    
    public void setNextOutputSocket(int socketIndex) {
        this.nextOutputSocket = socketIndex < 0 ? NO_OUTPUT : Math.max(0, socketIndex);
    }

    public int consumeNextOutputSocket() {
        int value = this.nextOutputSocket;
        this.nextOutputSocket = 0;
        return value;
    }
    
    public boolean isSocketClicked(int mouseX, int mouseY, int socketIndex, boolean isInput) {
        if (socketsHidden) {
            return false;
        }
        int socketX = getSocketX(isInput);
        int socketY = getSocketY(socketIndex, isInput);
        int socketRadius = 6; // Smaller size for more space

        return Math.abs(mouseX - socketX) <= socketRadius && Math.abs(mouseY - socketY) <= socketRadius;
    }

    public int getSensorSlotLeft() {
        return x + SENSOR_SLOT_MARGIN_HORIZONTAL;
    }

    private int getSlotAreaStartY() {
        int top = y + HEADER_HEIGHT;
        boolean hasSlots = hasSensorSlot() || hasActionSlot();
        if (hasParameters()) {
            top += PARAM_PADDING_TOP + parameters.size() * PARAM_LINE_HEIGHT + PARAM_PADDING_BOTTOM;
            if (hasSlots) {
                top += SLOT_AREA_PADDING_TOP;
            }
        } else if (hasSlots) {
            top += SLOT_AREA_PADDING_TOP;
        } else {
            top += BODY_PADDING_NO_PARAMS;
        }
        return top;
    }

    public int getSensorSlotTop() {
        return getSlotAreaStartY();
    }

    public int getSensorSlotWidth() {
        int minWidth = SENSOR_SLOT_MIN_CONTENT_WIDTH + 2 * SENSOR_SLOT_INNER_PADDING;
        int widthWithMargins = this.width - 2 * SENSOR_SLOT_MARGIN_HORIZONTAL;
        return Math.max(minWidth, widthWithMargins);
    }

    public int getSensorSlotHeight() {
        int sensorContentHeight = attachedSensor != null ? attachedSensor.getHeight() : SENSOR_SLOT_MIN_CONTENT_HEIGHT;
        return sensorContentHeight + 2 * SENSOR_SLOT_INNER_PADDING;
    }

    public boolean isPointInsideSensorSlot(int pointX, int pointY) {
        if (!hasSensorSlot()) {
            return false;
        }
        int slotLeft = getSensorSlotLeft();
        int slotTop = getSensorSlotTop();
        int slotWidth = getSensorSlotWidth();
        int slotHeight = getSensorSlotHeight();
        return pointX >= slotLeft && pointX <= slotLeft + slotWidth &&
               pointY >= slotTop && pointY <= slotTop + slotHeight;
    }

    public int getParameterSlotLeft() {
        return x + PARAMETER_SLOT_MARGIN_HORIZONTAL;
    }

    public int getParameterSlotTop() {
        return y + HEADER_HEIGHT;
    }

    public int getParameterSlotWidth() {
        int minWidth = PARAMETER_SLOT_MIN_CONTENT_WIDTH + 2 * PARAMETER_SLOT_INNER_PADDING;
        int widthWithMargins = this.width - 2 * PARAMETER_SLOT_MARGIN_HORIZONTAL;
        return Math.max(minWidth, widthWithMargins);
    }

    public int getParameterSlotHeight() {
        int parameterContentHeight = attachedParameter != null
            ? Math.max(PARAMETER_SLOT_MIN_CONTENT_HEIGHT, attachedParameter.getHeight())
            : PARAMETER_SLOT_MIN_CONTENT_HEIGHT;
        return parameterContentHeight + 2 * PARAMETER_SLOT_INNER_PADDING;
    }

    public boolean isPointInsideParameterSlot(int pointX, int pointY) {
        if (!hasParameterSlot()) {
            return false;
        }
        int slotLeft = getParameterSlotLeft();
        int slotTop = getParameterSlotTop();
        int slotWidth = getParameterSlotWidth();
        int slotHeight = getParameterSlotHeight();
        return pointX >= slotLeft && pointX <= slotLeft + slotWidth &&
               pointY >= slotTop && pointY <= slotTop + slotHeight;
    }

    public int getActionSlotLeft() {
        return x + ACTION_SLOT_MARGIN_HORIZONTAL;
    }

    public int getActionSlotTop() {
        int top = getSlotAreaStartY();
        if (hasSensorSlot()) {
            top += getSensorSlotHeight();
            if (hasActionSlot()) {
                top += SLOT_VERTICAL_SPACING;
            }
        }
        return top;
    }

    public int getActionSlotWidth() {
        int minWidth = ACTION_SLOT_MIN_CONTENT_WIDTH + 2 * ACTION_SLOT_INNER_PADDING;
        int widthWithMargins = this.width - 2 * ACTION_SLOT_MARGIN_HORIZONTAL;
        return Math.max(minWidth, widthWithMargins);
    }

    public int getActionSlotHeight() {
        int contentHeight = attachedActionNode != null ? attachedActionNode.getHeight() : ACTION_SLOT_MIN_CONTENT_HEIGHT;
        return contentHeight + 2 * ACTION_SLOT_INNER_PADDING;
    }

    public boolean isPointInsideActionSlot(int pointX, int pointY) {
        if (!hasActionSlot()) {
            return false;
        }
        int slotLeft = getActionSlotLeft();
        int slotTop = getActionSlotTop();
        int slotWidth = getActionSlotWidth();
        int slotHeight = getActionSlotHeight();
        return pointX >= slotLeft && pointX <= slotLeft + slotWidth &&
               pointY >= slotTop && pointY <= slotTop + slotHeight;
    }

    public void updateAttachedSensorPosition() {
        if (attachedSensor == null) {
            return;
        }
        int slotX = getSensorSlotLeft() + SENSOR_SLOT_INNER_PADDING;
        int slotY = getSensorSlotTop() + SENSOR_SLOT_INNER_PADDING;
        int availableWidth = getSensorSlotWidth() - 2 * SENSOR_SLOT_INNER_PADDING;
        int availableHeight = getSensorSlotHeight() - 2 * SENSOR_SLOT_INNER_PADDING;
        int sensorX = slotX + Math.max(0, (availableWidth - attachedSensor.getWidth()) / 2);
        int sensorY = slotY + Math.max(0, (availableHeight - attachedSensor.getHeight()) / 2);
        attachedSensor.setPositionSilently(sensorX, sensorY);
    }

    public void updateAttachedParameterPosition() {
        if (attachedParameter == null) {
            return;
        }
        int slotX = getParameterSlotLeft() + PARAMETER_SLOT_INNER_PADDING;
        int slotY = getParameterSlotTop() + PARAMETER_SLOT_INNER_PADDING;
        int availableWidth = getParameterSlotWidth() - 2 * PARAMETER_SLOT_INNER_PADDING;
        int availableHeight = getParameterSlotHeight() - 2 * PARAMETER_SLOT_INNER_PADDING;
        int paramX = slotX + Math.max(0, (availableWidth - attachedParameter.getWidth()) / 2);
        int paramY = slotY + Math.max(0, (availableHeight - attachedParameter.getHeight()) / 2);
        attachedParameter.setPositionSilently(paramX, paramY);
    }

    public void updateAttachedActionPosition() {
        if (attachedActionNode == null) {
            return;
        }
        int slotX = getActionSlotLeft() + ACTION_SLOT_INNER_PADDING;
        int slotY = getActionSlotTop() + ACTION_SLOT_INNER_PADDING;
        int availableWidth = getActionSlotWidth() - 2 * ACTION_SLOT_INNER_PADDING;
        int availableHeight = getActionSlotHeight() - 2 * ACTION_SLOT_INNER_PADDING;
        int nodeX = slotX + Math.max(0, (availableWidth - attachedActionNode.getWidth()) / 2);
        int nodeY = slotY + Math.max(0, (availableHeight - attachedActionNode.getHeight()) / 2);
        attachedActionNode.setPositionSilently(nodeX, nodeY);
    }

    public boolean attachSensor(Node sensor) {
        if (!canAcceptSensor() || sensor == null || !sensor.isSensorNode() || sensor == this) {
            return false;
        }

        if (sensor.parentControl == this && attachedSensor == sensor) {
            updateAttachedSensorPosition();
            return true;
        }

        if (sensor.parentControl != null) {
            sensor.parentControl.detachSensor();
        }

        if (attachedSensor != null && attachedSensor != sensor) {
            Node previousSensor = attachedSensor;
            previousSensor.parentControl = null;
            previousSensor.setDragging(false);
            previousSensor.setSelected(false);
            previousSensor.setPositionSilently(this.x + this.width + SENSOR_SLOT_MARGIN_HORIZONTAL, this.y);
        }

        attachedSensor = sensor;
        sensor.parentControl = this;
        sensor.setDragging(false);
        sensor.setSelected(false);

        recalculateDimensions();
        updateAttachedSensorPosition();
        return true;
    }

    public void detachSensor() {
        if (attachedSensor != null) {
            Node sensor = attachedSensor;
            sensor.parentControl = null;
            attachedSensor = null;
            recalculateDimensions();
        }
    }

    public boolean attachParameter(Node parameter) {
        if (!canAcceptParameter() || parameter == null || !parameter.isParameterNode() || parameter == this) {
            return false;
        }

        if (parameter.parameterConsumer == this && attachedParameter == parameter) {
            updateAttachedParameterPosition();
            onAttachedParameterChanged();
            return true;
        }

        if (parameter.parameterConsumer != null) {
            parameter.parameterConsumer.detachParameter();
        }

        if (attachedParameter != null && attachedParameter != parameter) {
            Node previous = attachedParameter;
            previous.parameterConsumer = null;
            previous.setDragging(false);
            previous.setSelected(false);
            previous.setSocketsHidden(false);
            previous.setPositionSilently(this.x + this.width + PARAMETER_SLOT_MARGIN_HORIZONTAL, this.y);
        }

        attachedParameter = parameter;
        parameter.parameterConsumer = this;
        parameter.setDragging(false);
        parameter.setSelected(false);
        parameter.setSocketsHidden(true);

        onAttachedParameterChanged();
        updateAttachedParameterPosition();
        recalculateDimensions();
        return true;
    }

    public void detachParameter() {
        if (attachedParameter != null) {
            Node parameter = attachedParameter;
            attachedParameter = null;
            activeParameterProfile = null;
            parameter.parameterConsumer = null;
            parameter.setSocketsHidden(false);
            recalculateDimensions();
        }
    }

    public boolean canAcceptActionNode(Node node) {
        if (!canAcceptActionNode() || node == null || node == this || node.isSensorNode()) {
            return false;
        }
        if (node.getType() == NodeType.EVENT_FUNCTION) {
            return false;
        }
        if (attachedActionNode != null && attachedActionNode != node) {
            return false;
        }
        return true;
    }

    public boolean attachActionNode(Node node) {
        if (!canAcceptActionNode(node)) {
            return false;
        }

        if (node.parentActionControl == this && attachedActionNode == node) {
            updateAttachedActionPosition();
            return true;
        }

        if (node.parentActionControl != null) {
            node.parentActionControl.detachActionNode();
        }

        if (attachedActionNode != null && attachedActionNode != node) {
            Node previous = attachedActionNode;
            previous.parentActionControl = null;
            previous.setDragging(false);
            previous.setSelected(false);
            previous.setPositionSilently(this.x + this.width + ACTION_SLOT_MARGIN_HORIZONTAL, this.y);
        }

        attachedActionNode = node;
        node.parentActionControl = this;
        node.setDragging(false);
        node.setSelected(false);
        node.setSocketsHidden(true);

        recalculateDimensions();
        updateAttachedActionPosition();
        return true;
    }

    public void detachActionNode() {
        if (attachedActionNode != null) {
            Node node = attachedActionNode;
            node.parentActionControl = null;
            node.setSocketsHidden(false);
            attachedActionNode = null;
            recalculateDimensions();
        }
    }

    public void setSocketsHidden(boolean hidden) {
        this.socketsHidden = hidden;
    }

    public boolean shouldRenderSockets() {
        return !socketsHidden;
    }

    /**
     * Initialize default parameters for each node type and mode
     */
    private void initializeParameters() {
        parameters.clear();
        if (isParameterNode()) {
            if (parameterProfile == null) {
                parameterProfile = ParameterProfile.POSITION_XYZ;
            }
            parameters.addAll(parameterProfile.instantiateParameters());
        }
    }

    /**
     * Get all parameters for this node
     */
    public List<NodeParameter> getParameters() {
        if (isParameterNode()) {
            return parameters;
        }
        if (attachedParameter != null) {
            return attachedParameter.getParameters();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Get a specific parameter by name
     */
    public NodeParameter getParameter(String name) {
        if (name == null) {
            return null;
        }
        if (isParameterNode()) {
            for (NodeParameter param : parameters) {
                if (param.getName().equals(name)) {
                    return param;
                }
            }
            return null;
        }
        if (attachedParameter != null) {
            return attachedParameter.getParameter(name);
        }
        return null;
    }

    public String getParameterLabel(NodeParameter parameter) {
        if (parameter == null) {
            return "";
        }
        String text = parameter.getName() + ": " + parameter.getDisplayValue();
        if (text.length() <= MAX_PARAMETER_LABEL_LENGTH) {
            return text;
        }
        int maxContentLength = Math.max(0, MAX_PARAMETER_LABEL_LENGTH - 3);
        return text.substring(0, maxContentLength) + "...";
    }

    /**
     * Check if this node has parameters (Start nodes don't)
     */
    public boolean hasParameters() {
        if (isParameterNode()) {
            return !parameters.isEmpty();
        }
        return attachedParameter != null;
    }

    public boolean supportsModeSelection() {
        NodeMode[] modes = NodeMode.getModesForNodeType(type);
        return modes != null && modes.length > 0;
    }

    /**
     * Recalculate node dimensions based on current content
     */
    public void recalculateDimensions() {
        if (type == NodeType.START) {
            this.width = START_END_SIZE;
            this.height = START_END_SIZE;
            return;
        }

        if (isParameterNode()) {
            int maxTextLength = Math.max(getHeaderTitle().length(), 1);
            for (NodeParameter param : parameters) {
                String paramText = getParameterLabel(param);
                if (paramText.length() > maxTextLength) {
                    maxTextLength = paramText.length();
                }
            }
            if (supportsModeSelection()) {
                String modeLabel = getModeDisplayLabel();
                if (!modeLabel.isEmpty()) {
                    maxTextLength = Math.max(maxTextLength, modeLabel.length());
                }
            }
            int computedWidth = Math.max(MIN_WIDTH, maxTextLength * CHAR_PIXEL_WIDTH + 24);
            this.width = computedWidth;

            int contentHeight = HEADER_HEIGHT;
            if (hasParameters()) {
                contentHeight += getParameterDisplayHeight();
            } else {
                contentHeight += BODY_PADDING_NO_PARAMS;
            }
            this.height = Math.max(MIN_HEIGHT, contentHeight);
            if (parameterConsumer != null) {
                parameterConsumer.updateAttachedParameterPosition();
            }
            return;
        }

        int computedWidth = Math.max(MIN_WIDTH, getHeaderTitle().length() * CHAR_PIXEL_WIDTH + 24);
        if (hasParameterSlot()) {
            int parameterContentWidth = PARAMETER_SLOT_MIN_CONTENT_WIDTH;
            if (attachedParameter != null) {
                parameterContentWidth = Math.max(parameterContentWidth, attachedParameter.getWidth());
            }
            int requiredWidth = parameterContentWidth + 2 * (PARAMETER_SLOT_INNER_PADDING + PARAMETER_SLOT_MARGIN_HORIZONTAL);
            computedWidth = Math.max(computedWidth, requiredWidth);
        }
        if (hasSensorSlot()) {
            int sensorContentWidth = SENSOR_SLOT_MIN_CONTENT_WIDTH;
            if (attachedSensor != null) {
                sensorContentWidth = Math.max(sensorContentWidth, attachedSensor.getWidth());
            }
            int requiredWidth = sensorContentWidth + 2 * (SENSOR_SLOT_INNER_PADDING + SENSOR_SLOT_MARGIN_HORIZONTAL);
            computedWidth = Math.max(computedWidth, requiredWidth);
        }
        if (hasActionSlot()) {
            int actionContentWidth = ACTION_SLOT_MIN_CONTENT_WIDTH;
            if (attachedActionNode != null) {
                actionContentWidth = Math.max(actionContentWidth, attachedActionNode.getWidth());
            }
            int requiredWidth = actionContentWidth + 2 * (ACTION_SLOT_INNER_PADDING + ACTION_SLOT_MARGIN_HORIZONTAL);
            computedWidth = Math.max(computedWidth, requiredWidth);
        }
        this.width = Math.max(MIN_WIDTH, computedWidth);

        int contentHeight = HEADER_HEIGHT;
        boolean hasSlots = hasSensorSlot() || hasActionSlot();
        if (hasParameterSlot()) {
            contentHeight += getParameterSlotHeight();
            if (hasSlots) {
                contentHeight += SLOT_VERTICAL_SPACING;
            } else {
                contentHeight += SLOT_AREA_PADDING_BOTTOM;
            }
        } else if (hasParameters()) {
            contentHeight += getParameterDisplayHeight();
            if (hasSlots) {
                contentHeight += SLOT_AREA_PADDING_TOP;
            }
        } else if (hasSlots) {
            contentHeight += SLOT_AREA_PADDING_TOP;
        } else {
            contentHeight += BODY_PADDING_NO_PARAMS;
        }

        if (hasSensorSlot()) {
            contentHeight += getSensorSlotHeight();
        }

        if (hasActionSlot()) {
            if (hasSensorSlot()) {
                contentHeight += SLOT_VERTICAL_SPACING;
            }
            contentHeight += getActionSlotHeight();
        }

        if (hasSlots || hasParameterSlot()) {
            contentHeight += SLOT_AREA_PADDING_BOTTOM;
        }

        this.height = Math.max(MIN_HEIGHT, contentHeight);

        if (attachedSensor != null) {
            updateAttachedSensorPosition();
        }
        if (attachedActionNode != null) {
            updateAttachedActionPosition();
        }
        if (attachedParameter != null) {
            updateAttachedParameterPosition();
        }
    }

    /**
     * Get the height needed to display parameters
     */
    public int getParameterDisplayHeight() {
        if (!hasParameters() && !supportsModeSelection()) {
            return 0;
        }
        int parameterLineCount = parameters.size();
        if (supportsModeSelection()) {
            parameterLineCount++;
        }
        return PARAM_PADDING_TOP + parameterLineCount * PARAM_LINE_HEIGHT + PARAM_PADDING_BOTTOM;
    }

    public String getModeDisplayLabel() {
        if (!supportsModeSelection()) {
            return "";
        }
        NodeMode nodeMode = getMode();
        String modeName = nodeMode != null ? nodeMode.getDisplayName() : "Select Mode";
        return "Mode: " + modeName;
    }

    /**
     * Execute this node asynchronously.
     * Returns a CompletableFuture that completes when the node's command is finished.
     */
    public CompletableFuture<Void> execute() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Execute on the main Minecraft thread
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                try {
                    executeNodeCommand(future);
                } catch (Exception e) {
                    System.err.println("Error executing node " + type + ": " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } else {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
        }
        
        return future;
    }
    
    /**
     * Execute the actual command for this node type.
     * This method should be overridden by specific node implementations if needed.
     */
    private void executeNodeCommand(CompletableFuture<Void> future) {
        switch (type) {
            case START:
                // START node doesn't execute any command, just passes through
                System.out.println("START node - passing through");
                future.complete(null);
                break;
            case EVENT_FUNCTION:
                System.out.println("Function node - awaiting body execution");
                future.complete(null);
                break;
            case EVENT_CALL:
                System.out.println("Call Function node - dispatching handlers");
                future.complete(null);
                break;

            // Generalized nodes
            case GOTO:
                executeGotoCommand(future);
                break;
            case GOAL:
                executeGoalCommand(future);
                break;
            case MINE:
                executeMineCommand(future);
                break;
            case BUILD:
                executeBuildCommand(future);
                break;
            case EXPLORE:
                executeExploreCommand(future);
                break;
            case FOLLOW:
                executeFollowCommand(future);
                break;
            case CONTROL_REPEAT:
                executeControlRepeat(future);
                break;
            case CONTROL_REPEAT_UNTIL:
                executeControlRepeatUntil(future);
                break;
            case CONTROL_FOREVER:
                executeControlForever(future);
                break;
            case CONTROL_IF:
                executeControlIf(future);
                break;
            case CONTROL_IF_ELSE:
                executeControlIfElse(future);
                break;
            case FARM:
                executeFarmCommand(future);
                break;
            case STOP:
                executeStopCommand(future);
                break;
            case PLACE:
                executePlaceCommand(future);
                break;
            case CRAFT:
                executeCraftCommand(future);
                break;
            case PLAYER_GUI:
                executePlayerGuiCommand(future);
                break;
            case SCREEN_CONTROL:
                executeScreenControlCommand(future);
                break;
            case WAIT:
                executeWaitCommand(future);
                break;
            case MESSAGE:
                executeMessageCommand(future);
                break;
            case HOTBAR:
                executeHotbarCommand(future);
                break;
            case DROP_ITEM:
                executeDropItemCommand(future);
                break;
            case DROP_SLOT:
                executeDropSlotCommand(future);
                break;
            case MOVE_ITEM:
                executeMoveItemCommand(future);
                break;
            case SWAP_SLOTS:
                executeSwapSlotsCommand(future);
                break;
            case CLEAR_SLOT:
                executeClearSlotCommand(future);
                break;
            case USE:
                executeUseCommand(future);
                break;
            case PLACE_HAND:
                executePlaceHandCommand(future);
                break;
            case LOOK:
                executeLookCommand(future);
                break;
            case TURN:
                executeTurnCommand(future);
                break;
            case JUMP:
                executeJumpCommand(future);
                break;
            case CROUCH:
                executeCrouchCommand(future);
                break;
            case SPRINT:
                executeSprintCommand(future);
                break;
            case INTERACT:
                executeInteractCommand(future);
                break;
            case ATTACK:
                executeAttackCommand(future);
                break;
            case SWING:
                executeSwingCommand(future);
                break;
            case SWAP_HANDS:
                executeSwapHandsCommand(future);
                break;
            case EQUIP_ARMOR:
                executeEquipArmorCommand(future);
                break;
            case UNEQUIP_ARMOR:
                executeUnequipArmorCommand(future);
                break;
            case EQUIP_HAND:
                executeEquipHandCommand(future);
                break;
            case UNEQUIP_HAND:
                executeUnequipHandCommand(future);
                break;
            case SENSOR_TOUCHING_BLOCK:
            case SENSOR_TOUCHING_ENTITY:
            case SENSOR_AT_COORDINATES:
            case SENSOR_BLOCK_AHEAD:
            case SENSOR_BLOCK_BELOW:
            case SENSOR_LIGHT_LEVEL_BELOW:
            case SENSOR_IS_DAYTIME:
            case SENSOR_IS_RAINING:
            case SENSOR_HEALTH_BELOW:
            case SENSOR_HUNGER_BELOW:
            case SENSOR_ENTITY_NEARBY:
            case SENSOR_ITEM_IN_INVENTORY:
            case SENSOR_IS_SWIMMING:
            case SENSOR_IS_IN_LAVA:
            case SENSOR_IS_UNDERWATER:
            case SENSOR_IS_FALLING:
                completeSensorEvaluation(future);
                break;
            
            // Legacy nodes
            case PATH:
                executePathCommand(future);
                break;
            case INVERT:
                executeInvertCommand(future);
                break;
            case COME:
                executeComeCommand(future);
                break;
            case SURFACE:
                executeSurfaceCommand(future);
                break;
            case TUNNEL:
                executeTunnelCommand(future);
                break;
                
            default:
                System.out.println("Unknown node type: " + type);
                future.complete(null);
                break;
        }
    }
    
    // Command execution methods that wait for Baritone completion
    private void executeGotoCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for GOTO node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for goto command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        
        switch (mode) {
            case GOTO_XYZ:
                int x = 0, y = 64, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Executing goto to: " + x + ", " + y + ", " + z);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                GoalBlock goal = new GoalBlock(x, y, z);
                customGoalProcess.setGoalAndPath(goal);
                break;
                
            case GOTO_XZ:
                int x2 = 0, z2 = 0;
                NodeParameter xParam2 = getParameter("X");
                NodeParameter zParam2 = getParameter("Z");
                
                if (xParam2 != null) x2 = xParam2.getIntValue();
                if (zParam2 != null) z2 = zParam2.getIntValue();
                
                System.out.println("Executing goto to: " + x2 + ", " + z2);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                GoalBlock goal2 = new GoalBlock(x2, 0, z2); // Y will be determined by pathfinding
                customGoalProcess.setGoalAndPath(goal2);
                break;
                
            case GOTO_Y:
                int y3 = 64;
                NodeParameter yParam3 = getParameter("Y");
                if (yParam3 != null) y3 = yParam3.getIntValue();
                
                System.out.println("Executing goto to Y level: " + y3);
                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                // For Y-only movement, we need to get current X,Z and set goal there
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    int currentX = (int) client.player.getX();
                    int currentZ = (int) client.player.getZ();
                    GoalBlock goal3 = new GoalBlock(currentX, y3, currentZ);
                    customGoalProcess.setGoalAndPath(goal3);
                }
                break;
                
            case GOTO_BLOCK:
                String block = "stone";
                NodeParameter blockParam = getParameter("Block");
                if (blockParam != null) {
                    block = blockParam.getStringValue();
                }

                System.out.println("Executing goto to block: " + block);
                IGetToBlockProcess getToBlockProcess = baritone.getGetToBlockProcess();
                if (getToBlockProcess == null) {
                    future.completeExceptionally(new RuntimeException("GetToBlock process not available"));
                    break;
                }

                PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_GOTO, future);
                getToBlockProcess.getToBlock(new BlockOptionalMeta(block));
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown GOTO mode: " + mode));
                break;
        }
    }
    
    private void executeMineCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for MINE node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for mine command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IMineProcess mineProcess = baritone.getMineProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_MINE, future);
        
        switch (mode) {
            case MINE_SINGLE:
                String block = "stone";
                NodeParameter blockParam = getParameter("Block");
                if (blockParam != null) {
                    block = blockParam.getStringValue();
                }
                
                System.out.println("Executing mine for: " + block);
                mineProcess.mineByName(block);
                break;
                
            case MINE_MULTIPLE:
                String blocks = "stone,dirt";
                NodeParameter blocksParam = getParameter("Blocks");
                if (blocksParam != null) {
                    blocks = blocksParam.getStringValue();
                }
                
                System.out.println("Executing mine for blocks: " + blocks);
                // Split the comma-separated block names and mine them
                String[] blockNames = blocks.split(",");
                for (String blockName : blockNames) {
                    mineProcess.mineByName(blockName.trim());
                }
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown MINE mode: " + mode));
                break;
        }
    }
    
    private void executeCraftCommand(CompletableFuture<Void> future) {
        String itemId = "stick";
        int quantity = 1;

        NodeParameter itemParam = getParameter("Item");
        NodeParameter quantityParam = getParameter("Quantity");

        if (itemParam != null) {
            itemId = itemParam.getStringValue();
        }
        if (quantityParam != null) {
            quantity = quantityParam.getIntValue();
        }

        NodeMode craftMode = mode != null ? mode : NodeMode.CRAFT_PLAYER_GUI;

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            sendNodeErrorMessage(client, "Cannot craft \"" + itemId + "\": unknown item identifier.");
            future.complete(null);
            return;
        }

        Item targetItem = Registries.ITEM.get(identifier);
        if (client == null || client.player == null || client.world == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        if (!isCraftingScreenAvailable(client, craftMode)) {
            String unavailableMessage = craftMode == NodeMode.CRAFT_CRAFTING_TABLE
                    ? "Cannot craft: open a crafting table GUI before running this node."
                    : "Cannot craft: open your inventory before running this node.";
            sendNodeErrorMessage(client, unavailableMessage);
            future.complete(null);
            return;
        }

        String itemDisplayName = targetItem.getName().getString();

        ScreenHandler handler = client.player.currentScreenHandler;
        if (!isCompatibleCraftingHandler(handler, craftMode)) {
            sendNodeErrorMessage(client, "Cannot craft " + itemDisplayName + ": the crafting screen closed.");
            future.complete(null);
            return;
        }

        RecipeEntry<CraftingRecipe> recipeEntry = findCraftingRecipe(client, targetItem, craftMode);
        if (recipeEntry == null) {
            sendNodeErrorMessage(client, "Cannot craft " + itemDisplayName + ": no matching recipe found.");
            future.complete(null);
            return;
        }

        List<ItemStack> emptyGrid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        ItemStack outputTemplate = recipeEntry.value().craft(CraftingRecipeInput.create(3, 3, emptyGrid), client.player.getWorld().getRegistryManager());
        if (outputTemplate.isEmpty()) {
            sendNodeErrorMessage(client, "Cannot craft " + itemDisplayName + ": the recipe produced no output.");
            future.complete(null);
            return;
        }

        int desiredCount = Math.max(1, quantity);
        int perCraftOutput = Math.max(1, outputTemplate.getCount());
        int craftsRequested = Math.max(1, (int) Math.ceil(desiredCount / (double) perCraftOutput));

        List<GridIngredient> gridIngredients = resolveGridIngredients(recipeEntry.value(), craftMode);
        if (gridIngredients.isEmpty()) {
            sendNodeErrorMessage(client, "Cannot craft " + itemDisplayName + ": the recipe has no ingredients.");
            future.complete(null);
            return;
        }

        int[] craftingGridSlots = getCraftingGridSlots(craftMode);

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return craftRecipeUsingScreen(client, craftMode, recipeEntry, targetItem, craftsRequested, desiredCount, itemDisplayName, gridIngredients, craftingGridSlots);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.util.concurrent.CompletionException(e);
                }
            })
            .whenComplete((summary, throwable) -> {
                if (throwable != null) {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (!(cause instanceof InterruptedException)) {
                        sendNodeErrorMessageOnClientThread(client, "Cannot craft " + itemDisplayName + ": " + cause.getMessage());
                    }
                    future.complete(null);
                    return;
                }

                if (summary.failureMessage != null) {
                    sendNodeErrorMessageOnClientThread(client, summary.failureMessage);
                }

                future.complete(null);
            });
    }

    private void executeScreenControlCommand(CompletableFuture<Void> future) {
        NodeMode screenMode = mode != null ? mode : NodeMode.SCREEN_OPEN_CHAT;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

        if (client == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        try {
            runOnClientThread(client, () -> {
                switch (screenMode) {
                    case SCREEN_OPEN_CHAT:
                        client.setScreen(new ChatScreen(""));
                        break;
                    case SCREEN_CLOSE_CURRENT:
                        if (client.player != null) {
                            client.player.closeHandledScreen();
                        }
                        client.setScreen(null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown screen control mode: " + screenMode);
                }
            });
            future.complete(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        } catch (RuntimeException e) {
            sendNodeErrorMessage(client, e.getMessage());
            future.complete(null);
        }
    }

    private void executePlayerGuiCommand(CompletableFuture<Void> future) {
        NodeMode playerGuiMode = mode != null ? mode : NodeMode.PLAYER_GUI_OPEN;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

        if (client == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        try {
            runOnClientThread(client, () -> {
                switch (playerGuiMode) {
                    case PLAYER_GUI_OPEN:
                        if (client.player == null || client.player.networkHandler == null) {
                            throw new RuntimeException("Cannot open the player GUI without an active player.");
                        }

                        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                                client.player,
                                ClientCommandC2SPacket.Mode.OPEN_INVENTORY
                        ));

                        if (!(client.currentScreen instanceof InventoryScreen)) {
                            client.setScreen(new InventoryScreen(client.player));
                        }
                        break;
                    case PLAYER_GUI_CLOSE:
                        if (client.player == null) {
                            throw new RuntimeException("Cannot close the player GUI without an active player.");
                        }

                        if (client.currentScreen instanceof InventoryScreen) {
                            client.player.closeHandledScreen();
                            client.setScreen(null);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown player GUI mode: " + playerGuiMode);
                }
            });
            future.complete(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        } catch (RuntimeException e) {
            sendNodeErrorMessage(client, e.getMessage());
            future.complete(null);
        }
    }

    private boolean isCraftingScreenAvailable(net.minecraft.client.MinecraftClient client, NodeMode craftMode) {
        if (client == null) {
            return false;
        }

        if (craftMode == NodeMode.CRAFT_CRAFTING_TABLE) {
            return client.currentScreen instanceof CraftingScreen;
        }

        return client.currentScreen instanceof InventoryScreen;
    }

    private boolean isCompatibleCraftingHandler(ScreenHandler handler, NodeMode craftMode) {
        if (handler == null) {
            return false;
        }

        if (craftMode == NodeMode.CRAFT_CRAFTING_TABLE) {
            return handler instanceof CraftingScreenHandler;
        }

        return handler instanceof PlayerScreenHandler;
    }

    private RecipeEntry<CraftingRecipe> findCraftingRecipe(net.minecraft.client.MinecraftClient client, Item targetItem, NodeMode craftMode) {
        MinecraftServer server = client.getServer();
        if (server == null) {
            return null;
        }

        ServerRecipeManager recipeManager = server.getRecipeManager();
        List<ServerRecipeManager.ServerRecipe> serverRecipes = getServerRecipeList(recipeManager);
        if (serverRecipes.isEmpty()) {
            return null;
        }

        List<ItemStack> emptyGrid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (ServerRecipeManager.ServerRecipe serverRecipe : serverRecipes) {
            RecipeEntry<?> entry = serverRecipe.parent();
            if (!(entry.value() instanceof CraftingRecipe craftingRecipe)) {
                continue;
            }

            if (craftMode == NodeMode.CRAFT_PLAYER_GUI && !recipeFitsPlayerGrid(craftingRecipe.getIngredientPlacement())) {
                continue;
            }

            ItemStack result = craftingRecipe.craft(CraftingRecipeInput.create(3, 3, emptyGrid), client.player.getWorld().getRegistryManager());
            if (!result.isOf(targetItem)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            RecipeEntry<CraftingRecipe> castEntry = (RecipeEntry<CraftingRecipe>) entry;
            return castEntry;
        }

        return null;
    }

    private List<ServerRecipeManager.ServerRecipe> getServerRecipeList(ServerRecipeManager manager) {
        if (SERVER_RECIPES_FIELD == null) {
            return Collections.emptyList();
        }
        try {
            @SuppressWarnings("unchecked")
            List<ServerRecipeManager.ServerRecipe> recipes = (List<ServerRecipeManager.ServerRecipe>) SERVER_RECIPES_FIELD.get(manager);
            return recipes != null ? recipes : Collections.emptyList();
        } catch (IllegalAccessException e) {
            return Collections.emptyList();
        }
    }

    private boolean recipeFitsPlayerGrid(IngredientPlacement placement) {
        if (placement == null) {
            return false;
        }

        if (placement.hasNoPlacement()) {
            return placement.getIngredients().size() <= 4;
        }

        IntList slots = placement.getPlacementSlots();
        if (slots == null || slots.isEmpty()) {
            return placement.getIngredients().size() <= 4;
        }

        int minX = 3;
        int minY = 3;
        int maxX = -1;
        int maxY = -1;
        for (int slot : slots) {
            int x = slot % 3;
            int y = slot / 3;
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        return width <= 2 && height <= 2;
    }

    private static Field initServerRecipesField() {
        try {
            Field field = ServerRecipeManager.class.getDeclaredField("field_54641");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static final Field SERVER_RECIPES_FIELD = initServerRecipesField();

    private CraftingSummary craftRecipeUsingScreen(net.minecraft.client.MinecraftClient client,
                                                   NodeMode craftMode,
                                                   RecipeEntry<CraftingRecipe> recipeEntry,
                                                   Item targetItem,
                                                   int craftsRequested,
                                                   int desiredCount,
                                                   String itemDisplayName,
                                                   List<GridIngredient> gridIngredients,
                                                   int[] gridSlots) throws InterruptedException {
        int totalProduced = 0;
        String failureMessage = null;

        for (int attempt = 0; attempt < craftsRequested; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (!isCraftingScreenAvailable(client, craftMode)) {
                failureMessage = craftMode == NodeMode.CRAFT_CRAFTING_TABLE
                        ? "Cannot craft " + itemDisplayName + ": open a crafting table GUI before running this node."
                        : "Cannot craft " + itemDisplayName + ": open your inventory before running this node.";
                break;
            }

            ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
            if (!isCompatibleCraftingHandler(handler, craftMode)) {
                failureMessage = "Cannot craft " + itemDisplayName + ": the crafting screen closed.";
                break;
            }

            CraftingAttemptResult attemptResult = performCraftingAttempt(client, targetItem, itemDisplayName, gridIngredients, gridSlots);
            if (attemptResult.errorMessage != null) {
                failureMessage = attemptResult.errorMessage;
                if (attemptResult.produced > 0) {
                    totalProduced += attemptResult.produced;
                }
                break;
            }

            if (attemptResult.produced <= 0) {
                failureMessage = "Cannot craft " + itemDisplayName + ": missing required ingredients.";
                break;
            }

            totalProduced += attemptResult.produced;

            if (totalProduced >= desiredCount) {
                break;
            }
        }

        if (totalProduced <= 0 && failureMessage == null) {
            failureMessage = "Cannot craft " + itemDisplayName + ": missing required ingredients.";
        }

        return new CraftingSummary(totalProduced, failureMessage);
    }

    private CraftingAttemptResult performCraftingAttempt(net.minecraft.client.MinecraftClient client,
                                                         Item targetItem,
                                                         String itemDisplayName,
                                                         List<GridIngredient> gridIngredients,
                                                         int[] gridSlots) throws InterruptedException {
        java.util.concurrent.atomic.AtomicReference<String> errorRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger producedRef = new java.util.concurrent.atomic.AtomicInteger();

        runOnClientThread(client, () -> {
            ClientPlayerInteractionManager interactionManager = client.interactionManager;
            if (interactionManager == null) {
                errorRef.set("Cannot craft " + itemDisplayName + ": interaction manager unavailable.");
                return;
            }

            ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
            if (handler == null) {
                errorRef.set("Cannot craft " + itemDisplayName + ": the crafting screen closed.");
                return;
            }

            clearCraftingGrid(client, interactionManager, handler, gridSlots);
        });

        if (errorRef.get() != null) {
            return new CraftingAttemptResult(0, errorRef.get());
        }

        for (GridIngredient ingredient : gridIngredients) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (ingredient == null || ingredient.ingredient().isEmpty()) {
                continue;
            }

            java.util.concurrent.atomic.AtomicBoolean placed = new java.util.concurrent.atomic.AtomicBoolean(false);

            runOnClientThread(client, () -> {
                ClientPlayerInteractionManager interactionManager = client.interactionManager;
                if (interactionManager == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": interaction manager unavailable.");
                    return;
                }

                ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
                if (handler == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": the crafting screen closed.");
                    return;
                }

                int sourceSlot = findIngredientSourceSlot(handler, ingredient.ingredient());
                if (sourceSlot == -1) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": missing required ingredients.");
                    return;
                }

                interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, client.player);
                interactionManager.clickSlot(handler.syncId, ingredient.slotIndex(), 1, SlotActionType.PICKUP, client.player);
                interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, client.player);
                placed.set(true);
            });

            if (errorRef.get() != null) {
                return new CraftingAttemptResult(producedRef.get(), errorRef.get());
            }

            if (!placed.get()) {
                return new CraftingAttemptResult(producedRef.get(), "Cannot craft " + itemDisplayName + ": failed to place ingredients.");
            }

            Thread.sleep(CRAFTING_ACTION_DELAY_MS);
        }

        for (int poll = 0; poll < CRAFTING_OUTPUT_POLL_LIMIT && producedRef.get() <= 0 && errorRef.get() == null; poll++) {
            runOnClientThread(client, () -> {
                ClientPlayerInteractionManager interactionManager = client.interactionManager;
                if (interactionManager == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": interaction manager unavailable.");
                    return;
                }

                ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
                if (handler == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": the crafting screen closed.");
                    return;
                }

                Slot outputSlot;
                try {
                    outputSlot = handler.getSlot(0);
                } catch (IndexOutOfBoundsException e) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": crafting output unavailable.");
                    return;
                }

                ItemStack resultStack = outputSlot.getStack();
                if (resultStack.isEmpty() || !resultStack.isOf(targetItem)) {
                    return;
                }

                producedRef.set(resultStack.getCount());
                interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, client.player);
            });

            if (producedRef.get() > 0 || errorRef.get() != null) {
                break;
            }

            Thread.sleep(CRAFTING_ACTION_DELAY_MS);
        }

        if (producedRef.get() > 0) {
            runOnClientThread(client, () -> {
                ClientPlayerInteractionManager interactionManager = client.interactionManager;
                if (interactionManager == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": interaction manager unavailable.");
                    return;
                }

                ScreenHandler handler = client.player != null ? client.player.currentScreenHandler : null;
                if (handler == null) {
                    errorRef.set("Cannot craft " + itemDisplayName + ": the crafting screen closed.");
                    return;
                }

                clearCraftingGrid(client, interactionManager, handler, gridSlots);
            });

            if (errorRef.get() != null) {
                return new CraftingAttemptResult(producedRef.get(), errorRef.get());
            }

            Thread.sleep(CRAFTING_ACTION_DELAY_MS);
            return new CraftingAttemptResult(producedRef.get(), null);
        }

        if (errorRef.get() != null) {
            return new CraftingAttemptResult(producedRef.get(), errorRef.get());
        }

        return new CraftingAttemptResult(0, "Cannot craft " + itemDisplayName + ": missing required ingredients.");
    }

    private void clearCraftingGrid(net.minecraft.client.MinecraftClient client,
                                   ClientPlayerInteractionManager interactionManager,
                                   ScreenHandler handler,
                                   int[] gridSlots) {
        if (client.player == null || interactionManager == null || handler == null || gridSlots == null) {
            return;
        }

        for (int slotIndex : gridSlots) {
            try {
                Slot slot = handler.getSlot(slotIndex);
                if (slot != null && slot.hasStack()) {
                    interactionManager.clickSlot(handler.syncId, slotIndex, 0, SlotActionType.QUICK_MOVE, client.player);
                }
            } catch (IndexOutOfBoundsException ignored) {
                // Ignore missing grid slots for the current handler.
            }
        }
    }

    private int findIngredientSourceSlot(ScreenHandler handler, Ingredient ingredient) {
        if (handler == null || ingredient == null || ingredient.isEmpty()) {
            return -1;
        }

        List<Slot> slots = handler.slots;
        for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++) {
            Slot slot = slots.get(slotIdx);
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }

            int inventoryIndex = slot.getIndex();
            if (inventoryIndex < 0 || inventoryIndex >= PlayerInventory.MAIN_SIZE) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            if (ingredient.test(stack)) {
                return slotIdx;
            }
        }

        return -1;
    }

    private List<GridIngredient> resolveGridIngredients(CraftingRecipe recipe, NodeMode craftMode) {
        List<GridIngredient> result = new ArrayList<>();
        if (recipe == null) {
            return result;
        }

        if (craftMode == NodeMode.CRAFT_PLAYER_GUI && recipe instanceof ShapedRecipe shapedRecipe) {
            return resolvePlayerGridIngredients(shapedRecipe);
        }

        IngredientPlacement placement = recipe.getIngredientPlacement();
        if (placement == null) {
            return result;
        }

        List<Ingredient> ingredients = placement.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return result;
        }

        IntList slots = placement.getPlacementSlots();
        int gridLimit = craftMode == NodeMode.CRAFT_CRAFTING_TABLE ? 9 : 4;

        if (placement.hasNoPlacement() || slots == null || slots.isEmpty()) {
            int limit = Math.min(ingredients.size(), gridLimit);
            for (int i = 0; i < limit; i++) {
                Ingredient ingredient = ingredients.get(i);
                if (ingredient == null || ingredient.isEmpty()) {
                    continue;
                }
                result.add(new GridIngredient(1 + i, ingredient));
            }
            return result;
        }

        int limit = Math.min(ingredients.size(), slots.size());
        for (int i = 0; i < limit; i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }

            int logicalSlot = slots.getInt(i);
            int resolvedSlot;
            if (craftMode == NodeMode.CRAFT_PLAYER_GUI) {
                int localX = logicalSlot % 3;
                int localY = logicalSlot / 3;

                if (localX >= 2 || localY >= 2) {
                    continue;
                }

                resolvedSlot = 1 + localX + (localY * 2);
            } else {
                resolvedSlot = 1 + logicalSlot;
            }

            if (resolvedSlot > gridLimit) {
                continue;
            }

            result.add(new GridIngredient(resolvedSlot, ingredient));
        }

        return result;
    }

    private List<GridIngredient> resolvePlayerGridIngredients(ShapedRecipe recipe) {
        List<GridIngredient> result = new ArrayList<>();
        List<Optional<Ingredient>> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return result;
        }

        int width = Math.min(recipe.getWidth(), 2);
        int height = Math.min(recipe.getHeight(), 2);
        int recipeWidth = Math.max(recipe.getWidth(), 1);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + (y * recipeWidth);
                if (index < 0 || index >= ingredients.size()) {
                    continue;
                }

                Optional<Ingredient> optional = ingredients.get(index);
                if (optional == null || optional.isEmpty()) {
                    continue;
                }

                Ingredient ingredient = optional.get();
                if (ingredient.isEmpty()) {
                    continue;
                }

                int slotIndex = 1 + x + (y * 2);
                result.add(new GridIngredient(slotIndex, ingredient));
            }
        }

        return result;
    }

    private int[] getCraftingGridSlots(NodeMode craftMode) {
        if (craftMode == NodeMode.CRAFT_CRAFTING_TABLE) {
            return new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        }
        return new int[] {1, 2, 3, 4};
    }

    private static class CraftingSummary {
        final int produced;
        final String failureMessage;

        CraftingSummary(int produced, String failureMessage) {
            this.produced = produced;
            this.failureMessage = failureMessage;
        }
    }

    private static class GridIngredient {
        private final int slotIndex;
        private final Ingredient ingredient;

        GridIngredient(int slotIndex, Ingredient ingredient) {
            this.slotIndex = slotIndex;
            this.ingredient = ingredient;
        }

        int slotIndex() {
            return slotIndex;
        }

        Ingredient ingredient() {
            return ingredient;
        }
    }

    private static class CraftingAttemptResult {
        final int produced;
        final String errorMessage;

        CraftingAttemptResult(int produced, String errorMessage) {
            this.produced = produced;
            this.errorMessage = errorMessage;
        }
    }

    private void executePlaceCommand(CompletableFuture<Void> future) {
        String block = "stone";
        int x = 0, y = 0, z = 0;

        NodeParameter blockParam = getParameter("Block");
        NodeParameter xParam = getParameter("X");
        NodeParameter yParam = getParameter("Y");
        NodeParameter zParam = getParameter("Z");
        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);

        if (blockParam != null) block = blockParam.getStringValue();
        if (xParam != null) x = xParam.getIntValue();
        if (yParam != null) y = yParam.getIntValue();
        if (zParam != null) z = zParam.getIntValue();

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.networkHandler == null || client.interactionManager == null || client.world == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        System.out.println("Placing block '" + block + "' at " + x + ", " + y + ", " + z);

        BlockPos targetPos = new BlockPos(x, y, z);
        Block desiredBlock = resolveBlockForPlacement(block);
        final String resolvedBlockId = block;

        try {
            runOnClientThread(client, () -> {
                if (desiredBlock != null && client.world.getBlockState(targetPos).isOf(desiredBlock)) {
                    return;
                }

                if (!ensureBlockInHand(client, resolvedBlockId, hand)) {
                    throw new RuntimeException("Block " + resolvedBlockId + " not found in hotbar");
                }

                BlockHitResult hitResult = createPlacementHitResult(client, targetPos);
                if (hitResult == null) {
                    throw new RuntimeException("No valid placement position at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ());
                }

                ActionResult result = client.interactionManager.interactBlock(client.player, hand, hitResult);
                if (!result.isAccepted()) {
                    throw new RuntimeException("Block placement rejected: " + result);
                }
            });
            future.complete(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        } catch (RuntimeException e) {
            sendNodeErrorMessage(client, e.getMessage());
            future.complete(null);
        }
    }
    
    private void executeBuildCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for BUILD node"));
            return;
        }
        
        String schematic = "house.schematic";
        NodeParameter schematicParam = getParameter("Schematic");
        if (schematicParam != null) {
            schematic = schematicParam.getStringValue();
        }
        
        String command;
        switch (mode) {
            case BUILD_PLAYER:
                command = String.format("#build %s", schematic);
                System.out.println("Executing build at player location: " + command);
                break;
                
            case BUILD_XYZ:
                int x = 0, y = 0, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                command = String.format("#build %s %d %d %d", schematic, x, y, z);
                System.out.println("Executing build at coordinates: " + command);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown BUILD mode: " + mode));
                return;
        }
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeExploreCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for EXPLORE node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for explore command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IExploreProcess exploreProcess = baritone.getExploreProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_EXPLORE, future);
        
        switch (mode) {
            case EXPLORE_CURRENT:
                System.out.println("Executing explore from current position");
                exploreProcess.explore(0, 0); // 0,0 means from current position
                break;
                
            case EXPLORE_XYZ:
                int x = 0, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Executing explore at: " + x + ", " + z);
                exploreProcess.explore(x, z);
                break;
                
            case EXPLORE_FILTER:
                String filter = "explore.txt";
                NodeParameter filterParam = getParameter("Filter");
                if (filterParam != null) {
                    filter = filterParam.getStringValue();
                }
                
                System.out.println("Executing explore with filter: " + filter);
                // For filter-based exploration, we need to use a different approach
                executeCommand("#explore " + filter);
                future.complete(null); // Command-based exploration completes immediately
                return;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown EXPLORE mode: " + mode));
                return;
        }
    }
    
    private void executeFollowCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for FOLLOW node"));
            return;
        }
        
        String command;
        switch (mode) {
            case FOLLOW_PLAYER:
                String player = "PlayerName";
                NodeParameter playerParam = getParameter("Player");
                if (playerParam != null) {
                    player = playerParam.getStringValue();
                }

                command = "#follow player " + player;
                System.out.println("Executing follow player: " + command);
                break;
                
            case FOLLOW_PLAYERS:
                command = "#follow players";
                System.out.println("Executing follow any players: " + command);
                break;
                
            case FOLLOW_ENTITIES:
                command = "#follow entities";
                System.out.println("Executing follow any entities: " + command);
                break;
                
            case FOLLOW_ENTITY_TYPE:
                String entity = "cow";
                NodeParameter entityParam = getParameter("Entity");
                if (entityParam != null) {
                    entity = entityParam.getStringValue();
                }

                command = "#follow entity " + entity;
                System.out.println("Executing follow entity type: " + command);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown FOLLOW mode: " + mode));
                return;
        }
        
        executeCommand(command);
        future.complete(null); // Follow commands complete immediately
    }
    
    private void executeWaitCommand(CompletableFuture<Void> future) {
        double baseDuration = Math.max(0.0, getDoubleParameter("Duration", 1.0));
        double minimum = Math.max(0.0, getDoubleParameter("MinimumDurationSeconds", 0.0));
        double variance = Math.max(0.0, getDoubleParameter("RandomVarianceSeconds", 0.0));

        double effectiveDuration = Math.max(baseDuration, minimum);
        if (variance > 0.0) {
            double randomOffset = (Math.random() * 2.0 - 1.0) * variance;
            effectiveDuration = Math.max(minimum, Math.max(0.0, effectiveDuration + randomOffset));
        }

        final double waitSeconds = effectiveDuration;
        System.out.println("Waiting for " + waitSeconds + " seconds (configured duration=" + baseDuration + ")");

        new Thread(() -> {
            try {
                Thread.sleep((long) (waitSeconds * 1000));
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-Wait").start();
    }
    
    private void executeControlRepeat(CompletableFuture<Void> future) {
        int count = Math.max(0, getIntParameter("Count", 1));
        if (!repeatActive) {
            repeatRemainingIterations = count;
            repeatActive = true;
        }
        if (repeatRemainingIterations > 0) {
            repeatRemainingIterations--;
            setNextOutputSocket(0);
        } else {
            repeatRemainingIterations = 0;
            repeatActive = false;
            setNextOutputSocket(1);
        }
        future.complete(null);
    }
    
    private void executeControlRepeatUntil(CompletableFuture<Void> future) {
        boolean conditionMet = evaluateConditionFromParameters();
        if (conditionMet) {
            repeatRemainingIterations = 0;
            repeatActive = false;
            setNextOutputSocket(1);
        } else {
            repeatActive = true;
            setNextOutputSocket(0);
        }
        future.complete(null);
    }
    
    private void executeControlForever(CompletableFuture<Void> future) {
        repeatActive = true;
        setNextOutputSocket(0);
        future.complete(null);
    }

    private void executeControlIf(CompletableFuture<Void> future) {
        boolean condition = evaluateConditionFromParameters();
        setNextOutputSocket(condition ? 0 : NO_OUTPUT);
        future.complete(null);
    }

    private void executeControlIfElse(CompletableFuture<Void> future) {
        boolean condition = evaluateConditionFromParameters();
        setNextOutputSocket(condition ? 0 : 1);
        future.complete(null);
    }
    
    private void executeMessageCommand(CompletableFuture<Void> future) {
        String text = "Hello World";
        NodeParameter textParam = getParameter("Text");
        if (textParam != null) {
            text = textParam.getStringValue();
        }

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatMessage(text);
        } else {
            System.err.println("Unable to send chat message: client or player not available");
        }
        future.complete(null); // Message commands complete immediately
    }
    
    private void executeGoalCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for GOAL node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for goal command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        
        switch (mode) {
            case GOAL_XYZ:
                int x = 0, y = 64, z = 0;
                NodeParameter xParam = getParameter("X");
                NodeParameter yParam = getParameter("Y");
                NodeParameter zParam = getParameter("Z");
                
                if (xParam != null) x = xParam.getIntValue();
                if (yParam != null) y = yParam.getIntValue();
                if (zParam != null) z = zParam.getIntValue();
                
                System.out.println("Setting goal to: " + x + ", " + y + ", " + z);
                GoalBlock goal = new GoalBlock(x, y, z);
                customGoalProcess.setGoal(goal);
                break;
                
            case GOAL_XZ:
                int x2 = 0, z2 = 0;
                NodeParameter xParam2 = getParameter("X");
                NodeParameter zParam2 = getParameter("Z");
                
                if (xParam2 != null) x2 = xParam2.getIntValue();
                if (zParam2 != null) z2 = zParam2.getIntValue();
                
                System.out.println("Setting goal to: " + x2 + ", " + z2);
                GoalBlock goal2 = new GoalBlock(x2, 0, z2); // Y will be determined by pathfinding
                customGoalProcess.setGoal(goal2);
                break;
                
            case GOAL_Y:
                int y3 = 64;
                NodeParameter yParam3 = getParameter("Y");
                if (yParam3 != null) y3 = yParam3.getIntValue();
                
                System.out.println("Setting goal to Y level: " + y3);
                // For Y-only goal, we need to get current X,Z and set goal there
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    int currentX = (int) client.player.getX();
                    int currentZ = (int) client.player.getZ();
                    GoalBlock goal3 = new GoalBlock(currentX, y3, currentZ);
                    customGoalProcess.setGoal(goal3);
                }
                break;
                
            case GOAL_CURRENT:
                System.out.println("Setting goal to current position");
                net.minecraft.client.MinecraftClient client2 = net.minecraft.client.MinecraftClient.getInstance();
                if (client2 != null && client2.player != null) {
                    int currentX = (int) client2.player.getX();
                    int currentY = (int) client2.player.getY();
                    int currentZ = (int) client2.player.getZ();
                    GoalBlock goal4 = new GoalBlock(currentX, currentY, currentZ);
                    customGoalProcess.setGoal(goal4);
                }
                break;
                
            case GOAL_CLEAR:
                System.out.println("Clearing current goal");
                customGoalProcess.setGoal(null);
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown GOAL mode: " + mode));
                return;
        }
        
        // Goal setting is immediate, no need to wait
        future.complete(null);
    }
    
    private void executePathCommand(CompletableFuture<Void> future) {
        System.out.println("Executing path command");
        
        IBaritone baritone = getBaritone();
        if (baritone != null) {
            // Start precise tracking of this task
            PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_PATH, future);
            
            // Start the Baritone pathing task
            ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
            customGoalProcess.path();
            
            // The future will be completed by the PreciseCompletionTracker when the path actually reaches the goal
        } else {
            System.err.println("Baritone not available for path command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
        }
    }
    
    private void executeStopCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for STOP node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for stop command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        switch (mode) {
            case STOP_NORMAL:
                System.out.println("Executing stop command");
                // Cancel all pending tasks first
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            case STOP_CANCEL:
                System.out.println("Executing cancel command");
                // Cancel all pending tasks first
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            case STOP_FORCE:
                System.out.println("Executing force cancel command");
                // Force cancel all tasks
                PreciseCompletionTracker.getInstance().cancelAllTasks();
                // Force stop all Baritone processes
                baritone.getPathingBehavior().cancelEverything();
                break;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown STOP mode: " + mode));
                return;
        }
        
        // Complete immediately since stop is immediate
        future.complete(null);
    }
    
    private void executeInvertCommand(CompletableFuture<Void> future) {
        String command = "#invert";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // Invert commands complete immediately
    }
    
    private void executeComeCommand(CompletableFuture<Void> future) {
        String command = "#come";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeSurfaceCommand(CompletableFuture<Void> future) {
        String command = "#surface";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeTunnelCommand(CompletableFuture<Void> future) {
        String command = "#tunnel";
        System.out.println("Executing command: " + command);
        
        executeCommand(command);
        future.complete(null); // These commands complete immediately
    }
    
    private void executeFarmCommand(CompletableFuture<Void> future) {
        if (mode == null) {
            future.completeExceptionally(new RuntimeException("No mode set for FARM node"));
            return;
        }
        
        IBaritone baritone = getBaritone();
        if (baritone == null) {
            System.err.println("Baritone not available for farm command");
            future.completeExceptionally(new RuntimeException("Baritone not available"));
            return;
        }
        
        IFarmProcess farmProcess = baritone.getFarmProcess();
        PreciseCompletionTracker.getInstance().startTrackingTask(PreciseCompletionTracker.TASK_FARM, future);
        
        switch (mode) {
            case FARM_RANGE:
                int range = 10;
                NodeParameter rangeParam = getParameter("Range");
                if (rangeParam != null) {
                    range = rangeParam.getIntValue();
                }
                
                System.out.println("Executing farm within range: " + range);
                farmProcess.farm(range);
                break;
                
            case FARM_WAYPOINT:
                String waypoint = "farm";
                int waypointRange = 10;
                NodeParameter waypointParam = getParameter("Waypoint");
                NodeParameter waypointRangeParam = getParameter("Range");
                
                if (waypointParam != null) {
                    waypoint = waypointParam.getStringValue();
                }
                if (waypointRangeParam != null) {
                    waypointRange = waypointRangeParam.getIntValue();
                }
                
                System.out.println("Executing farm around waypoint: " + waypoint + " with range: " + waypointRange);
                // For waypoint-based farming, we need to use a different approach
                executeCommand("#farm " + waypoint + " " + waypointRange);
                future.complete(null); // Command-based farming completes immediately
                return;
                
            default:
                future.completeExceptionally(new RuntimeException("Unknown FARM mode: " + mode));
                return;
        }
    }
    
    private void executeHotbarCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.networkHandler == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        int slot = MathHelper.clamp(getIntParameter("Slot", 0), 0, 8);
        client.player.getInventory().setSelectedSlot(slot);
        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        future.complete(null);
    }
    
    private void executeDropItemCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        boolean dropAll = getBooleanParameter("All", false);
        int count = Math.max(1, getIntParameter("Count", 1));
        double interval = Math.max(0.0, getDoubleParameter("IntervalSeconds", 0.0));

        if (dropAll) {
            count = 1; // Dropping all ignores repeat count
        }

        final int dropIterations = count;
        final boolean dropEntireStack = dropAll;

        new Thread(() -> {
            try {
                for (int i = 0; i < dropIterations; i++) {
                    runOnClientThread(client, () -> {
                        client.player.dropSelectedItem(dropEntireStack);
                        client.player.getInventory().markDirty();
                        client.player.playerScreenHandler.sendContentUpdates();
                    });

                    if (interval > 0.0 && i < dropIterations - 1) {
                        Thread.sleep((long) (interval * 1000));
                    }
                }
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-DropItem").start();
    }
    
    private void executeDropSlotCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int slot = clampInventorySlot(inventory, getIntParameter("Slot", 0));
        boolean entireStack = getBooleanParameter("EntireStack", true);
        int requestedCount = getIntParameter("Count", 0);
        
        ItemStack stack = inventory.getStack(slot);
        if (stack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack removed;
        if (entireStack || requestedCount <= 0 || requestedCount >= stack.getCount()) {
            removed = inventory.removeStack(slot);
        } else {
            removed = inventory.removeStack(slot, requestedCount);
        }
        
        if (!removed.isEmpty()) {
            client.player.dropItem(removed, true);
        }
        
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeMoveItemCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int sourceSlot = clampInventorySlot(inventory, getIntParameter("SourceSlot", 0));
        int targetSlot = clampInventorySlot(inventory, getIntParameter("TargetSlot", 0));
        int requestedCount = getIntParameter("Count", 0);
        
        if (sourceSlot == targetSlot) {
            future.complete(null);
            return;
        }
        
        ItemStack sourceStack = inventory.getStack(sourceSlot);
        if (sourceStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack movingStack;
        if (requestedCount <= 0 || requestedCount >= sourceStack.getCount()) {
            movingStack = sourceStack.copy();
            inventory.setStack(sourceSlot, ItemStack.EMPTY);
        } else {
            movingStack = sourceStack.copy();
            movingStack.setCount(requestedCount);
            sourceStack.decrement(requestedCount);
            inventory.setStack(sourceSlot, sourceStack.isEmpty() ? ItemStack.EMPTY : sourceStack);
        }
        
        ItemStack targetStack = inventory.getStack(targetSlot);
        if (targetStack.isEmpty()) {
            inventory.setStack(targetSlot, movingStack);
        } else if (canStacksCombine(targetStack, movingStack)) {
            int transferable = Math.min(targetStack.getMaxCount() - targetStack.getCount(), movingStack.getCount());
            if (transferable > 0) {
                targetStack.increment(transferable);
                movingStack.decrement(transferable);
            }
            if (!movingStack.isEmpty()) {
                if (inventory.getStack(sourceSlot).isEmpty()) {
                    inventory.setStack(sourceSlot, movingStack);
                } else {
                    client.player.dropItem(movingStack, true);
                }
            }
        } else {
            inventory.setStack(targetSlot, movingStack);
            if (inventory.getStack(sourceSlot).isEmpty()) {
                inventory.setStack(sourceSlot, targetStack);
            } else {
                client.player.dropItem(targetStack, true);
            }
        }
        
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeSwapSlotsCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int firstSlot = clampInventorySlot(inventory, getIntParameter("FirstSlot", 0));
        int secondSlot = clampInventorySlot(inventory, getIntParameter("SecondSlot", 0));
        
        if (firstSlot == secondSlot) {
            future.complete(null);
            return;
        }
        
        ItemStack first = inventory.getStack(firstSlot);
        ItemStack second = inventory.getStack(secondSlot);
        inventory.setStack(firstSlot, second);
        inventory.setStack(secondSlot, first);
        
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeClearSlotCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int slot = clampInventorySlot(inventory, getIntParameter("Slot", 0));
        boolean dropItems = getBooleanParameter("DropItems", false);
        
        ItemStack removed = inventory.removeStack(slot);
        if (!removed.isEmpty() && dropItems) {
            client.player.dropItem(removed, true);
        }
        
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeUseCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        int configuredCount = Math.max(0, getIntParameter("RepeatCount", 1));
        boolean useUntilEmpty = getBooleanParameter("UseUntilEmpty", false);
        boolean stopIfUnavailable = getBooleanParameter("StopIfUnavailable", true);
        double durationSeconds = Math.max(0.0, getDoubleParameter("UseDurationSeconds", 0.0));
        double intervalSeconds = Math.max(0.0, getDoubleParameter("UseIntervalSeconds", 0.0));
        boolean allowBlock = getBooleanParameter("AllowBlockInteraction", true);
        boolean allowEntity = getBooleanParameter("AllowEntityInteraction", true);
        boolean swingAfterUse = getBooleanParameter("SwingAfterUse", true);
        boolean sneakWhileUsing = getBooleanParameter("SneakWhileUsing", false);
        boolean restoreSneak = getBooleanParameter("RestoreSneakState", true);

        if (!useUntilEmpty && configuredCount == 0) {
            future.complete(null);
            return;
        }

        final int maxIterations = configuredCount == 0 ? Integer.MAX_VALUE : configuredCount;

        new Thread(() -> {
            try {
                boolean previousSneak = false;

                if (sneakWhileUsing) {
                    previousSneak = supplyFromClient(client, () -> client.player.isSneaking());
                }

                int iteration = 0;
                while (iteration < maxIterations) {
                    ItemStack stack = supplyFromClient(client, () -> client.player.getStackInHand(hand).copy());
                    if ((stack == null || stack.isEmpty()) && stopIfUnavailable) {
                        break;
                    }

                    if (sneakWhileUsing) {
                        runOnClientThread(client, () -> {
                            client.player.setSneaking(true);
                            if (client.options != null && client.options.sneakKey != null) {
                                client.options.sneakKey.setPressed(true);
                            }
                        });
                    }

                    runOnClientThread(client, () -> {
                        boolean performed = false;
                        HitResult target = client.crosshairTarget;
                        if (allowEntity && target instanceof EntityHitResult entityHit) {
                            ActionResult entityResult = client.interactionManager.interactEntity(client.player, entityHit.getEntity(), hand);
                            performed = entityResult.isAccepted();
                        }
                        if (!performed && allowBlock && target instanceof BlockHitResult blockHit) {
                            ActionResult blockResult = client.interactionManager.interactBlock(client.player, hand, blockHit);
                            performed = blockResult.isAccepted();
                        }
                        if (!performed) {
                            client.interactionManager.interactItem(client.player, hand);
                        }

                        if (durationSeconds > 0.0 && client.options != null && client.options.useKey != null) {
                            client.options.useKey.setPressed(true);
                        }

                        if (swingAfterUse) {
                            client.player.swingHand(hand);
                            if (client.player.networkHandler != null) {
                                client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                            }
                        }
                    });

                    if (durationSeconds > 0.0) {
                        Thread.sleep((long) (durationSeconds * 1000));
                        runOnClientThread(client, () -> {
                            if (client.options != null && client.options.useKey != null) {
                                client.options.useKey.setPressed(false);
                            }
                        });
                    }

                    if (sneakWhileUsing && restoreSneak) {
                        boolean sneakState = previousSneak;
                        runOnClientThread(client, () -> {
                            client.player.setSneaking(sneakState);
                            if (client.options != null && client.options.sneakKey != null) {
                                client.options.sneakKey.setPressed(sneakState);
                            }
                        });
                    }

                    if (useUntilEmpty) {
                        ItemStack afterUse = supplyFromClient(client, () -> client.player.getStackInHand(hand).copy());
                        if (afterUse == null || afterUse.isEmpty()) {
                            break;
                        }
                    }

                    iteration++;
                    if (iteration >= maxIterations) {
                        break;
                    }

                    if (intervalSeconds > 0.0) {
                        Thread.sleep((long) (intervalSeconds * 1000));
                    }
                }

                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-Use").start();
    }

    private void executePlaceHandCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        boolean sneakWhilePlacing = getBooleanParameter("SneakWhilePlacing", false);
        boolean restoreSneak = getBooleanParameter("RestoreSneakState", true);
        boolean swingOnPlace = getBooleanParameter("SwingOnPlace", true);
        boolean requireBlockHit = getBooleanParameter("RequireBlockHit", true);

        boolean previousSneak = client.player.isSneaking();
        if (sneakWhilePlacing) {
            client.player.setSneaking(true);
            if (client.options != null && client.options.sneakKey != null) {
                client.options.sneakKey.setPressed(true);
            }
        }

        boolean placed = false;
        HitResult target = client.crosshairTarget;
        if (target instanceof BlockHitResult blockHit) {
            ActionResult result = client.interactionManager.interactBlock(client.player, hand, blockHit);
            placed = result.isAccepted();
            if (!placed && !requireBlockHit) {
                ActionResult fallback = client.interactionManager.interactItem(client.player, hand);
                placed = fallback.isAccepted();
            }
        } else if (!requireBlockHit) {
            ActionResult fallback = client.interactionManager.interactItem(client.player, hand);
            placed = fallback.isAccepted();
        }

        if (swingOnPlace && placed) {
            client.player.swingHand(hand);
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
        }

        if (sneakWhilePlacing && restoreSneak) {
            client.player.setSneaking(previousSneak);
            if (client.options != null && client.options.sneakKey != null) {
                client.options.sneakKey.setPressed(previousSneak);
            }
        }

        future.complete(null);
    }

    private boolean ensureBlockInHand(net.minecraft.client.MinecraftClient client, String blockId, Hand hand) {
        if (blockId == null || blockId.isEmpty()) {
            return true;
        }

        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return false;
        }

        Item targetItem = Registries.ITEM.get(identifier);
        ItemStack current = client.player.getStackInHand(hand);
        if (!current.isEmpty() && current.isOf(targetItem)) {
            return true;
        }

        PlayerInventory inventory = client.player.getInventory();
        int slot = findHotbarSlotWithItem(inventory, targetItem);
        if (slot == -1) {
            return false;
        }

        if (hand == Hand.MAIN_HAND) {
            if (inventory.getSelectedSlot() != slot) {
                inventory.setSelectedSlot(slot);
                if (client.player.networkHandler != null) {
                    client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                }
            }
            return true;
        }

        ItemStack offhandStack = client.player.getOffHandStack();
        if (!offhandStack.isEmpty() && offhandStack.isOf(targetItem)) {
            return true;
        }

        inventory.setSelectedSlot(slot);
        if (client.player.networkHandler != null) {
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        return true;
    }

    private int findHotbarSlotWithItem(PlayerInventory inventory, Item targetItem) {
        int hotbarSize = PlayerInventory.getHotbarSize();
        for (int slot = 0; slot < hotbarSize; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                return slot;
            }
        }
        return -1;
    }

    private BlockHitResult createPlacementHitResult(net.minecraft.client.MinecraftClient client, BlockPos targetPos) {
        if (client.player == null || client.player.getWorld() == null) {
            return null;
        }

        net.minecraft.world.World world = client.player.getWorld();
        if (!isBlockReplaceable(world, targetPos)) {
            return null;
        }

        for (Direction direction : Direction.values()) {
            BlockPos clickedPos = targetPos.offset(direction);
            if (world.getBlockState(clickedPos).isAir()) {
                continue;
            }

            Direction placementSide = direction.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(clickedPos).add(
                placementSide.getOffsetX() * 0.5D,
                placementSide.getOffsetY() * 0.5D,
                placementSide.getOffsetZ() * 0.5D
            );
            return new BlockHitResult(hitPos, placementSide, clickedPos, false);
        }

        return null;
    }

    private Block resolveBlockForPlacement(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }

        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return null;
        }

        return Registries.BLOCK.get(identifier);
    }

    private boolean isBlockReplaceable(net.minecraft.world.World world, BlockPos targetPos) {
        BlockState state = world.getBlockState(targetPos);
        if (state.isAir()) {
            return true;
        }

        if (!state.getFluidState().isEmpty()) {
            return true;
        }

        return state.getCollisionShape(world, targetPos).isEmpty();
    }

    private void executeLookCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        float yaw = (float) getDoubleParameter("Yaw", client.player.getYaw());
        float pitch = MathHelper.clamp((float) getDoubleParameter("Pitch", client.player.getPitch()), -90.0F, 90.0F);
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.setHeadYaw(yaw);
        future.complete(null);
    }

    private void executeTurnCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        float yawOffset = (float) getDoubleParameter("YawOffset", 0.0D);
        float pitchOffset = (float) getDoubleParameter("PitchOffset", 0.0D);
        float newYaw = client.player.getYaw() + yawOffset;
        float newPitch = MathHelper.clamp(client.player.getPitch() + pitchOffset, -90.0F, 90.0F);
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
        client.player.setHeadYaw(newYaw);
        future.complete(null);
    }
    
    private void executeJumpCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        int count = Math.max(1, getIntParameter("Count", 1));
        double intervalSeconds = Math.max(0.0, getDoubleParameter("IntervalSeconds", 0.0));

        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    runOnClientThread(client, () -> client.player.jump());
                    if (intervalSeconds > 0.0 && i < count - 1) {
                        Thread.sleep((long) (intervalSeconds * 1000));
                    }
                }
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-Jump").start();
    }
    
    private void executeCrouchCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        boolean active = getBooleanParameter("Active", true);
        boolean toggleKey = getBooleanParameter("ToggleKey", false);
        client.player.setSneaking(active);
        if (client.options != null && client.options.sneakKey != null) {
            if (toggleKey) {
                client.options.sneakKey.setPressed(true);
                client.options.sneakKey.setPressed(false);
            } else {
                client.options.sneakKey.setPressed(active);
            }
        }
        future.complete(null);
    }

    private void executeSprintCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        boolean active = getBooleanParameter("Active", true);
        boolean allowFlying = getBooleanParameter("AllowFlying", false);

        if (!allowFlying && client.player.getAbilities() != null && client.player.getAbilities().flying) {
            future.complete(null);
            return;
        }

        boolean previous = client.player.isSprinting();
        client.player.setSprinting(active);
        if (client.player.networkHandler != null && previous != active) {
            ClientCommandC2SPacket.Mode mode = active ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING;
            client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, mode));
        }
        future.complete(null);
    }
    
    private void executeInteractCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        boolean preferEntity = getBooleanParameter("PreferEntity", true);
        boolean preferBlock = getBooleanParameter("PreferBlock", true);
        boolean fallbackToItem = getBooleanParameter("FallbackToItemUse", true);
        boolean swingOnSuccess = getBooleanParameter("SwingOnSuccess", true);
        boolean sneakWhileInteracting = getBooleanParameter("SneakWhileInteracting", false);
        boolean restoreSneak = getBooleanParameter("RestoreSneakState", true);

        boolean previousSneak = client.player.isSneaking();
        if (sneakWhileInteracting) {
            client.player.setSneaking(true);
            if (client.options != null && client.options.sneakKey != null) {
                client.options.sneakKey.setPressed(true);
            }
        }

        HitResult target = client.crosshairTarget;
        ActionResult result = ActionResult.PASS;
        boolean attemptedInteraction = false;

        if (preferEntity && target instanceof EntityHitResult entityHit) {
            result = client.interactionManager.interactEntity(client.player, entityHit.getEntity(), hand);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || !result.isAccepted()) && preferBlock && target instanceof BlockHitResult blockHit) {
            result = client.interactionManager.interactBlock(client.player, hand, blockHit);
            attemptedInteraction = true;
        }

        if ((!attemptedInteraction || (!result.isAccepted() && result != ActionResult.PASS)) && fallbackToItem) {
            result = client.interactionManager.interactItem(client.player, hand);
        }

        if (swingOnSuccess && (result.isAccepted() || result == ActionResult.PASS)) {
            client.player.swingHand(hand);
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
        }

        if (sneakWhileInteracting && restoreSneak) {
            client.player.setSneaking(previousSneak);
            if (client.options != null && client.options.sneakKey != null) {
                client.options.sneakKey.setPressed(previousSneak);
            }
        }

        future.complete(null);
    }
    
    private void executeAttackCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        boolean swingOnly = getBooleanParameter("SwingOnly", false);
        final boolean attackEntities = getBooleanParameter("AttackEntities", true);
        final boolean attackBlocks = getBooleanParameter("AttackBlocks", true);
        int repeatCount = Math.max(1, getIntParameter("RepeatCount", 1));
        double intervalSeconds = Math.max(0.0, getDoubleParameter("AttackIntervalSeconds", 0.0));
        boolean sneakWhileAttacking = getBooleanParameter("SneakWhileAttacking", false);
        boolean restoreSneak = getBooleanParameter("RestoreSneakState", true);

        if (!attackEntities && !attackBlocks) {
            swingOnly = true;
        }

        boolean previousSneak = client.player.isSneaking();
        final boolean finalSwingOnly = swingOnly;
        final boolean finalAttackEntities = attackEntities;
        final boolean finalAttackBlocks = attackBlocks;

        new Thread(() -> {
            try {
                if (sneakWhileAttacking) {
                    runOnClientThread(client, () -> {
                        client.player.setSneaking(true);
                        if (client.options != null && client.options.sneakKey != null) {
                            client.options.sneakKey.setPressed(true);
                        }
                    });
                }

                for (int i = 0; i < repeatCount; i++) {
                    runOnClientThread(client, () -> {
                        HitResult target = client.crosshairTarget;
                        if (!finalSwingOnly && target instanceof EntityHitResult entityHit && finalAttackEntities) {
                            client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                        } else if (!finalSwingOnly && target instanceof BlockHitResult blockHit && finalAttackBlocks) {
                            client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                        }

                        client.player.swingHand(hand);
                        if (client.player.networkHandler != null) {
                            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                        }
                    });

                    if (intervalSeconds > 0.0 && i < repeatCount - 1) {
                        Thread.sleep((long) (intervalSeconds * 1000));
                    }
                }

                if (sneakWhileAttacking && restoreSneak) {
                    runOnClientThread(client, () -> {
                        client.player.setSneaking(previousSneak);
                        if (client.options != null && client.options.sneakKey != null) {
                            client.options.sneakKey.setPressed(previousSneak);
                        }
                    });
                }

                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-Attack").start();
    }

    private void executeSwingCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }

        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        int count = Math.max(1, getIntParameter("Count", 1));
        double intervalSeconds = Math.max(0.0, getDoubleParameter("IntervalSeconds", 0.0));

        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    runOnClientThread(client, () -> {
                        client.player.swingHand(hand);
                        if (client.player.networkHandler != null) {
                            client.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                        }
                    });

                    if (intervalSeconds > 0.0 && i < count - 1) {
                        Thread.sleep((long) (intervalSeconds * 1000));
                    }
                }
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        }, "Pathmind-Swing").start();
    }
    
    private void executeSwapHandsCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.networkHandler == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        ItemStack mainStack = inventory.getStack(selectedSlot).copy();
        ItemStack offStack = inventory.getStack(PlayerInventory.OFF_HAND_SLOT).copy();
        inventory.setStack(selectedSlot, offStack);
        inventory.setStack(PlayerInventory.OFF_HAND_SLOT, mainStack);
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        if (client.player.networkHandler != null) {
            client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        }
        future.complete(null);
    }
    
    private void executeEquipArmorCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int sourceSlot = clampInventorySlot(inventory, getIntParameter("SourceSlot", 0));
        EquipmentSlot equipmentSlot = parseEquipmentSlot(getParameter("ArmorSlot"), EquipmentSlot.HEAD);
        
        ItemStack sourceStack = inventory.getStack(sourceSlot);
        if (sourceStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack current = client.player.getEquippedStack(equipmentSlot);
        inventory.setStack(sourceSlot, current);
        client.player.equipStack(equipmentSlot, sourceStack);
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeUnequipArmorCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        EquipmentSlot equipmentSlot = parseEquipmentSlot(getParameter("ArmorSlot"), EquipmentSlot.HEAD);
        int targetSlot = clampInventorySlot(inventory, getIntParameter("TargetSlot", 0));
        boolean dropIfFull = getBooleanParameter("DropIfFull", true);
        
        ItemStack equipped = client.player.getEquippedStack(equipmentSlot);
        if (equipped.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack targetStack = inventory.getStack(targetSlot);
        if (targetStack.isEmpty()) {
            inventory.setStack(targetSlot, equipped);
            client.player.equipStack(equipmentSlot, ItemStack.EMPTY);
        } else if (dropIfFull) {
            client.player.dropItem(equipped.copy(), true);
            client.player.equipStack(equipmentSlot, ItemStack.EMPTY);
        } else {
            client.player.equipStack(equipmentSlot, targetStack);
            inventory.setStack(targetSlot, equipped);
        }
        
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeEquipHandCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        int sourceSlot = clampInventorySlot(inventory, getIntParameter("SourceSlot", 0));
        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        
        ItemStack sourceStack = inventory.getStack(sourceSlot);
        if (sourceStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack handStack = client.player.getStackInHand(hand);
        client.player.setStackInHand(hand, sourceStack);
        inventory.setStack(sourceSlot, handStack);
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void executeUnequipHandCommand(CompletableFuture<Void> future) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            future.completeExceptionally(new RuntimeException("Minecraft client not available"));
            return;
        }
        
        PlayerInventory inventory = client.player.getInventory();
        Hand hand = resolveHand(getParameter("Hand"), Hand.MAIN_HAND);
        int targetSlot = clampInventorySlot(inventory, getIntParameter("TargetSlot", 0));
        boolean dropIfFull = getBooleanParameter("DropIfFull", true);
        
        ItemStack handStack = client.player.getStackInHand(hand);
        if (handStack.isEmpty()) {
            future.complete(null);
            return;
        }
        
        ItemStack targetStack = inventory.getStack(targetSlot);
        if (targetStack.isEmpty()) {
            inventory.setStack(targetSlot, handStack);
        } else if (dropIfFull) {
            client.player.dropItem(handStack.copy(), true);
        } else {
            inventory.setStack(targetSlot, handStack);
            client.player.setStackInHand(hand, targetStack);
            inventory.markDirty();
            client.player.playerScreenHandler.sendContentUpdates();
            future.complete(null);
            return;
        }
        
        client.player.setStackInHand(hand, ItemStack.EMPTY);
        inventory.markDirty();
        client.player.playerScreenHandler.sendContentUpdates();
        future.complete(null);
    }
    
    private void completeSensorEvaluation(CompletableFuture<Void> future) {
        boolean result = evaluateSensor();
        setNextOutputSocket(result ? 0 : 1);
        future.complete(null);
    }

    private void runOnClientThread(net.minecraft.client.MinecraftClient client, Runnable task) throws InterruptedException {
        if (client == null || client.isOnThread()) {
            task.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        client.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) {
            throw error.get();
        }
    }

    private <T> T supplyFromClient(net.minecraft.client.MinecraftClient client, java.util.function.Supplier<T> supplier) throws InterruptedException {
        if (client == null || client.isOnThread()) {
            return supplier.get();
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        client.execute(() -> {
            try {
                result.set(supplier.get());
            } catch (RuntimeException e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    private boolean canStacksCombine(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        if (!ItemStack.areItemsEqual(first, second)) {
            return false;
        }
        return first.getComponents().equals(second.getComponents());
    }

    private int clampInventorySlot(PlayerInventory inventory, int slot) {
        return MathHelper.clamp(slot, 0, inventory.size() - 1);
    }

    private EquipmentSlot parseEquipmentSlot(NodeParameter parameter, EquipmentSlot defaultSlot) {
        if (parameter == null || parameter.getStringValue() == null) {
            return defaultSlot;
        }
        String value = parameter.getStringValue().trim().toLowerCase(Locale.ROOT);
        switch (value) {
            case "head":
            case "helmet":
                return EquipmentSlot.HEAD;
            case "chest":
            case "chestplate":
                return EquipmentSlot.CHEST;
            case "legs":
            case "leggings":
                return EquipmentSlot.LEGS;
            case "feet":
            case "boots":
                return EquipmentSlot.FEET;
            default:
                return defaultSlot;
        }
    }

    private int getIntParameter(String name, int defaultValue) {
        NodeParameter param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        if (param.getType() == ParameterType.INTEGER) {
            return param.getIntValue();
        }
        try {
            return Integer.parseInt(param.getStringValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private String getStringParameter(String name, String defaultValue) {
        NodeParameter param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        String value = param.getStringValue();
        return value != null ? value : defaultValue;
    }
    
    private double getDoubleParameter(String name, double defaultValue) {
        NodeParameter param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        if (param.getType() == ParameterType.DOUBLE) {
            return param.getDoubleValue();
        }
        try {
            return Double.parseDouble(param.getStringValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private boolean getBooleanParameter(String name, boolean defaultValue) {
        NodeParameter param = getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        if (param.getType() == ParameterType.BOOLEAN) {
            return param.getBoolValue();
        }
        String value = param.getStringValue();
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    private Hand resolveHand(NodeParameter parameter, Hand defaultHand) {
        if (parameter == null || parameter.getStringValue() == null) {
            return defaultHand;
        }
        String value = parameter.getStringValue().trim().toLowerCase(Locale.ROOT);
        if (value.equals("off") || value.equals("offhand") || value.equals("off_hand") || value.equals("off-hand")) {
            return Hand.OFF_HAND;
        }
        return Hand.MAIN_HAND;
    }

    private void resetControlState() {
        this.repeatRemainingIterations = 0;
        this.repeatActive = false;
        this.lastSensorResult = false;
        this.nextOutputSocket = 0;
    }
    
    private enum SensorConditionType {
        TOUCHING_BLOCK("Touching Block"),
        TOUCHING_ENTITY("Touching Entity"),
        AT_COORDINATES("At Coordinates");

        private final String label;

        SensorConditionType(String label) {
            this.label = label;
        }

        static SensorConditionType fromLabel(String label) {
            if (label == null) {
                return TOUCHING_BLOCK;
            }
            String trimmed = label.trim();
            for (SensorConditionType type : values()) {
                if (type.label.equalsIgnoreCase(trimmed)) {
                    return type;
                }
            }
            return TOUCHING_BLOCK;
        }
    }

    public boolean evaluateSensor() {
        if (!isSensorNode()) {
            return false;
        }

        boolean result;
        switch (type) {
            case SENSOR_TOUCHING_BLOCK: {
                String blockId = getStringParameter("Block", "minecraft:stone");
                result = evaluateSensorCondition(SensorConditionType.TOUCHING_BLOCK, blockId, null, 0, 0, 0);
                break;
            }
            case SENSOR_TOUCHING_ENTITY: {
                String entityId = getStringParameter("Entity", "minecraft:zombie");
                result = evaluateSensorCondition(SensorConditionType.TOUCHING_ENTITY, null, entityId, 0, 0, 0);
                break;
            }
            case SENSOR_AT_COORDINATES: {
                int x = getIntParameter("X", 0);
                int y = getIntParameter("Y", 64);
                int z = getIntParameter("Z", 0);
                result = evaluateSensorCondition(SensorConditionType.AT_COORDINATES, null, null, x, y, z);
                break;
            }
            case SENSOR_BLOCK_AHEAD: {
                String blockId = getStringParameter("Block", "minecraft:stone");
                result = isBlockAhead(blockId);
                break;
            }
            case SENSOR_BLOCK_BELOW: {
                String blockId = getStringParameter("Block", "minecraft:stone");
                result = isBlockBelow(blockId);
                break;
            }
            case SENSOR_LIGHT_LEVEL_BELOW: {
                int threshold = MathHelper.clamp(getIntParameter("Threshold", 7), 0, 15);
                result = isLightLevelBelow(threshold);
                break;
            }
            case SENSOR_IS_DAYTIME:
                result = isDaytime();
                break;
            case SENSOR_IS_RAINING:
                result = isRaining();
                break;
            case SENSOR_HEALTH_BELOW: {
                double amount = MathHelper.clamp(getDoubleParameter("Amount", 10.0), 0.0, 40.0);
                result = isHealthBelow(amount);
                break;
            }
            case SENSOR_HUNGER_BELOW: {
                int amount = MathHelper.clamp(getIntParameter("Amount", 10), 0, 20);
                result = isHungerBelow(amount);
                break;
            }
            case SENSOR_ENTITY_NEARBY: {
                String entityId = getStringParameter("Entity", "minecraft:zombie");
                double range = Math.max(1.0, getIntParameter("Range", 6));
                result = isEntityNearby(entityId, range);
                break;
            }
            case SENSOR_ITEM_IN_INVENTORY: {
                String itemId = getStringParameter("Item", "minecraft:stone");
                result = hasItemInInventory(itemId);
                break;
            }
            case SENSOR_IS_SWIMMING:
                result = isSwimming();
                break;
            case SENSOR_IS_IN_LAVA:
                result = isInLava();
                break;
            case SENSOR_IS_UNDERWATER:
                result = isUnderwater();
                break;
            case SENSOR_IS_FALLING: {
                double distance = Math.max(0.0, getDoubleParameter("Distance", 2.0));
                result = isFalling(distance);
                break;
            }
            default:
                result = false;
                break;
        }

        this.lastSensorResult = result;
        return result;
    }

    private boolean evaluateConditionFromParameters() {
        if (attachedSensor != null) {
            boolean result = attachedSensor.evaluateSensor();
            this.lastSensorResult = result;
            return result;
        }

        // Legacy fallback when no sensor is attached
        String condition = getStringParameter("Condition", "Touching Block");
        String blockId = getStringParameter("Block", "minecraft:stone");
        String entityId = getStringParameter("Entity", "minecraft:zombie");
        int x = getIntParameter("X", 0);
        int y = getIntParameter("Y", 64);
        int z = getIntParameter("Z", 0);
        boolean result = evaluateSensorCondition(SensorConditionType.fromLabel(condition), blockId, entityId, x, y, z);
        this.lastSensorResult = result;
        return result;
    }
    
    private boolean evaluateSensorCondition(SensorConditionType type, String blockId, String entityId, int x, int y, int z) {
        if (type == null) {
            type = SensorConditionType.TOUCHING_BLOCK;
        }
        switch (type) {
            case TOUCHING_BLOCK:
                return isTouchingBlock(blockId);
            case TOUCHING_ENTITY:
                return isTouchingEntity(entityId);
            case AT_COORDINATES:
                return isAtCoordinates(x, y, z);
            default:
                return false;
        }
    }
    
    private boolean isTouchingBlock(String blockId) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || blockId == null || blockId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return false;
        }
        Block block = Registries.BLOCK.get(identifier);
        Box box = client.player.getBoundingBox().expand(0.05);
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    mutable.set(bx, by, bz);
                    if (client.player.getWorld().getBlockState(mutable).isOf(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isTouchingEntity(String entityId) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || entityId == null || entityId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(entityId);
        if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
            return false;
        }
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(identifier);
        List<Entity> entities = client.player.getWorld().getOtherEntities(
            client.player,
            client.player.getBoundingBox().expand(0.15),
            entity -> entity.getType() == entityType
        );
        return !entities.isEmpty();
    }
    
    private boolean isAtCoordinates(int x, int y, int z) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        BlockPos playerPos = client.player.getBlockPos();
        return playerPos.getX() == x && playerPos.getY() == y && playerPos.getZ() == z;
    }

    private boolean isBlockAhead(String blockId) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || blockId == null || blockId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return false;
        }
        Block block = Registries.BLOCK.get(identifier);
        Direction facing = client.player.getHorizontalFacing();
        BlockPos targetPos = client.player.getBlockPos().offset(facing);
        return client.player.getWorld().getBlockState(targetPos).isOf(block);
    }

    private boolean isBlockBelow(String blockId) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || blockId == null || blockId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return false;
        }
        Block block = Registries.BLOCK.get(identifier);
        BlockPos below = client.player.getBlockPos().down();
        return client.player.getWorld().getBlockState(below).isOf(block);
    }

    private boolean isLightLevelBelow(int threshold) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.getWorld() == null) {
            return false;
        }
        BlockPos pos = client.player.getBlockPos();
        return client.player.getWorld().getLightLevel(pos) < threshold;
    }

    private boolean isDaytime() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return false;
        }
        long time = client.world.getTimeOfDay() % 24000L;
        return time < 12000L;
    }

    private boolean isRaining() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return false;
        }
        return client.world.isRaining() || client.world.hasRain(client.player.getBlockPos());
    }

    private boolean isHealthBelow(double amount) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return client.player.getHealth() < amount;
    }

    private boolean isHungerBelow(int amount) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return client.player.getHungerManager().getFoodLevel() < amount;
    }

    private boolean isEntityNearby(String entityId, double range) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || entityId == null || entityId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(entityId);
        if (identifier == null || !Registries.ENTITY_TYPE.containsId(identifier)) {
            return false;
        }
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(identifier);
        Box searchBox = client.player.getBoundingBox().expand(range);
        List<Entity> entities = client.player.getWorld().getOtherEntities(
            client.player,
            searchBox,
            entity -> entity.getType() == entityType
        );
        return !entities.isEmpty();
    }

    private boolean hasItemInInventory(String itemId) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null || itemId == null || itemId.isEmpty()) {
            return false;
        }
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return false;
        }
        net.minecraft.item.Item item = Registries.ITEM.get(identifier);
        return client.player.getInventory().count(item) > 0;
    }

    private boolean isSwimming() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return client != null && client.player != null && client.player.isSwimming();
    }

    private boolean isInLava() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return client != null && client.player != null && client.player.isInLava();
    }

    private boolean isUnderwater() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        return client != null && client.player != null && client.player.isSubmergedInWater();
    }

    private boolean isFalling(double distance) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return client.player.fallDistance >= distance && !client.player.isOnGround();
    }
    
    private void executeCommand(String command) {
        try {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.networkHandler.sendChatMessage(command);
                System.out.println("Sent command to Minecraft: " + command);
            } else {
                System.out.println("Cannot execute command - client or player is null");
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
}
