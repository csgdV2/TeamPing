package com.teamping.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class TeamPingFriendsScreen extends Screen {

    private final Screen parent;
    private final String serverKey;
    private final List<String> friends;
    private final List<Button> rowWidgets = new ArrayList<>();

    private EditBox nameField;

    public TeamPingFriendsScreen(Screen parent) {
        super(Component.literal("TeamPing Friends"));
        this.parent = parent;
        this.serverKey = ServerKey.current();
        this.friends = new ArrayList<>(TeamPingConfig.get().friendsFor(serverKey));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        nameField = new EditBox(this.font, cx - 160, 40, 150, 20, Component.literal("name"));
        nameField.setMaxLength(16);
        nameField.setHint(Component.literal("Type a player name..."));
        nameField.setResponder(s -> refreshRows());
        addWidget(nameField);

        if (connected()) {
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> addFriend(nameField.getValue()))
                    .bounds(cx - 5, 40, 40, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Settings"),
                            b -> com.teamping.compat.MinecraftCompat.setScreen(this.minecraft, ModMenuIntegration.buildSettingsScreen(this)))
                    .bounds(cx + 40, 40, 90, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Settings"),
                            b -> com.teamping.compat.MinecraftCompat.setScreen(this.minecraft, ModMenuIntegration.buildSettingsScreen(this)))
                    .bounds(cx - 65, this.height / 2 + 16, 130, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - 100, this.height - 28, 200, 20).build());

        if (connected()) {
            setInitialFocus(nameField);
        }
        refreshRows();
    }

    private boolean connected() {
        return this.minecraft != null && this.minecraft.getConnection() != null;
    }

    private void refreshRows() {
        for (Button w : rowWidgets) {
            removeWidget(w);
        }
        rowWidgets.clear();

        if (!connected()) {
            return;
        }

        int cx = this.width / 2;
        int leftX = cx - 160;
        int sy = 80;
        for (String n : suggestions()) {
            Button b = Button.builder(Component.literal("+ " + n), btn -> addFriend(n))
                    .bounds(leftX, sy, 150, 16).build();
            addRenderableWidget(b);
            rowWidgets.add(b);
            sy += 18;
        }

        int rightX = cx + 10;
        int fy = 80;
        for (String n : new ArrayList<>(friends)) {
            Button b = Button.builder(Component.literal("✖ " + n), btn -> removeFriend(n))
                    .bounds(rightX, fy, 150, 16).build();
            addRenderableWidget(b);
            rowWidgets.add(b);
            fy += 18;
        }
    }

    private List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            String self = this.minecraft.player != null ? this.minecraft.player.getGameProfile().name() : null;
            for (PlayerInfo e : this.minecraft.getConnection().getOnlinePlayers()) {
                String n = e.getProfile().name();
                if (n != null && (self == null || !n.equalsIgnoreCase(self))) names.add(n);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private List<String> suggestions() {
        String q = nameField.getValue().trim().toLowerCase();
        List<String> out = new ArrayList<>();
        for (String n : onlinePlayers()) {
            if (containsIgnoreCase(friends, n)) continue;
            if (q.isEmpty() || n.toLowerCase().contains(q)) out.add(n);
            if (out.size() >= 8) break;
        }
        return out;
    }

    private void addFriend(String name) {
        if (name == null) return;
        String n = name.trim();
        if (n.isEmpty() || containsIgnoreCase(friends, n)) return;
        friends.add(n);
        nameField.setValue("");
        save();
        refreshRows();
    }

    private void removeFriend(String name) {
        friends.removeIf(f -> f.equalsIgnoreCase(name));
        save();
        refreshRows();
    }

    private void save() {
        TeamPingConfig cfg = TeamPingConfig.get();
        cfg.friendsByServer.put(serverKey, new ArrayList<>(friends));
        cfg.save();
    }

    private static boolean containsIgnoreCase(List<String> list, String v) {
        for (String s : list) if (s.equalsIgnoreCase(v)) return true;
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;

        ctx.centeredText(this.font, this.title, cx, 15, 0xFFFFFFFF);

        if (!connected()) {
            nameField.setVisible(false);
            ctx.centeredText(this.font,
                    Component.literal("Join a server to add players."), cx, this.height / 2 - 20, 0xFFFFAA55);
            ctx.centeredText(this.font,
                    Component.literal("Friends are saved separately for each server."), cx, this.height / 2 - 4, 0xFFAAAAAA);
            return;
        }

        nameField.setVisible(true);
        ctx.text(this.font, Component.literal("Server: " + serverKey), cx - 160, 28, 0xFFAAAAAA);

        nameField.extractRenderState(ctx, mouseX, mouseY, delta);

        ctx.text(this.font, Component.literal("Suggestions (click to add):"), cx - 160, 68, 0xFF88FFAA);
        ctx.text(this.font, Component.literal("Friends (click to remove):"), cx + 10, 68, 0xFF88CCFF);
        if (friends.isEmpty()) {
            ctx.text(this.font, Component.literal("(none yet)"), cx + 10, 82, 0xFF888888);
        }
        if (suggestions().isEmpty()) {
            ctx.text(this.font, Component.literal("(no online players)"), cx - 160, 82, 0xFF888888);
        }
    }

    @Override
    public void onClose() {
        save();
        com.teamping.compat.MinecraftCompat.setScreen(this.minecraft, parent);
    }
}
