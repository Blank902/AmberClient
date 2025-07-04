package com.amberclient.screens;

import com.amberclient.AmberClient;
import com.amberclient.utils.module.ModuleManager;
import com.amberclient.modules.miscellaneous.Transparency;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;

public class MacroRecorderGUI extends Screen {
    // Theme colors
    private static final int BASE_BG = new Color(20, 20, 25, 200).getRGB();
    private static final int PANEL_BG = new Color(30, 30, 35, 255).getRGB();
    private static final int ACCENT = new Color(255, 165, 0).getRGB();
    private static final int ACCENT_HOVER = new Color(255, 190, 50).getRGB();
    private static final int TEXT = new Color(220, 220, 220).getRGB();
    private static final int OUTLINE = new Color(255, 255, 255, 180).getRGB();
    private static final int SUCCESS_COLOR = new Color(0, 255, 0).getRGB();
    private static final int ERROR_COLOR = new Color(255, 0, 0).getRGB();
    private static final int RECORDING_COLOR = new Color(255, 100, 100).getRGB();

    // State variables
    private float animProgress = 0.0f;
    private float macroListScroll = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private final ScrollState macroListScrollState = new ScrollState();

    // Macro recorder state
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private String currentMacroName = "New Macro";
    private final List<MacroEntry> savedMacros = new ArrayList<>();
    private MacroEntry selectedMacro = null;
    private int selectedTab = 0; // 0 = Recorder, 1 = Saved Macros

    // UI state
    private boolean editingName = false;
    private String statusMessage = "Ready to record";
    private long statusMessageTime = 0;
    private int statusMessageColor = TEXT;

    public MacroRecorderGUI() {
        super(Text.literal("Macro Recorder - Amber Client"));
        initSavedMacros();
    }

    private static class ScrollState {
        boolean isDragging = false;
        int dragStartY;
        float dragStartOffset;
    }

    private record PanelBounds(int x, int y, int width, int height) {}

    private static class MacroEntry {
        String name;
        int actionCount;
        boolean isEnabled;
        long createdTime;

        MacroEntry(String name, int actionCount) {
            this.name = name;
            this.actionCount = actionCount;
            this.isEnabled = true;
            this.createdTime = System.currentTimeMillis();
        }
    }

    private void initSavedMacros() {
        // No example macros added
    }

    private float getTransparency() {
        return ModuleManager.getInstance().getModules().stream()
                .filter(m -> m instanceof Transparency && m.isEnabled())
                .map(m -> ((Transparency) m).getTransparencyLevel())
                .findFirst()
                .orElse(0.75f);
    }

