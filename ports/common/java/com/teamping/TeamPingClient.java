package com.teamping;

import com.teamping.config.ServerKey;
import com.teamping.config.TeamPingConfig;
import com.teamping.ping.MarkerManager;
import com.teamping.ping.PingCodec;
import com.teamping.render.MarkerRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


import java.util.List;
import java.util.Locale;

public class TeamPingClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("teamping", "main"));

    private static KeyMapping pingKey;
    private static KeyMapping clearKey;

    @Override
    public void onInitializeClient() {
        pingKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.teamping.ping",
                com.teamping.compat.MinecraftCompat.KEYBOARD,
                InputConstants.KEY_R,
                CATEGORY
        ));

        clearKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.teamping.clear",
                com.teamping.compat.MinecraftCompat.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (pingKey.consumeClick()) {
                sendPing(client);
            }
            while (clearKey.consumeClick()) {
                MarkerManager.clear();
            }
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signature, sender, params, timestamp) -> {
            return !handleIncoming(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return !handleIncoming(message.getString());
        });

        MarkerRenderer.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("teamping")
                    .then(ClientCommands.literal("add")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .executes(c -> {
                                        addFriend(c.getSource(), StringArgumentType.getString(c, "name"));
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("remove")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .executes(c -> {
                                        removeFriend(c.getSource(), StringArgumentType.getString(c, "name"));
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("list")
                            .executes(c -> {
                                listFriends(c.getSource());
                                return 1;
                            })));
        });
    }

    private void addFriend(FabricClientCommandSource source, String name) {
        String key = ServerKey.current();
        TeamPingConfig cfg = TeamPingConfig.get();
        List<String> friends = cfg.friendsFor(key);
        for (String f : friends) {
            if (f.equalsIgnoreCase(name)) {
                source.sendFeedback(Component.literal("[TeamPing] " + name + " is already a friend.").withStyle(ChatFormatting.YELLOW));
                return;
            }
        }
        friends.add(name);
        cfg.save();
        source.sendFeedback(Component.literal("[TeamPing] Added " + name + ".").withStyle(ChatFormatting.GREEN));
    }

    private void removeFriend(FabricClientCommandSource source, String name) {
        TeamPingConfig cfg = TeamPingConfig.get();
        List<String> friends = cfg.friendsFor(ServerKey.current());
        boolean removed = friends.removeIf(f -> f.equalsIgnoreCase(name));
        cfg.save();
        source.sendFeedback(Component.literal(removed ? "[TeamPing] Removed " + name + "." : "[TeamPing] " + name + " was not a friend.")
                .withStyle(removed ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void listFriends(FabricClientCommandSource source) {
        List<String> friends = TeamPingConfig.get().friendsFor(ServerKey.current());
        if (friends.isEmpty()) {
            source.sendFeedback(Component.literal("[TeamPing] No friends added for this server.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        source.sendFeedback(Component.literal("[TeamPing] Friends: " + String.join(", ", friends)).withStyle(ChatFormatting.AQUA));
    }

    private void playPingSound(boolean mine) {
        TeamPingConfig cfg = TeamPingConfig.get();
        TeamPingConfig.SoundMode mode = cfg.soundMode;
        if (mode == TeamPingConfig.SoundMode.NONE) return;
        if (mode == TeamPingConfig.SoundMode.MINE && !mine) return;
        Minecraft client = Minecraft.getInstance();
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f, 1.0f));
    }

    private boolean handleIncoming(String raw) {
        PingCodec.Ping ping = PingCodec.decode(raw);
        if (ping == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        String self = client.player != null ? client.player.getName().getString() : "";
        if (!ping.owner().equalsIgnoreCase(self) && isFriend(ping.owner())) {
            receivePing(ping.owner(), ping);
        }
        return true;
    }

    private boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : TeamPingConfig.get().friendsFor(ServerKey.current())) {
            if (f.equalsIgnoreCase(name.trim())) return true;
        }
        return false;
    }

    private void sendPing(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        Vec3 eye = client.player.getEyePosition();
        Vec3 look = client.player.getViewVector(1.0f);
        double reach = 512.0;
        Vec3 end = eye.add(look.scale(reach));

        Vec3 targetPos;
        String blockName;
        String icon = "";

        BlockHitResult blockHit = client.level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation().distanceTo(eye) : reach;

        AABB searchBox = client.player.getBoundingBox().expandTowards(look.scale(blockDist)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(client.player, eye, eye.add(look.scale(blockDist)),
                searchBox, e -> !e.isSpectator() && e.isAlive() && e != client.player, blockDist * blockDist);

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            targetPos = target.getBoundingBox().getCenter();
            if (target instanceof Player) {
                blockName = target.isInvisibleTo(client.player) ? "Unknown Player" : target.getName().getString();
            } else {
                blockName = target.isInvisibleTo(client.player) ? "Unknown" : target.getName().getString();
            }
            icon = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = blockHit.getBlockPos();
            targetPos = Vec3.atCenterOf(bp);
            var block = client.level.getBlockState(bp).getBlock();
            blockName = block.getName().getString();
            icon = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        } else {
            targetPos = end;
            blockName = "Location";
        }

        double dist = eye.distanceTo(targetPos);
        String self = client.player.getName().getString();
        String dimension = client.level.dimension().identifier().toString();
        String encoded = PingCodec.encode(self, dimension, targetPos.x, targetPos.y, targetPos.z, icon, blockName);

        client.player.sendSystemMessage(buildMarkedText("You", blockName, dist, targetPos));
        showMarker(self, dimension, icon, blockName, targetPos);
        playPingSound(true);

        String serverKey = ServerKey.current();
        List<String> friends = TeamPingConfig.get().friendsFor(serverKey);

        java.util.Set<String> online = new java.util.HashSet<>();
        if (client.getConnection() != null) {
            client.getConnection().getOnlinePlayers().forEach(e -> {
                String n = e.getProfile().name();
                if (n != null) online.add(n.toLowerCase());
            });
        }

        for (String friend : friends) {
            if (friend != null && !friend.isBlank() && online.contains(friend.trim().toLowerCase())) {
                client.player.connection.sendCommand("msg " + friend.trim() + " " + encoded);
            }
        }
    }

    private void receivePing(String owner, PingCodec.Ping ping) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        String myDim = client.level.dimension().identifier().toString();
        if (!myDim.equals(ping.dimension())) {
            return;
        }

        Vec3 pos = new Vec3(ping.x(), ping.y(), ping.z());
        double dist = client.player.getEyePosition().distanceTo(pos);

        client.player.sendSystemMessage(buildMarkedText(owner, ping.description(), dist, pos));
        showMarker(owner, ping.dimension(), ping.icon(), ping.description(), pos);
        playPingSound(false);
    }

    private void showMarker(String owner, String dimension, String icon, String description, Vec3 pos) {
        TeamPingConfig cfg = TeamPingConfig.get();
        long expires = System.currentTimeMillis() + cfg.markerDurationSeconds * 1000L;
        MarkerManager.add(new MarkerManager.Marker(pos, owner, dimension, icon, description, expires));
    }

    private Component buildMarkedText(String who, String blockName, double dist, Vec3 pos) {
        TeamPingConfig cfg = TeamPingConfig.get();
        boolean isSelf = "You".equals(who);
        TextColor name = TextColor.fromRgb(isSelf ? cfg.chatNameColor : cfg.chatDetailColor);
        TextColor block = TextColor.fromRgb(cfg.chatBlockColor);
        TextColor detail = TextColor.fromRgb(cfg.chatDetailColor);

        String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
        return Component.literal(who).withStyle(s -> s.withColor(name))
                .append(Component.literal(" marked ").withStyle(s -> s.withColor(detail)))
                .append(Component.literal(blockName).withStyle(s -> s.withColor(block)))
                .append(Component.literal(String.format(Locale.ROOT, " %.1f blocks away ", dist)).withStyle(s -> s.withColor(detail)))
                .append(Component.literal("(" + coords + ")").withStyle(s -> s.withColor(detail)));
    }
}
