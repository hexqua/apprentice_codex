package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record PsychicDriveGauntletsSelection(ItemStack stack, Mode mode) {
    private static final PsychicDriveGauntletsSelection HIDDEN =
            new PsychicDriveGauntletsSelection(ItemStack.EMPTY, Mode.HIDDEN);

    public enum Mode {
        HIDDEN, SHEATHED, DRAWN
    }

    public static boolean isGauntlets(ItemStack stack) {
        return stack.getItem() instanceof PsychicDriveGauntlets;
    }

    public static PsychicDriveGauntletsSelection find(Player player, boolean includeHotbar) {
        var offhand = player.getOffhandItem();
        if (isGauntlets(offhand)) {
            return new PsychicDriveGauntletsSelection(offhand, Mode.DRAWN);
        }
        var mainhand = player.getMainHandItem();
        if (isGauntlets(mainhand)) {
            return new PsychicDriveGauntletsSelection(mainhand, Mode.DRAWN);
        }
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
}
