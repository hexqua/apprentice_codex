package jp.aquafactory.apprenticecodex.item.magicitem.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.magicitem.WoodenWand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class WoodenWandClientRenderState {
    private WoodenWandClientRenderState() {
    }

    public static boolean isImbuedSpellOnCooldown(ItemStack stack, @Nullable LivingEntity living) {
        if (!(living instanceof LocalPlayer)) {
            return false;
        }

        var spellData = WoodenWand.getImbuedSpell(stack);
        return spellData != null
                && ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell());
    }
}
