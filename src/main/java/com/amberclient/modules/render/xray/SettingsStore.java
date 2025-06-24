package com.amberclient.modules.render.xray;

public class SettingsStore {
    private static SettingsStore instance;
    private final StateSettings settings;

    private SettingsStore() {
        settings = new StateSettings();
    }

    public static SettingsStore getInstance() {
        if (instance == null) {
            instance = new SettingsStore();
        }
        return instance;
    }

    public StateSettings get() {
        return settings;
    }

    public static class StateSettings {
        private boolean active = false;
        private boolean exposedOnly = false;
        private boolean showLava = false;
        private int halfRange = 4;
        private boolean oreSim = false;

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isShowLava() { return showLava; }
        public void setShowLava(boolean showLava) { this.showLava = showLava; }
        public boolean isExposedOnly() { return exposedOnly; }
        public void setExposedOnly(boolean exposedOnly) { this.exposedOnly = exposedOnly; }
        public int getHalfRange() { return halfRange; }
        public void setHalfRange(int halfRange) { this.halfRange = halfRange; }
        public boolean isOreSim() { return oreSim; }
        public void setOreSim(boolean oreSim) { this.oreSim = oreSim; }
    }
}