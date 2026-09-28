package com.teamping.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ServerKey {

    private ServerKey() {}

    public static String current() {
        MinecraftClient mc = MinecraftClient.getInstance();

        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) {
            return info.address.toLowerCase();
        }

        if (mc.isInSingleplayer() && mc.getServer() != null) {
            return "singleplayer:" + mc.getServer().getSaveProperties().getLevelName();
        }

        return "unknown";
    }
}
