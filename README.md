# Nexus Client

**A modern, customizable Minecraft client with advanced features and sleek design.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-API-orange.svg)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.java.net)
[![License](https://img.shields.io/badge/License-CC0--1.0-red.svg)](LICENSE)

---

## 🚀 Features

### ✨ Modern Interface
- **4 Built-in Themes**: Cyberpunk (default), Dark, Light, and Minimal
- **Smooth Animations**: Fluid transitions and responsive design
- **Advanced GUI**: Modern rounded corners, gradients, and visual effects
- **Theme Switching**: Real-time theme changes without restart

### 🎮 Enhanced Modules
- **Combat**: AimAssist, AutoClicker, AutoPotion, FakeLag, Hitbox, KillAura, Velocity
- **Movement**: AutoClutch, NoFall, SafeWalk
- **Render**: EntityESP, Fullbright, NoHurtCam, Xray
- **Player**: AntiHunger, FastBreak, FastPlace
- **World**: MacroRecorder for action automation
- **Minigames**: MMFinder (Murder Mystery detection)
- **Miscellaneous**: Discord RPC, ActiveMods display, Transparency effects

### 🔧 Advanced Configuration
- **Enhanced Module Manager**: Better error handling and performance monitoring
- **Custom Keybinds**: Flexible key binding system with persistence
- **Module Profiles**: Save and load different configurations
- **Performance Tracking**: Monitor module performance and error counts

---

## 📦 Installation

### Prerequisites
- **Java 21+** (OpenJDK recommended)
- **Minecraft 1.21.4** with Fabric Loader
- **Fabric API** for 1.21.4
- **Fabric Language Kotlin** (1.11.0+kotlin.2.0.0 or newer)

### Steps
1. Install [Fabric 1.21.4](https://fabricmc.net/use/installer/)
2. Download the [latest release](https://github.com/nexusclient/NexusClient/releases/latest)
3. Place the `.jar` file in your mods folder (`%appdata%/.minecraft/mods` on Windows)
4. Install [Fabric API](https://modrinth.com/mod/fabric-api/versions) and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
5. Launch Minecraft with Fabric!

**Default GUI Key**: `R-SHIFT`

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/nexusclient/NexusClient.git
cd NexusClient

# Build the project
./gradlew build

# The built jar will be in build/libs/
```

### Development Setup
```bash
# Generate IDE files
./gradlew genEclipseRuns  # For Eclipse
./gradlew genIntellijRuns # For IntelliJ IDEA

# Run in development
./gradlew runClient
```

---

## 🎨 Themes

### Cyberpunk (Default)
- **Primary**: Neon Green (#00FF96)
- **Secondary**: Neon Pink (#FF0096)
- **Background**: Dark (#0A0A0F)

### Dark Theme
- **Primary**: Blue (#64AAFF)
- **Secondary**: Light Blue (#5078C8)
- **Background**: Dark Gray (#141419)

### Light Theme
- **Primary**: Professional Blue (#0078D7)
- **Secondary**: Steel Blue (#4682B4)
- **Background**: Clean White (#FFFFFF)

### Minimal Theme
- **Primary**: Purple (#5856D6)
- **Secondary**: Lavender (#AF52DE)
- **Background**: Space Gray (#1C1C1E)

---

## ⌨️ Commands

All commands use the `nexus` prefix:

```
/nexus bind <module> <key>     - Bind a key to a module
/nexus unbind <module>         - Unbind a module's key
/nexus top                     - Teleport to highest block (SP only)
/nexus dummy                   - Spawn test dummy (SP only)
```

---

## 🔒 Security & Ethics

**⚠️ Important Notice:**
- This client is designed for **educational purposes** and **private servers**
- **Use at your own risk** on public servers
- Always respect server rules and terms of service
- Some features may be considered cheating on certain servers

---

## 🤝 Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow existing code conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Ensure compatibility with Minecraft 1.21.4

---

## 📝 Changelog

### v1.0.0 (Latest)
- **Complete rebrand** from Amber Client to Nexus Client
- **New theme system** with 4 customizable themes
- **Enhanced GUI** with modern design and smooth animations
- **Improved module manager** with better error handling
- **Performance optimizations** and thread safety improvements
- **Better configuration system** with JSON persistence
- **Updated commands** to use `nexus` prefix

---

## 📄 License

This project is licensed under the CC0-1.0 License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- **Fabric** team for the modding framework
- **Kotlin** team for the programming language
- **Minecraft** community for inspiration and support
- All contributors and testers

---

## 📞 Support

- **Discord**: [Join our server](https://discord.gg/nexusclient)
- **Issues**: [Report bugs](https://github.com/nexusclient/NexusClient/issues)
- **Wiki**: [Documentation](https://github.com/nexusclient/NexusClient/wiki)

---

*Made with ❤️ for the Minecraft community*
