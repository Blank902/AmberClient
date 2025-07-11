package com.amberclient.modules.world.MacroRecorder;

import com.amberclient.screens.MacroRecorderGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MacroRecordingSystem {
    private static final Logger LOGGER = LogManager.getLogger("amberclient-macrorecordingsystem");
    private static MacroRecordingSystem instance;

    private static final int[] VALID_KEYS = {
            // Letters A-Z
            65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
            // Numbers 0-9
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            // Common keys
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_BACKSPACE,
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT,
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT
    };

    private static final int[] MOUSE_BUTTONS = {
            GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_MOUSE_BUTTON_MIDDLE
    };

    private boolean isRecording = false;
    private final List<MacroAction> recordedActions = new CopyOnWriteArrayList<>();
    private final List<MacroRecordingListener> listeners = new ArrayList<>();
    private long recordingStartTime = 0;
    private final MinecraftClient client;

    private final boolean[] previousKeyStates = new boolean[512];
    private final boolean[] previousMouseStates = new boolean[8];
    private float lastYaw = 0, lastPitch = 0;
    private boolean scrollCallbackRegistered = false;

    public static class MacroAction {
        public enum Type {
            KEY_PRESS, KEY_RELEASE, KEYBINDING_PRESS, MOUSE_PRESS, MOUSE_RELEASE,
            MOUSE_MOVE, MOUSE_SCROLL, BLOCK_INTERACT, ENTITY_INTERACT, CHAT_MESSAGE, INVENTORY_ACTION
        }

        private final Type type;
        private final long timestamp;
        private final Object data, data2;

        public MacroAction(Type type, long timestamp, Object data) {
            this(type, timestamp, data, null);
        }

        public MacroAction(Type type, long timestamp, Object data, Object data2) {
            this.type = type;
            this.timestamp = timestamp;
            this.data = data;
            this.data2 = data2;
        }

        public Type getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public Object getData() { return data; }
        public Object getData2() { return data2; }

        @Override
        public String toString() {
            return String.format("MacroAction{type=%s, timestamp=%d, data=%s, data2=%s}",
                    type, timestamp, data, data2);
        }
    }

    private MacroRecordingSystem() {
        this.client = MinecraftClient.getInstance();
    }

    public static MacroRecordingSystem getInstance() {
        if (instance == null) {
            instance = new MacroRecordingSystem();
        }
        return instance;
    }

    public void startRecording() {
        if (isRecording) {
            LOGGER.warn("Already recording!");
            return;
        }

        isRecording = true;
        recordedActions.clear();
        recordingStartTime = System.currentTimeMillis();
        initializePreviousStates();

        if (client.currentScreen instanceof MacroRecorderGUI) {
            client.setScreen(null);
        }

        LOGGER.info("Macro recording started");
        notifyListeners(true);
    }

    public void stopRecording() {
        if (!isRecording) return;

        isRecording = false;
        LOGGER.info("Stopped recording - {} actions", recordedActions.size());
        notifyListeners(false);
    }

    public void tick() {
        if (!isRecording || client.player == null) return;

        if (client.currentScreen instanceof MacroRecorderGUI) {
            stopRecording();
            return;
        }

        long currentTime = System.currentTimeMillis() - recordingStartTime;
        recordKeyboardActions(currentTime);
        recordMouseActions(currentTime);
        recordMouseMovement(currentTime);
        recordInteractions(currentTime);
    }

    private void initializePreviousStates() {
        for (int i = 0; i < previousKeyStates.length; i++) {
            previousKeyStates[i] = safeKeyCheck(i);
        }

        for (int i = 0; i < MOUSE_BUTTONS.length; i++) {
            previousMouseStates[i] = safeMouseCheck(MOUSE_BUTTONS[i]);
        }

        if (client.player != null) {
            lastYaw = client.player.getYaw();
            lastPitch = client.player.getPitch();
        }
    }

    private boolean safeKeyCheck(int keyCode) {
        try {
            return InputUtil.isKeyPressed(client.getWindow().getHandle(), keyCode);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeMouseCheck(int mouseButton) {
        try {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void recordKeyboardActions(long timestamp) {
        for (int keyCode : VALID_KEYS) {
            boolean currentState = safeKeyCheck(keyCode);
            boolean previousState = keyCode < previousKeyStates.length && previousKeyStates[keyCode];

            if (currentState != previousState) {
                recordedActions.add(new MacroAction(
                        currentState ? MacroAction.Type.KEY_PRESS : MacroAction.Type.KEY_RELEASE,
                        timestamp, keyCode
                ));

                if (keyCode < previousKeyStates.length) {
                    previousKeyStates[keyCode] = currentState;
                }
            }
        }
    }

    private void recordMouseActions(long timestamp) {
        for (int i = 0; i < MOUSE_BUTTONS.length; i++) {
            boolean currentState = safeMouseCheck(MOUSE_BUTTONS[i]);
            boolean previousState = previousMouseStates[i];

            if (currentState != previousState) {
                recordedActions.add(new MacroAction(
                        currentState ? MacroAction.Type.MOUSE_PRESS : MacroAction.Type.MOUSE_RELEASE,
                        timestamp, MOUSE_BUTTONS[i]
                ));
                previousMouseStates[i] = currentState;
            }
        }

        if (!scrollCallbackRegistered) {
            registerScrollCallback();
            scrollCallbackRegistered = true;
        }
    }

    private void recordMouseMovement(long timestamp) {
        if (client.player == null) return;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        if (Math.abs(currentYaw - lastYaw) > 0.1f || Math.abs(currentPitch - lastPitch) > 0.1f) {
            recordedActions.add(new MacroAction(MacroAction.Type.MOUSE_MOVE, timestamp, currentYaw, currentPitch));
            lastYaw = currentYaw;
            lastPitch = currentPitch;
        }
    }

    private void registerScrollCallback() {
        GLFW.glfwSetScrollCallback(client.getWindow().getHandle(), (window, xoffset, yoffset) -> {
            if (isRecording) {
                long currentTime = System.currentTimeMillis() - recordingStartTime;
                recordedActions.add(new MacroAction(MacroAction.Type.MOUSE_SCROLL, currentTime, xoffset, yoffset));
            }
        });
    }

    private void recordInteractions(long timestamp) {
        if (client.player == null || client.crosshairTarget == null) return;

        if (client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
            recordedActions.add(new MacroAction(
                    MacroAction.Type.BLOCK_INTERACT, timestamp,
                    blockHit.getBlockPos().toString(), blockHit.getSide().toString()
            ));
        } else if (client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
            recordedActions.add(new MacroAction(
                    MacroAction.Type.ENTITY_INTERACT, timestamp,
                    entityHit.getEntity().getUuidAsString(), entityHit.getEntity().getType().toString()
            ));
        }
    }

    public boolean isRecording() { return isRecording; }
    public List<MacroAction> getRecordedActions() { return new ArrayList<>(recordedActions); }
    public int getActionCount() { return recordedActions.size(); }
    public void clearRecording() { recordedActions.clear(); }

    public void loadActions(List<MacroAction> actions) {
        if (isRecording) {
            LOGGER.warn("Cannot load actions while recording!");
            return;
        }
        recordedActions.clear();
        recordedActions.addAll(actions);
    }

    public void addListener(MacroRecordingListener listener) { listeners.add(listener); }
    public void removeListener(MacroRecordingListener listener) { listeners.remove(listener); }

    private void notifyListeners(boolean started) {
        for (MacroRecordingListener listener : listeners) {
            if (started) listener.onRecordingStarted();
            else listener.onRecordingStopped();
        }
    }

    public interface MacroRecordingListener {
        void onRecordingStarted();
        void onRecordingStopped();
    }
}