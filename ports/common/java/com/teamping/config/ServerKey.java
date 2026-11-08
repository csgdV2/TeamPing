package com.teamping.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerKey {

    private ServerKey() {}

    public static String current() {
        Minecraft mc = Minecraft.getInstance();

        ServerData info = mc.getCurrentServer();
        if (info != null && info.ip != null && !info.ip.isBlank()) {
            return info.ip.toLowerCase();
        }

        if (mc.isLocalServer() && mc.getSingleplayerServer() != null) {
            return "singleplayer:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }

        return "unknown";
    }
}
