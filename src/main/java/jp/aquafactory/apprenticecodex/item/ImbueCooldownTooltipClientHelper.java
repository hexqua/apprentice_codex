package jp.aquafactory.apprenticecodex.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class ImbueCooldownTooltipClientHelper {
    private ImbueCooldownTooltipClientHelper() {
    }

    static int resolveCooldownReductionAdjustedTicks(int baseCooldownTicks) {
        Player player = Minecraft.getInstance().player;
        return player == null
                ? Math.max(0, baseCooldownTicks)
                : WeaponImbueCooldownHelper.applyCooldownReductionAttribute(baseCooldownTicks, player);
    }
}
