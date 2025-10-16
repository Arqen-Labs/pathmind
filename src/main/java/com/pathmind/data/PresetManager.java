package com.pathmind.data;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class that manages Pathmind workspace presets.
 */
public final class PresetManager {
    private static final String BASE_DIRECTORY_NAME = "pathmind";
    private static final String PRESETS_DIRECTORY_NAME = "presets";
    private static final String ACTIVE_PRESET_FILE_NAME = "active_preset.txt";
    private static final String DEFAULT_PRESET_NAME = "Default";

    private PresetManager() {
    }

    /**
     * Ensure the base directories exist and that there is always an active preset defined.
     */
    public static void initialize() {
        try {
            ensureDirectoryExists(getBaseDirectory());
            ensureDirectoryExists(getPresetsDirectory());
            ensureActivePresetFile();
        } catch (IOException e) {
            System.err.println("Failed to initialize preset directories: " + e.getMessage());
        }
    }

    /**
     * Get the currently active preset name.
     */
    public static String getActivePreset() {
        initialize();
        Path activePresetFile = getBaseDirectory().resolve(ACTIVE_PRESET_FILE_NAME);
        if (Files.exists(activePresetFile)) {
            try {
                String value = Files.readString(activePresetFile, StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            } catch (IOException e) {
                System.err.println("Failed to read active preset: " + e.getMessage());
            }
        }
        return DEFAULT_PRESET_NAME;
    }

    /**
     * Set the active preset name.
     */
    public static void setActivePreset(String presetName) {
        String sanitized = sanitizePresetName(presetName);
        if (sanitized.isEmpty()) {
            return;
        }
        initialize();
        try {
            Files.writeString(getBaseDirectory().resolve(ACTIVE_PRESET_FILE_NAME), sanitized, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to write active preset: " + e.getMessage());
        }
    }

    /**
     * List all available presets.
     */
    public static List<String> getAvailablePresets() {
        initialize();
        List<String> presets = new ArrayList<>();
        Path presetsDirectory = getPresetsDirectory();
        try (Stream<Path> pathStream = Files.list(presetsDirectory)) {
            presets = pathStream
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                .map(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.substring(0, fileName.length() - 5);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            System.err.println("Failed to list presets: " + e.getMessage());
        }

        if (presets.stream().noneMatch(name -> name.equalsIgnoreCase(DEFAULT_PRESET_NAME))) {
            presets.add(DEFAULT_PRESET_NAME);
        }

        String activePreset = getActivePreset();
        if (presets.stream().noneMatch(name -> name.equalsIgnoreCase(activePreset))) {
            presets.add(activePreset);
        }

        presets.sort(Comparator.comparing(String::toLowerCase));
        return presets;
    }

    /**
     * Create a new preset file and return the sanitized preset name.
     */
    public static Optional<String> createPreset(String presetName) {
        String sanitized = sanitizePresetName(presetName);
        if (sanitized.isEmpty()) {
            return Optional.empty();
        }

        List<String> existing = getAvailablePresets();
        for (String preset : existing) {
            if (preset.equalsIgnoreCase(sanitized)) {
                return Optional.empty();
            }
        }

        Path presetPath = getPresetPath(sanitized);
        if (Files.exists(presetPath)) {
            return Optional.empty();
        }

        try {
            Files.writeString(presetPath, "{}", StandardCharsets.UTF_8);
            return Optional.of(sanitized);
        } catch (IOException e) {
            System.err.println("Failed to create preset: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete the preset file with the provided name.
     *
     * @param presetName name of the preset to delete
     * @return {@code true} if the preset file was removed
     */
    public static boolean deletePreset(String presetName) {
        String sanitized = sanitizePresetName(presetName);
        if (sanitized.isEmpty() || sanitized.equalsIgnoreCase(DEFAULT_PRESET_NAME)) {
            return false;
        }

        initialize();
        Path presetPath = getPresetsDirectory().resolve(sanitized + ".json");
        if (!Files.exists(presetPath)) {
            return false;
        }

        try {
            Files.delete(presetPath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete preset: " + e.getMessage());
            return false;
        }
    }

    /**
     * Expose the display name of the default preset.
     */
    public static String getDefaultPresetName() {
        return DEFAULT_PRESET_NAME;
    }

    /**
     * Resolve the save path for a preset.
     */
    public static Path getPresetPath(String presetName) {
        initialize();
        String sanitized = sanitizePresetName(presetName);
        if (sanitized.isEmpty()) {
            sanitized = DEFAULT_PRESET_NAME;
        }
        return getPresetsDirectory().resolve(sanitized + ".json");
    }

    /**
     * Ensure the Pathmind workspace directory exists inside the Minecraft directory.
     */
    public static Path getBaseDirectory() {
        Path minecraftDirectory = getMinecraftDirectory();
        return minecraftDirectory.resolve(BASE_DIRECTORY_NAME);
    }

    private static Path getPresetsDirectory() {
        return getBaseDirectory().resolve(PRESETS_DIRECTORY_NAME);
    }

    private static Path getMinecraftDirectory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.runDirectory != null) {
            return client.runDirectory.toPath();
        }
        return Paths.get(System.getProperty("user.home"), ".minecraft");
    }

    private static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private static void ensureActivePresetFile() throws IOException {
        Path activePresetFile = getBaseDirectory().resolve(ACTIVE_PRESET_FILE_NAME);
        if (!Files.exists(activePresetFile)) {
            Files.writeString(activePresetFile, DEFAULT_PRESET_NAME, StandardCharsets.UTF_8);
        }
    }

    private static String sanitizePresetName(String presetName) {
        if (presetName == null) {
            return "";
        }
        String trimmed = presetName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Replace characters that are invalid for file names on most systems
        String sanitized = trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Collapse multiple spaces to a single space
        sanitized = sanitized.replaceAll("\\s+", " ");
        return sanitized.trim();
    }
}
