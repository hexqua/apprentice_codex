package jp.aquafactory.apprenticecodex.item.ammo;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemainingCountNotificationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BowAmmoConsumptionNotification {
    private BowAmmoConsumptionNotification() {
    }

    public static void send(ServerPlayer player, ResourceLocation sourceId, ItemStack consumedStack) {
        if (player.connection == null || consumedStack.isEmpty()) {
            return;
        }

        Networks.sendToPlayer(player, createPacket(player, sourceId, consumedStack));
    }

    public static SyncRemainingCountNotificationPacket createPacket(
            Player player,
            ResourceLocation sourceId,
            ItemStack consumedStack
    ) {
        var iconStack = consumedStack.copyWithCount(1);
        return new SyncRemainingCountNotificationPacket(
                sourceId.toString(),
                iconStack,
                countRemaining(player, iconStack),
                SyncRemainingCountNotificationPacket.DisplayType.ITEM_REMAINING
        );
    }

    public static long countRemaining(Player player, ItemStack consumedStack) {
        if (consumedStack.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        for (var stack : player.getInventory().items) {
            total = addMatchingCount(total, stack, consumedStack);
        }
        for (var stack : player.getInventory().offhand) {
            total = addMatchingCount(total, stack, consumedStack);
        }

        var quiverTotal = new long[1];
        SpellcasterQuiver.forEachAccessibleArrow(player, (stack, count) -> {
            if (ItemStack.isSameItemSameTags(stack, consumedStack)) {
                quiverTotal[0] = addSaturated(quiverTotal[0], count);
            }
        });
        return addSaturated(total, quiverTotal[0]);
    }

    private static long addMatchingCount(long total, ItemStack stack, ItemStack consumedStack) {
        return ItemStack.isSameItemSameTags(stack, consumedStack)
                ? addSaturated(total, stack.getCount())
                : total;
    }

    private static long addSaturated(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
