package com.teamping.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.getNetworkHandler() == null) {
                return buildSettingsScreen(parent);
            }
            return new TeamPingFriendsScreen(parent);
        };
    }

    public static Screen buildSettingsScreen(Screen parent) {
        TeamPingConfig cfg = TeamPingConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("TeamPing Settings"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory marker = builder.getOrCreateCategory(Text.literal("Marker"));

        marker.addEntry(eb.startDoubleField(Text.literal("Scale"), cfg.markerScale)
                .setDefaultValue(1.0)
                .setMin(0.25)
                .setMax(5.0)
                .setTooltip(Text.literal("Overall size of the marker on screen. Markers also shrink automatically the farther away they are."))
                .setSaveConsumer(v -> cfg.markerScale = v)
                .build());

        marker.addEntry(eb.startIntSlider(Text.literal("Visible Duration (sec)"),
                        cfg.markerDurationSeconds, 3, 600)
                .setDefaultValue(20)
                .setTooltip(Text.literal("How long a marker stays on screen after it is placed, in seconds."))
                .setSaveConsumer(v -> cfg.markerDurationSeconds = v)
                .build());

        marker.addEntry(eb.startBooleanToggle(Text.literal("Animate Marker"), cfg.animateMarker)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Play a quick spawn-in animation when a marker appears."))
                .setSaveConsumer(v -> cfg.animateMarker = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Show Owner"),
                        TeamPingConfig.Visibility.class, cfg.showOwner)
                .setDefaultValue(TeamPingConfig.Visibility.ALWAYS)
                .setTooltip(Text.literal("When to show who placed the marker. Always, only when your crosshair is near it (On Hover), or Never."))
                .setSaveConsumer(v -> cfg.showOwner = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Show Description"),
                        TeamPingConfig.Visibility.class, cfg.showDescription)
                .setDefaultValue(TeamPingConfig.Visibility.NEVER)
                .setTooltip(Text.literal("When to show what was marked (block/entity name or its icon). Always, On Hover, or Never."))
                .setSaveConsumer(v -> cfg.showDescription = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Show Distance"),
                        TeamPingConfig.Visibility.class, cfg.showDistance)
                .setDefaultValue(TeamPingConfig.Visibility.NEVER)
                .setTooltip(Text.literal("When to show how far the marker is from you, in blocks. Always, On Hover, or Never."))
                .setSaveConsumer(v -> cfg.showDistance = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Show Coordinates"),
                        TeamPingConfig.Visibility.class, cfg.showCoordinates)
                .setDefaultValue(TeamPingConfig.Visibility.NEVER)
                .setTooltip(Text.literal("When to show the X Y Z coordinates of the marker. Always, On Hover, or Never."))
                .setSaveConsumer(v -> cfg.showCoordinates = v)
                .build());

        marker.addEntry(eb.startIntSlider(Text.literal("Hover Radius"), cfg.hoverRadius, 10, 200)
                .setDefaultValue(45)
                .setTooltip(Text.literal("How close your crosshair must be (in pixels) to trigger \"On Hover\" elements."))
                .setSaveConsumer(v -> cfg.hoverRadius = v)
                .build());

        marker.addEntry(eb.startStrField(Text.literal("Owner Suffix"), cfg.ownerSuffix)
                .setDefaultValue("'s Marker")
                .setTooltip(Text.literal("Text shown after the owner's name, e.g. \"'s Marker\". Leave blank to show just the name."))
                .setSaveConsumer(v -> cfg.ownerSuffix = v)
                .build());

        marker.addEntry(eb.startBooleanToggle(Text.literal("Compact Mode"), cfg.compactMode)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Tighter line spacing so the marker label takes up less vertical space."))
                .setSaveConsumer(v -> cfg.compactMode = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Owner Display"),
                        TeamPingConfig.OwnerDisplay.class, cfg.ownerDisplay)
                .setDefaultValue(TeamPingConfig.OwnerDisplay.HEAD)
                .setTooltip(Text.literal("Show the owner as their player Head (with skin) or as plain Name text."))
                .setSaveConsumer(v -> cfg.ownerDisplay = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Description Display"),
                        TeamPingConfig.DescriptionDisplay.class, cfg.descriptionDisplay)
                .setDefaultValue(TeamPingConfig.DescriptionDisplay.ICON)
                .setTooltip(Text.literal("Show the marked target as its item/block Icon, or as plain Text. Entities without an icon fall back to text."))
                .setSaveConsumer(v -> cfg.descriptionDisplay = v)
                .build());

        marker.addEntry(eb.startBooleanToggle(Text.literal("Text Shadow"), cfg.textShadow)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Draw a drop shadow behind marker text for better contrast."))
                .setSaveConsumer(v -> cfg.textShadow = v)
                .build());

        marker.addEntry(eb.startBooleanToggle(Text.literal("Show Background"), cfg.showBackground)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Draw a semi-transparent box behind the marker label."))
                .setSaveConsumer(v -> cfg.showBackground = v)
                .build());

        marker.addEntry(eb.startEnumSelector(Text.literal("Ping Sound"),
                        TeamPingConfig.SoundMode.class, cfg.soundMode)
                .setDefaultValue(TeamPingConfig.SoundMode.ALL)
                .setTooltip(Text.literal("When to play the ping sound: Everyone (any ping including yours), Only mine (just your own pings), or None."))
                .setSaveConsumer(v -> cfg.soundMode = v)
                .build());

        marker.addEntry(eb.startColorField(Text.literal("Marker Color"), cfg.markerColor)
                .setDefaultValue(0x55CCFF)
                .setTooltip(Text.literal("Color of the diamond icon that points at the marked spot."))
                .setSaveConsumer(v -> cfg.markerColor = v)
                .build());

        ConfigCategory chat = builder.getOrCreateCategory(Text.literal("Chat colours"));

        chat.addEntry(eb.startColorField(Text.literal("Player name colour"), cfg.chatNameColor)
                .setDefaultValue(0x55FF55)
                .setTooltip(Text.literal("Color of \"You\" in your own ping's chat message."))
                .setSaveConsumer(v -> cfg.chatNameColor = v)
                .build());

        chat.addEntry(eb.startColorField(Text.literal("Block name colour"), cfg.chatBlockColor)
                .setDefaultValue(0xFFFFFF)
                .setTooltip(Text.literal("Color of the marked block/entity name in the chat message."))
                .setSaveConsumer(v -> cfg.chatBlockColor = v)
                .build());

        chat.addEntry(eb.startColorField(Text.literal("Details colour"), cfg.chatDetailColor)
                .setDefaultValue(0xAAAAAA)
                .setTooltip(Text.literal("Color of the surrounding text (distance, coordinates, and other players' names)."))
                .setSaveConsumer(v -> cfg.chatDetailColor = v)
                .build());

        builder.setSavingRunnable(cfg::save);
        return builder.build();
    }
}
