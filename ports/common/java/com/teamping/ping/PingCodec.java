package com.teamping.ping;

public final class PingCodec {

    public static final String PREFIX = "[TP]";

    private PingCodec() {}

    public static String encode(String owner, String dimension, double x, double y, double z,
                                String icon, String description) {
        return PREFIX + "|" + safe(owner) + "|" + fmt(x) + "|" + fmt(y) + "|" + fmt(z) + "|"
                + safe(dimension) + "|" + safe(icon) + "|" + safe(description);
    }

    public static Ping decode(String message) {
        if (message == null) {
            return null;
        }
        int idx = message.indexOf(PREFIX + "|");
        if (idx < 0) {
            return null;
        }
        String payload = message.substring(idx);
        String[] parts = payload.split("\\|", 8);
        if (parts.length < 8) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double z = Double.parseDouble(parts[4]);
            return new Ping(parts[1], x, y, z, parts[5], parts[6], parts[7]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v.replace('|', ' ');
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    public record Ping(String owner, double x, double y, double z, String dimension, String icon, String description) {}
}
