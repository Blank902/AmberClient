package com.amberclient.modules.world.MacroRecorder;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MacroPlaybackSystem {
    private static final Logger LOGGER = LogManager.getLogger("MacroPlaybackSystem");
    private static MacroPlaybackSystem instance;

    private final MinecraftClient client;
    private boolean isPlaying = false;

    private MacroPlaybackSystem() {
        this.client = MinecraftClient.getInstance();
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
        if (client == null || client.player == null) {
            return;
        }

        switch (action.getType()) {
            case KEY_PRESS:
                simulateKeyPress((Integer) action.getData(), true);
                break;

            case KEY_RELEASE:
                simulateKeyPress((Integer) action.getData(), false);
                break;

            case KEYBINDING_PRESS:
                simulateKeybinding((String) action.getData());
                break;

            case MOUSE_PRESS:
                simulateMouseClick((Integer) action.getData(), true);
                break;

            case MOUSE_RELEASE:
                simulateMouseClick((Integer) action.getData(), false);
                break;

            case MOUSE_MOVE:
                if (action.getData() instanceof Float && action.getData2() instanceof Float) {
                    float yaw = (Float) action.getData();
                    float pitch = (Float) action.getData2();
                    simulateMouseMove(yaw, pitch);
                }
                break;

            default:
                LOGGER.debug("Action type not implemented for playback: {}", action.getType());
                break;
        }
    }

    private void simulateKeyPress(int keyCode, boolean pressed) {
        LOGGER.debug("Simulating key {} {}", keyCode, pressed ? "press" : "release");
        // TODO: Implement real-life simulation with Minecraft's input system
    }

    private void simulateKeybinding(String keybindingName) {
        LOGGER.debug("Simulating keybinding: {}", keybindingName);
        // TODO: Implement keybinding simulation
    }

    private void simulateMouseClick(int button, boolean pressed) {
        LOGGER.debug("Simulating mouse button {} {}", button, pressed ? "press" : "release");
        // TODO: Implement click simulation
    }

    private void simulateMouseMove(float yaw, float pitch) {
        LOGGER.debug("Simulating mouse move: yaw={}, pitch={}", yaw, pitch);
        if (client.player != null) {
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}