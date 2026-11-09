package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

final class ApprenticeCodexBoundSwordGameTests {
    private ApprenticeCodexBoundSwordGameTests() {}

    static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Bound Sword Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0,
                new ItemStack(ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }
}
