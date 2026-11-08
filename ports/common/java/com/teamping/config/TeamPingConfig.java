package com.teamping.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeamPingConfig {

    public enum Visibility {
        ALWAYS("Always"), ON_HOVER("On Hover"), NEVER("Never");
        private final String label;
        Visibility(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum OwnerDisplay {
        HEAD("Head"), NAME("Name");
        private final String label;
        OwnerDisplay(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum DescriptionDisplay {
        TEXT("Text"), ICON("Icon");
        private final String label;
        DescriptionDisplay(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum SoundMode {
        ALL("Everyone"), MINE("Only mine"), NONE("None");
        private final String label;
        SoundMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("teamping.json");

    private static TeamPingConfig INSTANCE;

    public Map<String, List<String>> friendsByServer = new LinkedHashMap<>();

    public double markerScale = 1.0;
    public int markerDurationSeconds = 20;
    public boolean animateMarker = true;

    public Visibility showOwner = Visibility.ALWAYS;
    public Visibility showDescription = Visibility.NEVER;
    public Visibility showDistance = Visibility.NEVER;
    public Visibility showCoordinates = Visibility.NEVER;
    public int hoverRadius = 45;

    public String ownerSuffix = "'s Marker";
    public boolean compactMode = false;
    public OwnerDisplay ownerDisplay = OwnerDisplay.HEAD;
    public DescriptionDisplay descriptionDisplay = DescriptionDisplay.ICON;
    public boolean textShadow = true;
    public boolean showBackground = false;
    public SoundMode soundMode = SoundMode.ALL;

    public int markerColor = 0x55CCFF;
    public int chatNameColor = 0x55FF55;
    public int chatBlockColor = 0xFFFFFF;
    public int chatDetailColor = 0xAAAAAA;

    public static TeamPingConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public List<String> friendsFor(String serverKey) {
        return friendsByServer.computeIfAbsent(serverKey, k -> new ArrayList<>());
    }

    private static TeamPingConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                TeamPingConfig cfg = GSON.fromJson(json, TeamPingConfig.class);
                if (cfg != null) {
                    if (cfg.friendsByServer == null) cfg.friendsByServer = new LinkedHashMap<>();
                    if (cfg.showOwner == null) cfg.showOwner = Visibility.ALWAYS;
                    if (cfg.showDescription == null) cfg.showDescription = Visibility.NEVER;
                    if (cfg.showDistance == null) cfg.showDistance = Visibility.NEVER;
                    if (cfg.showCoordinates == null) cfg.showCoordinates = Visibility.NEVER;
                    if (cfg.ownerDisplay == null) cfg.ownerDisplay = OwnerDisplay.HEAD;
                    if (cfg.descriptionDisplay == null) cfg.descriptionDisplay = DescriptionDisplay.ICON;
                    if (cfg.ownerSuffix == null) cfg.ownerSuffix = "'s Marker";
                    if (cfg.soundMode == null) cfg.soundMode = SoundMode.ALL;
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("[TeamPing] Failed to read config, using defaults: " + e);
            }
        }
        TeamPingConfig cfg = new TeamPingConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[TeamPing] Failed to save config: " + e);
        }
    }
}
