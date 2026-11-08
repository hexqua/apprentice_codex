package jp.aquafactory.apprenticecodex.gametest.equipment.shield;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.shield.ReflectcastShieldCalibrationGameTests.collectReflectcastAbilityTooltipLines;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spellgun.EquipmentSpellGunGameTestScenarios.containsTranslatableKey;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ReflectcastShieldCalibrationScenarios {
    private ReflectcastShieldCalibrationScenarios() {}

    static void reflectcastShieldCastRestrictionsFollowCalibration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            var item = (ReflectcastShield) stack.getItem();
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
            helper.assertTrue(ReflectcastShield.hasSilverRing(stack),
                    "Reflectcast Shield should store Silver Ring calibration");
            helper.assertTrue(ReflectcastShield.hasWisdomShard(stack),
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
            ReflectcastShieldRuntime
                    .onSpellCooldownAdded(longCooldownEvent);
            helper.assertTrue(longCooldownEvent.getEffectiveCooldown()
                            == baseCooldown + longSpell.getEffectiveCastTime(1, player),
                    "Reflectcast Shield should extend LONG cooldown by its effective cast time");
            magicData.setPlayerCastingItem(ItemStack.EMPTY);
            var manaBeforeCast = magicData.getMana();
            helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "A valid block trigger should start Reflectcast continuous casting immediately");
            helper.assertTrue(ReflectcastShieldRuntime
                            .shouldBypassMagicManager(magicData),
                    "Reflectcast continuous casting should bypass Iron's standard cast tick");
            helper.assertTrue(magicData.getMana() < manaBeforeCast,
                    "Reflectcast continuous casting should consume normal spell mana");
            helper.assertFalse(ReflectcastShieldRuntime.tryTriggerSpell(
                            player, castStack, InteractionHand.OFF_HAND),
                    "Additional blocks should not restart an active continuous cast");
            ReflectcastShieldRuntime.finishUse(player);
            helper.assertFalse(magicData.isCasting(),
                    "Releasing Reflectcast Shield should clear its continuous casting state");
            player.stopUsingItem();
            ReflectcastShieldRuntime.clear(player);
        });
    }
}
