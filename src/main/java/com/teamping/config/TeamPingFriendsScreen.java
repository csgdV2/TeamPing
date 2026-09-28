package com.teamping.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TeamPingFriendsScreen extends Screen {

    private final Screen parent;
    private final String serverKey;
    private final List<String> friends;
    private final List<ButtonWidget> rowWidgets = new ArrayList<>();

    private TextFieldWidget nameField;

    public TeamPingFriendsScreen(Screen parent) {
        super(Text.literal("TeamPing Friends"));
        this.parent = parent;
        this.serverKey = ServerKey.current();
        this.friends = new ArrayList<>(TeamPingConfig.get().friendsFor(serverKey));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        nameField = new TextFieldWidget(this.textRenderer, cx - 160, 40, 150, 20, Text.literal("name"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Type a player name..."));
        nameField.setChangedListener(s -> refreshRows());
        addSelectableChild(nameField);

        if (connected()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addFriend(nameField.getText()))
                    .dimensions(cx - 5, 40, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                            b -> this.client.setScreen(ModMenuIntegration.buildSettingsScreen(this)))
                    .dimensions(cx + 40, 40, 90, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                            b -> this.client.setScreen(ModMenuIntegration.buildSettingsScreen(this)))
                    .dimensions(cx - 65, this.height / 2 + 16, 130, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 100, this.height - 28, 200, 20).build());

        if (connected()) {
            setInitialFocus(nameField);
        }
        refreshRows();
    }

    private boolean connected() {
        return this.client != null && this.client.getNetworkHandler() != null;
    }

    private void refreshRows() {
        for (ButtonWidget w : rowWidgets) {
            remove(w);
        }
        rowWidgets.clear();

        if (!connected()) {
            return;
        }

        int cx = this.width / 2;
        int leftX = cx - 160;
        int sy = 80;
        for (String n : suggestions()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal("+ " + n), btn -> addFriend(n))
                    .dimensions(leftX, sy, 150, 16).build();
            addDrawableChild(b);
            rowWidgets.add(b);
            sy += 18;
        }

        int rightX = cx + 10;
        int fy = 80;
        for (String n : new ArrayList<>(friends)) {
            ButtonWidget b = ButtonWidget.builder(Text.literal("✖ " + n), btn -> removeFriend(n))
                    .dimensions(rightX, fy, 150, 16).build();
            addDrawableChild(b);
            rowWidgets.add(b);
            fy += 18;
        }
    }

    private List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        if (this.client != null && this.client.getNetworkHandler() != null) {
            String self = this.client.player != null ? this.client.player.getGameProfile().name() : null;
            for (PlayerListEntry e : this.client.getNetworkHandler().getPlayerList()) {
                String n = e.getProfile().name();
                if (n != null && (self == null || !n.equalsIgnoreCase(self))) names.add(n);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private List<String> suggestions() {
        String q = nameField.getText().trim().toLowerCase();
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
        nameField.setText("");
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 15, 0xFFFFFF);

        if (!connected()) {
            nameField.setVisible(false);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Join a server to add players."), cx, this.height / 2 - 20, 0xFFAA55);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Friends are saved separately for each server."), cx, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        nameField.setVisible(true);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Server: " + serverKey), cx - 160, 28, 0xAAAAAA);

        nameField.render(ctx, mouseX, mouseY, delta);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Suggestions (click to add):"), cx - 160, 68, 0x88FFAA);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Friends (click to remove):"), cx + 10, 68, 0x88CCFF);
        if (friends.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("(none yet)"), cx + 10, 82, 0x888888);
        }
        if (suggestions().isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("(no online players)"), cx - 160, 82, 0x888888);
        }
    }

    @Override
    public void close() {
        save();
        this.client.setScreen(parent);
    }
}
