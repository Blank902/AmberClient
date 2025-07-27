package com.nexusclient.ui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();
    private final Map<String, Theme> themes = new HashMap<>();
    private Theme currentTheme;
    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private ThemeManager() {
        configFile = new File("config/nexusclient/theme.json");
        initializeThemes();
        loadConfig();
    }
    
    public static ThemeManager getInstance() {
        return INSTANCE;
    }
    
    private void initializeThemes() {
        // Cyberpunk theme
        themes.put("Cyberpunk", new Theme("Cyberpunk",
            new Color(10, 10, 15, 220).getRGB(),     // baseBg
            new Color(15, 15, 25, 255).getRGB(),     // panelBg
            new Color(25, 25, 35, 240).getRGB(),     // moduleBg
            new Color(0, 255, 150, 100).getRGB(),    // moduleEnabledBg
            new Color(0, 255, 150).getRGB(),         // primaryAccent (neon green)
            new Color(255, 0, 150).getRGB(),         // secondaryAccent (neon pink)
            new Color(0, 200, 255).getRGB(),         // hoverAccent (neon blue)
            new Color(240, 240, 240).getRGB(),       // primaryText
            new Color(180, 180, 180).getRGB(),       // secondaryText
            new Color(120, 120, 120).getRGB(),       // disabledText
            new Color(0, 255, 150, 180).getRGB(),    // borderColor
            new Color(0, 200, 255, 220).getRGB(),    // focusBorder
            new Color(0, 255, 100).getRGB(),         // successColor
            new Color(255, 200, 0).getRGB(),         // warningColor
            new Color(255, 50, 50).getRGB()          // errorColor
        ));
        
        // Dark theme
        themes.put("Dark", new Theme("Dark",
            new Color(20, 20, 25, 200).getRGB(),
            new Color(30, 30, 35, 255).getRGB(),
            new Color(40, 40, 45, 220).getRGB(),
            new Color(60, 120, 180, 120).getRGB(),
            new Color(100, 150, 255).getRGB(),
            new Color(80, 120, 200).getRGB(),
            new Color(120, 170, 255).getRGB(),
            new Color(220, 220, 220).getRGB(),
            new Color(180, 180, 180).getRGB(),
            new Color(120, 120, 120).getRGB(),
            new Color(100, 100, 110, 180).getRGB(),
            new Color(100, 150, 255, 220).getRGB(),
            new Color(100, 200, 100).getRGB(),
            new Color(255, 180, 60).getRGB(),
            new Color(255, 100, 100).getRGB()
        ));
        
        // Light theme
        themes.put("Light", new Theme("Light",
            new Color(245, 245, 248, 200).getRGB(),
            new Color(255, 255, 255, 255).getRGB(),
            new Color(250, 250, 252, 220).getRGB(),
            new Color(0, 120, 215, 120).getRGB(),
            new Color(0, 120, 215).getRGB(),
            new Color(70, 130, 180).getRGB(),
            new Color(30, 144, 255).getRGB(),
            new Color(50, 50, 50).getRGB(),
            new Color(100, 100, 100).getRGB(),
            new Color(150, 150, 150).getRGB(),
            new Color(200, 200, 205, 180).getRGB(),
            new Color(0, 120, 215, 220).getRGB(),
            new Color(40, 167, 69).getRGB(),
            new Color(255, 193, 7).getRGB(),
            new Color(220, 53, 69).getRGB()
        ));
        
        // Minimal theme
        themes.put("Minimal", new Theme("Minimal",
            new Color(28, 28, 30, 200).getRGB(),
            new Color(44, 44, 46, 255).getRGB(),
            new Color(58, 58, 60, 220).getRGB(),
            new Color(88, 86, 214, 120).getRGB(),
            new Color(88, 86, 214).getRGB(),
            new Color(175, 82, 222).getRGB(),
            new Color(120, 120, 250).getRGB(),
            new Color(255, 255, 255).getRGB(),
            new Color(174, 174, 178).getRGB(),
            new Color(142, 142, 147).getRGB(),
            new Color(99, 99, 102, 180).getRGB(),
            new Color(88, 86, 214, 220).getRGB(),
            new Color(52, 199, 89).getRGB(),
            new Color(255, 159, 10).getRGB(),
            new Color(255, 69, 58).getRGB()
        ));
        
        // Set default theme
        currentTheme = themes.get("Cyberpunk");
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    public void setCurrentTheme(String themeName) {
        Theme theme = themes.get(themeName);
        if (theme != null) {
            currentTheme = theme;
            saveConfig();
        }
    }
    
    public Map<String, Theme> getAllThemes() {
        return new HashMap<>(themes);
    }
    
    private void loadConfig() {
        if (!configFile.exists()) {
            saveConfig();
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            String themeName = config.get("currentTheme").getAsString();
            if (themes.containsKey(themeName)) {
                currentTheme = themes.get(themeName);
            }
        } catch (IOException e) {
            System.err.println("Failed to load theme config: " + e.getMessage());
        }
    }
    
    private void saveConfig() {
        configFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject config = new JsonObject();
            config.addProperty("currentTheme", currentTheme.getName());
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Failed to save theme config: " + e.getMessage());
        }
    }
}
