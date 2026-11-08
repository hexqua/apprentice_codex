package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

final class MithrilFreecastStaffCalibrationScenarios {
    private MithrilFreecastStaffCalibrationScenarios() {}

    static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
            var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertFalse(stack.getItem() instanceof RestrictedSpellImbuableItem,
                    "Mithril Freecast Staff should not expose the restricted imbue API");
            helper.assertTrue(
                    SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Mithril Freecast Staff should reject Arcane Anvil spell imbuing"
            );
        });
    }
}
