package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.storage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.support.InventoryTestSupport.getCustomDataTag;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.roundTripItemStack;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CalibrationStorageGameTests {
    private CalibrationStorageGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void canonicalStoragePreservesComponentsAndNormalizesCount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharpness = enchantmentLookup.getOrThrow(Enchantments.SHARPNESS);
            var sharpnessBook = createEnchantedBook(sharpness, 1);
            sharpnessBook.set(DataComponents.CUSTOM_NAME, Component.literal("Stored calibration component"));
            sharpnessBook.setCount(4);
            var anotherSharpnessBook = sharpnessBook.copy();
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    sharpnessBook,
                    helper.getLevel().registryAccess()
            );
            var expectedStoredBook = sharpnessBook.copyWithCount(1);
            var storedBook = SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(gauntlet, 0);
            helper.assertTrue(storedBook.getCount() == 1
                            && ItemStack.isSameItemSameComponents(expectedStoredBook, storedBook),
                    "Common adjustment storage should preserve all components and normalize count to one");
            var restoredGauntlet = roundTripItemStack(helper, gauntlet);
            var restoredBook = SpellCalibrationAdjustmentGameTestSupport
                    .getCalibrationAdjustment(restoredGauntlet, 0);
            helper.assertTrue(ItemStack.isSameItemSameComponents(expectedStoredBook, restoredBook),
                    "Common adjustment storage should preserve components through ItemStack serialization");
            var commonRoot = getCustomDataTag(gauntlet);
            helper.assertTrue(commonRoot != null
                            && commonRoot.contains("ApprenticeCodexCalibrationAdjustments", Tag.TAG_COMPOUND),
                    "New writes should use the common adjustment storage root");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            gauntlet, 1, anotherSharpnessBook),
                    "Scrollcaster Gauntlet should reject new enchanted-book adjustments");

        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyDuplicateAdjustmentsRemainRemovable(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var legacyAutocast = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            CustomData.update(DataComponents.CUSTOM_DATA, legacyAutocast, rootTag -> {
                var storedCalibration = new CompoundTag();
                var adjustments = new ListTag();
                var storedItem = new CompoundTag();
                storedItem.putString("ItemId", BuiltInRegistries.ITEM.getKey(silverRing.getItem()).toString());
                for (var slot = 0; slot < 2; ++slot) {
                    var entry = new CompoundTag();
                    entry.putInt("Slot", slot);
                    entry.put("Item", storedItem.copy());
                    adjustments.add(entry);
                }
                storedCalibration.put("Adjustments", adjustments);
                rootTag.put("SpellCalibration", storedCalibration);
            });
            var legacySnapshot = getCustomDataTag(legacyAutocast);
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(legacyAutocast, 1).isEmpty(),
                    "Legacy duplicate adjustment should remain readable");
            helper.assertTrue(legacySnapshot != null && legacySnapshot.equals(getCustomDataTag(legacyAutocast)),
                    "Reading legacy adjustments should not mutate their storage");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            legacyAutocast, 0, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the first slot");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport
                            .getCalibrationAdjustment(legacyAutocast, 1).is(silverRing.getItem()),
                    "First legacy mutation should migrate untouched adjustment slots");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            legacyAutocast, 1, ItemStack.EMPTY),
                    "Legacy duplicate adjustment should be removable from the second slot");
            var migratedRoot = getCustomDataTag(legacyAutocast);
            helper.assertTrue(migratedRoot == null || !migratedRoot.contains("SpellCalibration", Tag.TAG_COMPOUND),
                    "First legacy mutation should remove the legacy adjustment field");
        });
    }
}
