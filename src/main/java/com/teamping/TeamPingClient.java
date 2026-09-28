package com.teamping;

import com.teamping.config.ServerKey;
import com.teamping.config.TeamPingConfig;
import com.teamping.ping.MarkerManager;
import com.teamping.ping.PingCodec;
import com.teamping.render.MarkerRenderer;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class TeamPingClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("teamping", "main"));

    private static KeyBinding pingKey;
    private static KeyBinding clearKey;

    @Override
    public void onInitializeClient() {
        pingKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamping.ping",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        clearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamping.clear",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (pingKey.wasPressed()) {
                sendPing(client);
            }
            while (clearKey.wasPressed()) {
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
            dispatcher.register(ClientCommandManager.literal("teamping")
                    .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(c -> {
                                        addFriend(c.getSource(), StringArgumentType.getString(c, "name"));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(c -> {
                                        removeFriend(c.getSource(), StringArgumentType.getString(c, "name"));
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("list")
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
                source.sendFeedback(Text.literal("[TeamPing] " + name + " is already a friend.").formatted(Formatting.YELLOW));
                return;
            }
        }
        friends.add(name);
        cfg.save();
        source.sendFeedback(Text.literal("[TeamPing] Added " + name + ".").formatted(Formatting.GREEN));
    }

    private void removeFriend(FabricClientCommandSource source, String name) {
        TeamPingConfig cfg = TeamPingConfig.get();
        List<String> friends = cfg.friendsFor(ServerKey.current());
        boolean removed = friends.removeIf(f -> f.equalsIgnoreCase(name));
        cfg.save();
        source.sendFeedback(Text.literal(removed ? "[TeamPing] Removed " + name + "." : "[TeamPing] " + name + " was not a friend.")
                .formatted(removed ? Formatting.GREEN : Formatting.RED));
    }

    private void listFriends(FabricClientCommandSource source) {
        List<String> friends = TeamPingConfig.get().friendsFor(ServerKey.current());
        if (friends.isEmpty()) {
            source.sendFeedback(Text.literal("[TeamPing] No friends added for this server.").formatted(Formatting.YELLOW));
            return;
        }
        source.sendFeedback(Text.literal("[TeamPing] Friends: " + String.join(", ", friends)).formatted(Formatting.AQUA));
    }

    private void playPingSound(boolean mine) {
        TeamPingConfig cfg = TeamPingConfig.get();
        TeamPingConfig.SoundMode mode = cfg.soundMode;
        if (mode == TeamPingConfig.SoundMode.NONE) return;
        if (mode == TeamPingConfig.SoundMode.MINE && !mine) return;
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.5f, 1.0f));
    }

    private boolean handleIncoming(String raw) {
        PingCodec.Ping ping = PingCodec.decode(raw);
        if (ping == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        String self = client.player != null ? client.player.getName().getString() : "";
        if (!ping.owner().equalsIgnoreCase(self)) {
            receivePing(ping.owner(), ping);
        }
        return true;
    }

    private void sendPing(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        Vec3d eye = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0f);
        double reach = 512.0;
        Vec3d end = eye.add(look.multiply(reach));

        Vec3d targetPos;
        String blockName;
        String icon = "";

        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getPos().distanceTo(eye) : reach;

        Box searchBox = client.player.getBoundingBox().stretch(look.multiply(blockDist)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(client.player, eye, eye.add(look.multiply(blockDist)),
                searchBox, e -> !e.isSpectator() && e.isAlive() && e != client.player, blockDist * blockDist);

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            targetPos = target.getBoundingBox().getCenter();
            if (target instanceof PlayerEntity) {
                blockName = target.isInvisibleTo(client.player) ? "Unknown Player" : target.getName().getString();
            } else {
                blockName = target.isInvisibleTo(client.player) ? "Unknown" : target.getName().getString();
            }
            icon = net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).toString();
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = blockHit.getBlockPos();
            targetPos = Vec3d.ofCenter(bp);
            var block = client.world.getBlockState(bp).getBlock();
            blockName = block.getName().getString();
            icon = net.minecraft.registry.Registries.BLOCK.getId(block).toString();
        } else {
            targetPos = end;
            blockName = "Location";
        }

        double dist = eye.distanceTo(targetPos);
        String self = client.player.getName().getString();
        String dimension = client.world.getRegistryKey().getValue().toString();
        String encoded = PingCodec.encode(self, dimension, targetPos.x, targetPos.y, targetPos.z, icon, blockName);

        client.player.sendMessage(buildMarkedText("You", blockName, dist, targetPos), false);
        showMarker(self, dimension, icon, blockName, targetPos);
        playPingSound(true);

        String serverKey = ServerKey.current();
        List<String> friends = TeamPingConfig.get().friendsFor(serverKey);

        java.util.Set<String> online = new java.util.HashSet<>();
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().getPlayerList().forEach(e -> {
                String n = e.getProfile().name();
                if (n != null) online.add(n.toLowerCase());
            });
        }

        for (String friend : friends) {
            if (friend != null && !friend.isBlank() && online.contains(friend.trim().toLowerCase())) {
                client.player.networkHandler.sendChatCommand("msg " + friend.trim() + " " + encoded);
            }
        }
    }

    private void receivePing(String owner, PingCodec.Ping ping) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        String myDim = client.world.getRegistryKey().getValue().toString();
        if (!myDim.equals(ping.dimension())) {
            return;
        }

        Vec3d pos = new Vec3d(ping.x(), ping.y(), ping.z());
        double dist = client.player.getEyePos().distanceTo(pos);

        client.player.sendMessage(buildMarkedText(owner, ping.description(), dist, pos), false);
        showMarker(owner, ping.dimension(), ping.icon(), ping.description(), pos);
        playPingSound(false);
    }

    private void showMarker(String owner, String dimension, String icon, String description, Vec3d pos) {
        TeamPingConfig cfg = TeamPingConfig.get();
        long expires = System.currentTimeMillis() + cfg.markerDurationSeconds * 1000L;
        MarkerManager.add(new MarkerManager.Marker(pos, owner, dimension, icon, description, expires));
    }

    private Text buildMarkedText(String who, String blockName, double dist, Vec3d pos) {
        TeamPingConfig cfg = TeamPingConfig.get();
        boolean isSelf = "You".equals(who);
        TextColor name = TextColor.fromRgb(isSelf ? cfg.chatNameColor : cfg.chatDetailColor);
        TextColor block = TextColor.fromRgb(cfg.chatBlockColor);
        TextColor detail = TextColor.fromRgb(cfg.chatDetailColor);

        String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
        return Text.literal(who).styled(s -> s.withColor(name))
                .append(Text.literal(" marked ").styled(s -> s.withColor(detail)))
                .append(Text.literal(blockName).styled(s -> s.withColor(block)))
                .append(Text.literal(String.format(Locale.ROOT, " %.1f blocks away ", dist)).styled(s -> s.withColor(detail)))
                .append(Text.literal("(" + coords + ")").styled(s -> s.withColor(detail)));
    }
}
