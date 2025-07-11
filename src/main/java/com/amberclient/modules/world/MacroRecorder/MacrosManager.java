package com.amberclient.modules.world.MacroRecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MacrosManager {
    private static final Logger LOGGER = LogManager.getLogger("amberclient-macrosmanager");
    private static final String MACROS_FOLDER = "amberclient/macros";
    private static final String MACROS_FILE = "saved_macros.json";
    private static final String ACTIONS_FOLDER = "actions";

    private final Gson gson;
    private final Path macrosDir;
    private final Path macrosFile;
    private final Path actionsDir;

    public MacrosManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        Path gameDir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath());
        this.macrosDir = gameDir.resolve(MACROS_FOLDER);
        this.macrosFile = macrosDir.resolve(MACROS_FILE);
        this.actionsDir = macrosDir.resolve(ACTIONS_FOLDER);

        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(macrosDir);
            Files.createDirectories(actionsDir);
            LOGGER.info("Created macro directories: {}", macrosDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create macro directories", e);
        }
    }

    public void saveMacro(String name, List<MacroRecordingSystem.MacroAction> actions) {
        try {
            String macroId = generateMacroId(name);
            saveActionsToFile(macroId, actions);
            SavedMacro savedMacro = new SavedMacro(
                    name,
                    macroId,
                    actions.size(),
                    System.currentTimeMillis()
            );
            List<SavedMacro> macros = loadMacrosList();
            macros.removeIf(m -> m.name.equals(name));
            macros.add(savedMacro);
            saveMacrosList(macros);
            LOGGER.info("Saved macro '{}' with {} actions", name, actions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to save macro '{}'", name, e);
            throw new RuntimeException("Failed to save macro: " + e.getMessage());
        }
    }

    public List<SavedMacro> loadMacros() {
        try {
            return loadMacrosList();
        } catch (Exception e) {
            LOGGER.error("Failed to load macros", e);
            return new ArrayList<>();
        }
    }

    public List<MacroRecordingSystem.MacroAction> loadMacroActions(String macroId) {
        try {
            Path actionsFile = actionsDir.resolve(macroId + ".json");
            if (!Files.exists(actionsFile)) {
                LOGGER.warn("Actions file not found for macro ID: {}", macroId);
                return new ArrayList<>();
            }
            String json = Files.readString(actionsFile);
            Type listType = new TypeToken<List<MacroRecordingSystem.MacroAction>>(){}.getType();
            List<MacroRecordingSystem.MacroAction> actions = gson.fromJson(json, listType);
            return actions != null ? actions : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load actions for macro ID: {}", macroId, e);
            return new ArrayList<>();
        }
    }

    public void deleteMacro(String macroId) {
        try {
            Path actionsFile = actionsDir.resolve(macroId + ".json");
            if (Files.exists(actionsFile)) {
                Files.delete(actionsFile);
            }
            List<SavedMacro> macros = loadMacrosList();
            macros.removeIf(m -> m.id.equals(macroId));
            saveMacrosList(macros);
            LOGGER.info("Deleted macro with ID: {}", macroId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete macro with ID: {}", macroId, e);
        }
    }

    public void renameMacro(String macroId, String newName) {
        try {
            List<SavedMacro> macros = loadMacrosList();
            SavedMacro macro = macros.stream()
                    .filter(m -> m.id.equals(macroId))
                    .findFirst()
                    .orElse(null);

            if (macro != null) {
                macros.removeIf(m -> m.name.equals(newName) && !m.id.equals(macroId));
                macro.name = newName;
                saveMacrosList(macros);
                LOGGER.info("Renamed macro {} to '{}'", macroId, newName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to rename macro {}", macroId, e);
        }
    }

    private List<SavedMacro> loadMacrosList() {
        try {
            if (!Files.exists(macrosFile)) {
                return new ArrayList<>();
            }
            String json = Files.readString(macrosFile);
            Type listType = new TypeToken<List<SavedMacro>>(){}.getType();
            List<SavedMacro> macros = gson.fromJson(json, listType);
            return macros != null ? macros : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load macros list", e);
            return new ArrayList<>();
        }
    }

    private void saveMacrosList(List<SavedMacro> macros) throws IOException {
        String json = gson.toJson(macros);
        Files.writeString(macrosFile, json);
    }

    private void saveActionsToFile(String macroId, List<MacroRecordingSystem.MacroAction> actions) throws IOException {
        Path actionsFile = actionsDir.resolve(macroId + ".json");
        String json = gson.toJson(actions);
        Files.writeString(actionsFile, json);
    }

    private String generateMacroId(String name) {
        String cleanName = name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return cleanName + "_" + System.currentTimeMillis();
    }

    public void cleanupOrphanedFiles() {
        try {
            List<SavedMacro> macros = loadMacrosList();
            List<String> validIds = macros.stream().map(m -> m.id).toList();
            Files.list(actionsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String id = fileName.substring(0, fileName.lastIndexOf('.'));
                        if (!validIds.contains(id)) {
                            try {
                                Files.delete(path);
                                LOGGER.info("Deleted orphaned actions file: {}", fileName);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete orphaned file: {}", fileName, e);
                            }
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup orphaned files", e);
        }
    }

    public void exportMacro(String macroId, Path exportPath) {
        try {
            List<SavedMacro> macros = loadMacrosList();
            SavedMacro macro = macros.stream()
                    .filter(m -> m.id.equals(macroId))
                    .findFirst()
                    .orElse(null);
            if (macro == null) {
                throw new IllegalArgumentException("Macro not found: " + macroId);
            }
            List<MacroRecordingSystem.MacroAction> actions = loadMacroActions(macroId);
            MacroExport export = new MacroExport(macro, actions);
            String json = gson.toJson(export);
            Files.writeString(exportPath, json);
            LOGGER.info("Exported macro '{}' to {}", macro.name, exportPath);
        } catch (Exception e) {
            LOGGER.error("Failed to export macro {}", macroId, e);
            throw new RuntimeException("Failed to export macro: " + e.getMessage());
        }
    }

    public void importMacro(Path importPath) {
        try {
            String json = Files.readString(importPath);
            MacroExport export = gson.fromJson(json, MacroExport.class);
            if (export.macro == null || export.actions == null) {
                throw new IllegalArgumentException("Invalid macro file format");
            }
            String newId = generateMacroId(export.macro.name);
            export.macro.id = newId;
            export.macro.createdTime = System.currentTimeMillis();
            saveActionsToFile(newId, export.actions);
            List<SavedMacro> macros = loadMacrosList();
            macros.add(export.macro);
            saveMacrosList(macros);
            LOGGER.info("Imported macro '{}' with {} actions", export.macro.name, export.actions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to import macro from {}", importPath, e);
            throw new RuntimeException("Failed to import macro: " + e.getMessage());
        }
    }

    public static class SavedMacro {
        public String name;
        public String id;
        public int actionCount;
        public long createdTime;
        public boolean isEnabled;

        public SavedMacro(String name, String id, int actionCount, long createdTime) {
            this.name = name;
            this.id = id;
            this.actionCount = actionCount;
            this.createdTime = createdTime;
            this.isEnabled = true;
        }

        public SavedMacro() {}
    }

    private static class MacroExport {
        public SavedMacro macro;
        public List<MacroRecordingSystem.MacroAction> actions;

        public MacroExport(SavedMacro macro, List<MacroRecordingSystem.MacroAction> actions) {
            this.macro = macro;
            this.actions = actions;
        }
    }
}