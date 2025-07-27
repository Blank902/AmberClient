# Nexus Client: Features Analysis

This document outlines the key features and enhancements of the Nexus Client, a rebranded and refactored Minecraft hack/cheat client for version 1.21.4.

## Core System Improvements

*   **Complete Rebranding**: Transitioned from "Amber Client" to "Nexus Client," accompanied by a complete refactor and updated package structure (com.nexusclient).
*   **Modern Theme System**: Implemented a highly customizable theme system (`ThemeManager`) with four built-in themes:
    *   **Cyberpunk** (default): Neon green/pink accents.
    *   **Dark**: Subtle blue accents.
    *   **Light**: Clean white design.
    *   **Minimal**: Elegant purple accents.
    Themes are configurable (backgrounds, accents, text, borders, status colors), persistent via JSON, and support real-time switching.
*   **Enhanced GUI (NexusClickGUI)**: A completely redesigned graphical user interface featuring:
    *   Smooth animations and transitions.
    *   Improved responsiveness and user feedback.
    *   Modern styling with rounded corners and gradients.
    *   Better color scheme integration with the theme system.
    *   Enhanced configuration panels.
*   **Improved Module Management (EnhancedModuleManager)**: Centralized and robust management of client features, including:
    *   Better error handling.
    *   Performance monitoring.
    *   Thread safety for concurrent operations.
*   **Better Code Organization**: Restructured codebase for improved maintainability and clear separation of concerns.
*   **Event-Driven System**: Utilizes an `EventManager` for efficient handling of various game events.
*   **Mixin-Based Integration**: Seamlessly hooks into Minecraft client internals.
*   **Custom Keybind System**: Flexible keybinding for module controls and user input.
*   **Configuration Management**: Robust system for saving and loading client settings.
*   **HUD Overlay System**: Displays in-game information.

## Enhanced Gameplay Features

### Combat

*   **AimAssist**: Provides assistance in aiming at targets.
*   **AutoClicker**: Automates mouse clicks for various actions.
*   **AutoPotion**: Automatically uses potions in combat situations.
*   **FakeLag**: Simulates network lag to gain an advantage.
*   **Hitbox**: Modifies hitbox sizes for improved targeting.
*   **KillAura**: Automatically attacks entities around the player.
*   **Velocity**: Controls player knockback.

### Movement

*   **AutoClutch**: Automatically places blocks to prevent falls.
*   **NoFall**: Prevents fall damage.
*   **SafeWalk**: Keeps the player from falling off edges.

### Render

*   **EntityESP**: Renders outlines or boxes around entities, making them visible through walls.
*   **Fullbright**: Illuminates dark areas, effectively providing permanent night vision.
*   **NoHurtCam**: Disables the visual "hurt" effect when taking damage.
*   **Xray**: Allows players to see through blocks to locate ores or other hidden items.

### Player

*   **AntiHunger**: Reduces or eliminates hunger depletion.
*   **FastBreak**: Increases the speed of block breaking.
*   **FastPlace**: Increases the speed of block placement.

### World

*   **MacroRecorder**: Records and automates sequences of actions.

### Minigames

*   **MMFinder (Murder Mystery Finder)**: Specifically designed for detecting elements in Murder Mystery games.

### Miscellaneous

*   **Discord RPC Integration**: Displays current game activity on Discord.
*   **ActiveMods Display**: Shows currently active modules in-game.
*   **Transparency Effects**: Customizable transparency for various GUI elements.

## Performance and Reliability

*   **Thread-Safe Collections**: Utilizes thread-safe collections in `EnhancedModuleManager` for stability.
*   **Performance Monitoring**: Monitors module performance to identify and address bottlenecks.
*   **Better Error Handling**: Includes robust error handling with automatic module disabling upon critical errors.
*   **Improved Tick Handling and Optimization**: Optimized game tick processing for smoother performance.

## Configuration and Commands

*   **Theme Settings**: Stored in `config/nexusclient/theme.json`.
*   **Enhanced Keybind Management**: Improved persistence for keybind configurations.
*   **Command System**: Updated to use "nexus" as the command prefix (e.g., `/nexus bind`), maintaining compatibility with existing `bind`/`unbind` functionality.
*   **Module Profiles and Presets**: Framework in place for future implementation of module profiles and presets.
