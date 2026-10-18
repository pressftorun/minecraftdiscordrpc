package dev.worldpresence;

import com.google.gson.JsonObject;

final class PresenceText {
    static boolean validApplicationId(String id) {
        if (id == null) return false;
        if (id.isEmpty()) return true;
        if (!id.matches("[1-9][0-9]{16,19}")) return false;
        try {
            Long.parseUnsignedLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static JsonObject baseActivity() {
        JsonObject activity = new JsonObject();
        activity.addProperty("type", 0);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", "rpc_cover");
        assets.addProperty("large_text", "Присутствие в мире");
        activity.add("assets", assets);
        return activity;
    }

    static JsonObject menuActivity(String nickname) {
        JsonObject activity = baseActivity();
        activity.addProperty("details", "В главном меню");
        activity.addProperty("state", limit("Ник: " + nickname));
        return activity;
    }

    static String timeOfDay(long ticks) {
        long time = Math.floorMod(ticks, 24000);
        if (time < 6000) return "Утро";
        if (time < 12000) return "День";
        if (time < 14000) return "Вечер";
        return "Ночь";
    }

    static String stats(String biome, String dimension, long ticks) {
        return limit("Биом: " + biome + " | Измерение: " + dimension + " | Время суток: " + timeOfDay(ticks));
    }

    static String limit(String text) {
        return limit(text, 128);
    }

    static String details(String nickname, String location) {
        String player = limit("Ник: " + nickname, 32);
        return player + " | " + limit(location, 128 - 3 - player.codePointCount(0, player.length()));
    }

    private static String limit(String text, int maximum) {
        int count = text.codePointCount(0, text.length());
        return count <= maximum ? text : text.substring(0, text.offsetByCodePoints(0, maximum - 1)) + "…";
    }
}
