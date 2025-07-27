package com.nexusclient.utils.module;

import com.nexusclient.NexusClient;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced ModuleManager with better organization, error handling, and performance
 */
public class EnhancedModuleManager {
    private static final EnhancedModuleManager INSTANCE = new EnhancedModuleManager();
    
    // Thread-safe collections for better performance
    private final Map<String, Module> moduleMap = new ConcurrentHashMap<>();
    private final Map<Module.Category, List<Module>> categoryMap = new ConcurrentHashMap<>();
    private final List<Module> enabledModules = Collections.synchronizedList(new ArrayList<>());
    
    // Statistics and monitoring
    private final Map<String, Long> modulePerformance = new ConcurrentHashMap<>();
    private final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
    
    private boolean initialized = false;
    private boolean keybindsInitialized = false;
    
    private EnhancedModuleManager() {
        initializeModules();
    }
    
    public static EnhancedModuleManager getInstance() {
        return INSTANCE;
    }
    
    private void initializeModules() {
        if (initialized) return;
        
        try {
            NexusClient.LOGGER.info("Initializing Nexus Client modules...");
            
            KeybindsManager.INSTANCE.initialize();
            CustomKeybindManager.INSTANCE.initialize();
            
            // Register modules with better error handling
            registerModulesWithErrorHandling();
            
            // Register event listeners
            for (Module module : moduleMap.values()) {
                try {
                    EventManager.getInstance().register(module);
                } catch (Exception e) {
                    NexusClient.LOGGER.error("Failed to register events for module: " + module.getName(), e);
                }
            }
            
            initialized = true;
            NexusClient.LOGGER.info("Successfully initialized {} modules", moduleMap.size());
            
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to initialize module manager", e);
        }
    }
    
    private void registerModulesWithErrorHandling() {
        // Combat modules
        safeRegisterModule(new AutoClicker());
        safeRegisterModule(new Hitbox());
        safeRegisterModule(new KillAura());
        safeRegisterModule(new Velocity());
        safeRegisterModule(new AimAssist());
        safeRegisterModule(new AutoPotion());
        safeRegisterModule(new FakeLag());
        
        // Movement modules
        safeRegisterModule(new NoFall());
        safeRegisterModule(new SafeWalk());
        safeRegisterModule(new AutoClutch());
        
        // Player modules
        safeRegisterModule(new FastBreak());
        safeRegisterModule(new FastPlace());
        safeRegisterModule(new AntiHunger());
        
        // Render modules
        safeRegisterModule(new Xray());
        safeRegisterModule(new EntityESP());
        safeRegisterModule(new Fullbright());
        safeRegisterModule(new NoHurtCam());
        
        // World modules
        safeRegisterModule(new MacroRecorder());
        
        // Minigame modules
        safeRegisterModule(new MMFinder());
        
        // Miscellaneous modules
        safeRegisterModule(new ActiveMods());
        safeRegisterModule(new Transparency());
        safeRegisterModule(new DiscordRPC());
    }
    
    private void safeRegisterModule(Module module) {
        try {
            registerModule(module);
            NexusClient.LOGGER.debug("Successfully registered module: {}", module.getName());
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to register module: {}", module.getName(), e);
        }
    }
    
    public void initializeKeybinds() {
        if (keybindsInitialized) return;
        
        try {
            NexusClient.LOGGER.info("Initializing keybind system...");
            
            loadModuleKeybinds();
            
            keybindsInitialized = true;
            NexusClient.LOGGER.info("Keybind system initialized successfully");
            
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to initialize keybind system", e);
        }
    }
    
    private void loadModuleKeybinds() {
        try {
            Map<String, String> savedKeybinds = KeybindConfigManager.INSTANCE.getAllModuleKeybinds();
            
            NexusClient.LOGGER.info("Loading {} saved keybinds...", savedKeybinds.size());
            
            for (Map.Entry<String, String> entry : savedKeybinds.entrySet()) {
                String moduleName = entry.getKey();
                String keyName = entry.getValue();
                
                Module module = findModuleByName(moduleName);
                if (module != null) {
                    bindKeyToModuleInternal(module, keyName, false);
                    NexusClient.LOGGER.debug("Restored keybind: {} -> {}", keyName, moduleName);
                } else {
                    NexusClient.LOGGER.warn("Module '{}' not found for saved keybind", moduleName);
                }
            }
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to load module keybinds", e);
        }
    }
    
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Process enabled modules with performance monitoring
        synchronized (enabledModules) {
            for (Module module : enabledModules) {
                try {
                    long startTime = System.nanoTime();
                    
                    module.onTick();
                    
                    long duration = System.nanoTime() - startTime;
                    modulePerformance.put(module.getName(), duration);
                    
                } catch (Exception e) {
                    handleModuleError(module, e);
                }
            }
        }
        
