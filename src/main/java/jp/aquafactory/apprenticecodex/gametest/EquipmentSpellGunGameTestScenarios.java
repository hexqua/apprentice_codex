package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

final class EquipmentSpellGunGameTestScenarios extends ApprenticeCodexGameTestScenarios {
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

    private static boolean containsTranslatableKey(List<Component> lines, String key) {
        return lines.stream().anyMatch(component ->
                component.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey())
        );
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
}
