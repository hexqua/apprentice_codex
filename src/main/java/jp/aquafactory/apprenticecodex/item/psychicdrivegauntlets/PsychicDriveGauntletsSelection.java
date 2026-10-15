package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record PsychicDriveGauntletsSelection(ItemStack stack, Mode mode) {
    private static final PsychicDriveGauntletsSelection HIDDEN =
            new PsychicDriveGauntletsSelection(ItemStack.EMPTY, Mode.HIDDEN);

    public enum Mode {
        HIDDEN, SHEATHED, DRAWN, DISABLED
    }

    public static boolean isGauntlets(ItemStack stack) {
        return stack.getItem() instanceof PsychicDriveGauntlets;
    }

    public static PsychicDriveGauntletsSelection find(Player player, boolean includeHotbar) {
        if (includeHotbar && duplicate(player)) {
            return new PsychicDriveGauntletsSelection(ItemStack.EMPTY, Mode.DISABLED);
        }
        var offhand = player.getOffhandItem();
        if (isGauntlets(offhand)) {
            return new PsychicDriveGauntletsSelection(offhand, Mode.DRAWN);
        }
        var mainhand = player.getMainHandItem();
        if (isGauntlets(mainhand)) {
            return new PsychicDriveGauntletsSelection(mainhand, Mode.DRAWN);
        }
        var physical = physicalOffhand(player);
        if (isGauntlets(physical)) return new PsychicDriveGauntletsSelection(physical, Mode.SHEATHED);
        // 他プレイヤーの非選択ホットバーは通常同期されないため、自分だけを走査する。
        if (includeHotbar) {
            for (var slot = 0; slot < 9; slot++) {
                var stack = player.getInventory().getItem(slot);
                if (isGauntlets(stack)) {
                    return new PsychicDriveGauntletsSelection(stack, Mode.SHEATHED);
                }
            }
        }
        return HIDDEN;
    }

    public static ItemStack physicalOffhand(Player player) {
        // Better Combatの両手武器は論理オフハンドを空にするため、所持数はinventoryで数える。
        var inventory = player.getInventory().offhand;
        return inventory.isEmpty() ? ItemStack.EMPTY : inventory.getFirst();
    }

    public static boolean duplicate(Player player) {
        int count = isGauntlets(physicalOffhand(player)) ? 1 : 0;
        for (int slot = 0; slot < 9; slot++) {
            if (isGauntlets(player.getInventory().getItem(slot)) && ++count >= 2) return true;
        }
        return false;
    }

    public static boolean canUse(Player player, InteractionHand hand) {
        return player.isAlive() && !player.isSpectator() && !duplicate(player)
                && isGauntlets(player.getItemInHand(hand))
                && (hand == InteractionHand.OFF_HAND || physicalOffhand(player).isEmpty());
    }
}
