package jp.aquafactory.apprenticecodex.item.ammo;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import net.minecraft.world.entity.player.Player;

public final class EmptyCasingReturnPolicy {
    private static final float BASE_RETURN_CHANCE = 0.2F;
    private static final float EQUIPPED_AMMO_POUCH_RETURN_CHANCE = 0.9F;

    private EmptyCasingReturnPolicy() {
    }

    public static float resolveReturnChance(Player player) {
        return SpellcasterAmmoPouch.isEquippedBy(player)
                ? EQUIPPED_AMMO_POUCH_RETURN_CHANCE
                : BASE_RETURN_CHANCE;
    }

    public static boolean shouldReturnEmptyCasing(Player player) {
        return player.getRandom().nextFloat() < resolveReturnChance(player);
    }
}
