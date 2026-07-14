package jp.aquafactory.apprenticecodex.compat.bettercombat;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BetterCombatScrollcasterGauntletCompat {
    private BetterCombatScrollcasterGauntletCompat() {
    }

    public static boolean isRescueActive(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        if (!isTwoHandedMainHandWeapon(player.getMainHandItem())) {
            return false;
        }

        return getPhysicalOffhandStack(player).getItem() instanceof ScrollcasterGauntlet;
    }

    public static ItemStack getResolvedHeldStack(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && isRescueActive(player)) {
            return getPhysicalOffhandStack(player);
        }
        return player.getItemInHand(hand);
    }

    public static ItemStack getPhysicalOffhandStack(Player player) {
        return BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
    }

    public static SpellData getSelectedOffhandSpell(Player player) {
        var offhandStack = getPhysicalOffhandStack(player);
        if (!(offhandStack.getItem() instanceof ScrollcasterGauntlet)) {
            return SpellData.EMPTY;
        }
        return ScrollcasterGauntlet.getSelectedSpellData(offhandStack);
    }

    private static boolean isTwoHandedMainHandWeapon(ItemStack mainHandStack) {
        if (mainHandStack.isEmpty()) {
            return false;
        }

        var weaponAttributes = WeaponRegistry.getAttributes(mainHandStack);
        return weaponAttributes != null && weaponAttributes.isTwoHanded();
    }
}