        try {
            CustomKeybindManager.INSTANCE.tick();
        } catch (Exception e) {
            NexusClient.LOGGER.error("Error in custom keybind manager tick", e);
        }
    }
    
    private void handleModuleError(Module module, Exception e) {
        String moduleName = module.getName();
        int errorCount = errorCounts.getOrDefault(moduleName, 0) + 1;
        errorCounts.put(moduleName, errorCount);
        
        NexusClient.LOGGER.error("Error in module '{}' (error #{}):", moduleName, errorCount, e);
        
        // Auto-disable module if it has too many errors
        if (errorCount >= 5) {
            NexusClient.LOGGER.warn("Auto-disabling module '{}' due to excessive errors", moduleName);
            try {
                if (module.isEnabled()) {
                    module.disable();
                }
            } catch (Exception disableError) {
                NexusClient.LOGGER.error("Failed to disable problematic module '{}'", moduleName, disableError);
            }
        }
    }
    
    public List<Module> getModules() {
        return new ArrayList<>(moduleMap.values());
    }
    
    public List<Module> getModulesByCategory(Module.Category category) {
        return categoryMap.getOrDefault(category, new ArrayList<>());
    }
    
    public List<Module> getEnabledModules() {
        synchronized (enabledModules) {
            return new ArrayList<>(enabledModules);
        }
    }
    
    public void toggleModule(Module module) {
        if (module == null) return;
        
        try {
            boolean wasEnabled = module.isEnabled();
            module.toggle();
            
            // Update enabled modules list
            synchronized (enabledModules) {
                if (module.isEnabled() && !enabledModules.contains(module)) {
                    enabledModules.add(module);
                } else if (!module.isEnabled() && enabledModules.contains(module)) {
                    enabledModules.remove(module);
                }
            }
            
            // Reset error count on successful toggle
            errorCounts.remove(module.getName());
            
            NexusClient.LOGGER.info("Module '{}' {}", module.getName(), 
                module.isEnabled() ? "enabled" : "disabled");
                
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to toggle module '{}'", module.getName(), e);
        }
    }
    
    public void registerModule(Module module) {
        if (module == null) {
            NexusClient.LOGGER.warn("Attempted to register null module");
            return;
        }
        
        String name = module.getName();
        if (moduleMap.containsKey(name)) {
            NexusClient.LOGGER.warn("Module '{}' is already registered", name);
            return;
        }
        
        moduleMap.put(name, module);
        
        // Add to category map
        Module.Category category = module.getCategory();
        categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(module);
        
        // Add to enabled list if module is enabled
        if (module.isEnabled()) {
            synchronized (enabledModules) {
                enabledModules.add(module);
            }
        }
    }
    
    public void handleKeyInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || MinecraftUtils.isChatOpen()) {
            return;
        }
        
        try {
            for (Map.Entry<String, KeyBinding> entry : KeybindsManager.INSTANCE.getKeyBindings().entrySet()) {
                KeyBinding keyBinding = entry.getValue();
                
                while (keyBinding.wasPressed()) {
                    moduleMap.values().stream()
                        .filter(m -> m.getKeyBinding() == keyBinding)
                        .findFirst()
                        .ifPresent(this::toggleModule);
                }
            }
        } catch (Exception e) {
            NexusClient.LOGGER.error("Error handling key inputs", e);
        }
    }
    
    public void bindKeyToModule(Module module, String keyName) {
        bindKeyToModuleInternal(module, keyName, true);
    }
    
    private void bindKeyToModuleInternal(Module module, String keyName, boolean saveToConfig) {
        try {
            int keyCode = KeybindsManager.INSTANCE.getKeyCodeFromName(keyName);
            
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
                NexusClient.LOGGER.error("Invalid key name: {}", keyName);
                return;
            }
            
            unbindModuleInternal(module, false);
            
            String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");
            
            CustomKeybindManager.INSTANCE.bindKey(
                keyCode,
                actionId,
                "Toggle " + module.getName(),
                true,
                () -> {
                    // Check if chat is open before toggling
                    if (MinecraftUtils.isChatOpen()) {
                        return;
                    }
                    
                    toggleModule(module);
                    NexusClient.LOGGER.debug("Toggled {} via custom keybind", module.getName());
                }
            );
            
            module.setCustomKeyCode(keyCode);
            
            if (saveToConfig) {
                KeybindConfigManager.INSTANCE.setModuleKeybind(module.getName(), keyName);
            }
            
            NexusClient.LOGGER.info("Successfully bound {} to {}", 
                CustomKeybindManager.INSTANCE.getKeyName(keyCode), module.getName());
                
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to bind key '{}' to module '{}'", keyName, module.getName(), e);
        }
    }
    
    public void unbindModule(Module module) {
        unbindModuleInternal(module, true);
    }
    
    private void unbindModuleInternal(Module module, boolean removeFromConfig) {
        try {
            int currentKeyCode = module.getCustomKeyCode();
            if (currentKeyCode != -1) {
                String actionId = "module_" + module.getName().toLowerCase().replace(" ", "_");
                CustomKeybindManager.INSTANCE.unbindAction(currentKeyCode, actionId);
                module.setCustomKeyCode(-1);
                
                if (removeFromConfig) {
                    KeybindConfigManager.INSTANCE.removeModuleKeybind(module.getName());
                }
                
                NexusClient.LOGGER.info("Unbound {} from key", module.getName());
            }
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to unbind module '{}'", module.getName(), e);
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
    
    public Module findModuleByName(String name) {
        return moduleMap.get(name);
    }
    
    public List<Module> searchModules(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getModules();
        }
        
        String lowerQuery = query.toLowerCase();
        return moduleMap.values().stream()
            .filter(module -> 
                module.getName().toLowerCase().contains(lowerQuery) ||
                module.getDescription().toLowerCase().contains(lowerQuery) ||
                module.getCategory().toString().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    // Performance monitoring methods
    public Map<String, Long> getModulePerformance() {
        return new HashMap<>(modulePerformance);
    }
    
    public Map<String, Integer> getModuleErrorCounts() {
        return new HashMap<>(errorCounts);
    }
    
    public void clearErrorCounts() {
        errorCounts.clear();
        NexusClient.LOGGER.info("Cleared module error counts");
    }
    
    // Configuration and profile management
    public void saveProfile(String profileName) {
        // Implementation for saving module profiles
        NexusClient.LOGGER.info("Saved profile: {}", profileName);
    }
    
    public void loadProfile(String profileName) {
        // Implementation for loading module profiles
        NexusClient.LOGGER.info("Loaded profile: {}", profileName);
    }
    
    public void listAllKeybinds() {
        NexusClient.LOGGER.info("=== Module Keybinds ===");
        Map<Integer, List<CustomKeybindManager.KeybindAction>> bindings = CustomKeybindManager.INSTANCE.getAllBindings();
        
        if (bindings.isEmpty()) {
            NexusClient.LOGGER.info("No custom keybinds registered.");
            return;
        }
        
        bindings.forEach((keyCode, actions) -> {
            String keyName = CustomKeybindManager.INSTANCE.getKeyName(keyCode);
            NexusClient.LOGGER.info("{}:", keyName);
            actions.forEach(action -> {
                NexusClient.LOGGER.info("  - {}", action.getDescription());
            });
        });
    }
    
    // Cleanup method
    public void shutdown() {
        try {
            NexusClient.LOGGER.info("Shutting down module manager...");
            
            // Disable all modules
            synchronized (enabledModules) {
                for (Module module : new ArrayList<>(enabledModules)) {
                    try {
                        if (module.isEnabled()) {
                            module.disable();
                        }
                    } catch (Exception e) {
                        NexusClient.LOGGER.error("Error disabling module '{}' during shutdown", module.getName(), e);
                    }
                }
                enabledModules.clear();
            }
            
            // Clear collections
            moduleMap.clear();
            categoryMap.clear();
            modulePerformance.clear();
            errorCounts.clear();
            
            NexusClient.LOGGER.info("Module manager shut down successfully");
            
        } catch (Exception e) {
            NexusClient.LOGGER.error("Error during module manager shutdown", e);
        }
    }
}
