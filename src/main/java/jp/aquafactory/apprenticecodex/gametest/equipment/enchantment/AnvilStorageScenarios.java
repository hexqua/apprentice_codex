package jp.aquafactory.apprenticecodex.gametest.equipment.enchantment;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeHelper;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

final class AnvilStorageScenarios {
    private AnvilStorageScenarios() {}

    static void nonDamageableAnvilMergeRejectsStoredRightContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var sharpness = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SHARPNESS);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "non_damageable_anvil_stored_contents_test");
            var left = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var right = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            right.enchant(sharpness, 1);

            var emptyMerge = NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player);
            helper.assertTrue(emptyMerge != null,
                    "Non-damageable anvil merge should accept an empty right input");

            var leftAdjustment = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            CalibrationAdjustmentStorage.set(
                    left,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    leftAdjustment,
                    registryAccess
            );
            var leftContentMerge = NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player);
            helper.assertTrue(leftContentMerge != null,
                    "Non-damageable anvil merge should accept stored contents on the left input");
            helper.assertTrue(!CalibrationAdjustmentStorage.get(
                    leftContentMerge.output(),
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    registryAccess
            ).isEmpty(), "Non-damageable anvil merge should preserve stored left contents");

            CalibrationAdjustmentStorage.set(
                    right,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    leftAdjustment,
                    registryAccess
            );
            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) == null,
                    "Non-damageable anvil merge should reject a right input with a calibration adjustment");
            CalibrationAdjustmentStorage.set(
                    right,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    ItemStack.EMPTY,
                    registryAccess
            );

            var disabledStoredSlot = ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT - 1;
            ScrollcasterGauntlet.setCalibrationScroll(
                    right,
                    disabledStoredSlot,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) == null,
                    "Non-damageable anvil merge should reject a right input with a stored scroll in a disabled slot");
            ScrollcasterGauntlet.setCalibrationScroll(right, disabledStoredSlot, ItemStack.EMPTY);

            helper.assertTrue(NonDamageableAnvilMergeHelper.tryMergeSameItem(left, right, null, player) != null,
                    "Non-damageable anvil merge should recover after clearing stored right contents");
        });
    }
}
