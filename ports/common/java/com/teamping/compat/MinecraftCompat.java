package com.teamping.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** The small set of Minecraft APIs that differ between release families. */
public final class MinecraftCompat {
    private MinecraftCompat() {}
    public static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;
    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
    public static Camera camera(Minecraft client) {
        return client.gameRenderer.getMainCamera();
    }
    public static boolean hudHidden(Minecraft client) {
        return client.options.hideGui;
    }
}
