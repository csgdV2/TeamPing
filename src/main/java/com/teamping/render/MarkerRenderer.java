package com.teamping.render;

import com.teamping.config.TeamPingConfig;
import com.teamping.config.TeamPingConfig.DescriptionDisplay;
import com.teamping.config.TeamPingConfig.OwnerDisplay;
import com.teamping.config.TeamPingConfig.Visibility;
import com.teamping.ping.MarkerManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class MarkerRenderer {

    private MarkerRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

            String dim = mc.world.getRegistryKey().getValue().toString();
            List<MarkerManager.Marker> markers = MarkerManager.active(dim);
            if (markers.isEmpty()) return;

            Camera cam = mc.gameRenderer.getCamera();
            if (cam == null) return;

            Vec3d camPos = cam.getCameraPos();
            double yaw = Math.toRadians(cam.getYaw());
            double pitch = Math.toRadians(cam.getPitch());
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double sy = Math.sin(yaw), cy = Math.cos(yaw);

            Vec3d forward = new Vec3d(-sy * cp, -sp, cy * cp);
            Vec3d right = new Vec3d(-cy, 0, -sy);
            Vec3d up = new Vec3d(-sy * sp, cp, cy * sp);

            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            double fovRad = Math.toRadians(mc.options.getFov().getValue());
            double focal = (sh / 2.0) / Math.tan(fovRad / 2.0);

            for (MarkerManager.Marker marker : markers) {
                drawMarker(ctx, mc, marker, camPos, forward, right, up, focal, sw, sh);
            }
        });
    }

    private static final java.util.Map<String, Float> HOVER_FADE = new java.util.HashMap<>();

    private static final class Line {
        int width;
        int height;
        java.util.function.Consumer<Integer> draw;
    }

    private static void drawMarker(DrawContext ctx, MinecraftClient mc, MarkerManager.Marker marker,
                                   Vec3d camPos, Vec3d forward, Vec3d right, Vec3d up,
                                   double focal, int sw, int sh) {
        Vec3d d = marker.pos.subtract(camPos);
        double cz = d.dotProduct(forward);
        double dist = Math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z);
        if (cz < 0.5) return;

        double cx = d.dotProduct(right);
        double cyv = d.dotProduct(up);

        float screenX = (float) (sw / 2.0 + (cx / cz) * focal);
        float screenY = (float) (sh / 2.0 - (cyv / cz) * focal);

        TeamPingConfig cfg = TeamPingConfig.get();
        TextRenderer tr = mc.textRenderer;

        double sdx = screenX - sw / 2.0;
        double sdy = screenY - sh / 2.0;
        boolean hovering = Math.sqrt(sdx * sdx + sdy * sdy) < cfg.hoverRadius;

        float hover = HOVER_FADE.getOrDefault(marker.ownerName, 0f);
        hover += ((hovering ? 1f : 0f) - hover) * 0.2f;
        if (hover < 0.001f) hover = 0f;
        if (hover > 0.999f) hover = 1f;
        HOVER_FADE.put(marker.ownerName, hover);

        long age = System.currentTimeMillis() - marker.createdAt;
        float ease = cfg.animateMarker ? clamp(age / 180f, 0f, 1f) : 1f;
        float spawn = 1f - (1f - ease) * (1f - ease);

        float nearFade = clamp((float) (dist - 2.0), 0f, 1f);
        float appear = spawn * nearFade;

        int baseAlpha = (int) (255 * appear);
        if (baseAlpha <= 4) return;

        boolean shadow = cfg.textShadow;
        int lineH = cfg.compactMode ? 9 : 10;
        final float fhover = hover;

        List<Line> lines = new ArrayList<>();

        float ownerF = factor(cfg.showOwner, fhover);
        if (ownerF > 0.02f) {
            int a = (int) (baseAlpha * ownerF);
            int white = (a << 24) | 0xFFFFFF;
            int tint = (a << 24) | 0xFFFFFF;
            SkinTextures skin = cfg.ownerDisplay == OwnerDisplay.HEAD ? skinFor(mc, marker.ownerName) : null;
            String suffix = cfg.ownerSuffix == null ? "" : cfg.ownerSuffix;
            if (skin != null) {
                int head = 8, gap = 2;
                Line ln = new Line();
                ln.width = head + gap + tr.getWidth(suffix);
                ln.height = Math.round(Math.max(head, lineH) * ownerF);
                final SkinTextures fskin = skin;
                final int lx0 = -ln.width / 2;
                ln.draw = (y) -> {
                    PlayerSkinDrawer.draw(ctx, fskin, lx0, y, head, tint);
                    ctx.drawText(tr, suffix, lx0 + head + gap, y + 1, white, shadow);
                };
                lines.add(ln);
            } else {
                addText(ctx, lines, tr, marker.ownerName + suffix, white, shadow, Math.round(lineH * ownerF));
            }
        }

        float descF = factor(cfg.showDescription, fhover);
        if (descF > 0.02f && marker.description != null && !marker.description.isBlank()) {
            int a = (int) (baseAlpha * descF);
            ItemStack icon = cfg.descriptionDisplay == DescriptionDisplay.ICON ? iconStack(marker.icon) : ItemStack.EMPTY;
            if (!icon.isEmpty()) {
                int size = 16;
                Line ln = new Line();
                ln.width = size;
                ln.height = Math.round(size * descF);
                final ItemStack fic = icon;
                final int lx0 = -size / 2;
                ln.draw = (y) -> ctx.drawItem(fic, lx0, y);
                lines.add(ln);
            } else {
                addText(ctx, lines, tr, marker.description, (a << 24) | 0xFFFFFF, shadow, Math.round(lineH * descF));
            }
        }

        float distF = factor(cfg.showDistance, fhover);
        if (distF > 0.02f) {
            int a = (int) (baseAlpha * distF);
            addText(ctx, lines, tr, String.format(java.util.Locale.ROOT, "%.0fm", dist),
                    (a << 24) | 0xAAAAAA, shadow, Math.round(lineH * distF));
        }

        float coordF = factor(cfg.showCoordinates, fhover);
        if (coordF > 0.02f) {
            int a = (int) (baseAlpha * coordF);
            addText(ctx, lines, tr, String.format(java.util.Locale.ROOT, "%.0f %.0f %.0f",
                    marker.pos.x, marker.pos.y, marker.pos.z), (a << 24) | 0xAAAAAA, shadow, Math.round(lineH * coordF));
        }

        int totalH = 0, maxW = 0;
        for (Line ln : lines) { totalH += ln.height; maxW = Math.max(maxW, ln.width); }

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(screenX, screenY);
        float distScale;
        if (dist >= 8.0) {
            float t = clamp((float) ((dist - 8.0) / 16.0), 0f, 1f);
            distScale = 0.8f - t * 0.2f;
        } else {
            distScale = 0.8f;
        }
        float s = (float) cfg.markerScale * distScale;
        m.scale(s, s);

        int blockTop = -totalH - 8;
        int pad = 3;

        if (cfg.showBackground && !lines.isEmpty()) {
            int bgA = (int) (Math.min(baseAlpha, 160));
            int bg = bgA << 24;
            ctx.fill(-maxW / 2 - pad, blockTop - pad, maxW / 2 + pad, blockTop + totalH + pad, bg);
        }

        int y = blockTop;
        for (Line ln : lines) {
            ln.draw.accept(y);
            y += ln.height;
        }

        float diamondR = 4f;
        int gap = pad + 5;
        boolean diamondBelow = screenY < sh * 0.20f;
        int diamondCy = diamondBelow
                ? (int) (blockTop + totalH + gap + diamondR)
                : (int) (blockTop - gap - diamondR);
        int diamondCol = (baseAlpha << 24) | (cfg.markerColor & 0xFFFFFF);
        drawDiamond(ctx, 0, diamondCy, diamondR, diamondCol);

        m.popMatrix();
    }

    private static float factor(Visibility v, float hover) {
        return switch (v) {
            case ALWAYS -> 1f;
            case ON_HOVER -> hover;
            case NEVER -> 0f;
        };
    }

    private static void addText(DrawContext ctx, List<Line> lines, TextRenderer tr, String text, int color, boolean shadow, int lineH) {
        Line ln = new Line();
        ln.width = tr.getWidth(text);
        ln.height = lineH;
        final int lx0 = -ln.width / 2;
        ln.draw = (y) -> ctx.drawText(tr, text, lx0, y, color, shadow);
        lines.add(ln);
    }

    private static ItemStack iconStack(String id) {
        if (id == null || id.isBlank()) return ItemStack.EMPTY;
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) return ItemStack.EMPTY;
        if (Registries.BLOCK.containsId(ident)) {
            var item = Registries.BLOCK.get(ident).asItem();
            ItemStack st = new ItemStack(item);
            return st.isEmpty() ? ItemStack.EMPTY : st;
        }
        if (Registries.ITEM.containsId(ident)) {
            return new ItemStack(Registries.ITEM.get(ident));
        }
        return ItemStack.EMPTY;
    }

    private static void drawDiamond(DrawContext ctx, int cx, int cy, float radius, int fill) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.rotate((float) (Math.PI / 4.0));
        ctx.fill((int) -radius, (int) -radius, (int) radius, (int) radius, fill);
        m.popMatrix();
    }

    private static SkinTextures skinFor(MinecraftClient mc, String name) {
        if (mc.getNetworkHandler() == null) return null;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(name);
        return e != null ? e.getSkinTextures() : null;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

