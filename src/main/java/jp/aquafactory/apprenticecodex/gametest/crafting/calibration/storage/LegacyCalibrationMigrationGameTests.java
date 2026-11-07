package jp.aquafactory.apprenticecodex.gametest.crafting.calibration.storage;

import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.InventoryTestSupport.getCustomDataTag;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LegacyCalibrationMigrationGameTests {
    private LegacyCalibrationMigrationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void fullStackLegacyListsPreserveComponents(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.SCROLLCASTER_GAUNTLET.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get(), "SpellCalibration", "Adjustments", Format.FULL_STACK, true),
                new MigrationCase(ItemRegistry.REVOLVERCAST_STAFF.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get(), "SpellCalibration", "Adjustments", Format.FULL_STACK, false),
                new MigrationCase(ItemRegistry.MITHRIL_FREECAST_STAFF.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get(), "SpellCalibration", "Adjustments", Format.FULL_STACK, false),
                new MigrationCase(ItemRegistry.CHARGECAST_CATALYSTBOOK.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get(), "ChargecastCalibration", "Adjustments", Format.FULL_STACK, false)
        )));
    }

    @GameTest(template = "gametest/basic_floor")
    public static void nestedIdLegacyAmuletsMigrateOnMutation(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.AUTOCAST_AMULET.get(), ItemRegistry.WISDOM_SHARD.get(), "SpellCalibration", "Adjustments", Format.NESTED_ID, false),
                new MigrationCase(ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get(), "SpellCalibration", "Adjustments", Format.NESTED_ID, false)
        )));
    }

    @GameTest(template = "gametest/basic_floor")
    public static void idListLegacyShieldsMigrateOnMutation(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.BULWARK_GREATSHIELD.get(), ItemRegistry.WISDOM_SHARD.get(), "BulwarkGreatshieldCalibration", "Adjustments", Format.ID_LIST, false),
                new MigrationCase(ItemRegistry.PARRYCAST_BUCKLER.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get(), "ParrycastBucklerCalibration", "Adjustments", Format.ID_LIST, false),
                new MigrationCase(ItemRegistry.REFLECTCAST_SHIELD.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get(), "ReflectcastShieldCalibration", "Adjustments", Format.ID_LIST, false)
        )));
    }

    @GameTest(template = "gametest/basic_floor")
    public static void singleIdLegacyArmorMigratesOnMutation(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get(), "MagiAgentSuitCalibration", "AdjustmentItem", Format.SINGLE_ID, false),
                new MigrationCase(ItemRegistry.MAGI_AGENT_SUIT_COAT.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get(), "MagiAgentSuitCalibration", "AdjustmentItem", Format.SINGLE_ID, false),
                new MigrationCase(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get(), "MagiAgentSuitCalibration", "AdjustmentItem", Format.SINGLE_ID, false),
                new MigrationCase(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get(), io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get(), "MagiAgentSuitCalibration", "AdjustmentItem", Format.SINGLE_ID, false)
        )));
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullStackLegacySpellgunsPreserveComponents(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.IRON_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "Adjustment", Format.SPELLGUN_STACK, false),
                new MigrationCase(ItemRegistry.COPPER_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "Adjustment", Format.SPELLGUN_STACK, false),
                new MigrationCase(ItemRegistry.GOLD_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "Adjustment", Format.SPELLGUN_STACK, false),
                new MigrationCase(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "Adjustment", Format.SPELLGUN_STACK, false),
                new MigrationCase(ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "Adjustment", Format.SPELLGUN_STACK, false)
        )));
    }

    @GameTest(template = "gametest/basic_floor")
    public static void idOnlyLegacySpellgunFallbackMigratesOnMutation(GameTestHelper helper) {
        helper.succeedIf(() -> verifyCases(helper, List.of(
                new MigrationCase(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(), ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), "SpellgunCalibration", "AdjustmentItem", Format.SPELLGUN_ID, false)
        )));
    }

    private enum Format { FULL_STACK, NESTED_ID, ID_LIST, SINGLE_ID, SPELLGUN_STACK, SPELLGUN_ID }

    private record MigrationCase(Item item, Item adjustment, String root, String field,
                                 Format format, boolean preservedSibling) {}

    private static void verifyCases(GameTestHelper helper, List<MigrationCase> cases) {
        var lookup = helper.getLevel().registryAccess();
        for (var testCase : cases) {
            var stack = new ItemStack(testCase.item());
            var adjustment = new ItemStack(testCase.adjustment());
            // 完全なスタックを保存する旧形式では、IDだけ残る退行も検出する。
            if (testCase.format() == Format.FULL_STACK || testCase.format() == Format.SPELLGUN_STACK) {
                adjustment.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy adjustment components"));
            }
            switch (testCase.format()) {
                case FULL_STACK -> putLegacyFullStackList(stack, testCase.root(), adjustment,
                        lookup, testCase.preservedSibling());
                case NESTED_ID -> putLegacyNestedIdList(stack, testCase.root(), adjustment);
                case ID_LIST -> putLegacyIdList(stack, testCase.root(), adjustment);
                case SINGLE_ID -> putLegacySingleId(stack, testCase.root(), testCase.field(), adjustment);
                case SPELLGUN_STACK -> putLegacySpellGun(stack, adjustment, lookup, false);
                case SPELLGUN_ID -> putLegacySpellGun(stack, adjustment, lookup, true);
            }
            assertLegacyMigration(helper, stack, adjustment, testCase.root(), testCase.field(),
                    testCase.preservedSibling(), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }

    private static void assertLegacyMigration(
            GameTestHelper helper,
            ItemStack targetStack,
            ItemStack expectedAdjustment,
            String legacyRootName,
            String legacyFieldName,
            boolean expectPreservedSibling,
            String context
    ) {
        var lookupProvider = helper.getLevel().registryAccess();
        var target = (SpellCalibrationAdjustmentTarget) targetStack.getItem();
        var legacySnapshot = getCustomDataTag(targetStack);
        var legacyRead = target.getCalibrationAdjustment(targetStack, 0, lookupProvider);
        helper.assertTrue(ItemStack.matches(expectedAdjustment.copyWithCount(1), legacyRead),
                "Legacy adjustment should remain readable: " + context);
        helper.assertTrue(legacySnapshot != null && legacySnapshot.equals(getCustomDataTag(targetStack)),
                "Legacy reads should be side-effect free: " + context);
        helper.assertTrue(target.trySetCalibrationAdjustment(
                        targetStack,
                        0,
                        expectedAdjustment,
                        lookupProvider
                ),
                "First legacy mutation should succeed: " + context);

        var migratedRead = target.getCalibrationAdjustment(targetStack, 0, lookupProvider);
        helper.assertTrue(ItemStack.matches(expectedAdjustment.copyWithCount(1), migratedRead),
                "First legacy mutation should write the common full ItemStack format: " + context);
        var migratedRoot = getCustomDataTag(targetStack);
        helper.assertTrue(migratedRoot != null
                        && migratedRoot.contains("ApprenticeCodexCalibrationAdjustments", Tag.TAG_COMPOUND),
                "First legacy mutation should create common storage: " + context);
        if (migratedRoot.contains(legacyRootName, Tag.TAG_COMPOUND)) {
            helper.assertFalse(migratedRoot.getCompound(legacyRootName).contains(legacyFieldName),
                    "Migration should remove only the legacy adjustment field: " + context);
        }
        if (expectPreservedSibling) {
            helper.assertTrue(migratedRoot.getCompound(legacyRootName).getInt("PreservedSibling") == 1,
                    "Migration should preserve unrelated legacy-root data: " + context);
        }
    }

    private static void putLegacyFullStackList(
            ItemStack targetStack,
            String rootName,
            ItemStack adjustment,
            HolderLookup.Provider lookupProvider,
            boolean addPreservedSibling
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            if (addPreservedSibling) {
                legacyRoot.putInt("PreservedSibling", 1);
            }
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.put("Item", adjustment.copyWithCount(1).saveOptional(lookupProvider));
            var adjustments = new ListTag();
            adjustments.add(entry);
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacyNestedIdList(ItemStack targetStack, String rootName, ItemStack adjustment) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var storedItem = new CompoundTag();
            storedItem.putString("ItemId", BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.put("Item", storedItem);
            var adjustments = new ListTag();
            adjustments.add(entry);
            var legacyRoot = new CompoundTag();
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacyIdList(ItemStack targetStack, String rootName, ItemStack adjustment) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var entry = new CompoundTag();
            entry.putInt("Slot", 0);
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            var adjustments = new ListTag();
            adjustments.add(entry);
            var legacyRoot = new CompoundTag();
            legacyRoot.put("Adjustments", adjustments);
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacySingleId(
            ItemStack targetStack,
            String rootName,
            String fieldName,
            ItemStack adjustment
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            legacyRoot.putString(fieldName, BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString());
            root.put(rootName, legacyRoot);
        });
    }

    private static void putLegacySpellGun(
            ItemStack targetStack,
            ItemStack adjustment,
            HolderLookup.Provider lookupProvider,
            boolean idOnly
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, targetStack, root -> {
            var legacyRoot = new CompoundTag();
            if (idOnly) {
                legacyRoot.putString(
                        "AdjustmentItem",
                        BuiltInRegistries.ITEM.getKey(adjustment.getItem()).toString()
                );
            } else {
                legacyRoot.put("Adjustment", adjustment.copyWithCount(1).saveOptional(lookupProvider));
            }
            root.put("SpellgunCalibration", legacyRoot);
        });
    }
}
