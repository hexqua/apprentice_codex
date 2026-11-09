package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.armor.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios.assertSoulcollectorMagicProficiency;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios.assertSoulcollectorSpiritInfusions;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MAGIC_PROFICIENCY;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.LODESTONE_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.postSpellOnCast;

final class MagicArmorGameTestScenarios {
    private MagicArmorGameTestScenarios() {}

    static void soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID) || !ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var magicProficiency = ForgeRegistries.ATTRIBUTES.getValue(LODESTONE_MAGIC_PROFICIENCY);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");
            helper.assertTrue(Math.abs(ApprenticeCodexServerConfig.soulcollectorRobeMagicProficiencyBonusPerPiece() - 0.15D)
                            < 1.0e-9D,
                    "Soulcollector Robe default magic proficiency bonus should be 0.15");

            assertSoulcollectorMagicProficiency(helper, ItemRegistry.SOULCOLLECTOR_HAT.get(), ArmorItem.Type.HELMET,
                    magicProficiency);
            assertSoulcollectorMagicProficiency(helper, ItemRegistry.SOULCOLLECTOR_ROBE.get(), ArmorItem.Type.CHESTPLATE,
                    magicProficiency);
            assertSoulcollectorMagicProficiency(helper, ItemRegistry.SOULCOLLECTOR_LEGGINGS.get(), ArmorItem.Type.LEGGINGS,
                    magicProficiency);
            assertSoulcollectorMagicProficiency(helper, ItemRegistry.SOULCOLLECTOR_BOOTS.get(), ArmorItem.Type.BOOTS,
                    magicProficiency);

            assertSoulcollectorSpiritInfusions(helper);
        });
    }

    static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                    ArmorItem.Type.CHESTPLATE, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                    ArmorItem.Type.LEGGINGS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == (armorType == ArmorItem.Type.CHESTPLATE),
                        "Apprentice Mage Robe " + armorType + " imbue surface regression");
            }
        });
    }

    static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var lightningSpellPowerAttribute = AttributeRegistry.LIGHTNING_SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - EnchantressRobeStats.MAX_MANA_BONUS_PER_PIECE) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Enchantress Robe " + armorType + " imbue surface regression: hasImbueSlot="
                                + item.hasImbueSlot() + " stack=" + stack);

                var lightningSpellPowerBonus = sumModifierAmount(
                        modifiers.get(lightningSpellPowerAttribute),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(lightningSpellPowerBonus) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " should not gain school spell power before imbue: "
                                + describeModifiers(modifiers));
            }
        });
    }

    static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);

            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Enchantress Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute =
                    MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Enchantress Robe chestplate test could not resolve school spell power attribute");

            var modifiers = item.getAttributeModifiers(EquipmentSlot.CHEST, stack);
            var globalSpellPowerBonus = sumModifierAmount(
                    modifiers.get(AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            var expectedGlobalSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            helper.assertTrue(Math.abs(globalSpellPowerBonus - expectedGlobalSpellPower) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep configured spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }

    static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == expectedChromaticMagiaDressDefense(armorType),
                        "Chromatic Magia Dress " + armorType + " defense changed");
                helper.assertTrue(Math.abs(item.getMaterial().getToughness() - 1.0F) < 1.0e-6F,
                        "Chromatic Magia Dress " + armorType + " toughness should be 1");
                helper.assertTrue(item.getEnchantmentValue(stack) == ChromaticMagiaDressStats.enchantmentValue(),
                        "Chromatic Magia Dress " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Chromatic Magia Dress " + armorType + " should repair with mithril scrap");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 125.0D) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Chromatic Magia Dress " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && (item.getDescriptionId() + ".desc").equals(contents.getKey())),
                        "Chromatic Magia Dress " + armorType + " should show its lang desc key");
            }
        });
    }

    static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var fireSpellPowerAttribute = AttributeRegistry.FIRE_SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSpellPowerBonus();
            var expectedSchoolSpellPower = ApprenticeCodexServerConfig.magiAgentSuitSchoolSpellPowerBonus();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(),
                    ArmorItem.Type.CHESTPLATE, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (MagiAgentSuitItem) ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == expectedMagiAgentSuitDefense(armorType),
                        "Magi Agent Suit " + armorType + " defense changed");
                helper.assertTrue(item.getEnchantmentValue(stack) == MagiAgentSuitStats.enchantmentValue(),
                        "Magi Agent Suit " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                        ),
                        "Magi Agent Suit " + armorType + " should repair with magic cloth");
                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Magi Agent Suit " + armorType + " imbue surface regression");

                var hintLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), hintLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(hintLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.rune_hint".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its rune hint before calibration");

                SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        stack,
                        0,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
                );
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(stack, 0)
                                .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Magi Agent Suit " + armorType + " should store the calibration rune");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - MagiAgentSuitStats.MAX_MANA_BONUS) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                var schoolSpellPowerBonus = sumModifierAmount(modifiers.get(fireSpellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(schoolSpellPowerBonus - expectedSchoolSpellPower) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " school rune spell power regression: " + describeModifiers(modifiers));

                var toughnessBonus = sumModifierAmount(modifiers.get(Attributes.ARMOR_TOUGHNESS), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(toughnessBonus - expectedMagiAgentSuitToughness(armorType)) < 1.0e-9D,
                        "Magi Agent Suit " + armorType + " toughness regression: " + describeModifiers(modifiers));

                var tunedLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tunedLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tunedLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.magi_agent_suit.school_rune".equals(contents.getKey())),
                        "Magi Agent Suit " + armorType + " should show its tuned school tooltip");
            }
        });
    }

    static int expectedChromaticMagiaDressDefense(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET, BOOTS -> 2;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
        };
    }

    static int expectedMagiAgentSuitDefense(ArmorItem.Type armorType) {
        return switch (armorType) {
            case HELMET, BOOTS -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
        };
    }

    static double expectedMagiAgentSuitToughness(ArmorItem.Type armorType) {
        return armorType == ArmorItem.Type.LEGGINGS ? 2.0D : 1.0D;
    }

    static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.stealthRuneArmorSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                    ArmorItem.Type.CHESTPLATE, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                    ArmorItem.Type.LEGGINGS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                    ArmorItem.Type.BOOTS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Stealth Rune Armor " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && ("item." + ApprenticeCodex.MODID + ".stealth_rune_armor.desc").equals(contents.getKey())),
                        "Stealth Rune Armor " + armorType + " should show its lang desc key");
            }
        });
    }

    static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "chromatic_magia_dress_history_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Chromatic Magia Dress test could not resolve player mana data");

            var helmet = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
            var chestplate = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var leggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            var boots = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            player.setItemSlot(EquipmentSlot.LEGS, leggings);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            var schoolSpellPowerBonusPerHistory =
                    ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();

            var longSpell = SpellRegistry.COMPOUND_PHIAL.get();
            for (int i = 0; i < 21; ++i) {
                postSpellOnCast(player, longSpell, 1);
            }
            assertSchoolSpellPowerBonus(helper, helmet, EquipmentSlot.HEAD, longSpell,
                    20.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress helmet should keep only the latest 20 LONG histories");
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, longSpell, 0.0D,
                    "Chromatic Magia Dress chestplate should ignore non-recast LONG spells");

            var continuousSpell = SpellRegistry.FORCE_FIELD.get();
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record CONTINUOUS spells");

            var heldContinuousLeggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            player.setItemSlot(EquipmentSlot.LEGS, heldContinuousLeggings);
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record only once during one CONTINUOUS hold");

            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            magicData.initiateCast(continuousSpell, 1, 60, CastSource.SPELLBOOK, "gametest");
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, heldContinuousLeggings, EquipmentSlot.LEGS, continuousSpell,
                    2.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record a new CONTINUOUS hold after the previous cast ends");
            magicData.resetCastingState();
            ChromaticMagiaDressCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var instantSpell = SpellRegistry.MANA_SLASH.get();
            postSpellOnCast(player, instantSpell, 1);
            assertSchoolSpellPowerBonus(helper, boots, EquipmentSlot.FEET, instantSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress boots should record INSTANT spells");

            var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should record initial recast-capable casts");

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    recastSpell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should ignore casts while the same spell is in Recast");
        });
    }
}
