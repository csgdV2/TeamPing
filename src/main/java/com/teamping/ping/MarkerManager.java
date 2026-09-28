package com.teamping.ping;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarkerManager {

    public static final class Marker {
        public final Vec3d pos;
        public final String ownerName;
        public final String dimension;
        public final String icon;
        public final String description;
        public final long expiresAt;
        public final long createdAt;

        public Marker(Vec3d pos, String ownerName, String dimension, String icon, String description, long expiresAt) {
            this.pos = pos;
            this.ownerName = ownerName;
            this.dimension = dimension;
            this.icon = icon;
            this.description = description;
            this.expiresAt = expiresAt;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private static final Map<String, Marker> MARKERS = new LinkedHashMap<>();

    private MarkerManager() {}

    public static synchronized void add(Marker marker) {
        MARKERS.put(marker.ownerName.toLowerCase(), marker);
    }

    public static synchronized List<Marker> active(String dimension) {
        long now = System.currentTimeMillis();
        MARKERS.values().removeIf(m -> m.expiresAt <= now);
        List<Marker> out = new ArrayList<>();
        for (Marker m : MARKERS.values()) {
            if (dimension == null || dimension.equals(m.dimension)) {
                out.add(m);
            }
        }
        return out;
    }

    public static synchronized void clear() {
        MARKERS.clear();
    }
}
