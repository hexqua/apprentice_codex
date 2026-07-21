package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;

import java.util.List;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitCooldownEvent;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class EquipmentSpellBehaviorBridgeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentSpellBehaviorBridgeGameTestScenarios() {
    }

    static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCastingMoveSpeedAdjustment(helper, 0.0D, 0.8D, "No external bonus should keep full cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.25D, 0.55D, "Diamond-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.50D, 0.30D, "Netherite-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.75D, 0.05D, "Small remaining headroom should stay positive");
            assertCastingMoveSpeedAdjustment(helper, 0.80D, 0.0D, "Exact cap should stop adding more casting move speed");
            assertCastingMoveSpeedAdjustment(helper, 1.10D, 0.0D, "External overshoot should not become a negative correction");
        });
    }
    static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var movementSpeedModifier = effect.getAttributeModifiers().get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeedModifier != null, "LongStride is missing the movement speed attribute modifier");

            var actualAmount = effect.getAttributeModifierValue(0, movementSpeedModifier);
            helper.assertTrue(Math.abs(actualAmount - 0.15D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.15 but got " + actualAmount);
        });
    }
    static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dynamic_casting_movespeed_rebalance_test");
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var castingMoveSpeed = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get());
            helper.assertTrue(castingMoveSpeed != null,
                    "Dynamic casting mobility test could not resolve the CASTING_MOVESPEED attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 200, 0));
            helper.assertTrue(castingMoveSpeed != null, "Dynamic casting mobility test lost CASTING_MOVESPEED after addEffect");
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should initially fill the full cancellation headroom"
            );

            castingMoveSpeed.addTransientModifier(new AttributeModifier(
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    "apprenticecodex.casting_movespeed.dynamic_test",
                    0.5D,
                    AttributeModifier.Operation.ADDITION
            ));
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    0.3D,
                    "Dynamic casting mobility effect should shrink after an external casting move speed bonus is added"
            );

            castingMoveSpeed.removeModifier(CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID);
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should recover once the external casting move speed bonus is removed"
            );
        });
    }
    static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = ItemRegistry.COMFORT_BERRIES.get().getFoodProperties();
            helper.assertTrue(foodProperties != null, "Comfort Berries should remain edible");
            helper.assertTrue(foodProperties != null && foodProperties.getNutrition() == 1,
                    "Comfort Berries nutrition regression: " + (foodProperties == null ? "null" : foodProperties.getNutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.getSaturationModifier() - 1.2f) < 1.0e-6F,
                    "Comfort Berries saturation modifier regression: "
                            + (foodProperties == null ? "null" : foodProperties.getSaturationModifier()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Berries should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float>>of()
                    : foodProperties.getEffects().stream()
                    .filter(effectPair -> effectPair.getFirst().getEffect() == EffectRegistry.MANA_REGENERATION.get())
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Berries should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.getFirst().getDuration() == 20 * 10,
                    "Comfort Berries mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.getFirst().getAmplifier() == 2,
                    "Comfort Berries mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.getSecond() - 1.0f) < 1.0e-6F,
                    "Comfort Berries mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.getSecond()));
        });
    }
    static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = ItemRegistry.COMFORT_SANDWICH.get().getFoodProperties();
            helper.assertTrue(foodProperties != null, "Comfort Sandwich should be edible");
            helper.assertTrue(foodProperties != null && foodProperties.getNutrition() == 7,
                    "Comfort Sandwich nutrition regression: " + (foodProperties == null ? "null" : foodProperties.getNutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.getSaturationModifier() - 1.6f) < 1.0e-6F,
                    "Comfort Sandwich saturation modifier regression: "
                            + (foodProperties == null ? "null" : foodProperties.getSaturationModifier()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Sandwich should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float>>of()
                    : foodProperties.getEffects().stream()
                    .filter(effectPair -> effectPair.getFirst().getEffect() == EffectRegistry.MANA_REGENERATION.get())
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Sandwich should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.getFirst().getDuration() == 20 * 60,
                    "Comfort Sandwich mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.getFirst().getAmplifier() == 0,
                    "Comfort Sandwich mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.getSecond() - 1.0f) < 1.0e-6F,
                    "Comfort Sandwich mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.getSecond()));
        });
    }
    static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (jp.aquafactory.apprenticecodex.effect.ManaRegeneration) EffectRegistry.MANA_REGENERATION.get();
            var manaRegenModifier = effect.getAttributeModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get());
            helper.assertTrue(manaRegenModifier != null, "Mana Regeneration is missing the mana regen attribute modifier");
            helper.assertTrue(manaRegenModifier != null
                            && manaRegenModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL,
                    "Mana Regeneration should use MULTIPLY_TOTAL but got "
                            + (manaRegenModifier == null ? "missing" : manaRegenModifier.getOperation()));

            var levelOneAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(0, manaRegenModifier);
            helper.assertTrue(Math.abs(levelOneAmount - 0.25D) < 1.0e-9D,
                    "Mana Regeneration Lv1 regression: expected 0.25 but got " + levelOneAmount);

            var levelTwoAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(1, manaRegenModifier);
            helper.assertTrue(Math.abs(levelTwoAmount - 0.50D) < 1.0e-9D,
                    "Mana Regeneration Lv2 regression: expected 0.50 but got " + levelTwoAmount);
        });
    }
    static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertPotionEffect(helper, PotionRegistry.MEDITATION.get(), "apprenticecodex:meditation", 20 * 60 * 3, 0);
            assertPotionEffect(helper, PotionRegistry.LONG_MEDITATION.get(), "apprenticecodex:long_meditation", 20 * 60 * 8, 0);
            assertPotionEffect(helper, PotionRegistry.STRONG_MEDITATION.get(), "apprenticecodex:strong_meditation", 20 * 90, 1);
        });
    }
    static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_external_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
            var spectralHammerSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get();

            var touchDigManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.TOUCH_DIG_SPELL_ID,
                    1,
                    15,
                    touchDigSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(touchDigManaEvent);
            helper.assertTrue(touchDigManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Touch Dig mana to 8 but got " + touchDigManaEvent.getManaCost());
            var expectedTouchDigBaseCooldown = Math.max(1, (int) (touchDigSpell.getSpellCooldown() * 0.5D));
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(touchDigSpell.getSpellCooldown(), player) == expectedTouchDigBaseCooldown,
                    "CraftsmansDelight should apply the default 0.5 cooldown multiplier before player modifiers");

            var touchDigCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    10,
                    touchDigSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(touchDigCooldownEvent);
            helper.assertTrue(touchDigCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(touchDigSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Touch Dig cooldown through the reduced cooldown helper");

            var spectralHammerManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.SPECTRAL_HAMMER_SPELL_ID,
                    1,
                    15,
                    spectralHammerSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(spectralHammerManaEvent);
            helper.assertTrue(spectralHammerManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Spectral Hammer mana to 8 but got " + spectralHammerManaEvent.getManaCost());
            var expectedSpectralHammerBaseCooldown = Math.max(1, (int) (spectralHammerSpell.getSpellCooldown() * 0.5D));
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(spectralHammerSpell.getSpellCooldown(), player) == expectedSpectralHammerBaseCooldown,
                    "CraftsmansDelight should apply the default 0.5 cooldown multiplier before player modifiers");

            var spectralHammerCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    40,
                    spectralHammerSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(spectralHammerCooldownEvent);
            helper.assertTrue(spectralHammerCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(spectralHammerSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Spectral Hammer cooldown through the reduced cooldown helper");
        });
    }
    static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_apprentice_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.HARVEST_MOON.get(), 60, "Harvest Moon");
            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.EARTH_FORGE.get(), 20, "Earth Forge");
        });
    }

    static void craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "craftsmans_scrollcaster_sword_cooldown_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var spell = SpellRegistry.HARVEST_MOON.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown test could not resolve player magic data");
            magicData.setPlayerCastingItem(gauntlet.copy());

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(expectedCooldown < io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                            spell,
                            player,
                            CastSource.SWORD
                    ),
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown should be reduced from the normal sword cooldown");

            var craftsmansFirstEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            helper.assertTrue(craftsmansFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "CraftsmansDelight -> Scrollcaster Gauntlet cooldown order should keep the reduced sword cooldown but got "
                            + craftsmansFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            var scrollcasterFirstEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            helper.assertTrue(scrollcasterFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "Scrollcaster Gauntlet -> CraftsmansDelight cooldown order should keep the reduced sword cooldown but got "
                            + scrollcasterFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_cooldown_test");
            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == baseCooldown,
                    "Magi Agent Suit Boots should not reduce cooldown while unequipped");

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var bootsEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(bootsEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(expectedCooldown < baseCooldown,
                    "Magi Agent Suit Boots expected cooldown should be shorter than base cooldown");
            helper.assertTrue(bootsEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should reduce target spell cooldown but got "
                            + bootsEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void equipmentCooldownReductionsDoNotStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_craftsmans_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var spell = SpellRegistry.THERMAL_PROCESS.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(cooldownEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var singleReductionCooldown = Math.max(1, (int) (spell.getSpellCooldown() * 0.5D));
            helper.assertTrue(expectedCooldown == singleReductionCooldown,
                    "Equal equipment cooldown multipliers should apply once instead of stacking");
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots and CraftsmansDelight should keep the strongest cooldown but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "equipment_spell_timing_multiplier_config_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var craftsmansSpell = SpellRegistry.HARVEST_MOON.get();
            var magiAgentSpell = SpellRegistry.COMMENCE_FIRE.get();

            var effectiveCastTime = 101;

            try (var ignored = ApprenticeCodexServerConfig.useEquipmentSpellTimingMultipliersOverrideForGameTest(
                    0.25D,
                    0.75D,
                    0.25D
            )) {
                helper.assertTrue(
                        CraftsmansDelight.applyCooldownDiscount(craftsmansSpell.getSpellCooldown(), player)
                                == Math.max(1, (int) (craftsmansSpell.getSpellCooldown() * 0.25D)),
                        "CraftsmansDelight should follow its server cooldown multiplier"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCooldownDiscount(
                                magiAgentSpell.getSpellCooldown(),
                                magiAgentSpell,
                                player
                        ) == Math.max(1, (int) (magiAgentSpell.getSpellCooldown() * 0.75D)),
                        "Magi Agent Suit Boots should follow their server cooldown multiplier"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCastTimeReduction(
                                magiAgentSpell,
                                effectiveCastTime,
                                player
                        ) == Math.max(1, (int) Math.round(effectiveCastTime * 0.25D)),
                        "Magi Agent Suit Boots should follow their server cast time multiplier"
                );
            }

            try (var ignored = ApprenticeCodexServerConfig.useEquipmentSpellTimingMultipliersOverrideForGameTest(
                    0.0D,
                    0.0D,
                    0.0D
            )) {
                helper.assertTrue(
                        CraftsmansDelight.applyCooldownDiscount(craftsmansSpell.getSpellCooldown(), player) == 1,
                        "CraftsmansDelight should keep a 1 tick minimum at multiplier 0.0"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCooldownDiscount(
                                magiAgentSpell.getSpellCooldown(),
                                magiAgentSpell,
                                player
                        ) == 1,
                        "Magi Agent Suit Boots should keep a 1 tick minimum at multiplier 0.0"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCastTimeReduction(
                                magiAgentSpell,
                                effectiveCastTime,
                                player
                        ) == 1,
                        "Magi Agent Suit Boots should keep a 1 tick cast time minimum at multiplier 0.0"
                );
            }
        });
    }

    static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_additive_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));

            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var bootsCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var extraCooldown = 37;
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    baseCooldown + extraCooldown,
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            MagiAgentSuitCooldownEvent.onSpellCooldownAdded(cooldownEvent);

            var expectedCooldown = bootsCooldown + extraCooldown;
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should keep additive cooldown components but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90) == 90,
                    "No limited cooldown candidate should keep the base cooldown");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 45, 30) == 30,
                    "Limited cooldown candidates should choose only the strongest reduction");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 0, -1, 60) == 60,
                    "Invalid limited cooldown candidates should be ignored");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(0, 1) == 0,
                    "Zero base cooldown should stay zero");
        });
    }

    static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = new TouchDigSpell();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_range_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(0, 23, 0));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(-90.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped");
            helper.assertTrue(spell.getUniqueInfo(1, player).stream().anyMatch(component -> component.getString().contains("16")),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }
    static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_ring_enchant_merge_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(Enchantments.BLOCK_FORTUNE, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            equipRingCurio(player, ringStack);

            var mergedFortuneTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedFortuneTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 3,
                    "Touch Dig should prefer the higher Fortune level from the ring");

            ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var mergedSilkTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should inherit Silk Touch from the ring");
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 0,
                    "Touch Dig should drop Fortune when Silk Touch is present");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = getFreshItemDrops(helper.getLevel(), blockPos, 1.5D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_bare_hand_ring_enchant_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var synthesizedTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertFalse(synthesizedTool.isEmpty(),
                    "Touch Dig should synthesize a mining tool when the caster is bare-handed but the ring has mining enchantments");
            helper.assertTrue(synthesizedTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should copy Silk Touch onto the synthesized bare-hand tool");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = getFreshItemDrops(helper.getLevel(), blockPos, 1.5D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_ring_enchant_test");
            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var targetPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 12, 1)));
            hammer.setPos(hammerPos.x, hammerPos.y, hammerPos.z);
            helper.getLevel().addFreshEntity(hammer);

            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            var drops = getFreshItemDrops(helper.getLevel(), targetPos, 2.0D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_crystal_harvest_test");
            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist with CraftsmansDelight should harvest mature crystals growing from budding amethyst");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist with CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(hasItemEntityWithin(level, Blocks.AMETHYST_CLUSTER.asItem(), Vec3.atCenterOf(clusterPos), 1.5D),
                        "Heavenly Fist with ring Silk Touch should drop the crystal block itself");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistWithoutCraftsmansDelightLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_no_crystal_harvest_test");

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist without CraftsmansDelight should leave the crystal intact");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_crystal_source_guard_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var supportPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = supportPos.east();
            level.setBlock(supportPos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(supportPos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist with CraftsmansDelight should skip crystals not attached to harvest sources");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_immature_bud_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var budPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(budPos, Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(budPos).is(Blocks.LARGE_AMETHYST_BUD),
                        "Heavenly Fist with CraftsmansDelight should skip immature amethyst buds");
                helper.succeed();
            });
        });
    }

    private static net.minecraft.world.level.block.state.BlockState matureAmethystCluster(Direction facing) {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    private static void spawnHeavenlyFist(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity owner,
                                          Vec3 center, float radius) {
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, radius, 0);
        level.addFreshEntity(fist);
    }

    static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var existingItemPos = Vec3.atCenterOf(logPos);
            var existingItem = new ItemEntity(
                    level,
                    existingItemPos.x,
                    existingItemPos.y,
                    existingItemPos.z,
                    new ItemStack(Items.COBBLESTONE)
            );
            level.addFreshEntity(existingItem);

            var job = new TinyLumberjackJob(originPos, 1, player);
            job.tick(level);

            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should move new log drops to the initial chopped block while CraftsmansDelight is equipped");
            helper.assertTrue(!existingItem.isRemoved() && existingItem.position().distanceToSqr(existingItemPos) < 0.01D,
                    "Tiny Lumberjack drop moving should not move ItemEntities that existed before the block break");
            helper.succeed();
        });
    }
    static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_unequip_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var job = new TinyLumberjackJob(originPos, 1, player);
            equipRingCurio(player, ItemStack.EMPTY);
            job.tick(level);

            helper.assertFalse(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should stop moving job drops after CraftsmansDelight is unequipped");
            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(logPos), 1.25D),
                    "Tiny Lumberjack should leave log drops near the broken block when CraftsmansDelight is not currently equipped");
            helper.succeed();
        });
    }
    static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_penetrated_armor_test");
            var armor = player.getAttribute(Attributes.ARMOR);
            var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            helper.assertTrue(armor != null, "Player is missing armor attribute");
            helper.assertTrue(toughness != null, "Player is missing armor toughness attribute");

            armor.setBaseValue(10.0D);
            toughness.setBaseValue(8.0D);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR.get(), 100, 0));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 8.0D) < 1.0E-6D,
                    "Penetrated Armor I should reduce armor by 20%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor should reduce armor toughness by 100%");

            player.removeEffect(EffectRegistry.PENETRATED_ARMOR.get());
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR.get(), 100, 3));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 2.0D) < 1.0E-6D,
                    "Penetrated Armor IV should reduce armor by 80%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor toughness reduction should not depend on amplifier");

            var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                    helper.getLevel(),
                    player,
                    DamageTypes.WORLD_FLATTER
            );
            helper.assertTrue(source.is(DamageTypes.WORLD_FLATTER),
                    "World Flatter damage source should use apprenticecodex:world_flatter");
            helper.assertTrue(!source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "World Flatter should no longer use apprenticecodex:bypasses_iframe");
            helper.assertTrue(!source.is(DamageTypeTags.BYPASSES_COOLDOWN),
                    "World Flatter should no longer bypass vanilla cooldown i-frame");
        });
    }
    static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_block_filter_test");
            var pos = helper.absolutePos(new BlockPos(0, 2, 0));

            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should target pickaxe-mineable stone");
            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
                    "World Flatter should target shovel-mineable dirt");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.GLASS.defaultBlockState(), Blocks.GLASS.defaultBlockState()),
                    "World Flatter should reject glass because it has no specific pickaxe/shovel tool tag");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()),
                    "World Flatter should reject axe-mineable logs");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()),
                    "World Flatter should reject unbreakable blocks");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should not splash unrelated ore blocks from a non-ore center");
        });
    }
    static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_single_target_owner");
            owner.setYRot(0.0F);
            owner.setXRot(0.0F);

            var target = helper.spawn(EntityType.SHEEP, new BlockPos(0, 2, 4));
            var bystander = helper.spawn(EntityType.SHEEP, new BlockPos(1, 2, 4));
            target.setNoAi(true);
            bystander.setNoAi(true);
            var targetHealth = target.getHealth();
            var bystanderHealth = bystander.getHealth();

            var weapon = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, owner);
            weapon.setDamage(4.0F);
            weapon.setPenetratedArmorAmplifier(1);
            weapon.setToolSpeed(4.0F);
            weapon.updateOwnerTarget(level, new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    target.getBoundingBox().getCenter(),
                    target,
                    null
            ));

            for (var i = 0; i < 14; ++i) {
                target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
                weapon.tickOnServer(level);
            }
            helper.assertTrue(Math.abs(target.getHealth() - targetHealth) < 1.0E-6F,
                    "World Flatter should not damage an entity before the 15 tick attach completes");

            target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
            weapon.tickOnServer(level);
            helper.assertTrue(target.getHealth() < targetHealth,
                    "World Flatter should damage the attached moving target after 15 ticks");
            helper.assertTrue(target.hasEffect(EffectRegistry.PENETRATED_ARMOR.get()),
                    "World Flatter should apply Penetrated Armor after successful damage");
            helper.assertTrue(Math.abs(bystander.getHealth() - bystanderHealth) < 1.0E-6F,
                    "World Flatter should not damage nearby non-target entities");
        });
    }
    static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = MalumHauntedCompat.getHauntedEnchantment();
            helper.assertTrue(haunted != null, "malum:haunted is not registered");

            var pastelStaff = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            pastelStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(pastelStaff),
                    "Pastel Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(pastelStaff) > 0.0D,
                    "Pastel Staff should resolve a positive Haunted magic damage bonus");

            var multicastEchoStaff = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
            multicastEchoStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(multicastEchoStaff),
                    "Multicast Echo Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(multicastEchoStaff) > 0.0D,
                    "Multicast Echo Staff should resolve a positive Haunted magic damage bonus");

            var crystalBladedStaff = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            crystalBladedStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(crystalBladedStaff),
                    "Crystal Bladed Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(crystalBladedStaff) > 0.0D,
                    "Crystal Bladed Staff should resolve a positive Haunted magic damage bonus");

            var focusStaffbow = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            focusStaffbow.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(focusStaffbow),
                    "Focus Staffbow should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(focusStaffbow) > 0.0D,
                    "Focus Staffbow should resolve a positive Haunted magic damage bonus");

            var chargedTwinBladeStaff = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            chargedTwinBladeStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(chargedTwinBladeStaff),
                    "Charged Twin Blade Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(chargedTwinBladeStaff) > 0.0D,
                    "Charged Twin Blade Staff should resolve a positive Haunted magic damage bonus");

            var manaForceBlade = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            manaForceBlade.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(manaForceBlade),
                    "Mana Force Blade should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(manaForceBlade) > 0.0D,
                    "Mana Force Blade should resolve a positive Haunted magic damage bonus");

            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get())),
                    "Spellgun should stay outside Haunted support");
            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get())),
                    "Reflectcast Shield should stay outside Haunted support");
        });
    }
    static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var source = MalumHauntedCompat.createHauntedBonusDamageSource(attacker);
            helper.assertTrue(source.is(DamageTypes.HAUNTED_BONUS),
                    "Haunted bonus should use apprenticecodex:haunted_bonus");
            helper.assertTrue(source.is(DamageTypeTagGenerator.MAGIC_DAMAGE),
                    "Haunted bonus should stay on the magic damage tag path");
            helper.assertTrue(source.is(DamageTypeTagGenerator.FORGE_IS_MAGIC),
                    "Haunted bonus should stay on the forge:is_magic path for Lodestone magic_proficiency");
            helper.assertTrue(source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "Haunted bonus should bypass cooldown-based I-Frame checks");
        });
    }
    static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID)) {
                return;
            }

            var magicProficiency = ForgeRegistries.ATTRIBUTES.getValue(LODESTONE_MAGIC_PROFICIENCY);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");

            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var proficiencyInstance = attacker.getAttribute(magicProficiency);
            helper.assertTrue(proficiencyInstance != null, "Attacker is missing lodestone:magic_proficiency");

            var baselineTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(1, 2, 0));
            var amplifiedTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(2, 2, 0));
            var baseDamage = 4.0F;

            var baselineHealth = baselineTarget.getHealth();
            helper.assertTrue(baselineTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Baseline haunted bonus damage should apply");
            var baselineTaken = baselineHealth - baselineTarget.getHealth();
            helper.assertTrue(Math.abs(baselineTaken - baseDamage) < 1.0e-4F,
                    "Baseline haunted bonus damage should stay unscaled at proficiency 1.0, actual=" + baselineTaken);

            proficiencyInstance.setBaseValue(1.5D);
            var amplifiedHealth = amplifiedTarget.getHealth();
            helper.assertTrue(amplifiedTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Amplified haunted bonus damage should apply");
            var amplifiedTaken = amplifiedHealth - amplifiedTarget.getHealth();
            helper.assertTrue(Math.abs(amplifiedTaken - 6.0F) < 1.0e-4F,
                    "Amplified haunted bonus damage should scale to 6.0 at proficiency 1.5, actual=" + amplifiedTaken);
            helper.assertTrue(amplifiedTaken > baselineTaken,
                    "Amplified haunted bonus damage should exceed baseline damage");
        });
    }
}
