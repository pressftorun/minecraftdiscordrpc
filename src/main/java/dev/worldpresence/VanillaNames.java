package dev.worldpresence;

/** Vanilla biome names from Minecraft 26.3's Russian language assets. */
final class VanillaNames {
    static String biome(String id) {
        return switch (id) {
            case "minecraft:badlands" -> "Бесплодные земли";
            case "minecraft:bamboo_jungle" -> "Бамбуковые заросли";
            case "minecraft:basalt_deltas" -> "Базальтовые дельты";
            case "minecraft:beach" -> "Пляж";
            case "minecraft:birch_forest" -> "Березняк";
            case "minecraft:cherry_grove" -> "Вишнёвая роща";
            case "minecraft:cold_ocean" -> "Холодный океан";
            case "minecraft:crimson_forest" -> "Багровый лес";
            case "minecraft:dappled_forest" -> "Пёстрый лес";
            case "minecraft:dark_forest" -> "Тёмный лес";
            case "minecraft:deep_cold_ocean" -> "Глубокий холодный океан";
            case "minecraft:deep_dark" -> "Тёмные подземелья";
            case "minecraft:deep_frozen_ocean" -> "Глубокий замёрзший океан";
            case "minecraft:deep_lukewarm_ocean" -> "Глубокий тепловатый океан";
            case "minecraft:deep_ocean" -> "Глубокий океан";
            case "minecraft:desert" -> "Пустыня";
            case "minecraft:dripstone_caves" -> "Карстовые пещеры";
            case "minecraft:end_barrens" -> "Окраины Энда";
            case "minecraft:end_highlands" -> "Возвышенности Энда";
            case "minecraft:end_midlands" -> "Равнины Энда";
            case "minecraft:eroded_badlands" -> "Выветренные бесплодные земли";
            case "minecraft:flower_forest" -> "Цветочный лес";
            case "minecraft:forest" -> "Лес";
            case "minecraft:frozen_ocean" -> "Замёрзший океан";
            case "minecraft:frozen_peaks" -> "Замёрзшие вершины";
            case "minecraft:frozen_river" -> "Замёрзшая река";
            case "minecraft:grove" -> "Роща";
            case "minecraft:ice_caves" -> "Ледяные пещеры";
            case "minecraft:ice_spikes" -> "Ледяные пики";
            case "minecraft:jagged_peaks" -> "Зубчатые вершины";
            case "minecraft:jungle" -> "Джунгли";
            case "minecraft:lukewarm_ocean" -> "Тепловатый океан";
            case "minecraft:lush_caves" -> "Заросшие пещеры";
            case "minecraft:mangrove_swamp" -> "Мангровое болото";
            case "minecraft:meadow" -> "Луг";
            case "minecraft:mushroom_fields" -> "Грибные поля";
            case "minecraft:nether_wastes" -> "Пустоши Незера";
            case "minecraft:ocean" -> "Океан";
            case "minecraft:old_growth_birch_forest" -> "Реликтовый березняк";
            case "minecraft:old_growth_pine_taiga" -> "Реликтовая сосновая тайга";
            case "minecraft:old_growth_spruce_taiga" -> "Реликтовая еловая тайга";
            case "minecraft:pale_garden" -> "Бледный сад";
            case "minecraft:plains" -> "Равнины";
            case "minecraft:river" -> "Река";
            case "minecraft:savanna" -> "Саванна";
            case "minecraft:savanna_plateau" -> "Саванна (плато)";
            case "minecraft:small_end_islands" -> "Островки Энда";
            case "minecraft:snowy_beach" -> "Заснеженный пляж";
            case "minecraft:snowy_plains" -> "Заснеженные равнины";
            case "minecraft:snowy_slopes" -> "Заснеженные склоны";
            case "minecraft:snowy_taiga" -> "Заснеженная тайга";
            case "minecraft:soul_sand_valley" -> "Долина песка душ";
            case "minecraft:sparse_jungle" -> "Редкие джунгли";
            case "minecraft:stony_peaks" -> "Каменные вершины";
            case "minecraft:stony_shore" -> "Каменистый берег";
            case "minecraft:sulfur_caves" -> "Серные пещеры";
            case "minecraft:sunflower_plains" -> "Подсолнуховые поля";
            case "minecraft:swamp" -> "Болото";
            case "minecraft:taiga" -> "Тайга";
            case "minecraft:the_end" -> "Энд";
            case "minecraft:the_void" -> "Бездна";
            case "minecraft:warm_ocean" -> "Тёплый океан";
            case "minecraft:warped_forest" -> "Искажённый лес";
            case "minecraft:windswept_forest" -> "Выветренный лес";
            case "minecraft:windswept_gravelly_hills" -> "Выветренные гравийные холмы";
            case "minecraft:windswept_hills" -> "Выветренные холмы";
            case "minecraft:windswept_savanna" -> "Выветренная саванна";
            case "minecraft:wooded_badlands" -> "Лесистые бесплодные земли";
            default -> "Неизвестный биом";
        };
    }

    static String dimension(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "Верхний мир";
            case "minecraft:the_nether" -> "Незер";
            case "minecraft:the_end" -> "Энд";
            default -> "Неизвестное измерение";
        };
    }
}