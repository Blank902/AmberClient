package com.nexusclient.ui.theme;

import java.awt.Color;

public class Theme {
    // Background colors
    private final int baseBg;
    private final int panelBg;
    private final int moduleBg;
    private final int moduleEnabledBg;
    
    // Accent colors
    private final int primaryAccent;
    private final int secondaryAccent;
    private final int hoverAccent;
    
    // Text colors
    private final int primaryText;
    private final int secondaryText;
    private final int disabledText;
    
    // Border colors
    private final int borderColor;
    private final int focusBorder;
    
    // Special colors
    private final int successColor;
    private final int warningColor;
    private final int errorColor;
    
    private final String name;
    
    public Theme(String name,
                 int baseBg, int panelBg, int moduleBg, int moduleEnabledBg,
                 int primaryAccent, int secondaryAccent, int hoverAccent,
                 int primaryText, int secondaryText, int disabledText,
                 int borderColor, int focusBorder,
                 int successColor, int warningColor, int errorColor) {
        this.name = name;
        this.baseBg = baseBg;
        this.panelBg = panelBg;
        this.moduleBg = moduleBg;
        this.moduleEnabledBg = moduleEnabledBg;
        this.primaryAccent = primaryAccent;
        this.secondaryAccent = secondaryAccent;
        this.hoverAccent = hoverAccent;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.disabledText = disabledText;
        this.borderColor = borderColor;
        this.focusBorder = focusBorder;
        this.successColor = successColor;
        this.warningColor = warningColor;
        this.errorColor = errorColor;
    }
    
    // Getters
    public String getName() { return name; }
    public int getBaseBg() { return baseBg; }
    public int getPanelBg() { return panelBg; }
    public int getModuleBg() { return moduleBg; }
    public int getModuleEnabledBg() { return moduleEnabledBg; }
    public int getPrimaryAccent() { return primaryAccent; }
    public int getSecondaryAccent() { return secondaryAccent; }
    public int getHoverAccent() { return hoverAccent; }
    public int getPrimaryText() { return primaryText; }
    public int getSecondaryText() { return secondaryText; }
    public int getDisabledText() { return disabledText; }
    public int getBorderColor() { return borderColor; }
    public int getFocusBorder() { return focusBorder; }
    public int getSuccessColor() { return successColor; }
    public int getWarningColor() { return warningColor; }
    public int getErrorColor() { return errorColor; }
    
    public int withAlpha(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * alpha) << 24) | (color & 0xFFFFFF);
    }
}
