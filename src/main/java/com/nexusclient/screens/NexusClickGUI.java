package com.nexusclient.screens;

import com.nexusclient.NexusClient;
import com.nexusclient.modules.miscellaneous.Transparency;
import com.nexusclient.ui.theme.Theme;
import com.nexusclient.ui.theme.ThemeManager;
import com.nexusclient.utils.module.Module;
import com.nexusclient.utils.module.ModuleManager;
import com.nexusclient.utils.module.EnhancedModuleManager;
import com.nexusclient.utils.module.ConfigurableModule;
import com.nexusclient.utils.module.ModuleSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class NexusClickGUI extends Screen {
    // Animation and state
    private float animProgress = 0.0f;
    private float configAnim = 0.0f;
    private float mainScroll = 0.0f;
    private float configScroll = 0.0f;
    private long lastTime = System.currentTimeMillis();
    
    // Categories and modules
    private final List<Category> categories = new ArrayList<>();
    private int selectedCat = 0;
    private final ScrollState main = new ScrollState();
    private final ScrollState config = new ScrollState();
    
    // Configuration panel
    private ModuleWrapper configModule = null;
    private int configOffsetX = -350;
    private int configOffsetY = 0;
    private boolean configDragging = false;
    private int configDragX, configDragY;
    private ModuleSettings draggedSetting = null;
    private ModuleSettings openDropdown = null;
    
    // Visual effects
    private final List<ModuleWrapper> clickedModules = new ArrayList<>();
    private long clickTime = 0;
    private static final long CLICK_DURATION = 300;
    
    // Search functionality
    private String searchQuery = "";
    private boolean searchFocused = false;
    
    public NexusClickGUI() {
        super(Text.literal("Nexus Client - Advanced Minecraft Enhancement"));
        initCategories();
    }
    
    private static class ScrollState {
        boolean isDragging = false;
        int dragStartY;
        float dragStartOffset;
    }
    
    private record PanelBounds(int x, int y, int width, int height) {}
    
    private void initCategories() {
        Map<String, List<Module>> catMap = new HashMap<>();
        EnhancedModuleManager.getInstance().getModules().forEach(m -> 
            catMap.computeIfAbsent(String.valueOf(m.getCategory()), k -> new ArrayList<>()).add(m));
        
        // Sort categories with Combat last
        catMap.entrySet().stream()
            .filter(entry -> !entry.getKey().equals("Miscellaneous") && !entry.getKey().equals("COMBAT"))
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .forEach(entry -> categories.add(new Category(entry.getKey(), 
                entry.getValue().stream().map(ModuleWrapper::new).toList())));
        
        // Add Combat category with warning
        List<Module> combatModules = catMap.get("COMBAT");
        if (combatModules != null && !combatModules.isEmpty()) {
            categories.add(new Category("COMBAT", 
                combatModules.stream().map(ModuleWrapper::new).toList()));
        }
        
        // Add Miscellaneous last
        List<Module> miscModules = catMap.get("Miscellaneous");
        if (miscModules != null && !miscModules.isEmpty()) {
            categories.add(new Category("Miscellaneous", 
                miscModules.stream().map(ModuleWrapper::new).toList()));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Close button with improved styling
        addDrawableChild(ButtonWidget.builder(Text.literal("✕"), b -> close())
            .dimensions(width - 30, 5, 25, 25)
            .tooltip(Tooltip.of(Text.literal("Close Nexus Client")))
            .build());
            
        // Theme selector button
        addDrawableChild(ButtonWidget.builder(Text.literal("⚙"), b -> openThemeSelector())
            .dimensions(width - 60, 5, 25, 25)
            .tooltip(Tooltip.of(Text.literal("Theme Settings")))
            .build());
    }
    
    private void openThemeSelector() {
        // Cycle through themes for now
        ThemeManager themeManager = ThemeManager.getInstance();
        String[] themeNames = {"Cyberpunk", "Dark", "Light", "Minimal"};
        String currentTheme = themeManager.getCurrentTheme().getName();
        
        int currentIndex = Arrays.asList(themeNames).indexOf(currentTheme);
        int nextIndex = (currentIndex + 1) % themeNames.length;
        
        themeManager.setCurrentTheme(themeNames[nextIndex]);
    }
    
    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        long time = System.currentTimeMillis();
        float deltaTime = (time - lastTime) / 1000.0f;
        lastTime = time;
        
        // Smooth animations
        animProgress = MathHelper.clamp(animProgress + deltaTime * 3.0f, 0.0f, 1.0f);
        configAnim = MathHelper.clamp(configAnim + (configModule != null ? 1 : -1) * deltaTime * 4.0f, 0.0f, 1.0f);
        
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        float transparency = getTransparency();
        
        // Background with glass effect
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, theme.withAlpha(theme.getBaseBg(), transparency));
        
        super.render(context, mouseX, mouseY, delta);
        
        // Header with gradient
        renderHeader(context, theme);
        
        // Main panel
        PanelBounds mainPanel = calcPanel();
        renderMainPanel(context, mainPanel, theme, transparency, mouseX, mouseY);
        
        // Status bar
        renderStatusBar(context, mainPanel, theme, transparency);
        
        // Configuration panel
        if (configAnim > 0.0f && configModule != null) {
            renderConfigPanel(context, mouseX, mouseY, theme);
        }
    }
    
    private void renderHeader(DrawContext context, Theme theme) {
        int centerX = width / 2;
        
        // Main title with glow effect
        String title = "NEXUS CLIENT";
        int titleWidth = textRenderer.getWidth(title);
        
        // Glow effect
        for (int i = 0; i < 3; i++) {
            context.drawTextWithShadow(textRenderer, title, 
                centerX - titleWidth / 2 + i - 1, 42 + i - 1, 
                theme.withAlpha(theme.getPrimaryAccent(), 0.3f));
        }
        
        // Main title
        context.drawTextWithShadow(textRenderer, title, centerX - titleWidth / 2, 42, 
            theme.getPrimaryAccent());
        
        // Version and theme info
        String info = NexusClient.MOD_VERSION + " • " + ThemeManager.getInstance().getCurrentTheme().getName();
        context.drawTextWithShadow(textRenderer, info, centerX - textRenderer.getWidth(info) / 2, 56, 
            theme.getSecondaryText());
    }
    
    private void renderMainPanel(DrawContext context, PanelBounds panel, Theme theme, float transparency, int mouseX, int mouseY) {
        // Panel background with border
        context.fill(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height,
            theme.withAlpha(theme.getPanelBg(), animProgress * transparency));
        
        drawBorder(context, panel.x, panel.y, panel.width, panel.height, theme.getBorderColor());
        
        // Category separator
        int sepX = panel.x + 180;
        drawVerticalGradient(context, sepX, panel.y + 10, 2, panel.height - 20, 
            theme.getPrimaryAccent(), theme.getSecondaryAccent());
        
        renderCategories(context, panel.x, panel.y, panel.height, mouseX, mouseY, theme, transparency);
        renderModules(context, sepX + 15, panel.y, panel.width - 195, panel.height, mouseX, mouseY, theme, transparency);
    }
    
    private void renderCategories(DrawContext context, int x, int y, int h, int mouseX, int mouseY, Theme theme, float transparency) {
        int catH = 45;
        int spacing = 8;
        int totalH = categories.size() * (catH + spacing) - spacing;
        int startY = y + (h - totalH) / 2;
        
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int catY = startY + i * (catH + spacing);
            int catX = x + 15;
            int catW = 150;
            
            boolean hover = isMouseOver(mouseX, mouseY, catX, catY, catW, catH);
            boolean selected = selectedCat == i;
            
            // Category background
            int bgColor;
            if (selected) {
                bgColor = theme.getPrimaryAccent();
            } else if (hover) {
                bgColor = theme.withAlpha(theme.getHoverAccent(), transparency * 0.7f);
            } else {
                bgColor = theme.withAlpha(theme.getModuleBg(), transparency * 0.5f);
            }
            
            drawRoundedRect(context, catX, catY, catW, catH, 8, bgColor);
            
            if (selected) {
                drawBorder(context, catX, catY, catW, catH, theme.getFocusBorder());
            }
            
            // Category text
            int textColor = selected ? Color.WHITE.getRGB() : theme.getPrimaryText();
            context.drawCenteredTextWithShadow(textRenderer, cat.name.toUpperCase(), 
                catX + catW / 2, catY + (catH - 8) / 2, textColor);
            
            // Module count indicator
            String count = String.valueOf(cat.modules.size());
            context.drawTextWithShadow(textRenderer, count, catX + catW - 15, catY + 5, theme.getSecondaryText());
        }
    }
    
    private void renderModules(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, Theme theme, float transparency) {
        if (selectedCat < 0 || selectedCat >= categories.size()) return;
        
        Category cat = categories.get(selectedCat);
        
        // Category header
        context.drawTextWithShadow(textRenderer, cat.name.toUpperCase(), x, y + 15, theme.getPrimaryAccent());
        
        // Warning for Combat category
        if (cat.name.equalsIgnoreCase("COMBAT")) {
            context.drawTextWithShadow(textRenderer, "⚠ Use at your own risk", x, y + 30, theme.getWarningColor());
        }
        
        int top = y + (cat.name.equalsIgnoreCase("COMBAT") ? 45 : 35);
        int areaH = h - (cat.name.equalsIgnoreCase("COMBAT") ? 55 : 45);
        
        context.enableScissor(x, top, x + w, top + areaH);
        
        int modH = 35;
        int spacing = 6;
        int contentH = cat.modules.size() * (modH + spacing) - spacing;
        mainScroll = MathHelper.clamp(mainScroll, 0, Math.max(0, contentH - areaH));
        
        for (int i = 0; i < cat.modules.size(); i++) {
            ModuleWrapper mod = cat.modules.get(i);
            int modY = top + i * (modH + spacing) - (int)mainScroll;
            if (modY + modH < top || modY > top + areaH) continue;
            
            renderModule(context, x, modY, w, modH, mod, mouseX, mouseY, theme, transparency);
        }
        
        // Scrollbar
        if (contentH > areaH) {
            renderScrollbar(context, x + w - 8, top, 6, areaH, mainScroll, contentH, theme);
        }
        
        context.disableScissor();
    }
    
    private void renderModule(DrawContext context, int x, int y, int w, int h, ModuleWrapper mod, int mouseX, int mouseY, Theme theme, float transparency) {
        boolean hover = isMouseOver(mouseX, mouseY, x, y, w, h);
        boolean clicked = clickedModules.contains(mod) && (System.currentTimeMillis() - clickTime) < CLICK_DURATION;
        
        // Module background
        int bgColor;
        if (mod.isEnabled()) {
            bgColor = theme.withAlpha(theme.getModuleEnabledBg(), transparency);
        } else if (hover || clicked) {
            bgColor = theme.withAlpha(theme.getHoverAccent(), transparency * 0.3f);
        } else {
            bgColor = theme.withAlpha(theme.getModuleBg(), transparency * 0.8f);
        }
        
        drawRoundedRect(context, x, y, w, h, 6, bgColor);
        
        // Module border for enabled modules
        if (mod.isEnabled()) {
            drawBorder(context, x, y, w, h, theme.withAlpha(theme.getPrimaryAccent(), 0.8f));
        }
        
        // Module name
        context.drawTextWithShadow(textRenderer, mod.name, x + 12, y + 8, 
            mod.isEnabled() ? theme.getPrimaryAccent() : theme.getPrimaryText());
        
        // Module description
        context.drawTextWithShadow(textRenderer, mod.desc, x + 12, y + 22, theme.getSecondaryText());
        
        // Configuration gear
        if (mod.isConfigurable) {
            boolean gearHover = isMouseOver(mouseX, mouseY, x + w - 65, y + 8, 15, 15);
            int gearColor = gearHover ? theme.getHoverAccent() : theme.getSecondaryText();
            context.drawTextWithShadow(textRenderer, "⚙", x + w - 60, y + 8, gearColor);
        }
        
        // Toggle button
        int togX = x + w - 35;
        int togY = y + 8;
        int togSize = 20;
        boolean togHover = isMouseOver(mouseX, mouseY, togX, togY, togSize, togSize);
        
        int toggleBg = mod.isEnabled() ? theme.getSuccessColor() : theme.getDisabledText();
        if (togHover) {
            toggleBg = mod.isEnabled() ? theme.getHoverAccent() : theme.getSecondaryText();
        }
        
        drawRoundedRect(context, togX, togY, togSize, togSize, togSize / 2, toggleBg);
        
        if (mod.isEnabled()) {
            context.drawTextWithShadow(textRenderer, "✓", togX + 7, togY + 6, Color.WHITE.getRGB());
        }
    }
    
    private void renderConfigPanel(DrawContext context, int mouseX, int mouseY, Theme theme) {
        PanelBounds p = calcConfigPanel();
        
        // Panel background
        drawRoundedRect(context, p.x, p.y, p.width, p.height, 12, theme.getPanelBg());
        drawBorder(context, p.x, p.y, p.width, p.height, theme.getBorderColor());
        
        // Header
        drawRoundedRect(context, p.x, p.y, p.width, 35, 12, theme.getPrimaryAccent());
        context.drawTextWithShadow(textRenderer, configModule.name + " Configuration", 
            p.x + 15, p.y + 13, Color.WHITE.getRGB());
        
        // Close button
        boolean closeHover = isMouseOver(mouseX, mouseY, p.x + p.width - 30, p.y + 5, 25, 25);
        int closeBg = closeHover ? theme.getErrorColor() : theme.withAlpha(theme.getErrorColor(), 0.7f);
        drawRoundedRect(context, p.x + p.width - 30, p.y + 5, 25, 25, 12, closeBg);
        context.drawTextWithShadow(textRenderer, "✕", p.x + p.width - 22, p.y + 13, Color.WHITE.getRGB());
        
        // Settings list
        renderConfigSettings(context, p, mouseX, mouseY, theme);
    }
    
    private void renderConfigSettings(DrawContext context, PanelBounds p, int mouseX, int mouseY, Theme theme) {
        List<ModuleSettings> settings = configModule.settings;
        int top = p.y + 45;
        int areaH = p.height - 55;
        
        context.enableScissor(p.x, top, p.x + p.width, top + areaH);
        
        int setH = 45;
        int spacing = 8;
        int contentH = settings.size() * (setH + spacing) - spacing;
        configScroll = MathHelper.clamp(configScroll, 0, Math.max(0, contentH - areaH));
        
        for (int i = 0; i < settings.size(); i++) {
            ModuleSettings s = settings.get(i);
            int setY = top + i * (setH + spacing) - (int)configScroll;
            if (setY + setH < top || setY > top + areaH) continue;
            
            renderConfigSetting(context, p.x + 10, setY, p.width - 20, setH, s, mouseX, mouseY, theme);
        }
        
        // Scrollbar
        if (contentH > areaH) {
            renderScrollbar(context, p.x + p.width - 12, top, 6, areaH, configScroll, contentH, theme);
        }
        
        context.disableScissor();
    }
    
    private void renderConfigSetting(DrawContext context, int x, int y, int w, int h, ModuleSettings setting, int mouseX, int mouseY, Theme theme) {
        // Setting background
        drawRoundedRect(context, x, y, w, h, 8, theme.withAlpha(theme.getModuleBg(), 0.8f));
        
        // Setting name and description
        context.drawTextWithShadow(textRenderer, setting.getName(), x + 12, y + 8, theme.getPrimaryText());
        context.drawTextWithShadow(textRenderer, setting.getDescription(), x + 12, y + 25, theme.getSecondaryText());
        
        // Setting control
        if (setting.getType() == ModuleSettings.SettingType.BOOLEAN) {
            renderBooleanSetting(context, x + w - 80, y + 12, 60, 20, setting, mouseX, mouseY, theme);
        } else if (setting.getType() == ModuleSettings.SettingType.DOUBLE && setting.hasRange()) {
            renderSliderSetting(context, x + w - 120, y + 12, 100, 20, setting, mouseX, mouseY, theme);
        } else if (setting.getType() == ModuleSettings.SettingType.ENUM) {
            renderEnumSetting(context, x + w - 120, y + 12, 100, 20, setting, mouseX, mouseY, theme);
        }
    }
    
    private void renderBooleanSetting(DrawContext context, int x, int y, int w, int h, ModuleSettings setting, int mouseX, int mouseY, Theme theme) {
        boolean enabled = setting.getBooleanValue();
        boolean hover = isMouseOver(mouseX, mouseY, x, y, w, h);
        
        int bgColor = enabled ? theme.getSuccessColor() : theme.getDisabledText();
        if (hover) {
            bgColor = enabled ? theme.getHoverAccent() : theme.getSecondaryText();
        }
        
        drawRoundedRect(context, x, y, w, h, h / 2, bgColor);
        
        String text = enabled ? "ENABLED" : "DISABLED";
        int textWidth = textRenderer.getWidth(text);
        context.drawTextWithShadow(textRenderer, text, x + (w - textWidth) / 2, y + (h - 8) / 2, Color.WHITE.getRGB());
    }
    
    private void renderSliderSetting(DrawContext context, int x, int y, int w, int h, ModuleSettings setting, int mouseX, int mouseY, Theme theme) {
        double value = setting.getDoubleValue();
        double min = setting.getMinValue().doubleValue();
        double max = setting.getMaxValue().doubleValue();
        double progress = (value - min) / (max - min);
        
        // Track
        drawRoundedRect(context, x, y + h / 2 - 2, w, 4, 2, theme.getDisabledText());
        
        // Progress
        drawRoundedRect(context, x, y + h / 2 - 2, (int)(w * progress), 4, 2, theme.getPrimaryAccent());
        
        // Handle
        int handleX = x + (int)(w * progress) - 6;
        drawRoundedRect(context, handleX, y + 2, 12, h - 4, 6, theme.getPrimaryAccent());
        
        // Value text
        String valueText = String.format("%.2f", value);
        context.drawTextWithShadow(textRenderer, valueText, x + w + 8, y + (h - 8) / 2, theme.getPrimaryText());
    }
    
    private void renderEnumSetting(DrawContext context, int x, int y, int w, int h, ModuleSettings setting, int mouseX, int mouseY, Theme theme) {
        Enum<?> currentValue = setting.getEnumValue();
        boolean hover = isMouseOver(mouseX, mouseY, x, y, w, h);
        
        int bgColor = hover ? theme.getHoverAccent() : theme.getModuleBg();
        drawRoundedRect(context, x, y, w, h, 6, bgColor);
        drawBorder(context, x, y, w, h, theme.getBorderColor());
        
        // Current value
        context.drawTextWithShadow(textRenderer, currentValue.name(), x + 8, y + (h - 8) / 2, theme.getPrimaryText());
        
        // Dropdown arrow
        context.drawTextWithShadow(textRenderer, "▼", x + w - 15, y + (h - 8) / 2, theme.getSecondaryText());
        
        // Dropdown options
        if (openDropdown == setting) {
            Enum<?>[] enumValues = currentValue.getDeclaringClass().getEnumConstants();
            int optionY = y + h;
            for (Enum<?> enumValue : enumValues) {
                boolean optionHover = isMouseOver(mouseX, mouseY, x, optionY, w, 20);
                int optionColor = enumValue == currentValue ? theme.getPrimaryAccent() :
                    (optionHover ? theme.getHoverAccent() : theme.getModuleBg());
                
                drawRoundedRect(context, x, optionY, w, 20, 4, optionColor);
                context.drawTextWithShadow(textRenderer, enumValue.name(), x + 8, optionY + 6, 
                    enumValue == currentValue ? Color.WHITE.getRGB() : theme.getPrimaryText());
                optionY += 20;
            }
        }
    }
    
    private void renderStatusBar(DrawContext context, PanelBounds panel, Theme theme, float transparency) {
        int statusY = panel.y + panel.height + 8;
        int statusH = 25;
        
        drawRoundedRect(context, panel.x, statusY, panel.width, statusH, 6, 
            theme.withAlpha(theme.getPanelBg(), transparency));
        
        String statusText = configModule != null ? 
            "Configuring: " + configModule.name : 
            "Nexus Client " + NexusClient.MOD_VERSION + " • Ready";
        
        context.drawTextWithShadow(textRenderer, statusText, panel.x + 15, statusY + 8, theme.getPrimaryText());
        
        // Theme indicator
        String themeText = ThemeManager.getInstance().getCurrentTheme().getName();
        int themeX = panel.x + panel.width - textRenderer.getWidth(themeText) - 15;
        context.drawTextWithShadow(textRenderer, themeText, themeX, statusY + 8, theme.getSecondaryAccent());
    }
    
    private void renderScrollbar(DrawContext context, int x, int y, int w, int h, float scroll, int contentH, Theme theme) {
        // Track
        drawRoundedRect(context, x, y, w, h, w / 2, theme.withAlpha(theme.getDisabledText(), 0.3f));
        
        // Thumb
        float ratio = (float) h / contentH;
        int thumbH = Math.max(20, (int)(h * ratio));
        int thumbY = y + (int)((h - thumbH) * (scroll / Math.max(1, contentH - h)));
        
        drawRoundedRect(context, x, thumbY, w, thumbH, w / 2, theme.getPrimaryAccent());
    }
    
    // Utility methods
    private void drawRoundedRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        // For now, just draw a regular rectangle
        // In a full implementation, you'd draw rounded corners
        context.fill(x, y, x + w, y + h, color);
    }
    
    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x - 1, y - 1, x + w + 1, y, color);
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        context.fill(x - 1, y, x, y + h, color);
        context.fill(x + w, y, x + w + 1, y + h, color);
    }
    
    private void drawVerticalGradient(DrawContext context, int x, int y, int w, int h, int topColor, int bottomColor) {
        // Simple vertical line for now
        context.fill(x, y, x + w, y + h, topColor);
    }
    
    private boolean isMouseOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    
    private float getTransparency() {
        return EnhancedModuleManager.getInstance().getModules().stream()
            .filter(m -> m instanceof Transparency && m.isEnabled())
            .map(m -> ((Transparency) m).getTransparencyLevel())
            .findFirst()
            .orElse(0.85f);
    }
    
    private PanelBounds calcPanel() {
        int w = Math.min(width - 60, 900);
        int h = Math.min(height - 120, 500);
        float scale = 0.7f + 0.3f * animProgress;
        int scaledW = (int)(w * scale);
        int scaledH = (int)(h * scale);
        return new PanelBounds(width / 2 - scaledW / 2, 90 + (h - scaledH) / 2, scaledW, scaledH);
    }
    
    private PanelBounds calcConfigPanel() {
        int w = Math.min(width - 60, 350);
        int h = Math.min(height - 120, 450);
        int x = (width - w) / 2 + configOffsetX;
        int y = 90 + configOffsetY;
        
        x = MathHelper.clamp(x, 15, width - w - 15);
        y = MathHelper.clamp(y, 15, height - h - 15);
        
        return new PanelBounds(x, y, w, h);
    }
    
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (animProgress < 1.0f) return false;
        
        // Handle config panel clicks first
        if (configModule != null && configAnim > 0.0f) {
            PanelBounds p = calcConfigPanel();
            if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width, p.height)) {
                return handleConfigPanelClick(mx, my, button, p);
            }
        }
        
        if (super.mouseClicked(mx, my, button)) return true;
        
        PanelBounds panel = calcPanel();
        
        // Handle category clicks
        if (handleCategoryClick(mx, my, panel)) return true;
        
        // Handle module clicks
        if (handleModuleClick(mx, my, button, panel)) return true;
        
        clickedModules.clear();
        return false;
    }
    
    private boolean handleConfigPanelClick(double mx, double my, int button, PanelBounds p) {
        // Close button
        if (isMouseOver((int)mx, (int)my, p.x + p.width - 30, p.y + 5, 25, 25)) {
            configModule = null;
            configScroll = 0.0f;
            draggedSetting = null;
            openDropdown = null;
            return true;
        }
        
        // Title bar for dragging
        if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width - 30, 35)) {
            configDragging = true;
            configDragX = (int)mx;
            configDragY = (int)my;
            return true;
        }
        
        // Settings area
        List<ModuleSettings> settings = configModule.settings;
        int top = p.y + 45;
        int setH = 45;
        int spacing = 8;
        
        for (int i = 0; i < settings.size(); i++) {
            ModuleSettings s = settings.get(i);
            int setY = top + i * (setH + spacing) - (int)configScroll;
            if (setY + setH < top || setY > top + p.height - 55) continue;
            
            if (handleSettingClick(s, mx, my, p.x + 10, setY, p.width - 20, setH)) {
                return true;
            }
        }
        
        // Scrollbar
        int areaH = p.height - 55;
        int contentH = settings.size() * (setH + spacing) - spacing;
        if (contentH > areaH && isMouseOver((int)mx, (int)my, p.x + p.width - 12, top, 6, areaH)) {
            config.isDragging = true;
            config.dragStartY = (int)my;
            config.dragStartOffset = configScroll;
            return true;
        }
        
        return true; // Consume click within panel
    }
    
    private boolean handleSettingClick(ModuleSettings setting, double mx, double my, int x, int y, int w, int h) {
        if (setting.getType() == ModuleSettings.SettingType.BOOLEAN) {
            if (isMouseOver((int)mx, (int)my, x + w - 80, y + 12, 60, 20)) {
                setting.setBooleanValue(!setting.getBooleanValue());
                ((ConfigurableModule)configModule.module).onSettingChanged(setting);
                return true;
            }
        } else if (setting.getType() == ModuleSettings.SettingType.DOUBLE && setting.hasRange()) {
            if (isMouseOver((int)mx, (int)my, x + w - 120, y + 12, 100, 20)) {
                draggedSetting = setting;
                updateSliderValue(setting, mx, x + w - 120, 100);
                return true;
            }
        } else if (setting.getType() == ModuleSettings.SettingType.ENUM) {
            if (isMouseOver((int)mx, (int)my, x + w - 120, y + 12, 100, 20)) {
                openDropdown = (openDropdown == setting) ? null : setting;
                return true;
            }
            
            // Handle dropdown options
            if (openDropdown == setting) {
                Enum<?> currentValue = setting.getEnumValue();
                Enum<?>[] enumValues = currentValue.getDeclaringClass().getEnumConstants();
                int optionY = y + 32;
                
                for (Enum<?> enumValue : enumValues) {
                    if (isMouseOver((int)mx, (int)my, x + w - 120, optionY, 100, 20)) {
                        setting.setEnumValue(enumValue);
                        ((ConfigurableModule)configModule.module).onSettingChanged(setting);
                        openDropdown = null;
                        return true;
                    }
                    optionY += 20;
                }
            }
        }
        
        return false;
    }
    
    private boolean handleCategoryClick(double mx, double my, PanelBounds panel) {
        int catH = 45;
        int spacing = 8;
        int totalH = categories.size() * (catH + spacing) - spacing;
        int startY = panel.y + (panel.height - totalH) / 2;
        
        for (int i = 0; i < categories.size(); i++) {
            int catY = startY + i * (catH + spacing);
            if (isMouseOver((int)mx, (int)my, panel.x + 15, catY, 150, catH)) {
                selectedCat = i;
                mainScroll = 0;
                return true;
            }
        }
        
        return false;
    }
    
    private boolean handleModuleClick(double mx, double my, int button, PanelBounds panel) {
        if (selectedCat < 0 || selectedCat >= categories.size()) return false;
        
        Category cat = categories.get(selectedCat);
        int modX = panel.x + 195;
        int modY = panel.y + (cat.name.equalsIgnoreCase("COMBAT") ? 60 : 50);
        int modW = panel.width - 210;
        int modH = 35;
        int spacing = 6;
        
        for (int i = 0; i < cat.modules.size(); i++) {
            ModuleWrapper mod = cat.modules.get(i);
            int y = modY + i * (modH + spacing) - (int)mainScroll;
            if (y + modH < modY || y > modY + panel.height - 65) continue;
            
            // Configuration gear click  
            if (mod.isConfigurable && button == 0 && 
                isMouseOver((int)mx, (int)my, modX + modW - 65, y + 8, 15, 15)) {
                openConfigPanel(mod);
                return true;
            }
            
            // Right-click for config
            if (mod.isConfigurable && button == 1 && 
                isMouseOver((int)mx, (int)my, modX, y, modW, modH)) {
                openConfigPanel(mod);
                return true;
            }
            
            // Toggle button click
            if (isMouseOver((int)mx, (int)my, modX + modW - 35, y + 8, 20, 20)) {
                clickedModules.clear();
                clickedModules.add(mod);
                clickTime = System.currentTimeMillis();
                EnhancedModuleManager.getInstance().toggleModule(mod.module);
                return true;
            }
        }
        
        // Main scrollbar
        int areaH = panel.height - (cat.name.equalsIgnoreCase("COMBAT") ? 75 : 65);
        int contentH = cat.modules.size() * (modH + spacing) - spacing;
        if (contentH > areaH && isMouseOver((int)mx, (int)my, modX + modW - 8, modY, 6, areaH)) {
            main.isDragging = true;
            main.dragStartY = (int)my;
            main.dragStartOffset = mainScroll;
            return true;
        }
        
        return false;
    }
    
    private void openConfigPanel(ModuleWrapper module) {
        configModule = module;
        configScroll = 0.0f;
        draggedSetting = null;
        openDropdown = null;
    }
    
    private void updateSliderValue(ModuleSettings setting, double mouseX, int sliderX, int sliderW) {
        double min = setting.getMinValue().doubleValue();
        double max = setting.getMaxValue().doubleValue();
        double normalized = (mouseX - sliderX) / sliderW;
        normalized = MathHelper.clamp(normalized, 0.0, 1.0);
        double value = min + (max - min) * normalized;
        
        setting.setDoubleValue(value);
        ((ConfigurableModule)configModule.module).onSettingChanged(setting);
    }
    
    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        main.isDragging = false;
        config.isDragging = false;
        configDragging = false;
        draggedSetting = null;
        clickedModules.clear();
        return super.mouseReleased(mx, my, button);
    }
    
    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        // Config panel dragging
        if (configDragging && configModule != null) {
            configOffsetX += (int)mx - configDragX;
            configOffsetY += (int)my - configDragY;
            
            // Clamp to screen bounds
            int maxW = Math.min(width - 60, 350);
            int maxH = Math.min(height - 120, 450);
            configOffsetX = MathHelper.clamp(configOffsetX, -width + maxW + 15, width - 15);
            configOffsetY = MathHelper.clamp(configOffsetY, -maxH + 30, height - 30);
            
            configDragX = (int)mx;
            configDragY = (int)my;
            return true;
        }
        
        // Slider dragging
        if (draggedSetting != null && draggedSetting.getType() == ModuleSettings.SettingType.DOUBLE) {
            PanelBounds p = calcConfigPanel();
            updateSliderValue(draggedSetting, mx, p.x + p.width - 130, 100);
            return true;
        }
        
        // Config scrollbar dragging
        if (config.isDragging && configModule != null) {
            PanelBounds p = calcConfigPanel();
            int areaH = p.height - 55;
            int setH = 45;
            int spacing = 8;
            int contentH = configModule.settings.size() * (setH + spacing) - spacing;
            
            float scrollRange = Math.max(0, contentH - areaH);
            configScroll = config.dragStartOffset + ((int)my - config.dragStartY) * scrollRange / (areaH - 20);
            configScroll = MathHelper.clamp(configScroll, 0, scrollRange);
            return true;
        }
        
        // Main scrollbar dragging
        if (main.isDragging && selectedCat >= 0 && selectedCat < categories.size()) {
            PanelBounds panel = calcPanel();
            Category cat = categories.get(selectedCat);
            int areaH = panel.height - (cat.name.equalsIgnoreCase("COMBAT") ? 75 : 65);
            int modH = 35;
            int spacing = 6;
            int contentH = cat.modules.size() * (modH + spacing) - spacing;
            
            float scrollRange = Math.max(0, contentH - areaH);
            mainScroll = main.dragStartOffset + ((int)my - main.dragStartY) * scrollRange / (areaH - 20);
            mainScroll = MathHelper.clamp(mainScroll, 0, scrollRange);
            return true;
        }
        
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        // Config panel scrolling
        if (configModule != null && configAnim > 0.0f) {
            PanelBounds p = calcConfigPanel();
            if (isMouseOver((int)mx, (int)my, p.x, p.y, p.width, p.height)) {
                int setH = 45;
                int spacing = 8;
                int contentH = configModule.settings.size() * (setH + spacing) - spacing;
                int areaH = p.height - 55;
                
                float scrollAmount = (float) scrollY * 20;
                configScroll = MathHelper.clamp(configScroll - scrollAmount, 0, Math.max(0, contentH - areaH));
                return true;
            }
        }
        
        // Main panel scrolling
        if (selectedCat >= 0 && selectedCat < categories.size()) {
            PanelBounds panel = calcPanel();
            Category cat = categories.get(selectedCat);
            int areaH = panel.height - (cat.name.equalsIgnoreCase("COMBAT") ? 75 : 65);
            int modH = 35;
            int spacing = 6;
            int contentH = cat.modules.size() * (modH + spacing) - spacing;
            
            float scrollAmount = (float) scrollY * 20;
            mainScroll = MathHelper.clamp(mainScroll - scrollAmount, 0, Math.max(0, contentH - areaH));
            return true;
        }
        
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (configModule != null) {
                configModule = null;
                configScroll = 0.0f;
                draggedSetting = null;
                openDropdown = null;
                return true;
            }
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, mods);
    }
    
    @Override
    public void close() {
        configModule = null;
        configScroll = 0.0f;
        draggedSetting = null;
        openDropdown = null;
        super.close();
    }
    
    // Inner classes
    private static class Category {
        final String name;
        final List<ModuleWrapper> modules;
        
        Category(String name, List<ModuleWrapper> modules) {
            this.name = name;
            this.modules = modules;
        }
    }
    
    private static class ModuleWrapper {
        final Module module;
        final String name;
        final String desc;
        final boolean isConfigurable;
        final List<ModuleSettings> settings;
        
        ModuleWrapper(Module module) {
            this.module = module;
            this.name = module.getName();
            this.desc = module.getDescription();
            this.isConfigurable = module instanceof ConfigurableModule;
            this.settings = isConfigurable ? ((ConfigurableModule) module).getSettings() : new ArrayList<>();
        }
        
        boolean isEnabled() {
            return module.isEnabled();
        }
    }
}
