# Changelog

All notable changes to Nexus Client will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-07-27

### 🎉 Major Release - Complete Rebrand and Enhancement

This release represents a complete transformation from "Amber Client" to "Nexus Client" with extensive improvements across all aspects of the codebase.

### ✨ Added

#### **Branding & Identity**
- Complete rebrand from "Amber Client" to "Nexus Client"
- New package structure: `com.nexusclient`
- Updated mod ID, commands, and all references
- Professional README with badges and documentation
- Comprehensive changelog documentation

#### **Modern Theme System**
- **4 Built-in Themes:**
  - 🌈 **Cyberpunk** (Default): Neon green/pink with dark backgrounds
  - 🌙 **Dark**: Professional blue accents with dark grays
  - ☀️ **Light**: Clean white design with blue accents
  - 💜 **Minimal**: Purple accents with space gray backgrounds
- Real-time theme switching without restart
- JSON-based theme configuration persistence
- Theme manager with automatic initialization

#### **Enhanced GUI (NexusClickGUI)**
- Complete redesign with modern aesthetics
- Smooth animations and transitions
- Rounded corners and gradient effects
- Better color schemes with theme support
- Improved module categorization and display
- Enhanced configuration panels
- Better mouse interaction and feedback
- Draggable configuration panels
- Smooth scrolling with visual scrollbars
- Theme selector button in GUI

#### **Advanced Module Management (EnhancedModuleManager)**
- Thread-safe collections for better performance
- Comprehensive error handling and logging
- Performance monitoring for all modules
- Automatic module disabling on excessive errors
- Better module organization by category
- Enhanced keybind management
- Module search functionality
- Performance tracking and statistics
- Graceful shutdown handling

#### **Improved Architecture**
- Better separation of concerns
- Enhanced error handling throughout codebase
- Improved logging with structured messages
- Thread safety improvements
- Performance optimizations

### 🔧 Changed

#### **Commands System**
- Updated command prefix from `/amber` to `/nexus`
- Maintained all existing functionality
- Better command organization and help text

#### **Configuration System**
- Enhanced keybind persistence
- JSON-based configuration files
- Theme settings stored in `config/nexusclient/theme.json`
- Better error handling for configuration loading/saving

#### **User Experience**
- More responsive GUI interactions
- Better visual feedback for user actions
- Improved module toggle animations
- Enhanced configuration interface

### 🛠️ Technical Improvements

#### **Code Quality**
- Extensive refactoring for maintainability
- Better code organization and structure
- Improved method naming and documentation
- Reduced code duplication
- Enhanced type safety

#### **Performance**
- Optimized module tick processing
- Better memory management
- Reduced CPU usage during GUI rendering
- Improved startup time

#### **Stability**
- Better exception handling
- Graceful degradation on errors
- Improved module lifecycle management
- Enhanced thread safety

### 📁 File Structure Changes

#### **Renamed Files**
- `AmberClient.java` → `NexusClient.java`
- `AmberClientDataGenerator.java` → `NexusClientDataGenerator.java`
- `AmberCommand.java` → `NexusCommand.java`
- `ClickGUI.java` → `NexusClickGUI.java`
- `amber-client.mixins.json` → `nexus-client.mixins.json`

#### **New Files**
- `ui/theme/Theme.java` - Theme data structure
- `ui/theme/ThemeManager.java` - Theme management system
- `utils/module/EnhancedModuleManager.java` - Advanced module management
- `screens/NexusClickGUI.java` - Modern GUI implementation

#### **Updated Resources**
- Language files updated with new branding
- Asset folder renamed: `assets/amber-client` → `assets/nexus-client`
- Mixin configurations updated
- Fabric mod metadata updated

### 🎯 Module Features

All original modules maintained with improved:
- **Combat**: AimAssist, AutoClicker, AutoPotion, FakeLag, Hitbox, KillAura, Velocity
- **Movement**: AutoClutch, NoFall, SafeWalk
- **Render**: EntityESP, Fullbright, NoHurtCam, Xray
- **Player**: AntiHunger, FastBreak, FastPlace
- **World**: MacroRecorder
- **Minigames**: MMFinder (Murder Mystery detection)
- **Miscellaneous**: Discord RPC, ActiveMods, Transparency

### 🔒 Compatibility

- **Minecraft**: 1.21.4
- **Fabric Loader**: 0.16.14+
- **Java**: 21+
- **Fabric API**: 0.119.2+1.21.4
- **Fabric Language Kotlin**: 1.11.0+kotlin.2.0.0

### 📝 Documentation

- Comprehensive README with installation instructions
- Theme documentation with color specifications
- Command reference and usage examples
- Building and development instructions
- Contributing guidelines

### 🙏 Credits

This release represents a complete overhaul of the original Amber Client, transforming it into a modern, professional-grade Minecraft client with enhanced features, better performance, and a superior user experience.

---

## Previous Versions

### [0.6.0] - Previous Amber Client Release
- Original Amber Client functionality
- Basic module system
- Simple GUI interface
- Legacy command system

---

**Note**: This changelog covers the transformation from Amber Client to Nexus Client. Future releases will continue to build upon this solid foundation with additional features and improvements.
