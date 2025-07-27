package com.nexusclient.utils.module;

import com.nexusclient.events.core.EventManager;
import com.nexusclient.modules.combat.*;
import com.nexusclient.modules.miscellaneous.DiscordRPC;
import com.nexusclient.modules.miscellaneous.ActiveMods;
import com.nexusclient.modules.miscellaneous.Transparency;
import com.nexusclient.modules.minigames.MMFinder;
import com.nexusclient.modules.movement.AutoClutch;
import com.nexusclient.modules.movement.NoFall;
import com.nexusclient.modules.movement.SafeWalk;
import com.nexusclient.modules.player.AntiHunger;
import com.nexusclient.modules.player.FastBreak;
import com.nexusclient.modules.player.FastPlace;
import com.nexusclient.modules.render.EntityESP;
import com.nexusclient.modules.render.Fullbright;
import com.nexusclient.modules.render.NoHurtCam;
import com.nexusclient.modules.render.xray.Xray;
import com.nexusclient.modules.world.MacroRecorder.MacroRecorder;
import com.nexusclient.utils.input.keybinds.KeybindConfigManager;
import com.nexusclient.utils.input.keybinds.CustomKeybindManager;
import com.nexusclient.utils.input.keybinds.KeybindsManager;
import com.nexusclient.utils.minecraft.MinecraftUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();
    private boolean keybindsInitialized = false;

    private ModuleManager() {
        KeybindsManager.INSTANCE.initialize();
        CustomKeybindManager.INSTANCE.initialize();

        // Register modules
        registerModule(new AutoClicker());
        registerModule(new Hitbox());
        registerModule(new KillAura());
        registerModule(new ActiveMods());
        registerModule(new Transparency());
        registerModule(new DiscordRPC());
        registerModule(new NoFall());
        registerModule(new SafeWalk());
        registerModule(new FastBreak());
        registerModule(new FastPlace());
        registerModule(new Xray());
        registerModule(new EntityESP());
        registerModule(new Fullbright());
        registerModule(new MMFinder());
        registerModule(new AutoClutch());
        registerModule(new Velocity());
        registerModule(new NoHurtCam());
        registerModule(new AimAssist());
        registerModule(new MacroRecorder());
        registerModule(new AutoPotion());
        registerModule(new AntiHunger());
        registerModule(new FakeLag());

        for (Module module : modules) {
            EventManager.getInstance().register(module);
        }
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void initializeKeybinds() {
        if (keybindsInitialized) return;

        System.out.println("[ModuleManager] Initializing keybind system...");

        loadModuleKeybinds();

        keybindsInitialized = true;
        System.out.println("[ModuleManager] Keybind system initialized!");
    }

    private void loadModuleKeybinds() {
        Map<String, String> savedKeybinds = KeybindConfigManager.INSTANCE.getAllModuleKeybinds();

        System.out.println("[ModuleManager] Loading " + savedKeybinds.size() + " saved keybinds...");

        for (Map.Entry<String, String> entry : savedKeybinds.entrySet()) {
            String moduleName = entry.getKey();
            String keyName = entry.getValue();

            Module module = modules.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                    .findFirst()
                    .orElse(null);

            if (module != null) {
                bindKeyToModuleInternal(module, keyName, false);
                System.out.println("[ModuleManager] Restored keybind: " + keyName + " -> " + moduleName);
            } else {
                System.out.println("[ModuleManager] Warning: Module '" + moduleName + "' not found for saved keybind");
            }
        }
    }

    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        modules.stream()
                .filter(Module::isEnabled)
                .forEach(module -> {
                    try {
                        module.onTick();
                    } catch (Exception e) {
                        System.err.println("Error in " + module.getName() + ": " + e.getMessage());
                    }
                });

        CustomKeybindManager.INSTANCE.tick();
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void toggleModule(Module module) {
        if (module != null) module.toggle();
    }

    public void registerModule(Module module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
    }

    public void handleKeyInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || MinecraftUtils.isChatOpen()) {
            return;
        }

        for (Map.Entry<String, KeyBinding> entry : KeybindsManager.INSTANCE.getKeyBindings().entrySet()) {
            KeyBinding keyBinding = entry.getValue();

            while (keyBinding.wasPressed()) {
                modules.stream()
                        .filter(m -> m.getKeyBinding() == keyBinding)
                        .findFirst().ifPresent(Module::toggle);
            }
        }
    }

    public void bindKeyToModule(Module module, String keyName) {
        bindKeyToModuleInternal(module, keyName, true);
    }

    private void bindKeyToModuleInternal(Module module, String keyName, boolean saveToConfig) {
        int keyCode = KeybindsManager.INSTANCE.getKeyCodeFromName(keyName);

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            System.err.println("[ModuleManager] Invalid key name: " + keyName);
            return;
        }

        unbindModuleInternal(module, false);

        String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");

        CustomKeybindManager.INSTANCE.bindKey(
                keyCode,
                actionId,
                "Toggle " + module.getName(),
                true,
                new Runnable() {
                    @Override
                    public void run() {
                        // Check if chat is open before toggling
                        if (MinecraftUtils.isChatOpen()) {
                            return;
                        }

                        module.toggle();
                        System.out.println("Toggled " + module.getName() + " via custom keybind");
                    }
                }
        );

        module.setCustomKeyCode(keyCode);

        if (saveToConfig) {
            KeybindConfigManager.INSTANCE.setModuleKeybind(module.getName(), keyName);
        }

        System.out.println("[ModuleManager] Successfully bound " + CustomKeybindManager.INSTANCE.getKeyName(keyCode) +
                " to " + module.getName());
    }

    public void unbindModule(Module module) {
        unbindModuleInternal(module, true);
    }

    private void unbindModuleInternal(Module module, boolean removeFromConfig) {
        int currentKeyCode = module.getCustomKeyCode();
        if (currentKeyCode != -1) {
            String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");
            CustomKeybindManager.INSTANCE.unbindAction(currentKeyCode, actionId);
            module.setCustomKeyCode(-1);

            if (removeFromConfig) {
                KeybindConfigManager.INSTANCE.removeModuleKeybind(module.getName());
            }

            System.out.println("[ModuleManager] Unbound " + module.getName() + " from key");
        }
    }

    public String getModuleKeybind(Module module) {
        return KeybindConfigManager.INSTANCE.getModuleKeybind(module.getName());
    }

    public boolean hasModuleKeybind(Module module) {
        return getModuleKeybind(module) != null;
    }

    public Map<String, String> getAllModuleKeybinds() {
        return KeybindConfigManager.INSTANCE.getAllModuleKeybinds();
    }

    public String getModuleKeyName(Module module) {
        int keyCode = module.getCustomKeyCode();
        if (keyCode == -1) {
            return "Not bound";
        }
        return CustomKeybindManager.INSTANCE.getKeyName(keyCode);
    }

    public void listAllKeybinds() {
        System.out.println("=== Custom Keybinds ===");
        Map<Integer, List<CustomKeybindManager.KeybindAction>> bindings = CustomKeybindManager.INSTANCE.getAllBindings();

        if (bindings.isEmpty()) {
            System.out.println("No custom keybinds registered.");
            return;
        }

        bindings.forEach((keyCode, actions) -> {
            String keyName = CustomKeybindManager.INSTANCE.getKeyName(keyCode);
            System.out.println(keyName + ":");
            actions.forEach(action -> {
                System.out.println("  - " + action.getDescription());
            });
        });
    }

    public Module findModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}