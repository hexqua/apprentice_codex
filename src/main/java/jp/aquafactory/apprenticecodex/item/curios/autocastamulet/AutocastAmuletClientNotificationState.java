package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class AutocastAmuletClientNotificationState {
    private static final AutocastAmuletNotificationController CONTROLLER = new AutocastAmuletNotificationController();

    private AutocastAmuletClientNotificationState() {
    }

    public static void queueCooldownCast(ResourceLocation spellId, ResourceLocation spellIcon, int cooldownTicks) {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return;
        }

        CONTROLLER.queueCooldownCast(gameTime, spellId, spellIcon, cooldownTicks);
    }

    public static void queueManaLow(ResourceLocation spellId, ResourceLocation spellIcon) {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return;
        }

        CONTROLLER.queueManaLow(gameTime, spellId, spellIcon);
    }

    public static void tick() {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            CONTROLLER.clear();
            return;
        }

        CONTROLLER.advance(gameTime);
    }

    public static void clear() {
        CONTROLLER.clear();
    }

    @Nullable
    public static AutocastAmuletNotificationController.NotificationEntry getActiveNotification() {
        return CONTROLLER.getActiveNotification();
    }

    public static float getActiveNotificationAlpha() {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return 0.0F;
        }

        return CONTROLLER.getActiveNotificationAlpha(gameTime);
    }

    private static long resolveCurrentGameTime() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return -1L;
        }

        return minecraft.level.getGameTime();
    }
}
