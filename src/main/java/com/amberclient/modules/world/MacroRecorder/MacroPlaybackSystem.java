package com.amberclient.modules.world.MacroRecorder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MacroPlaybackSystem {
    private static final Logger LOGGER = LogManager.getLogger(MacroPlaybackSystem.class);
    private static volatile MacroPlaybackSystem instance;

    private final MinecraftClient mc;
    private volatile boolean isPlaying = false;

    private MacroPlaybackSystem() {
        this.mc = MinecraftClient.getInstance();
    }

    public static MacroPlaybackSystem getInstance() {
        if (instance == null) {
            synchronized (MacroPlaybackSystem.class) {
                if (instance == null) {
                    instance = new MacroPlaybackSystem();
                }
            }
        }
        return instance;
    }

    public void playMacro(List<MacroRecordingSystem.MacroAction> actions) {
        if (isPlaying) {
            LOGGER.warn("Playback already in progress!");
            return;
        }

        if (actions == null || actions.isEmpty()) {
            LOGGER.warn("No actions to play!");
            return;
        }

        isPlaying = true;

        CompletableFuture.runAsync(() -> {
            try {
                playMacroActions(actions);
            } catch (Exception e) {
                LOGGER.error("Error during macro playback", e);
            } finally {
                isPlaying = false;
            }
        });
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void stopPlayback() {
        if (isPlaying) {
            isPlaying = false;
            LOGGER.info("Macro playback stopped");
        }
    }

    private void playMacroActions(List<MacroRecordingSystem.MacroAction> actions) {
        LOGGER.info("Starting macro playback with {} actions", actions.size());

        final long startTime = System.currentTimeMillis();
        final long firstActionTime = actions.getFirst().getTimestamp();

        for (MacroRecordingSystem.MacroAction action : actions) {
            if (!isPlaying) {
                LOGGER.info("Macro playback stopped by user");
                break;
            }

            try {
                waitForActionTiming(action, startTime, firstActionTime);
                executeAction(action);
            } catch (InterruptedException e) {
                LOGGER.info("Macro playback interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("Error executing action: {}", action, e);
            }
        }

        LOGGER.info("Macro playback completed");
    }

    private void waitForActionTiming(MacroRecordingSystem.MacroAction action, long startTime, long firstActionTime)
            throws InterruptedException {
        long relativeTime = action.getTimestamp() - firstActionTime;
        long targetTime = startTime + relativeTime;
        long currentTime = System.currentTimeMillis();

        if (targetTime > currentTime) {
            Thread.sleep(targetTime - currentTime);
        }
    }

    private void executeAction(MacroRecordingSystem.MacroAction action) {
        if (isValidGameState()) {
            return;
        }

        switch (action.getType()) {
            case KEY_PRESS -> simulateKeyPress(extractInteger(action.getData()), true);
            case KEY_RELEASE -> simulateKeyPress(extractInteger(action.getData()), false);
            case KEYBINDING_PRESS -> simulateKeybindingByName((String) action.getData());
            case MOUSE_PRESS -> simulateMouseClick(extractInteger(action.getData()), true);
            case MOUSE_RELEASE -> simulateMouseClick(extractInteger(action.getData()), false);
            case MOUSE_MOVE -> simulateMouseMove(action);
            default -> LOGGER.debug("Action type not implemented for playback: {}", action.getType());
        }
    }

    private boolean isValidGameState() {
        return mc == null || mc.player == null || mc.getWindow() == null;
    }

    private int extractInteger(Object data) {
        return switch (data) {
            case Integer i -> i;
            case Number n -> n.intValue();
            default -> {
                LOGGER.warn("Unexpected data type for integer conversion: {}", data.getClass().getSimpleName());
                yield 0;
            }
        };
    }

    private void simulateMouseMove(MacroRecordingSystem.MacroAction action) {
        if (!(action.getData() instanceof Number) || !(action.getData2() instanceof Number)) {
            LOGGER.warn("Invalid mouse move data");
            return;
        }

        float yaw = ((Number) action.getData()).floatValue();
        float pitch = ((Number) action.getData2()).floatValue();

        LOGGER.debug("Simulating mouse move: yaw={}, pitch={}", yaw, pitch);

        if (mc.player != null) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private void simulateKeyPress(int keyCode, boolean pressed) {
        LOGGER.debug("Simulating key {} {}", keyCode, pressed ? "press" : "release");

        if (isValidGameState() || isGamePausedOrInMenu()) {
            return;
        }

        mc.execute(() -> {
            try {
                if (mc.getWindow().getHandle() == 0) {
                    LOGGER.warn("Invalid window handle, cannot simulate key press");
                    return;
                }

                processKeybindings(keyCode, pressed);
                LOGGER.debug("Successfully simulated key {} {}", keyCode, pressed ? "press" : "release");

            } catch (Exception e) {
                LOGGER.error("Key press simulation failed for key {}: {}", keyCode, e.getMessage(), e);
            }
        });
    }

    private boolean isGamePausedOrInMenu() {
        return mc.isPaused() || mc.currentScreen != null;
    }

    private void processKeybindings(int keyCode, boolean pressed) {
        KeyBinding[] gameKeyBindings = getAllGameKeyBindings();

        for (KeyBinding keyBinding : gameKeyBindings) {
            if (keyBinding != null && keyBinding.matchesKey(keyCode, 0)) {
                if (pressed) {
                    simulateKeybindingPress(keyBinding);
                } else {
                    simulateKeybindingRelease(keyBinding);
                }
                return;
            }
        }

        simulateKeyViaDirectInput(keyCode, pressed);
    }

    private KeyBinding[] getAllGameKeyBindings() {
        KeyBinding[] basicKeys = {
                mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey, mc.options.sneakKey, mc.options.sprintKey, mc.options.attackKey,
                mc.options.useKey, mc.options.pickItemKey, mc.options.inventoryKey, mc.options.chatKey,
                mc.options.playerListKey, mc.options.commandKey, mc.options.screenshotKey,
                mc.options.togglePerspectiveKey, mc.options.smoothCameraKey, mc.options.fullscreenKey,
                mc.options.spectatorOutlinesKey, mc.options.swapHandsKey, mc.options.dropKey
        };

        KeyBinding[] allKeys = Arrays.copyOf(basicKeys, basicKeys.length + mc.options.hotbarKeys.length);
        System.arraycopy(mc.options.hotbarKeys, 0, allKeys, basicKeys.length, mc.options.hotbarKeys.length);

        return allKeys;
    }

    private void simulateKeybindingByName(String keybindingName) {
        LOGGER.debug("Simulating keybinding by name: {}", keybindingName);

        if (isValidGameState() || isGamePausedOrInMenu()) {
            return;
        }

        mc.execute(() -> {
            try {
                KeyBinding keyBinding = findKeybindingByName(keybindingName);
                if (keyBinding != null) {
                    simulateKeybindingPress(keyBinding);

                    mc.execute(() -> {
                        try {
                            simulateKeybindingRelease(keyBinding);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to release keybinding {}: {}", keybindingName, e.getMessage());
                        }
                    });

                    LOGGER.debug("Successfully simulated keybinding: {}", keybindingName);
                } else {
                    LOGGER.warn("Keybinding not found: {}", keybindingName);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to simulate keybinding {}: {}", keybindingName, e.getMessage(), e);
            }
        });
    }

    private KeyBinding findKeybindingByName(String name) {
        if (name == null) {
            return null;
        }

        for (KeyBinding keyBinding : getAllGameKeyBindings()) {
            if (keyBinding != null && name.equals(keyBinding.getTranslationKey())) {
                return keyBinding;
            }
        }

        return null;
    }

    private void simulateKeyViaDirectInput(int keyCode, boolean pressed) {
        try {
            if (mc.keyboard != null) {
                handleDirectKeyAction(keyCode, pressed);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to simulate key via direct input: {}", e.getMessage());
        }
    }

    private void simulateKeybindingPress(KeyBinding keyBinding) {
        try {
            setFieldValue(keyBinding, "timesPressed", 1);
            setFieldValue(keyBinding, "pressed", true);
        } catch (Exception e) {
            LOGGER.warn("Failed to simulate keybinding press: {}", e.getMessage());
        }
    }

    private void simulateKeybindingRelease(KeyBinding keyBinding) {
        try {
            setFieldValue(keyBinding, "pressed", false);
            setFieldValue(keyBinding, "pressTime", 0);
        } catch (Exception e) {
            LOGGER.warn("Failed to simulate keybinding release: {}", e.getMessage());
        }
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        if (value instanceof Integer) {
            field.setInt(target, (Integer) value);
        } else if (value instanceof Boolean) {
            field.setBoolean(target, (Boolean) value);
        } else {
            field.set(target, value);
        }
    }

    private void handleDirectKeyAction(int keyCode, boolean pressed) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> handleMovementKey(mc.options.forwardKey, pressed);
            case GLFW.GLFW_KEY_S -> handleMovementKey(mc.options.backKey, pressed);
            case GLFW.GLFW_KEY_A -> handleMovementKey(mc.options.leftKey, pressed);
            case GLFW.GLFW_KEY_D -> handleMovementKey(mc.options.rightKey, pressed);
            case GLFW.GLFW_KEY_SPACE -> handleMovementKey(mc.options.jumpKey, pressed);
            case GLFW.GLFW_KEY_LEFT_SHIFT -> handleMovementKey(mc.options.sneakKey, pressed);
            case GLFW.GLFW_KEY_LEFT_CONTROL -> handleMovementKey(mc.options.sprintKey, pressed);
            case GLFW.GLFW_KEY_E -> { if (pressed) handleActionKey(mc.options.inventoryKey); }
            case GLFW.GLFW_KEY_T -> { if (pressed) handleActionKey(mc.options.chatKey); }
            case GLFW.GLFW_KEY_TAB -> handleMovementKey(mc.options.playerListKey, pressed);
            case GLFW.GLFW_KEY_F5 -> { if (pressed) handleActionKey(mc.options.togglePerspectiveKey); }
            case GLFW.GLFW_KEY_F11 -> { if (pressed) handleActionKey(mc.options.fullscreenKey); }
            case GLFW.GLFW_KEY_Q -> { if (pressed) handleActionKey(mc.options.dropKey); }
            case GLFW.GLFW_KEY_F -> { if (pressed) handleActionKey(mc.options.swapHandsKey); }
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
                 GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9 -> {
                if (pressed) {
                    int index = keyCode - GLFW.GLFW_KEY_1;
                    if (index >= 0 && index < mc.options.hotbarKeys.length) {
                        handleActionKey(mc.options.hotbarKeys[index]);
                    }
                }
            }
            default -> LOGGER.debug("Unhandled key code: {}", keyCode);
        }
    }

    private void handleMovementKey(KeyBinding keyBinding, boolean pressed) {
        try {
            setFieldValue(keyBinding, "pressed", pressed);
            if (pressed) {
                setFieldValue(keyBinding, "pressTime", 1);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to handle movement key: {}", e.getMessage());
        }
    }

    private void handleActionKey(KeyBinding keyBinding) {
        try {
            setFieldValue(keyBinding, "pressTime", 1);

            mc.execute(() -> {
                try {
                    setFieldValue(keyBinding, "pressTime", 0);
                } catch (Exception e) {
                    LOGGER.warn("Failed to release action key: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to handle action key: {}", e.getMessage());
        }
    }

    private void simulateMouseClick(int button, boolean pressed) {
        LOGGER.debug("Simulating mouse button {} {}", button, pressed ? "press" : "release");

        if (isValidGameState() || isGamePausedOrInMenu()) {
            return;
        }

        mc.execute(() -> {
            try {
                long windowHandle = mc.getWindow().getHandle();
                if (windowHandle == 0) {
                    LOGGER.warn("Invalid window handle, cannot simulate mouse click");
                    return;
                }

                boolean wasGrabbed = mc.mouse.isCursorLocked();

                Method onMouseButtonMethod = mc.mouse.getClass().getDeclaredMethod(
                        "onMouseButton", long.class, int.class, int.class, int.class
                );
                onMouseButtonMethod.setAccessible(true);
                onMouseButtonMethod.invoke(mc.mouse, windowHandle, button,
                        pressed ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, 0);

                if (wasGrabbed && !mc.mouse.isCursorLocked()) {
                    mc.mouse.lockCursor();
                }

                LOGGER.debug("Successfully simulated mouse button {} {}", button, pressed ? "press" : "release");

            } catch (Exception e) {
                LOGGER.error("Mouse click simulation failed for button {}: {}", button, e.getMessage(), e);
            }
        });
    }
}