    private int applyTransparency(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0xFFFFFF);
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.literal("×"), b -> close())
                .dimensions(width - 25, 5, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Close")))
                .build());
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        long time = System.currentTimeMillis();
        animProgress = MathHelper.clamp(animProgress + (time - lastTime) / 300.0f, 0.0f, 1.0f);
        lastTime = time;

        float trans = getTransparency();
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, applyTransparency(BASE_BG, trans));

        renderPlayerInfo(context, mouseX, mouseY, trans);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, "MACRO RECORDER", centerX, 52, ACCENT);

        PanelBounds mainPanel = calcMainPanel();
        context.fill(mainPanel.x, mainPanel.y, mainPanel.x + mainPanel.width, mainPanel.y + mainPanel.height,
                applyTransparency(PANEL_BG, animProgress * trans));

        renderTabs(context, mainPanel, mouseX, mouseY, trans);

        if (selectedTab == 0) {
            renderRecorderTab(context, mainPanel, mouseX, mouseY, trans);
        } else {
            renderSavedMacrosTab(context, mainPanel, mouseX, mouseY, trans);
        }

        renderStatusBar(context, mainPanel, trans);
        super.render(context, mouseX, mouseY, delta);
    }

    private PanelBounds calcMainPanel() {
        int w = Math.min(width - 40, 900);
        int h = Math.min(height - 100, 500);
        float scale = 0.8f + 0.2f * animProgress;
        int scaledW = (int)(w * scale);
        int scaledH = (int)(h * scale);
        return new PanelBounds(width / 2 - scaledW / 2, 82 + (h - scaledH) / 2, scaledW, scaledH);
    }

    private void renderTabs(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        String[] tabs = {"Recorder", "Saved Macros"};
        int tabWidth = 150;
        int tabHeight = 30;
        int startX = panel.x + 10;
        int tabY = panel.y + 10;

        for (int i = 0; i < tabs.length; i++) {
            int tabX = startX + i * (tabWidth + 5);
            boolean isSelected = selectedTab == i;
            boolean isHovered = isMouseOver(mouseX, mouseY, tabX, tabY, tabWidth, tabHeight);

            int bgColor = isSelected ? ACCENT :
                    isHovered ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                            applyTransparency(new Color(40, 40, 45, 200).getRGB(), trans);

            context.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, bgColor);
            drawBorder(context, tabX, tabY, tabWidth, tabHeight);

            int textColor = isSelected ? Color.WHITE.getRGB() : TEXT;
            context.drawCenteredTextWithShadow(textRenderer, tabs[i], tabX + tabWidth / 2, tabY + (tabHeight - 8) / 2, textColor);
        }
    }

    private void renderRecorderTab(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        int contentY = panel.y + 50;
        int contentHeight = panel.height - 100;

        renderMacroInfoSection(context, panel.x + 20, contentY, panel.width - 40, 120, mouseX, mouseY, trans);
        renderControlsSection(context, panel.x + 20, contentY + 130, panel.width - 40, 100, mouseX, mouseY, trans);
        renderStatsSection(context, panel.x + 20, contentY + 240, panel.width - 40, contentHeight - 240, mouseX, mouseY, trans);
    }

    private void renderMacroInfoSection(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + height, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, height);

        context.drawTextWithShadow(textRenderer, "Macro configuration", x + 10, y + 10, ACCENT);

        context.drawTextWithShadow(textRenderer, "Name:", x + 10, y + 30, TEXT);
        int nameFieldX = x + 60;
        int nameFieldY = y + 25;
        int nameFieldW = 200;
        int nameFieldH = 20;

        boolean nameHovered = isMouseOver(mouseX, mouseY, nameFieldX, nameFieldY, nameFieldW, nameFieldH);
        int nameFieldColor = editingName ? ACCENT_HOVER : (nameHovered ? new Color(60, 60, 65).getRGB() : new Color(50, 50, 55).getRGB());
        context.fill(nameFieldX, nameFieldY, nameFieldX + nameFieldW, nameFieldY + nameFieldH, nameFieldColor);
        drawBorder(context, nameFieldX, nameFieldY, nameFieldW, nameFieldH);
        context.drawTextWithShadow(textRenderer, currentMacroName, nameFieldX + 5, nameFieldY + 5, Color.WHITE.getRGB());
    }

    private void renderControlsSection(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + height, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, height);

        context.drawTextWithShadow(textRenderer, "Controls", x + 10, y + 10, ACCENT);

        int buttonWidth = 100;
        int buttonHeight = 25;
        int buttonSpacing = 10;
        int buttonsStartX = x + 20;
        int buttonsY = y + 35;

        boolean recordHovered = isMouseOver(mouseX, mouseY, buttonsStartX, buttonsY, buttonWidth, buttonHeight);
        int recordColor = isRecording ? RECORDING_COLOR : (recordHovered ? ACCENT_HOVER : ACCENT);
        context.fill(buttonsStartX, buttonsY, buttonsStartX + buttonWidth, buttonsY + buttonHeight, recordColor);
        drawBorder(context, buttonsStartX, buttonsY, buttonWidth, buttonHeight);
        String recordText = isRecording ? "Stop" : "Record";
        context.drawCenteredTextWithShadow(textRenderer, recordText, buttonsStartX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int playButtonX = buttonsStartX + buttonWidth + buttonSpacing;
        boolean playHovered = isMouseOver(mouseX, mouseY, playButtonX, buttonsY, buttonWidth, buttonHeight);
        int playColor = isPlaying ? SUCCESS_COLOR : (playHovered ? ACCENT_HOVER : ACCENT);
        boolean playEnabled = !isRecording && !isPlaying;
        if (!playEnabled) playColor = new Color(100, 100, 100).getRGB();
        context.fill(playButtonX, buttonsY, playButtonX + buttonWidth, buttonsY + buttonHeight, playColor);
        drawBorder(context, playButtonX, buttonsY, buttonWidth, buttonHeight);
        String playText = isPlaying ? "Playback.." : "Play";
        context.drawCenteredTextWithShadow(textRenderer, playText, playButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int saveButtonX = playButtonX + buttonWidth + buttonSpacing;
        boolean saveHovered = isMouseOver(mouseX, mouseY, saveButtonX, buttonsY, buttonWidth, buttonHeight);
        int saveColor = saveHovered ? ACCENT_HOVER : ACCENT;
        boolean saveEnabled = !isRecording && !isPlaying;
        if (!saveEnabled) saveColor = new Color(100, 100, 100).getRGB();
        context.fill(saveButtonX, buttonsY, saveButtonX + buttonWidth, buttonsY + buttonHeight, saveColor);
        drawBorder(context, saveButtonX, buttonsY, buttonWidth, buttonHeight);
        context.drawCenteredTextWithShadow(textRenderer, "Save", saveButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());

        int resetButtonX = saveButtonX + buttonWidth + buttonSpacing;
        boolean resetHovered = isMouseOver(mouseX, mouseY, resetButtonX, buttonsY, buttonWidth, buttonHeight);
        int resetColor = resetHovered ? new Color(255, 100, 100).getRGB() : new Color(200, 80, 80).getRGB();
        boolean resetEnabled = !isRecording && !isPlaying;
        if (!resetEnabled) resetColor = new Color(100, 100, 100).getRGB();
        context.fill(resetButtonX, buttonsY, resetButtonX + buttonWidth, buttonsY + buttonHeight, resetColor);
        drawBorder(context, resetButtonX, buttonsY, buttonWidth, buttonHeight);
        context.drawCenteredTextWithShadow(textRenderer, "Reinitialize", resetButtonX + buttonWidth / 2, buttonsY + 8, Color.WHITE.getRGB());
    }

    private void renderStatsSection(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float trans) {
        context.fill(x, y, x + width, y + height, applyTransparency(new Color(40, 40, 45, 220).getRGB(), trans));
        drawBorder(context, x, y, width, height);

        context.drawTextWithShadow(textRenderer, "Statistics", x + 10, y + 10, ACCENT);

        String statusText = isRecording ? "🔴 Recording in progress.." :
                isPlaying ? "▶️ Playback in progress.." : "⏸️ Ready";
        int statusColor = isRecording ? RECORDING_COLOR :
                isPlaying ? SUCCESS_COLOR : TEXT;
        context.drawTextWithShadow(textRenderer, statusText, x + 10, y + 30, statusColor);

        context.drawTextWithShadow(textRenderer, "Actions in the current macro: 0", x + 10, y + 50, TEXT);
        context.drawTextWithShadow(textRenderer, "Saved macros: " + savedMacros.size(), x + 10, y + 70, TEXT);

        context.drawTextWithShadow(textRenderer, "Shortcuts:", x + 10, y + 100, ACCENT);
        context.drawTextWithShadow(textRenderer, "R - Start/Stop", x + 10, y + 120, new Color(180, 180, 180).getRGB());
        context.drawTextWithShadow(textRenderer, "P - Play", x + 10, y + 135, new Color(180, 180, 180).getRGB());
        context.drawTextWithShadow(textRenderer, "S - Save", x + 10, y + 150, new Color(180, 180, 180).getRGB());
    }

    private void renderSavedMacrosTab(DrawContext context, PanelBounds panel, int mouseX, int mouseY, float trans) {
        int contentY = panel.y + 50;
        int contentHeight = panel.height - 100;

        context.drawTextWithShadow(textRenderer, "Saved macros (" + savedMacros.size() + ")", panel.x + 20, contentY, ACCENT);

        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 40;
        int listHeight = contentHeight - 20;

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int macroHeight = 50;
        int macroSpacing = 5;
        int totalContentHeight = savedMacros.size() * (macroHeight + macroSpacing) - macroSpacing;

        macroListScroll = MathHelper.clamp(macroListScroll, 0, Math.max(0, totalContentHeight - listHeight));

        for (int i = 0; i < savedMacros.size(); i++) {
            MacroEntry macro = savedMacros.get(i);
            int macroY = listY + i * (macroHeight + macroSpacing) - (int)macroListScroll;

            if (macroY + macroHeight < listY || macroY > listY + listHeight) continue;

            boolean isSelected = selectedMacro == macro;
            boolean isHovered = isMouseOver(mouseX, mouseY, listX, macroY, listWidth - 20, macroHeight);

            int bgColor = isSelected ? ACCENT :
                    isHovered ? applyTransparency(new Color(50, 50, 55, 220).getRGB(), trans) :
                            applyTransparency(new Color(40, 40, 45, 200).getRGB(), trans);

            context.fill(listX, macroY, listX + listWidth - 20, macroY + macroHeight, bgColor);
            drawBorder(context, listX, macroY, listWidth - 20, macroHeight);

            context.drawTextWithShadow(textRenderer, macro.name, listX + 10, macroY + 8, Color.WHITE.getRGB());
            context.drawTextWithShadow(textRenderer, "Actions: " + macro.actionCount, listX + 10, macroY + 22, new Color(180, 180, 180).getRGB());

            int buttonY = macroY + 10;
            int buttonWidth = 60;
            int buttonHeight = 20;

            int playButtonX = listX + listWidth - 200;
            boolean playButtonHovered = isMouseOver(mouseX, mouseY, playButtonX, buttonY, buttonWidth, buttonHeight);
            int playButtonColor = playButtonHovered ? SUCCESS_COLOR : new Color(0, 150, 0).getRGB();
            context.fill(playButtonX, buttonY, playButtonX + buttonWidth, buttonY + buttonHeight, playButtonColor);
            drawBorder(context, playButtonX, buttonY, buttonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Play", playButtonX + buttonWidth / 2, buttonY + 5, Color.WHITE.getRGB());

            int editButtonX = playButtonX + buttonWidth + 10;
            boolean editButtonHovered = isMouseOver(mouseX, mouseY, editButtonX, buttonY, buttonWidth, buttonHeight);
            int editButtonColor = editButtonHovered ? ACCENT_HOVER : ACCENT;
            context.fill(editButtonX, buttonY, editButtonX + buttonWidth, buttonY + buttonHeight, editButtonColor);
            drawBorder(context, editButtonX, buttonY, buttonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Edit", editButtonX + buttonWidth / 2, buttonY + 5, Color.WHITE.getRGB());

            int deleteButtonX = editButtonX + buttonWidth + 10;
            boolean deleteButtonHovered = isMouseOver(mouseX, mouseY, deleteButtonX, buttonY, buttonWidth, buttonHeight);
            int deleteButtonColor = deleteButtonHovered ? new Color(255, 100, 100).getRGB() : ERROR_COLOR;
            context.fill(deleteButtonX, buttonY, deleteButtonX + buttonWidth, buttonY + buttonHeight, deleteButtonColor);
            drawBorder(context, deleteButtonX, buttonY, buttonWidth, buttonHeight);
            context.drawCenteredTextWithShadow(textRenderer, "Delete", deleteButtonX + buttonWidth / 2, buttonY + 5, Color.WHITE.getRGB());
        }

        if (totalContentHeight > listHeight) {
            float scrollRatio = (float) listHeight / totalContentHeight;
            int thumbHeight = Math.max(20, (int)(listHeight * scrollRatio));
            int thumbY = listY + (int)((listHeight - thumbHeight) * (macroListScroll / Math.max(1, totalContentHeight - listHeight)));
            context.fill(listX + listWidth - 15, listY, listX + listWidth - 10, listY + listHeight, applyTransparency(new Color(50, 50, 55).getRGB(), trans));
            context.fill(listX + listWidth - 15, thumbY, listX + listWidth - 10, thumbY + thumbHeight, ACCENT);
        }

        context.disableScissor();
    }

    private void renderStatusBar(DrawContext context, PanelBounds panel, float trans) {
        int statusY = panel.y + panel.height + 5;
        context.fill(panel.x, statusY, panel.x + panel.width, statusY + 20, applyTransparency(PANEL_BG, trans));

        long currentTime = System.currentTimeMillis();
        if (currentTime - statusMessageTime > 5000) {
            statusMessage = isRecording ? "Active recording" :
                    isPlaying ? "Playback in progress" : "Ready";
            statusMessageColor = isRecording ? RECORDING_COLOR :
                    isPlaying ? SUCCESS_COLOR : TEXT;
        }
        context.drawTextWithShadow(textRenderer, statusMessage, panel.x + 10, statusY + 6, statusMessageColor);

        String versionText = "Macro Recorder • Amber Client " + AmberClient.MOD_VERSION;
        context.drawTextWithShadow(textRenderer, versionText, panel.x + panel.width - textRenderer.getWidth(versionText) - 10, statusY + 6, TEXT);
    }

    private void renderPlayerInfo(DrawContext context, int mouseX, int mouseY, float trans) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String playerName = client.player.getName().getString();
        SkinTextures skinTextures = client.player.getSkinTextures();
        Identifier skinTexture = skinTextures.texture();

        int headSize = 25;
        int padding = 12;
        int x = padding + 100;
        int y = height - headSize - padding - 35;

        if (skinTexture != null) {
            context.getMatrices().push();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            PlayerSkinDrawer.draw(context, skinTextures, x, y, headSize);
            context.getMatrices().pop();
        }

        int textX = x + headSize + 8;
        int textY = y + (headSize - 8) / 2;
        context.drawTextWithShadow(textRenderer, playerName, textX, textY, 0xFFFFFF);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h) {
        context.fill(x - 1, y - 1, x + w + 1, y, OUTLINE);
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, OUTLINE);
        context.fill(x - 1, y, x, y + h, OUTLINE);
        context.fill(x + w, y, x + w + 1, y + h, OUTLINE);
    }

    private boolean isMouseOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void setStatusMessage(String message, int color) {
        statusMessage = message;
        statusMessageColor = color;
        statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (animProgress < 1.0f) return false;

        PanelBounds panel = calcMainPanel();

        String[] tabs = {"Recorder", "Saved Macros"};
        int tabWidth = 150;
        int tabHeight = 30;
        int startX = panel.x + 10;
        int tabY = panel.y + 10;

        for (int i = 0; i < tabs.length; i++) {
            int tabX = startX + i * (tabWidth + 5);
            if (isMouseOver((int)mx, (int)my, tabX, tabY, tabWidth, tabHeight)) {
                selectedTab = i;
                return true;
            }
        }

        if (selectedTab == 0) {
            return handleRecorderTabClick(mx, my, button, panel);
        } else {
            return handleSavedMacrosTabClick(mx, my, button, panel);
        }
    }

    private boolean handleRecorderTabClick(double mx, double my, int button, PanelBounds panel) {
        int contentY = panel.y + 50;

        int nameFieldX = panel.x + 80;
        int nameFieldY = contentY + 25;
        int nameFieldW = 200;
        int nameFieldH = 20;
        if (isMouseOver((int)mx, (int)my, nameFieldX, nameFieldY, nameFieldW, nameFieldH)) {
            editingName = true;
            return true;
        }

        int buttonWidth = 100;
        int buttonHeight = 25;
        int buttonSpacing = 10;
        int buttonsStartX = panel.x + 40;
        int buttonsY = contentY + 165;

        if (isMouseOver((int)mx, (int)my, buttonsStartX, buttonsY, buttonWidth, buttonHeight)) {
            toggleRecording();
            return true;
        }

        int playButtonX = buttonsStartX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, playButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            playMacro();
            return true;
        }

        int saveButtonX = playButtonX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, saveButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            saveMacro();
            return true;
        }

        int resetButtonX = saveButtonX + buttonWidth + buttonSpacing;
        if (isMouseOver((int)mx, (int)my, resetButtonX, buttonsY, buttonWidth, buttonHeight) && !isRecording && !isPlaying) {
            resetMacro();
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleSavedMacrosTabClick(double mx, double my, int button, PanelBounds panel) {
        int contentY = panel.y + 50;
        int listX = panel.x + 20;
        int listY = contentY + 20;
        int listWidth = panel.width - 40;
        int listHeight = panel.height - 120;

        int macroHeight = 50;
        int macroSpacing = 5;

        for (int i = 0; i < savedMacros.size(); i++) {
            MacroEntry macro = savedMacros.get(i);
            int macroY = listY + i * (macroHeight + macroSpacing) - (int)macroListScroll;

            if (macroY + macroHeight < listY || macroY > listY + listHeight) continue;

            if (isMouseOver((int)mx, (int)my, listX, macroY, listWidth - 20, macroHeight)) {
                selectedMacro = macro;
                int buttonY = macroY + 10;
                int buttonWidth = 60;
                int playButtonX = listX + listWidth - 200;
                if (isMouseOver((int)mx, (int)my, playButtonX, buttonY, buttonWidth, 20)) {
                    playSavedMacro(macro);
                    return true;
                }
                int editButtonX = playButtonX + buttonWidth + 10;
                if (isMouseOver((int)mx, (int)my, editButtonX, buttonY, buttonWidth, 20)) {
                    editSavedMacro(macro);
                    return true;
                }
                int deleteButtonX = editButtonX + buttonWidth + 10;
                if (isMouseOver((int)mx, (int)my, deleteButtonX, buttonY, buttonWidth, 20)) {
                    deleteSavedMacro(macro);
                    return true;
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (selectedTab == 1) {
            PanelBounds panel = calcMainPanel();
            int listHeight = panel.height - 120;
            int totalContentHeight = savedMacros.size() * 55 - 5;
            macroListScroll = (float) MathHelper.clamp(macroListScroll - scrollY * 15, 0, Math.max(0, totalContentHeight - listHeight));
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                editingName = false;
                return true;
            }
            // TODO: Add actual logic to edit currentMacroName
        }
        if (keyCode == GLFW.GLFW_KEY_R) toggleRecording();
        if (keyCode == GLFW.GLFW_KEY_P && !isRecording && !isPlaying) playMacro();
        if (keyCode == GLFW.GLFW_KEY_S && !isRecording && !isPlaying) saveMacro();
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public void renderBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        setStatusMessage(isRecording ? "Recording started" : "Recording stopped", isRecording ? RECORDING_COLOR : SUCCESS_COLOR);
    }

    private void playMacro() {
        isPlaying = true;
        setStatusMessage("Registration stopped", SUCCESS_COLOR);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                isPlaying = false;
                setStatusMessage("Playback completed", SUCCESS_COLOR);
            }
        }, 2000); //TODO: Simulation
    }

    private void saveMacro() {
        MacroEntry newMacro = new MacroEntry(currentMacroName, 0); //TODO: Replace 0 with the real number of actions
        savedMacros.add(newMacro);
        setStatusMessage("Saved macro: " + currentMacroName, SUCCESS_COLOR);
    }

    private void resetMacro() {
        currentMacroName = "New Macro";
        setStatusMessage("Macro reinitialized", SUCCESS_COLOR);
    }

    private void playSavedMacro(MacroEntry macro) {
        setStatusMessage("Reading of " + macro.name, SUCCESS_COLOR);
    }

    private void editSavedMacro(MacroEntry macro) {
        currentMacroName = macro.name;
        selectedTab = 0;
        setStatusMessage("Edition of " + macro.name, ACCENT);
    }

    private void deleteSavedMacro(MacroEntry macro) {
        savedMacros.remove(macro);
        if (selectedMacro == macro) selectedMacro = null;
        setStatusMessage("Deleted macro: " + macro.name, ERROR_COLOR);
    }
}