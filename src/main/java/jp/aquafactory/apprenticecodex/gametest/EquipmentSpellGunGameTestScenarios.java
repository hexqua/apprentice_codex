package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class EquipmentSpellGunGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final UUID FIXED_COOLDOWN_REDUCTION_TEST_MODIFIER_ID =
            UUID.fromString("b8a97df3-35e8-4c26-b4aa-74a0960b524e");

    private EquipmentSpellGunGameTestScenarios() {
    }

    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var copper = (AbstractSpellGunItem) ItemRegistry.COPPER_SPELLCASTER_GUN.get();
            var gold = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var diamond = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var instantRecastSpell = SpellRegistry.HIGANBANA.get();
            var longRecastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            var supportedLongSpell = SpellRegistry.MANTIS_LEAP.get();
            var continuousSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();

            helper.assertFalse(iron.canImbueSpell(instantRecastSpell, 1),
                    "Iron Spellcaster Gun should continue rejecting recast spells");
            helper.assertTrue(copper.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Copper Spellcaster Gun should allow instant spell imbuing");
            helper.assertTrue(supportedLongSpell.getSpellCooldown() <= 20 * 20
                            && copper.canImbueSpell(supportedLongSpell, 1),
                    "Copper Spellcaster Gun should continue allowing long spell imbuing");
            helper.assertTrue(gold.canImbueSpell(instantRecastSpell, 1),
                    "Gold Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(instantRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(longRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow long recast spell imbuing");
            helper.assertFalse(diamond.canImbueSpell(continuousSpell, 1),
                    "Diamond Spellcaster Gun should continue rejecting continuous spells");
        });
    }
    static void spellcasterGunAbilityTooltipUsesInstantLongCastOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    false,
                    "Iron Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    true,
                    "Copper Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                    false,
                    "Gold Spellcaster Gun"
            );
            assertSpellgunAbilityTooltipKeys(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(),
                    true,
                    "Diamond Spellcaster Gun"
            );

            var ironLines = collectSpellgunAbilityTooltipLines(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get()
            );
            var copperLines = collectSpellgunAbilityTooltipLines(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.COPPER_SPELLCASTER_GUN.get()
            );
            var goldLines = collectSpellgunAbilityTooltipLines(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get()
            );
            var diamondLines = collectSpellgunAbilityTooltipLines(
                    helper,
                    (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
            );
            helper.assertTrue(containsTranslatableKey(ironLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast"),
                    "Iron Spellcaster Gun should show its fixed cooldown ability");
            helper.assertTrue(containsTranslatableKey(copperLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast"),
                    "Copper Spellcaster Gun should show its fixed cooldown ability");
            assertTooltipStringArgument(helper, ironLines,
                    "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast", "0.2",
                    "Iron Spellcaster Gun server tooltip should fall back to its fixed base cooldown");
            assertTooltipStringArgument(helper, copperLines,
                    "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast", "1",
                    "Copper Spellcaster Gun server tooltip should fall back to its fixed base cooldown");
            helper.assertTrue(containsTranslatableKey(goldLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_subtract_cooldown")
                            && !containsTranslatableKey(goldLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast"),
                    "Gold Spellcaster Gun should show only its subtractive cooldown ability");
            helper.assertFalse(containsTranslatableKey(diamondLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_reduce_recast")
                            || containsTranslatableKey(diamondLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_subtract_cooldown"),
                    "Diamond Spellcaster Gun should not show a cooldown adjustment ability");
        });
    }

    static void spellcasterGunTooltipsUseCommonOperationDescriptions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var item : List.of(
                    ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                    ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
            )) {
                var stack = new ItemStack(item);
                assertTooltipKeyAt(helper, stack, 0,
                        "item.apprenticecodex.common.spellgun.desc_1",
                        item.getDescriptionId() + " should show the common mainhand operation tooltip first");
                assertTooltipKeyArgument(helper, stack, 0, "key.attack",
                        item.getDescriptionId() + " should use the configured attack key name");
                assertTooltipKeyAt(helper, stack, 1,
                        "item.apprenticecodex.common.spellgun.desc_2",
                        item.getDescriptionId() + " should show the common offhand operation tooltip second");
                assertTooltipKeyArgument(helper, stack, 1, "key.use",
                        item.getDescriptionId() + " should use the configured use key name");
                if (ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                    assertTooltipKeyAt(helper, stack, 2,
                            "item.apprenticecodex.common.spellgun.epicfight.offhand_warning",
                            item.getDescriptionId() + " should show the Epic Fight offhand Guard warning third");
                } else {
                    var lines = new ArrayList<Component>();
                    item.appendHoverText(stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL);
                    helper.assertFalse(containsTranslatableKey(lines,
                                    "item.apprenticecodex.common.spellgun.epicfight.offhand_warning"),
                            item.getDescriptionId() + " should hide the Epic Fight offhand Guard warning without Epic Fight");
                }
            }
        });
    }

    static void spellcasterRoundTooltipsUseSharedKeys(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var item : List.of(
                    ItemRegistry.RAPID_SPELLCASTER_ROUND.get(),
                    ItemRegistry.BASIC_SPELLCASTER_ROUND.get(),
                    ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(),
                    ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(),
                    ItemRegistry.SPELL_DOMINATOR_ROUND.get()
            )) {
                assertTooltipKeyAt(helper, new ItemStack(item), 0,
                        "item.apprenticecodex.common.round.desc",
                        item.getDescriptionId() + " should use the common round tooltip");
            }

            assertTooltipKeyAt(helper, new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()), 0,
                    "item.apprenticecodex.multi_purpose_spell_round.desc",
                    "Multi-purpose Spell Round should retain its dedicated tooltip");

            for (var item : List.of(
                    ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                    ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                    ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                    ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                    ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                    ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()
            )) {
                assertTooltipKeyAt(helper, new ItemStack(item), 0,
                        "item.apprenticecodex.common.empty_casing.desc",
                        item.getDescriptionId() + " should use the common empty casing tooltip");
            }
        });
    }

    static void spellcasterGunsRemoveBaseSpellPowerButKeepSurge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            for (var item : List.of(
                    ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                    ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                    ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
            )) {
                var stack = new ItemStack(item);
                var baseModifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
                helper.assertTrue(baseModifiers.get(spellPower).isEmpty(),
                        item.getDescriptionId() + " should not have base spell power");

                stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
                var enchantedSpellPower = sumModifierAmount(
                        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(spellPower),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(enchantedSpellPower - 0.02D) < 1.0e-9D,
                        item.getDescriptionId() + " should retain Surge spell power");
            }
        });
    }

    static void spellcasterGunsAcceptOnlySilverSpellAmplifierCalibration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellguns = List.of(
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())
            );
            for (var spellgun : spellguns) {
                helper.assertTrue(spellgun.getItem() instanceof SpellCalibrationAdjustmentTarget,
                        spellgun.getDescriptionId() + " should support calibration adjustments");
                var target = (SpellCalibrationAdjustmentTarget) spellgun.getItem();
                helper.assertTrue(target.getCalibrationAdjustmentSlotCount(spellgun) == 1,
                        spellgun.getDescriptionId() + " should expose exactly one adjustment slot");
                helper.assertTrue(target.canPlaceCalibrationAdjustment(
                                spellgun, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                        spellgun.getDescriptionId() + " should accept Silver Spell Amplifier");
                helper.assertFalse(target.canPlaceCalibrationAdjustment(
                                spellgun, 0, new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get())),
                        spellgun.getDescriptionId() + " should reject other Spell Amplifiers");
                helper.assertFalse(target.canPlaceCalibrationAdjustment(
                                spellgun, 1, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                        spellgun.getDescriptionId() + " should reject out-of-range adjustment slots");

                var adjustment = new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get(), 4);
                helper.assertTrue(target.trySetCalibrationAdjustment(spellgun, 0, adjustment),
                        spellgun.getDescriptionId() + " should store Silver Spell Amplifier calibration");
                helper.assertTrue(target.getCalibrationAdjustment(spellgun, 0).getCount() == 1,
                        spellgun.getDescriptionId() + " should store one adjustment item");

                var restored = roundTripItemStack(spellgun);
                var restoredTarget = (SpellCalibrationAdjustmentTarget) restored.getItem();
                helper.assertTrue(restoredTarget.getCalibrationAdjustment(restored, 0)
                                .is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                        spellgun.getDescriptionId() + " should retain calibration after save/load");
                helper.assertTrue(restoredTarget.trySetCalibrationAdjustment(restored, 0, ItemStack.EMPTY),
                        spellgun.getDescriptionId() + " should allow adjustment removal");
                helper.assertTrue(restoredTarget.getCalibrationAdjustment(restored, 0).isEmpty(),
                        spellgun.getDescriptionId() + " should clear removed calibration data");
            }

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellgun_calibration_slot_test");
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT)
                    .set(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()));
            helper.assertTrue(menu.isAdjustmentSlotEnabled(0),
                    "Spellgun should enable its first adjustment slot");
            helper.assertFalse(menu.isAdjustmentSlotEnabled(1) || menu.isAdjustmentSlotEnabled(2),
                    "Spellgun should keep the remaining adjustment slots disabled");
        });
    }

    static void spellcasterGunsKeepCalibrationBenchImbueOperationalAndSafe(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellgun_calibration_imbue_test");
            for (var spellgun : List.of(
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())
            )) {
                var menu = createSpellCalibrationBenchMenuWithTarget(player, spellgun);
                helper.assertTrue(menu.hasOperationalImbueTarget(),
                        spellgun.getDescriptionId() + " should remain an operational Calibration Bench imbue target");
                helper.assertTrue(menu.isScrollSlotEnabled(0),
                        spellgun.getDescriptionId() + " should keep its spell scroll slot enabled");
                helper.assertFalse(menu.shouldRenderUnsupportedImbueOverlay(0),
                        spellgun.getDescriptionId() + " should not show the Arcane Anvil requirement overlay");
            }

            var goldSpellgun = new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get());
            var goldMenu = createSpellCalibrationBenchMenuWithTarget(player, goldSpellgun);
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicMissileScroll = createSpellScroll(magicMissile);
            helper.assertTrue(goldMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(magicMissileScroll),
                    "Spellgun scroll slot should accept a supported spell");

            var playerInventoryMenuSlot = SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START
                    + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
            player.getInventory().setItem(9, magicMissileScroll.copy());
            var insertedScroll = goldMenu.quickMoveStack(player, playerInventoryMenuSlot);
            helper.assertTrue(insertedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Shift-clicking a supported scroll should report a successful move");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(),
                    "Successfully inserted Spellgun scroll should leave the player inventory");
            assertStackHasSpell(helper, goldMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).getItem(),
                    magicMissile, 1, "Shift-clicked scroll should be applied to Spellgun");

            var extractedScroll = goldMenu.quickMoveStack(player, SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START);
            helper.assertTrue(extractedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Shift-clicking the Spellgun scroll slot should extract the imbued spell");
            var extractedContainer = ISpellContainer.get(
                    goldMenu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).getItem());
            helper.assertTrue(extractedContainer != null && extractedContainer.getActiveSpellCount() == 0,
                    "Extracting the Spellgun scroll should clear its imbued spell");

            var ironMenu = createSpellCalibrationBenchMenuWithTarget(
                    player,
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get())
            );
            var rejectedScroll = createSpellScroll(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());
            player.getInventory().setItem(9, rejectedScroll.copy());
            helper.assertFalse(ironMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START)
                            .mayPlace(rejectedScroll),
                    "Spellgun scroll slot should reject an unsupported spell before moving it");
            helper.assertTrue(ironMenu.quickMoveStack(player, playerInventoryMenuSlot).isEmpty(),
                    "Shift-clicking an unsupported Spellgun scroll should fail without moving it");
            helper.assertTrue(player.getInventory().getItem(9).is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Rejected Spellgun scroll should remain in the player inventory");
        });
    }

    static void silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var attunementAttribute = MagicTools.resolveSchoolPowerAttribute(magicMissile.getSchoolType());
            helper.assertTrue(attunementAttribute != null,
                    "Magic Missile school power attribute should be available for Spellgun calibration test");

            var cases = List.of(
                    new SpellgunAttributeCase(EnchantmentRegistry.ALACRITY.get(),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.02D),
                    new SpellgunAttributeCase(EnchantmentRegistry.REFLUX.get(),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.05D),
                    new SpellgunAttributeCase(EnchantmentRegistry.RESERVOIR.get(),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                            AttributeModifier.Operation.ADDITION, 20.0D),
                    new SpellgunAttributeCase(EnchantmentRegistry.SURGE.get(),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE, 0.02D),
                    new SpellgunAttributeCase(EnchantmentRegistry.ATTUNEMENT.get(), attunementAttribute,
                            AttributeModifier.Operation.MULTIPLY_BASE,
                            AttributeEnchantmentType.ATTUNEMENT.amountPerLevel()),
                    new SpellgunAttributeCase(EnchantmentRegistry.TENSE.get(),
                            io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(),
                            AttributeModifier.Operation.MULTIPLY_BASE,
                            AttributeEnchantmentType.TENSE.amountPerLevel())
            );
            for (var attributeCase : cases) {
                var stack = createInitializedPresetStack(item);
                stack.enchant(attributeCase.enchantment(), 1);
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase,
                        "Unadjusted Spellgun should apply enchanted Attribute in mainhand");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase.withAmount(0.0D),
                        "Unadjusted Spellgun should not apply enchanted Attribute in offhand");

                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                stack, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                        "Spellgun should accept Silver Spell Amplifier calibration");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase.withAmount(0.0D),
                        "Adjusted Spellgun should remove enchanted Attribute from mainhand");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase,
                        "Adjusted Spellgun should apply enchanted Attribute in offhand");

                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                stack, 0, ItemStack.EMPTY),
                        "Spellgun should allow Silver Spell Amplifier removal");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.MAINHAND, attributeCase,
                        "Spellgun should restore enchanted Attribute to mainhand after adjustment removal");
                assertSpellgunAttributeSlot(helper, stack, EquipmentSlot.OFFHAND, attributeCase.withAmount(0.0D),
                        "Spellgun should remove enchanted Attribute from offhand after adjustment removal");
            }

        });
    }

    static void silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var spellPower = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var mainhandStack = createInitializedPresetStack(item);
            mainhandStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            var offhandStack = createInitializedPresetStack(item);
            offhandStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            offhandStack, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Offhand Spellgun should accept Silver Spell Amplifier calibration");

            var mainhandModifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, mainhandStack)
                    .get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            var offhandModifiers = item.getAttributeModifiers(EquipmentSlot.OFFHAND, offhandStack)
                    .get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            helper.assertTrue(mainhandModifiers.size() == 1 && offhandModifiers.size() == 1,
                    "Dual Spellguns should each expose one Surge modifier: main=" + mainhandModifiers
                            + ", off=" + offhandModifiers);
            helper.assertFalse(mainhandModifiers.get(0).getId().equals(offhandModifiers.get(0).getId()),
                    "Mainhand and offhand Spellgun modifiers must use different UUIDs");

            var attributeInstance = new AttributeInstance(spellPower, unused -> {
            });
            mainhandModifiers.forEach(attributeInstance::addTransientModifier);
            offhandModifiers.forEach(attributeInstance::addTransientModifier);
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.04D) < 1.0e-9D,
                    "Dual Spellgun Surge modifiers should stack to 0.04");

            mainhandModifiers.forEach(modifier -> attributeInstance.removeModifier(modifier.getId()));
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.02D) < 1.0e-9D,
                    "Removing one Spellgun should leave the other Spellgun modifier active");
        });
    }

    static void silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var maxMana = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();

            var orbBeforeCalibration = createInitializedPresetStack(item);
            orbBeforeCalibration.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    orbBeforeCalibration,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            orbBeforeCalibration, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Spellgun should accept Silver Spell Amplifier after Upgrade Orb data");
            assertAdjustedUpgradeOrbSlots(helper, orbBeforeCalibration, maxMana, "Orb-before-calibration");

            var calibrationBeforeOrb = createInitializedPresetStack(item);
            calibrationBeforeOrb.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            calibrationBeforeOrb, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Spellgun should accept Silver Spell Amplifier before Upgrade Orb data");
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    calibrationBeforeOrb,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            assertAdjustedUpgradeOrbSlots(helper, calibrationBeforeOrb, maxMana, "Calibration-before-orb");

            var restored = roundTripItemStack(orbBeforeCalibration);
            assertAdjustedUpgradeOrbSlots(helper, restored, maxMana, "Save-load");

            var unadjusted = createInitializedPresetStack(item);
            unadjusted.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            createUpgradeData(
                    helper.getLevel().registryAccess(),
                    unadjusted,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );
            var unadjustedMainhand = resolveRuntimeSpellgunModifiers(unadjusted, EquipmentSlot.MAINHAND);
            helper.assertTrue(Math.abs(sumModifierAmount(
                            unadjustedMainhand.get(maxMana), AttributeModifier.Operation.ADDITION) - 70.0D) < 1.0e-9D,
                    "Unadjusted Spellgun should retain Reservoir plus Upgrade Orb in mainhand: "
                            + describeModifiers(unadjustedMainhand));
            helper.assertTrue(resolveRuntimeSpellgunModifiers(unadjusted, EquipmentSlot.OFFHAND).get(maxMana).isEmpty(),
                    "Unadjusted Spellgun should not expose Upgrade Orb in offhand");
        });
    }
    static void reflectcastShieldCastRestrictionsFollowCalibration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            var item = (jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield) stack.getItem();
            var defaultTooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, helper.getLevel(), defaultTooltipLines, TooltipFlag.Default.NORMAL);
            helper.assertTrue(containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_default"),
                    "Reflectcast Shield should show the imbued-spell cast tooltip by default");
            helper.assertFalse(containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield." + "hint")
                            || containsTranslatableKey(defaultTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_" + "hint"),
                    "Reflectcast Shield should not show removed legacy tooltip keys");
            var defaultAbilityLines = collectReflectcastAbilityTooltipLines(helper, item, stack);
            helper.assertTrue(containsTranslatableKey(defaultAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_none"),
                    "Reflectcast Shield should show the empty ability line without Silver Ring");
            var defaultRestrictionLines = item.getImbueRestrictionTooltipLines(stack);
            helper.assertTrue(defaultRestrictionLines.size() == 2
                            && containsTranslatableKey(defaultRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only")
                            && containsTranslatableKey(defaultRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_no_recast"),
                    "Reflectcast Shield should show instant-only and no-recast restrictions without Silver Ring");
            helper.assertFalse(item.supportsManaBypass(SpellRegistry.SENSE_EVIL.get()),
                    "Reflectcast Shield should consume normal spell mana");
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.SENSE_EVIL.get(), 1),
                    "Reflectcast Shield should use instant spells without calibration");
            helper.assertFalse(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Reflectcast Shield should require Silver Ring for long spells");
            helper.assertFalse(item.canUseConfiguredSpell(stack,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Reflectcast Shield should require Silver Ring for continuous spells");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 0, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 1, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.hasSilverRing(stack),
                    "Reflectcast Shield should store Silver Ring calibration");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.hasWisdomShard(stack),
                    "Reflectcast Shield should store Wisdom Shard calibration alongside Silver Ring");
            var wisdomTooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, helper.getLevel(), wisdomTooltipLines, TooltipFlag.Default.NORMAL);
            helper.assertTrue(containsTranslatableKey(wisdomTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_wisdom"),
                    "Wisdom Shard should switch Reflectcast Shield to the selected-spell cast tooltip");
            helper.assertFalse(containsTranslatableKey(wisdomTooltipLines,
                            "item.apprenticecodex.reflectcast_shield.cast_default"),
                    "Wisdom Shard should hide Reflectcast Shield's imbued-spell cast tooltip");
            var silverAbilityLines = collectReflectcastAbilityTooltipLines(helper, item, stack);
            helper.assertTrue(containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_long_to_instant")
                            && containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_hold_continuous")
                            && containsTranslatableKey(silverAbilityLines,
                            "item.apprenticecodex.spellgun.tooltip.ability_extend_cooldown"),
                    "Silver Ring should show all Reflectcast Shield ability lines");
            var silverRestrictionLines = item.getImbueRestrictionTooltipLines(stack);
            helper.assertTrue(silverRestrictionLines.size() == 1
                            && containsTranslatableKey(silverRestrictionLines,
                            "item.apprenticecodex.spellgun.tooltip.restrict_restrict_no_recast"),
                    "Silver Ring should leave only the no-recast restriction");
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Silver Ring should allow long spells");
            helper.assertTrue(item.canUseConfiguredSpell(stack,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Silver Ring should allow continuous spells");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "reflectcast_shield_calibration_test");
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
            for (var slot = 0; slot < 3; slot++) {
                helper.assertTrue(menu.isAdjustmentSlotEnabled(slot),
                        "Reflectcast Shield adjustment slot should be enabled: " + slot);
            }

            var castStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    castStack, 0, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            var castContainer = ISpellContainer.create(1, false, false).mutableCopy();
            castContainer.addSpellAtIndex(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false);
            ISpellContainer.set(castStack, castContainer.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, castStack);
            castStack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(player.getUseItemRemainingTicks() == castStack.getUseDuration(),
                    "Reflectcast Shield should keep vanilla shield block preparation time");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Reflectcast Shield continuous cast requires MagicData");
            magicData.setMana(1000.0F);
            var longSpell = SpellRegistry.MANTIS_LEAP.get();
            var baseCooldown = 80;
            magicData.setPlayerCastingItem(castStack);
            var longCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    baseCooldown, longSpell, player, CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime
                    .onSpellCooldownAdded(longCooldownEvent);
            helper.assertTrue(longCooldownEvent.getEffectiveCooldown()
                            == baseCooldown + longSpell.getEffectiveCastTime(1, player),
                    "Reflectcast Shield should extend LONG cooldown by its effective cast time");
            magicData.setPlayerCastingItem(ItemStack.EMPTY);
            var manaBeforeCast = magicData.getMana();
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "A valid block trigger should start Reflectcast continuous casting immediately");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime
                            .shouldBypassMagicManager(magicData),
                    "Reflectcast continuous casting should bypass Iron's standard cast tick");
            helper.assertTrue(magicData.getMana() < manaBeforeCast,
                    "Reflectcast continuous casting should consume normal spell mana");
            helper.assertFalse(jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "Additional blocks should not restart an active continuous cast");
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.finishUse(player);
            helper.assertFalse(magicData.isCasting(),
                    "Releasing Reflectcast Shield should clear its continuous casting state");
            player.stopUsingItem();
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime.clear(player);
        });
    }

    static void spellgunServerConfigDefaultsMatchBalanceValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunMaxInstantImbueCooldownTicks() == 20 * 5,
                    "Iron Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunOverriddenSpellCooldownTicks() == 4,
                    "Iron Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunMaxInstantImbueCooldownTicks() == 20 * 20,
                    "Copper Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunOverriddenSpellCooldownTicks() == 20,
                    "Copper Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunReducedCooldownMinimumTicks() == 10,
                    "Gold Spellcaster Gun reduced cooldown minimum default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunCooldownReductionTicks() == 200,
                    "Gold Spellcaster Gun cooldown reduction default changed");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_default_config_test");
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 4,
                    "Iron Spellcaster Gun should use its default fixed cooldown");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()), spell, 200, 20,
                    "Copper Spellcaster Gun should use its default fixed cooldown");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 400, 200,
                    "Gold Spellcaster Gun should subtract its default reduction");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 100, 10,
                    "Gold Spellcaster Gun should honor its reduced cooldown minimum");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 5, 5,
                    "Gold Spellcaster Gun should not extend cooldowns below its minimum");
            var longSpell = SpellRegistry.MANTIS_LEAP.get();
            helper.assertTrue(longSpell.getEffectiveCastTime(1, player) > 0,
                    "Diamond Spellcaster Gun cooldown test requires a long spell cast time");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()), longSpell, 200, 200,
                    "Diamond Spellcaster Gun should keep the original cooldown without adding cast time");
        });
    }

    static void spellgunFixedCooldownUsesCooldownReductionAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_fixed_cooldown_attribute_test");
            var cooldownAttribute = player.getAttribute(
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get()
            );
            helper.assertTrue(cooldownAttribute != null,
                    "Spellgun fixed cooldown test player is missing cooldown reduction attribute");
            cooldownAttribute.addTransientModifier(new AttributeModifier(
                    FIXED_COOLDOWN_REDUCTION_TEST_MODIFIER_ID,
                    "apprenticecodex.spellgun.fixed_cooldown_attribute_test",
                    0.25D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 3,
                    "Iron Spellcaster Gun should apply cooldown reduction to its fixed base cooldown");
            assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()), spell, 200, 15,
                    "Copper Spellcaster Gun should apply cooldown reduction without the sword multiplier");
        });
    }

    static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    0,
                    4,
                    1,
                    20,
                    10,
                    200
            ))) {
                var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
                var copper = (AbstractSpellGunItem) ItemRegistry.COPPER_SPELLCASTER_GUN.get();
                var gold = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
                var diamond = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
                var cooldownLimitedSpell = SpellRegistry.SEARCH_BEACON.get();
                helper.assertTrue(cooldownLimitedSpell.getSpellCooldown() > 20 * 5,
                        "Search Beacon should remain above Iron Spellcaster Gun's default cooldown limit");
                helper.assertTrue(iron.canImbueSpell(cooldownLimitedSpell, 1),
                        "Iron Spellcaster Gun maxInstantImbueCooldownTicks=0 should disable only the cooldown limit");
                helper.assertFalse(iron.canImbueSpell(SpellRegistry.HIGANBANA.get(), 1),
                        "Iron Spellcaster Gun should still reject recast spells when only the cooldown limit is disabled");
                helper.assertFalse(copper.canImbueSpell(cooldownLimitedSpell, 1),
                        "Copper Spellcaster Gun should enforce an overridden imbue cooldown limit");
                helper.assertTrue(gold.canImbueSpell(cooldownLimitedSpell, 1),
                        "Gold Spellcaster Gun should have no imbue cooldown limit");
                helper.assertTrue(diamond.canImbueSpell(cooldownLimitedSpell, 1),
                        "Diamond Spellcaster Gun should have no imbue cooldown limit");
                helper.assertFalse(containsTranslatableKey(gold.getImbueRestrictionTooltipLines(),
                                "item.apprenticecodex.spellgun.tooltip.restrict_restrict_cooldown"),
                        "Gold Spellcaster Gun should not show an imbue cooldown limit");
                helper.assertFalse(containsTranslatableKey(diamond.getImbueRestrictionTooltipLines(),
                                "item.apprenticecodex.spellgun.tooltip.restrict_restrict_cooldown"),
                        "Diamond Spellcaster Gun should not show an imbue cooldown limit");
            }
        });
    }
    static void spellgunZeroCooldownSettingsRemainNonNegative(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    20 * 5,
                    0,
                    20 * 20,
                    0,
                    0,
                    0
            ))) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_zero_cooldown_config_test");
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Iron Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
                assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Copper Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
                assertSpellgunCooldownAdjustment(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 200, 200,
                        "Gold Spellcaster Gun zero reduction should preserve the original cooldown");
            }
        });
    }
    static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            applyRestrictedImbueNormalization(helper, stack, item, spell, 1);

            var player = createArcherMultiplePlayer(helper, new BlockPos(0, 12, 0), "spellgun_recast_ammo_bypass_test");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Selected offhand Spellcaster Gun should consume the input even when its cast fails without ammo");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(magicData.isCasting(),
                    "Failed initial Archer Multiple cast should not start casting");
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);

            var recastUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(recastUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should allow recast without ammo");

            var ammoStack = new ItemStack(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 1);
            player.getInventory().add(ammoStack);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);
            magicData.setPlayerCastingItem(stack);
            MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SWORD
            ));
            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get()
            ) == 1, "Recast Spellcaster Gun cast should not consume ammo from the cast event");
        });
    }

    static void spellgunHandUseContractDoesNotFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var imbuedStack = createInitializedPresetStack(item);
            applyRestrictedImbueNormalization(
                    helper,
                    imbuedStack,
                    item,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1
            );
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_hand_contract_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, imbuedStack);

            var mainHandUse = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertFalse(mainHandUse.getResult().consumesAction(),
                    "Mainhand Spellgun right-click should always pass without casting");
            helper.assertFalse(RightClickSpellItemHelper.hasMainHandRightClickBehavior(player, imbuedStack),
                    "Mainhand Spellgun should expose no right-click behavior to offhand magic items");

            var emptyStack = new ItemStack(item);
            ISpellContainer.set(emptyStack, ISpellContainer.create(1, false, false));
            player.setItemInHand(InteractionHand.OFF_HAND, emptyStack);
            var offhandUse = item.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(offhandUse.getResult().consumesAction(),
                    "Selected offhand Spellgun should consume right-click even when it is not imbued");
        });
    }

    static void spellgunsUseOneHandRangedEpicFightCapabilityWithoutInnate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellgun_epicfight_capability_test");
            for (var spellgun : List.of(
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()),
                    new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())
            )) {
                helper.assertTrue(hasExpectedEpicFightSpellgunCapability(player, spellgun),
                        spellgun.getDescriptionId()
                                + " should use the one-hand ranged Epic Fight capability without an innate skill");
            }

            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get())
            );
            helper.assertTrue(enterEpicFightBattleMode(player),
                    "Spellgun basic attack integration test should enter Epic Fight battle mode");
            helper.assertTrue(isEpicFightMainhandSpellgunBasicAttack(player),
                    "Epic Fight basic attack should recognize a mainhand Spellgun");
            helper.assertTrue(hasNoEpicFightSpellgunAttackMotion(player, player.getMainHandItem()),
                    "Epic Fight basic attack should leave Spellgun casting without an Epic Fight motion");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            helper.assertFalse(isEpicFightMainhandSpellgunBasicAttack(player),
                    "Epic Fight Spellgun basic attack handling should not affect non-Spellgun weapons");
        });
    }

    static void spellgunEpicFightOffhandPolicyUsesOnlyValidOneHandMainhands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            var spellgun = new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
            var swordPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellgun_epicfight_sword_offhand_test");
            helper.assertTrue(enterEpicFightBattleMode(swordPlayer),
                    "Spellgun sword offhand test should enter Epic Fight battle mode");
            swordPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            helper.assertTrue(canExecuteEpicFightGuard(swordPlayer),
                    "A one-hand Epic Fight sword should allow Guard while the offhand is empty");
            swordPlayer.setItemInHand(InteractionHand.OFF_HAND, spellgun.copy());
            helper.assertTrue(canUseEpicFightOffhandSpellgun(swordPlayer),
                    "A one-hand Epic Fight sword should allow the offhand Spellgun");
            helper.assertFalse(canExecuteEpicFightGuard(swordPlayer),
                    "Guard should be intentionally disabled while a valid offhand Spellgun provides ranged attacks");

            var axePlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0),
                    "spellgun_epicfight_axe_offhand_test");
            helper.assertTrue(enterEpicFightBattleMode(axePlayer),
                    "Spellgun axe offhand test should enter Epic Fight battle mode");
            axePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
            helper.assertTrue(canExecuteEpicFightGuard(axePlayer),
                    "A one-hand Epic Fight axe should allow Guard while the offhand is empty");
            axePlayer.setItemInHand(InteractionHand.OFF_HAND, spellgun.copy());
            helper.assertTrue(canUseEpicFightOffhandSpellgun(axePlayer),
                    "A one-hand Epic Fight axe should allow the offhand Spellgun");
            helper.assertFalse(canExecuteEpicFightGuard(axePlayer),
                    "Guard should be intentionally disabled for an axe while a valid offhand Spellgun provides ranged attacks");

            var spear = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath(EpicFightCompat.MOD_ID, "iron_spear")
            );
            helper.assertTrue(spear != null, "Missing epicfight:iron_spear for the Spellgun offhand policy test");
            var spearPlayer = createEquipmentTestPlayer(helper, new BlockPos(4, 2, 0),
                    "spellgun_epicfight_spear_offhand_test");
            spearPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(spear));
            spearPlayer.setItemInHand(InteractionHand.OFF_HAND, spellgun.copy());
            helper.assertTrue(canUseEpicFightOffhandSpellgun(spearPlayer),
                    "Epic Fight vanilla mode should keep vanilla offhand Spellgun use behavior");
            helper.assertTrue(enterEpicFightBattleMode(spearPlayer),
                    "Spellgun spear offhand test should enter Epic Fight battle mode");
            helper.assertFalse(canUseEpicFightOffhandSpellgun(spearPlayer),
                    "Epic Fight spear should not gain its shield-only one-hand behavior from an offhand Spellgun");

            var rejectedUse = spellgun.getItem().use(
                    helper.getLevel(),
                    spearPlayer,
                    InteractionHand.OFF_HAND
            );
            helper.assertFalse(rejectedUse.getResult().consumesAction(),
                    "An invalid Epic Fight offhand Spellgun use should pass without casting");
        });
    }

    private static boolean hasExpectedEpicFightSpellgunCapability(Object player, ItemStack stack) {
        return invokeEpicFightSpellgunBoolean(
                "hasExpectedSpellgunCapability",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class, ItemStack.class},
                player,
                stack
        );
    }

    private static boolean enterEpicFightBattleMode(Object player) {
        return invokeEpicFightSpellgunBoolean(
                "enterBattleMode",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class},
                player
        );
    }

    private static boolean isEpicFightMainhandSpellgunBasicAttack(Object player) {
        return invokeEpicFightSpellgunBoolean(
                "isMainhandSpellgunBasicAttack",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class},
                player
        );
    }

    private static boolean hasNoEpicFightSpellgunAttackMotion(Object player, ItemStack stack) {
        return invokeEpicFightSpellgunBoolean(
                "hasNoSpellgunAttackMotion",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class, ItemStack.class},
                player,
                stack
        );
    }

    private static boolean canUseEpicFightOffhandSpellgun(Object player) {
        return invokeEpicFightSpellgunBoolean(
                "canUseOffhandSpellgun",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class},
                player
        );
    }

    private static boolean canExecuteEpicFightGuard(Object player) {
        return invokeEpicFightSpellgunBoolean(
                "canExecuteGuard",
                new Class<?>[]{net.minecraft.server.level.ServerPlayer.class},
                player
        );
    }

    private static boolean invokeEpicFightSpellgunBoolean(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) {
        try {
            var compatClass = Class.forName(
                    "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellgunCompat"
            );
            return (boolean) compatClass.getMethod(methodName, parameterTypes).invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to inspect Spellgun Epic Fight compatibility", exception);
        }
    }

    static void invalidSpellgunSpellUsesDedicatedError(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = new ItemStack(item);
            var invalidSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var container = ISpellContainer.create(1, false, false).mutableCopy();
            container.addSpellAtIndex(invalidSpell, 1, 0, false);
            ISpellContainer.set(stack, container.toImmutable());

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellgun_invalid_spell_error_test");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            helper.assertFalse(item.tryTriggerImbuedSpell(player, InteractionHand.OFF_HAND, null),
                    "Spellgun should reject a persisted spell that no longer meets its restrictions");

            var spellData = ISpellContainer.get(stack).getSpellAtIndex(0);
            var message = createInvalidSpellError(helper, player, stack, spellData);
            assertTranslatableKey(helper, message, "ui.apprenticecodex.spellgun.invalid_spell",
                    "Invalid Spellgun spell should use the dedicated error key");
            var contents = (TranslatableContents) message.getContents();
            var args = contents.getArgs();
            helper.assertTrue(args.length == 2,
                    "Invalid Spellgun error should contain spell and item arguments");
            if (args.length == 2 && args[0] instanceof Component spellName && args[1] instanceof Component itemName) {
                helper.assertTrue(invalidSpell.getDisplayName(player).getString().equals(spellName.getString()),
                        "Invalid Spellgun error should include the invalid spell name");
                helper.assertTrue(stack.getHoverName().getString().equals(itemName.getString()),
                        "Invalid Spellgun error should include the Spellgun item name");
            }
        });
    }

    static void spellgunCastAttemptPreservesExistingCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            applyRestrictedImbueNormalization(
                    helper,
                    stack,
                    item,
                    SpellRegistry.ARCHER_MULTIPLE.get(),
                    1
            );
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_existing_cast_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
            var activeSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            magicData.initiateCast(
                    activeSpell,
                    1,
                    activeSpell.getEffectiveCastTime(1, player),
                    CastSource.SPELLBOOK,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(item.tryTriggerImbuedSpell(player, InteractionHand.MAIN_HAND, null),
                    "Spellgun should reject a cast while another spell is already casting");
            helper.assertTrue(magicData.isCasting(),
                    "Rejected Spellgun cast should preserve the existing casting state");
            helper.assertTrue(activeSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Rejected Spellgun cast should preserve the existing spell id");
        });
    }

    private static void assertSpellgunAbilityTooltipKeys(
            GameTestHelper helper,
            AbstractSpellGunItem item,
            boolean expectInstantLongCast,
            String itemName
    ) {
        var lines = collectSpellgunAbilityTooltipLines(helper, item);
        var hasInstantLongCast = containsTranslatableKey(
                lines,
                "item.apprenticecodex.spellgun.tooltip.ability_long_to_instant"
        );
        var hasReduceCast = containsTranslatableKey(
                lines,
                "item.apprenticecodex.spellgun.tooltip." + "ability_reduce_" + "cast"
        );
        helper.assertTrue(hasInstantLongCast == expectInstantLongCast,
                itemName + " instant LONG cast tooltip mismatch");
        helper.assertFalse(hasReduceCast,
                itemName + " should not show removed reduce-cast tooltip key");
    }

    @SuppressWarnings("unchecked")
    private static List<Component> collectSpellgunAbilityTooltipLines(GameTestHelper helper, AbstractSpellGunItem item) {
        try {
            var method = AbstractSpellGunItem.class.getDeclaredMethod("collectSpellGunAbilityTooltipSection");
            method.setAccessible(true);
            return (List<Component>) method.invoke(item);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Spellgun ability tooltip reflection failed: " + exception);
            return List.of();
        }
    }

    private static void assertTooltipKeyArgument(
            GameTestHelper helper,
            ItemStack stack,
            int tooltipIndex,
            String expectedArgumentKey,
            String message
    ) {
        var lines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL);
        helper.assertTrue(lines.size() > tooltipIndex,
                message + " (tooltip line count=" + lines.size() + ")");
        if (lines.size() <= tooltipIndex
                || !(lines.get(tooltipIndex).getContents() instanceof TranslatableContents contents)) {
            return;
        }

        var args = contents.getArgs();
        helper.assertTrue(args.length == 1 && args[0] instanceof Component,
                message + " (unexpected argument count=" + args.length + ")");
        if (args.length == 1 && args[0] instanceof Component keyName) {
            assertTranslatableKey(helper, keyName, expectedArgumentKey, message);
        }
    }

    private static Component createInvalidSpellError(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            SpellData spellData
    ) {
        try {
            var method = AbstractSpellGunItem.class.getDeclaredMethod(
                    "createInvalidSpellError",
                    Player.class,
                    ItemStack.class,
                    SpellData.class
            );
            method.setAccessible(true);
            return (Component) method.invoke(null, player, stack, spellData);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Spellgun invalid spell error reflection failed: " + exception);
            return Component.empty();
        }
    }

    private static boolean containsTranslatableKey(List<Component> lines, String key) {
        return lines.stream().anyMatch(component ->
                component.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey())
        );
    }

    private static void assertTooltipStringArgument(
            GameTestHelper helper,
            List<Component> lines,
            String key,
            String expectedArgument,
            String message
    ) {
        var matchingLine = lines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()))
                .findFirst()
                .orElse(null);
        helper.assertTrue(matchingLine != null, message + " (tooltip key is missing)");
        if (matchingLine == null || !(matchingLine.getContents() instanceof TranslatableContents contents)) {
            return;
        }

        var args = contents.getArgs();
        helper.assertTrue(args.length == 1 && expectedArgument.equals(args[0]),
                message + ": expected=" + expectedArgument + ", actual=" + java.util.Arrays.toString(args));
    }

    @SuppressWarnings("unchecked")
    private static List<Component> collectReflectcastAbilityTooltipLines(
            GameTestHelper helper,
            jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield item,
            ItemStack stack
    ) {
        try {
            var method = jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield.class
                    .getDeclaredMethod("getImbueShieldAbilityTooltipSection", ItemStack.class);
            method.setAccessible(true);
            return (List<Component>) method.invoke(item, stack);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Reflectcast Shield ability tooltip reflection failed: " + exception);
            return List.of();
        }
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable after save/load");
        });
    }
    static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Spellcaster Gun should stay cleared after save/load");
        });
    }
    static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun legacy locked replacement should be recovered after save/load");
        });
    }

    private static void assertSpellgunAttributeSlot(
            GameTestHelper helper,
            ItemStack stack,
            EquipmentSlot slot,
            SpellgunAttributeCase attributeCase,
            String message
    ) {
        var modifiers = stack.getAttributeModifiers(slot);
        var actualAmount = sumModifierAmount(
                modifiers.get(attributeCase.attribute()),
                attributeCase.operation()
        );
        helper.assertTrue(Math.abs(actualAmount - attributeCase.amount()) < 1.0e-9D,
                message + ": expected=" + attributeCase.amount()
                        + ", actual=" + actualAmount
                        + ", modifiers=" + describeModifiers(modifiers));
    }

    private static void assertAdjustedUpgradeOrbSlots(
            GameTestHelper helper,
            ItemStack stack,
            Attribute attribute,
            String context
    ) {
        var mainhandModifiers = resolveRuntimeSpellgunModifiers(stack, EquipmentSlot.MAINHAND);
        helper.assertTrue(mainhandModifiers.get(attribute).isEmpty(),
                context + " adjusted Spellgun should not retain Upgrade Orb in mainhand: "
                        + describeModifiers(mainhandModifiers));

        var offhandModifiers = resolveRuntimeSpellgunModifiers(stack, EquipmentSlot.OFFHAND);
        var matchingModifiers = offhandModifiers.get(attribute).stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                context + " adjusted Spellgun should merge Reservoir and Upgrade Orb in offhand: "
                        + describeModifiers(offhandModifiers));
        helper.assertTrue(Math.abs(matchingModifiers.get(0).getAmount() - 70.0D) < 1.0e-9D,
                context + " adjusted Spellgun offhand modifier should be 70 but got "
                        + matchingModifiers.get(0).getAmount());
        helper.assertTrue(matchingModifiers.get(0).getId().equals(
                        io.redspace.ironsspellbooks.util.UpgradeUtils.UUIDForSlot(EquipmentSlot.OFFHAND)),
                context + " adjusted Spellgun Upgrade Orb should use the offhand UUID");
    }

    private static com.google.common.collect.Multimap<Attribute, AttributeModifier> resolveRuntimeSpellgunModifiers(
            ItemStack stack,
            EquipmentSlot slot
    ) {
        var event = new net.minecraftforge.event.ItemAttributeModifierEvent(
                stack,
                slot,
                stack.getItem().getAttributeModifiers(slot, stack)
        );
        MinecraftForge.EVENT_BUS.post(event);
        return event.getModifiers();
    }

    private record SpellgunAttributeCase(
            Enchantment enchantment,
            Attribute attribute,
            AttributeModifier.Operation operation,
            double amount
    ) {
        private SpellgunAttributeCase withAmount(double replacementAmount) {
            return new SpellgunAttributeCase(enchantment, attribute, operation, replacementAmount);
        }
    }

}
