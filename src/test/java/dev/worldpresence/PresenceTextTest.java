package dev.worldpresence;

public final class PresenceTextTest {
    public static void main(String[] args) throws Exception {
        assert PresenceText.validApplicationId("");
        assert PresenceText.validApplicationId("123456789012345678");
        assert !PresenceText.validApplicationId(null);
        assert !PresenceText.validApplicationId("123");
        assert !PresenceText.validApplicationId("a".repeat(18));
        assert !PresenceText.validApplicationId("1".repeat(21));
        assert !PresenceText.validApplicationId("0".repeat(18));
        assert PresenceText.validApplicationId("18446744073709551615");
        assert !PresenceText.validApplicationId("18446744073709551616");
        assert !PresenceText.validApplicationId("99999999999999999999");
        assert PresenceText.menuActivity("Alex").get("details").getAsString().equals("В главном меню");
        assert PresenceText.menuActivity("Alex").get("state").getAsString().equals("Ник: Alex");
        assert !PresenceText.menuActivity("Alex").has("timestamps");
        assert PresenceText.menuActivity("Alex").getAsJsonObject("assets").get("large_image").getAsString().equals("rpc_cover");
        assert PresenceText.baseActivity().getAsJsonObject("assets").get("large_image").getAsString().equals("rpc_cover");
        assert PresenceText.timeOfDay(0).equals("Утро");
        assert PresenceText.timeOfDay(5999).equals("Утро");
        assert PresenceText.timeOfDay(6000).equals("День");
        assert PresenceText.timeOfDay(11999).equals("День");
        assert PresenceText.timeOfDay(12000).equals("Вечер");
        assert PresenceText.timeOfDay(13999).equals("Вечер");
        assert PresenceText.timeOfDay(14000).equals("Ночь");
        assert PresenceText.timeOfDay(23999).equals("Ночь");
        assert PresenceText.timeOfDay(24000).equals("Утро");
        assert PresenceText.timeOfDay(-1).equals("Ночь");
        assert PresenceText.timeOfDay(Long.MAX_VALUE).equals(PresenceText.timeOfDay(Math.floorMod(Long.MAX_VALUE, 24000)));
        assert PresenceText.timeOfDay(Long.MIN_VALUE).equals(PresenceText.timeOfDay(Math.floorMod(Long.MIN_VALUE, 24000)));
        for (int ticks = 0; ticks < 24000; ticks++) {
            assert PresenceText.timeOfDay(ticks).equals(PresenceText.timeOfDay(ticks + 24000));
        }
        String state = PresenceText.stats("Равнины", "Верхний мир", 0);
        assert state.equals("Биом: Равнины | Измерение: Верхний мир | Время суток: Утро");
        assert VanillaNames.biome("minecraft:plains").equals("Равнины");
        assert VanillaNames.biome("minecraft:warped_forest").equals("Искажённый лес");
        assert VanillaNames.biome("minecraft:end_highlands").equals("Возвышенности Энда");
        assert VanillaNames.biome("mod:plains").equals("Неизвестный биом");
        assert VanillaNames.biome("minecraft:custom").equals("Неизвестный биом");
        assert VanillaNames.dimension("minecraft:overworld").equals("Верхний мир");
        assert VanillaNames.dimension("minecraft:the_nether").equals("Незер");
        assert VanillaNames.dimension("minecraft:the_end").equals("Энд");
        assert VanillaNames.dimension("mod:overworld").equals("Неизвестное измерение");
        assert PresenceText.stats("Выветренные бесплодные земли", "Неизвестное измерение", 0).length() <= 128;
        assert PresenceText.limit("a".repeat(200)).length() == 128;
        assert PresenceText.limit("😀".repeat(200)).codePointCount(0, PresenceText.limit("😀".repeat(200)).length()) == 128;
        String details = PresenceText.details("Steve", "Сервер: " + "a".repeat(200));
        assert details.startsWith("Ник: Steve | Сервер: ");
        assert details.codePointCount(0, details.length()) == 128;
        assert PresenceText.details("Steve", "Одиночная игра").equals("Ник: Steve | Одиночная игра");
        System.out.println("Presence text checks passed");
        ConfigTest.run();
        DiscordIpcTest.run();
    }
}
