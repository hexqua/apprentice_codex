package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class HandStackResolver {
    private HandStackResolver() {
    }

    public static ItemStack resolve(
            Player player,
            InteractionHand hand,
            OffhandResolution offhandResolution
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(offhandResolution, "offhandResolution");

        if (hand != InteractionHand.OFF_HAND || offhandResolution == OffhandResolution.LOGICAL) {
            return player.getItemInHand(hand);
        }

        // 物理参照は Better Combat などが論理 OFF_HAND を空へ差し替えても実スロットを確実に見る機能だけで選ぶ。
        var offhand = player.getInventory().offhand;
        return offhand.isEmpty() ? ItemStack.EMPTY : offhand.get(0);
    }

    public enum OffhandResolution {
        LOGICAL,
        PHYSICAL
    }
}
