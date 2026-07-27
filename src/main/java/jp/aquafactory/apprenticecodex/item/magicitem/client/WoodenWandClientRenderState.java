package jp.aquafactory.apprenticecodex.item.magicitem.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.magicitem.WoodenWand;
import net.minecraft.world.item.ItemStack;

public final class WoodenWandClientRenderState {
    private WoodenWandClientRenderState() {
    }

    public static boolean isImbuedSpellOnCooldown(ItemStack stack) {
        var spellData = WoodenWand.getImbuedSpell(stack);
        return spellData != null
                && ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell());
    }
}
