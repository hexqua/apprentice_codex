package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class RifleAmmunitionScenarios {
    private RifleAmmunitionScenarios() {}

    static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_ammo_policy_test");

            helper.assertTrue(item.getAmmoItem(stack) == ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(),
                    "Multipurpose Staffrifle should use Multi-purpose Spell Round");
            helper.assertTrue(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get() instanceof SpellcasterRoundItem,
                    "Multi-purpose Spell Round should be a SpellcasterRoundItem");
            var roundItem = (SpellcasterRoundItem) ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get();
            helper.assertTrue(roundItem.getEmptyCasingItem() == ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get(),
                    "Multi-purpose Spell Round should return Empty Multi-purpose Spell Casing");
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.2F,
                    "Multipurpose Staffrifle should use 20% empty casing return chance without Spellcaster Ammo Pouch");

            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()));
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.9F,
                    "Multipurpose Staffrifle should use 90% empty casing return chance with Spellcaster Ammo Pouch");
        });
    }
}
