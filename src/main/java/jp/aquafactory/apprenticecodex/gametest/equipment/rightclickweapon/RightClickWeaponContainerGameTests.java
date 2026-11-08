package jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickWeaponContainerGameTests {
    private RightClickWeaponContainerGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void preparationInitializesOnlyImbuedWeaponContainers(GameTestHelper helper) {
        for (var item : new Item[]{ItemRegistry.MITHRIL_FREECAST_STAFF.get(),
                ItemRegistry.REVOLVERCAST_STAFF.get(),
                ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get()}) {
            var stack = item.getDefaultInstance();
            SpellCalibrationImbueHelper.prepareTarget(stack);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Wheel weapons must remain container-free");
        }
        for (var item : new Item[]{ItemRegistry.SMASHCAST_SCEPTER.get(), ItemRegistry.CRYSTAL_BLADED_STAFF.get()}) {
            var stack = item.getDefaultInstance();
            SpellCalibrationImbueHelper.prepareTarget(stack);
            var container = ISpellContainer.get(stack);
            helper.assertTrue(container != null && container.getMaxSpellCount() > 0,
                    item + ": preparing an imbued weapon must initialize usable spell slots");
            var snapshot = stack.copy();
            SpellCalibrationImbueHelper.prepareTarget(stack);
            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, snapshot),
                    "Repeated preparation must preserve initialized container state");
        }
        helper.succeed();
    }
}
