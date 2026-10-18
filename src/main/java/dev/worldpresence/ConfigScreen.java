package dev.worldpresence;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

final class ConfigScreen extends Screen {
    private final Screen parent;
    private String draft;
    private String error = "";
    private CompletableFuture<DiscordIpc.CheckResult> check;
    private Button checkButton;

    ConfigScreen(Screen parent) {
        super(Component.literal("Присутствие в мире"));
        this.parent = parent;
        draft = WorldPresence.instance.applicationId();
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(300, width - 32);
        int left = (width - fieldWidth) / 2;
        int top = height / 2 - 30;
        EditBox id = addRenderableWidget(new EditBox(font, left, top, fieldWidth, 20,
                Component.literal("ID приложения Discord")));
        // Leave invalid pasted values visible, rather than silently changing the ID приложения.
        id.setMaxLength(128);
        id.setValue(draft);
        Button save = addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> {
            try {
                WorldPresence.instance.saveApplicationId(draft);
                onClose();
            } catch (IOException e) {
                error = "Не удалось сохранить конфиг";
                WorldPresence.LOG.error(error, e);
            }
        }).bounds(left, top + 62, (fieldWidth - 8) / 2, 20).build());
        save.active = PresenceText.validApplicationId(draft.strip());
        id.setResponder(value -> {
            draft = value;
            save.active = PresenceText.validApplicationId(value.strip());
            error = save.active ? "" : "ID должен содержать 17–20 цифр";
            cancelCheck();
            checkButton.active = canCheck();
        });
        addRenderableWidget(Button.builder(Component.literal("Отмена"), button -> onClose())
                .bounds(left + (fieldWidth + 8) / 2, top + 62, (fieldWidth - 8) / 2, 20).build());
        checkButton = addRenderableWidget(Button.builder(Component.literal("Проверить ID"), button -> {
            check = DiscordIpc.checkApplicationId(draft.strip());
            error = "";
            checkButton.active = canCheck();
        }).bounds(left, top + 88, fieldWidth, 20).build());
        checkButton.active = canCheck();
        setInitialFocus(id);
    }

    private boolean canCheck() {
        String id = draft.strip();
        return !id.isEmpty() && PresenceText.validApplicationId(id) && (check == null || check.isDone());
    }

    private void cancelCheck() {
        if (check != null) check.cancel(false);
        check = null;
    }

    @Override
    public void tick() {
        super.tick();
        checkButton.active = canCheck();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int top = height / 2 - 30;
        graphics.centeredText(font, title, width / 2, top - 46, 0xFFFFFFFF);
        graphics.centeredText(font, "ID приложения Discord", width / 2, top - 14, 0xFFFFFFFF);
        graphics.centeredText(font, "Пустое поле отключает RPC", width / 2, top + 28, 0xFFAAAAAA);
        graphics.centeredText(font, "Применяется без перезапуска игры", width / 2, top + 40, 0xFFAAAAAA);
        String status = error;
        int color = 0xFFFF5555;
        if (status.isEmpty() && check != null) {
            DiscordIpc.CheckResult result = check.getNow(null);
            status = result == null ? "Проверка подключения к Discord..." : result.message();
            color = result == null ? 0xFFAAAAAA : result.successful() ? 0xFF55FF55 : 0xFFFF5555;
        }
        if (!status.isEmpty()) graphics.centeredText(font,
                font.plainSubstrByWidth(status, width - 20), width / 2, top + 116, color);
    }

    @Override
    public void onClose() {
        cancelCheck();
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        cancelCheck();
        super.removed();
    }
}
