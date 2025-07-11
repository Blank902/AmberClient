package com.amberclient.modules.world.MacroRecorder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.List;

public class MacroPlaybackSystem {
    private static final Logger LOGGER = LogManager.getLogger("amberclient-macroplaybacksystem");
    private static MacroPlaybackSystem instance;

    private final MinecraftClient mc;
    private boolean isPlaying = false;

    private MacroPlaybackSystem() {
        this.mc = MinecraftClient.getInstance();
    }

    public static MacroPlaybackSystem getInstance() {
        if (instance == null) {
            instance = new MacroPlaybackSystem();
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

        Thread playbackThread = new Thread(() -> {
            try {
                playMacroActions(actions);
            } catch (Exception e) {
                LOGGER.error("Error during macro playback", e);
            } finally {
                isPlaying = false;
            }
        });

        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void playMacroActions(List<MacroRecordingSystem.MacroAction> actions) {
        LOGGER.info("Starting macro playback with {} actions", actions.size());

        long startTime = System.currentTimeMillis();
        long firstActionTime = actions.getFirst().getTimestamp();

        for (MacroRecordingSystem.MacroAction action : actions) {
            try {
                long relativeTime = action.getTimestamp() - firstActionTime;
                long targetTime = startTime + relativeTime;
                long currentTime = System.currentTimeMillis();

                if (targetTime > currentTime) {
                    Thread.sleep(targetTime - currentTime);
                }

                executeAction(action);

            } catch (InterruptedException e) {
                LOGGER.info("Macro playback interrupted");
                break;
            } catch (Exception e) {
                LOGGER.error("Error executing action: {}", action, e);
            }
        }

        LOGGER.info("Macro playback completed");
    }

    private void executeAction(MacroRecordingSystem.MacroAction action) {
        if (mc == null || mc.player == null) {
            return;
        }

        switch (action.getType()) {
            case KEY_PRESS:
                simulateKeyPress(getIntegerFromData(action.getData()), true);
                break;

            case KEY_RELEASE:
                simulateKeyPress(getIntegerFromData(action.getData()), false);
                break;

            case KEYBINDING_PRESS:
                simulateKeybindingByName((String) action.getData());
                break;

            case MOUSE_PRESS:
                simulateMouseClick(getIntegerFromData(action.getData()), true);
                break;

            case MOUSE_RELEASE:
                simulateMouseClick(getIntegerFromData(action.getData()), false);
                break;

            case MOUSE_MOVE:
                if (action.getData() instanceof Number && action.getData2() instanceof Number) {
                    float yaw = ((Number) action.getData()).floatValue();
                    float pitch = ((Number) action.getData2()).floatValue();
                    simulateMouseMove(yaw, pitch);
                }
                break;

            default:
                LOGGER.debug("Action type not implemented for playback: {}", action.getType());
                break;
        }
    }

    private int getIntegerFromData(Object data) {
        switch (data) {
            case Integer i -> {
                return i;
            }
            case Double v -> {
                return v.intValue();
            }
            case Number number -> {
                return number.intValue();
            }
            default -> {
                LOGGER.warn("Unexpected data type for integer conversion: {}", data.getClass().getSimpleName());
                return 0;
            }
        }
    }

    private void simulateKeyPress(int keyCode, boolean pressed) {
        LOGGER.debug("Simulating key {} {}", keyCode, pressed ? "press" : "release");

        if (mc == null) {
            LOGGER.warn("MinecraftClient is null, cannot simulate key press");
            return;
        }

        if (mc.getWindow() == null) {
            LOGGER.warn("Window is null, cannot simulate key press");
            return;
        }

        if (mc.player == null) {
            LOGGER.warn("Player is null, cannot simulate key press");
            return;
        }

        if (mc.isPaused() || mc.currentScreen != null) {
            LOGGER.debug("Game is paused or in menu, skipping key press simulation");
            return;
        }

        mc.execute(() -> {
            try {
                long windowHandle = mc.getWindow().getHandle();

                if (windowHandle == 0) {
                    LOGGER.warn("Invalid window handle, cannot simulate key press");
                    return;
                }

                simulateKeybindings(keyCode, pressed);

                LOGGER.debug("Successfully simulated key {} {}", keyCode, pressed ? "press" : "release");

            } catch (Exception e) {
                LOGGER.error("Key press simulation failed for key {}: {}", keyCode, e.getMessage(), e);
            }
        });
    }

    private void simulateKeybindings(int keyCode, boolean pressed) {
        try {
            KeyBinding[] gameKeyBindings = {
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey,
                    mc.options.sneakKey,
                    mc.options.sprintKey,
                    mc.options.attackKey,
                    mc.options.useKey,
                    mc.options.pickItemKey,
                    mc.options.inventoryKey,
                    mc.options.chatKey,
                    mc.options.playerListKey,
                    mc.options.commandKey,
                    mc.options.screenshotKey,
                    mc.options.togglePerspectiveKey,
                    mc.options.smoothCameraKey,
                    mc.options.fullscreenKey,
                    mc.options.spectatorOutlinesKey,
                    mc.options.swapHandsKey,
                    mc.options.dropKey,
                    mc.options.hotbarKeys[0],
                    mc.options.hotbarKeys[1],
                    mc.options.hotbarKeys[2],
                    mc.options.hotbarKeys[3],
                    mc.options.hotbarKeys[4],
                    mc.options.hotbarKeys[5],
                    mc.options.hotbarKeys[6],
                    mc.options.hotbarKeys[7],
                    mc.options.hotbarKeys[8]
            };

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

        } catch (Exception e) {
            LOGGER.warn("Failed to simulate key via keybindings: {}", e.getMessage());
        }
    }

    private void simulateKeybindingByName(String keybindingName) {
        LOGGER.debug("Simulating keybinding by name: {}", keybindingName);

        if (mc == null || mc.player == null) {
            LOGGER.warn("Cannot simulate keybinding: mc or player is null");
            return;
        }

        if (mc.isPaused() || mc.currentScreen != null) {
            LOGGER.debug("Game is paused or in menu, skipping keybinding simulation");
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
        if (name == null) return null;

        KeyBinding[] allKeybindings = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sneakKey,
                mc.options.sprintKey,
                mc.options.attackKey,
                mc.options.useKey,
                mc.options.pickItemKey,
                mc.options.inventoryKey,
                mc.options.chatKey,
                mc.options.playerListKey,
                mc.options.commandKey,
                mc.options.screenshotKey,
                mc.options.togglePerspectiveKey,
                mc.options.smoothCameraKey,
                mc.options.fullscreenKey,
                mc.options.spectatorOutlinesKey,
                mc.options.swapHandsKey,
                mc.options.dropKey
        };

        for (KeyBinding keyBinding : allKeybindings) {
            if (keyBinding != null && name.equals(keyBinding.getTranslationKey())) {
                return keyBinding;
            }
        }

        // Chercher dans les touches de hotbar
        for (int i = 0; i < mc.options.hotbarKeys.length; i++) {
            if (mc.options.hotbarKeys[i] != null && name.equals(mc.options.hotbarKeys[i].getTranslationKey())) {
                return mc.options.hotbarKeys[i];
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
            java.lang.reflect.Field timesPressedField = KeyBinding.class.getDeclaredField("timesPressed");
            timesPressedField.setAccessible(true);
            timesPressedField.setInt(keyBinding, 1);

            java.lang.reflect.Field pressedField = KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.setBoolean(keyBinding, true);
        } catch (Exception e) {
            LOGGER.warn("Failed to simulate keybinding press: {}", e.getMessage());
        }
    }

    private void simulateKeybindingRelease(KeyBinding keyBinding) {
        try {
            java.lang.reflect.Field pressedField = KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.setBoolean(keyBinding, false);

            java.lang.reflect.Field pressTimeField = KeyBinding.class.getDeclaredField("pressTime");
            pressTimeField.setAccessible(true);
            pressTimeField.setInt(keyBinding, 0);

        } catch (Exception e) {
            LOGGER.warn("Failed to simulate keybinding release: {}", e.getMessage());
        }
    }

    private void handleDirectKeyAction(int keyCode, boolean pressed) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W:
                handleMovementKey(mc.options.forwardKey, pressed);
                break;
            case GLFW.GLFW_KEY_S:
                handleMovementKey(mc.options.backKey, pressed);
                break;
            case GLFW.GLFW_KEY_A:
                handleMovementKey(mc.options.leftKey, pressed);
                break;
            case GLFW.GLFW_KEY_D:
                handleMovementKey(mc.options.rightKey, pressed);
                break;
            case GLFW.GLFW_KEY_SPACE:
                handleMovementKey(mc.options.jumpKey, pressed);
                break;
            case GLFW.GLFW_KEY_LEFT_SHIFT:
                handleMovementKey(mc.options.sneakKey, pressed);
                break;
            case GLFW.GLFW_KEY_LEFT_CONTROL:
                handleMovementKey(mc.options.sprintKey, pressed);
                break;
            case GLFW.GLFW_KEY_E:
                if (pressed) {
                    handleActionKey(mc.options.inventoryKey);
                }
                break;
            case GLFW.GLFW_KEY_T:
                if (pressed) {
                    handleActionKey(mc.options.chatKey);
                }
                break;
            case GLFW.GLFW_KEY_TAB:
                handleMovementKey(mc.options.playerListKey, pressed);
                break;
            case GLFW.GLFW_KEY_F5:
                if (pressed) {
                    handleActionKey(mc.options.togglePerspectiveKey);
                }
                break;
            case GLFW.GLFW_KEY_F11:
                if (pressed) {
                    handleActionKey(mc.options.fullscreenKey);
                }
                break;
            case GLFW.GLFW_KEY_Q:
                if (pressed) {
                    handleActionKey(mc.options.dropKey);
                }
                break;
            case GLFW.GLFW_KEY_F:
                if (pressed) {
                    handleActionKey(mc.options.swapHandsKey);
                }
                break;
            case GLFW.GLFW_KEY_1:
                if (pressed) handleActionKey(mc.options.hotbarKeys[0]);
                break;
            case GLFW.GLFW_KEY_2:
                if (pressed) handleActionKey(mc.options.hotbarKeys[1]);
                break;
            case GLFW.GLFW_KEY_3:
                if (pressed) handleActionKey(mc.options.hotbarKeys[2]);
                break;
            case GLFW.GLFW_KEY_4:
                if (pressed) handleActionKey(mc.options.hotbarKeys[3]);
                break;
            case GLFW.GLFW_KEY_5:
                if (pressed) handleActionKey(mc.options.hotbarKeys[4]);
                break;
            case GLFW.GLFW_KEY_6:
                if (pressed) handleActionKey(mc.options.hotbarKeys[5]);
                break;
            case GLFW.GLFW_KEY_7:
                if (pressed) handleActionKey(mc.options.hotbarKeys[6]);
                break;
            case GLFW.GLFW_KEY_8:
                if (pressed) handleActionKey(mc.options.hotbarKeys[7]);
                break;
            case GLFW.GLFW_KEY_9:
                if (pressed) handleActionKey(mc.options.hotbarKeys[8]);
                break;
            default:
                LOGGER.debug("Unhandled key code: {}", keyCode);
                break;
        }
    }

    private void handleMovementKey(KeyBinding keyBinding, boolean pressed) {
        try {
            java.lang.reflect.Field pressedField = KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.setBoolean(keyBinding, pressed);

            if (pressed) {
                java.lang.reflect.Field pressTimeField = KeyBinding.class.getDeclaredField("pressTime");
                pressTimeField.setAccessible(true);
                pressTimeField.setInt(keyBinding, 1);
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to handle movement key: {}", e.getMessage());
        }
    }

    private void handleActionKey(KeyBinding keyBinding) {
        try {
            java.lang.reflect.Field pressTimeField = KeyBinding.class.getDeclaredField("pressTime");
            pressTimeField.setAccessible(true);
            pressTimeField.setInt(keyBinding, 1);

            mc.execute(() -> {
                try {
                    pressTimeField.setInt(keyBinding, 0);
                } catch (Exception e) {
                    LOGGER.warn("Failed to release action key: {}", e.getMessage());
                }
            });

        } catch (Exception e) {
            LOGGER.warn("Failed to handle action key: {}", e.getMessage());
        }
    }

    private void simulateMouseClick(int button, boolean pressed) {
        LOGGER.debug("Enhanced mouse button simulation {} {}", button, pressed ? "press" : "release");

        // Safety checks
        if (mc == null) {
            LOGGER.warn("MinecraftClient is null, cannot simulate mouse click");
            return;
        }

        if (mc.getWindow() == null) {
            LOGGER.warn("Window is null, cannot simulate mouse click");
            return;
        }

        if (mc.player == null) {
            LOGGER.warn("Player is null, cannot simulate mouse click");
            return;
        }

        if (mc.isPaused() || mc.currentScreen != null) {
            LOGGER.debug("Game is paused or in menu, skipping mouse click simulation");
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
                LOGGER.error("Enhanced mouse click simulation failed for button {}: {}", button, e.getMessage(), e);
            }
        });
    }

    private void simulateMouseMove(float yaw, float pitch) {
        LOGGER.debug("Simulating mouse move: yaw={}, pitch={}", yaw, pitch);
        if (mc.player != null) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }
}