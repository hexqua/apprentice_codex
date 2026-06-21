package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AutocastAmuletNotificationController {
    public static final int DISPLAY_DURATION_TICKS = 30;
    public static final int FADE_OUT_TICKS = 10;
    private static final int[] THRESHOLD_SECONDS = {60, 30, 10};
    private static final String MANA_LOW_LABEL = "MP!";

    private final ArrayDeque<NotificationEntry> pendingQueue = new ArrayDeque<>();
    private final List<ScheduledNotification> scheduledNotifications = new ArrayList<>();

    @Nullable
    private NotificationEntry activeNotification;
    private long activeNotificationStartedTick = Long.MIN_VALUE;

    public void queueCooldownCast(long currentTick, ResourceLocation spellId, ResourceLocation spellIcon, int cooldownTicks) {
        var normalizedCooldown = Math.max(0, cooldownTicks);
        if (normalizedCooldown <= 0) {
            return;
        }

        var displaySeconds = toDisplayCooldownSeconds(normalizedCooldown);
        pendingQueue.addLast(new NotificationEntry(
                NotificationType.CAST,
                spellId,
                spellIcon,
                displaySeconds,
                formatSecondsLabel(displaySeconds)
        ));

        for (var thresholdSeconds : THRESHOLD_SECONDS) {
            var thresholdTicks = thresholdSeconds * 20;
            if (normalizedCooldown <= thresholdTicks) {
                continue;
            }

            scheduledNotifications.add(new ScheduledNotification(
                    currentTick + normalizedCooldown - thresholdTicks,
                    new NotificationEntry(
                            NotificationType.THRESHOLD,
                            spellId,
                            spellIcon,
                            thresholdSeconds,
                            formatSecondsLabel(thresholdSeconds)
                    )
            ));
        }

        advance(currentTick);
    }

    public void queueManaLow(long currentTick, ResourceLocation spellId, ResourceLocation spellIcon) {
        pendingQueue.addLast(new NotificationEntry(
                NotificationType.MANA_LOW,
                spellId,
                spellIcon,
                -1,
                MANA_LOW_LABEL
        ));
        advance(currentTick);
    }

    public void queueLinearBuildRemaining(
            long currentTick,
            ResourceLocation spellId,
            ItemStack iconStack,
            String countLabel
    ) {
        var displayStack = iconStack.copy();
        displayStack.setCount(1);
        pendingQueue.addLast(new NotificationEntry(
                NotificationType.LINEAR_BUILD_REMAINING,
                spellId,
                spellId,
                displayStack,
                -1,
                countLabel
        ));
        advance(currentTick);
    }

    public void advance(long currentTick) {
        expireActiveNotificationIfNeeded(currentTick);
        drainDueScheduledNotifications(currentTick);
        if (activeNotification == null && !pendingQueue.isEmpty()) {
            activeNotification = pendingQueue.removeFirst();
            activeNotificationStartedTick = currentTick;
        }
    }

    public void clear() {
        pendingQueue.clear();
        scheduledNotifications.clear();
        activeNotification = null;
        activeNotificationStartedTick = Long.MIN_VALUE;
    }

    @Nullable
    public NotificationEntry getActiveNotification() {
        return activeNotification;
    }

    public int getPendingQueueSize() {
        return pendingQueue.size();
    }

    public List<ScheduledNotification> getScheduledNotifications() {
        return List.copyOf(scheduledNotifications);
    }

    public float getActiveNotificationAlpha(long currentTick) {
        if (activeNotification == null || activeNotificationStartedTick == Long.MIN_VALUE) {
            return 0.0F;
        }

        var ageTicks = Math.max(0L, currentTick - activeNotificationStartedTick);
        var fadeStartAge = Math.max(0, DISPLAY_DURATION_TICKS - FADE_OUT_TICKS);
        if (ageTicks <= fadeStartAge) {
            return 1.0F;
        }

        var fadeProgress = Math.min(1.0F, (ageTicks - fadeStartAge) / (float) FADE_OUT_TICKS);
        return 1.0F - fadeProgress * fadeProgress * fadeProgress;
    }

    public static int toDisplayCooldownSeconds(int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return 0;
        }
        return (cooldownTicks + 19) / 20;
    }

    public static int toDisplayCooldownSeconds(float cooldownTicks) {
        if (cooldownTicks <= 0.0F) {
            return 0;
        }
        return (int) Math.ceil(cooldownTicks / 20.0F);
    }

    private static String formatSecondsLabel(int seconds) {
        return seconds + "s";
    }

    private void expireActiveNotificationIfNeeded(long currentTick) {
        if (activeNotification == null) {
            return;
        }
        if (currentTick - activeNotificationStartedTick < DISPLAY_DURATION_TICKS) {
            return;
        }

        activeNotification = null;
        activeNotificationStartedTick = Long.MIN_VALUE;
    }

    private void drainDueScheduledNotifications(long currentTick) {
        for (Iterator<ScheduledNotification> iterator = scheduledNotifications.iterator(); iterator.hasNext(); ) {
            var scheduled = iterator.next();
            if (scheduled.triggerTick() > currentTick) {
                continue;
            }

            pendingQueue.addLast(scheduled.entry());
            iterator.remove();
        }
    }

    public enum NotificationType {
        CAST,
        THRESHOLD,
        MANA_LOW,
        LINEAR_BUILD_REMAINING
    }

    public record NotificationEntry(
            NotificationType type,
            ResourceLocation spellId,
            ResourceLocation spellIcon,
            ItemStack itemIcon,
            int displaySeconds,
            String displayText
    ) {
        public NotificationEntry(
                NotificationType type,
                ResourceLocation spellId,
                ResourceLocation spellIcon,
                int displaySeconds,
                String displayText
        ) {
            this(type, spellId, spellIcon, ItemStack.EMPTY, displaySeconds, displayText);
        }
    }

    public record ScheduledNotification(long triggerTick, NotificationEntry entry) {
    }
}
