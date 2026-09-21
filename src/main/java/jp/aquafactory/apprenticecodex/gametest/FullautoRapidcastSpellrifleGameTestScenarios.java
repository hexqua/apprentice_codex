package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleAdsMovement;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRateLimiter;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRecoil;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FullautoRapidcastSpellrifleGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    @GameTest(template = "gametest/basic_floor")
    public static void fullautoAdsMovementHonorsConfigAndSprintPriority(GameTestHelper helper) {
        ModConfigSpec.DoubleValue multiplier = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.adsMovementSpeedMultiplier");
        double previous = multiplier.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_ads_movement");
        var rifle = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var otherId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_other_movement");
        speed.addTransientModifier(new AttributeModifier(otherId, 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        double baseline = speed.getValue();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
            multiplier.set(0.7D);
            // バニラの使用状態を開始せず、射撃中にも維持するADS入力の契約を確認する。
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            helper.assertTrue(Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS must apply the configured multiplier once, preserving other modifiers");
            multiplier.set(0.0D);
            player.setSprinting(true);
            FullautoRapidcastSpellrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(speed.getValue() == 0.0D, "Zero ADS multiplier must disable movement");
            helper.assertTrue(!player.isSprinting(), "ADS must suppress sprinting even at zero movement multiplier");
            FullautoRapidcastSpellrifleAdsMovement.update(player, false);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D,
                    "Releasing ADS must restore movement even at zero multiplier");
            multiplier.set(1.0D);
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D, "Multiplier one must preserve speed");
            multiplier.set(0.7D);
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            player.setSprinting(true);
            FullautoRapidcastSpellrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.isSprinting() && Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS must stop sprinting and preserve configured slowdown");
            FullautoRapidcastSpellrifleAdsMovement.update(player, false);
            player.setSprinting(true);
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            helper.assertTrue(!player.isSprinting() && Math.abs(speed.getValue() - baseline * 0.7D) < 1.0E-8D,
                    "ADS requests during sprint must enter ADS and stop sprinting");
            FullautoRapidcastSpellrifleAdsMovement.update(player, false);
            player.setSprinting(true);
            FullautoRapidcastSpellrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.isSprinting(), "Releasing ADS must allow sprinting again");
            player.setSprinting(false);
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, rifle);
            player.setSprinting(true);
            FullautoRapidcastSpellrifleAdsMovement.onPlayerTick(new PlayerTickEvent.Post(player));
            FullautoRapidcastSpellrifleAdsMovement.update(player, true);
            helper.assertTrue(player.isSprinting(), "Offhand ADS requests must not cancel sprinting");
            player.setSprinting(false);
            helper.assertTrue(Math.abs(speed.getValue() - baseline) < 1.0E-8D,
                    "Switching away must remove slowdown and offhand ADS requests must be ignored");
            helper.assertTrue(speed.hasModifier(otherId), "Cleanup must preserve unrelated modifiers");
        } finally {
            multiplier.set(previous);
            FullautoRapidcastSpellrifleAdsMovement.update(player, false);
            speed.removeModifier(otherId);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRecoveryRuneIsUniqueAndUpdatesDescription(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        var rune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());
        helper.assertFalse(FullautoRapidcastSpellrifle.hasRecoveryRune(stack, lookup), "Unadjusted rifle must retain recoil");
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 2, rune, lookup), "Recovery Rune must fit any adjustment slot");
        helper.assertTrue(FullautoRapidcastSpellrifle.hasRecoveryRune(stack, lookup), "Recovery Rune must disable camera recoil");
        helper.assertFalse(rifle.trySetCalibrationAdjustment(stack, 0, rune, lookup), "Duplicate Recovery Rune must be rejected");
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 0,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), lookup),
                "Recovery Rune must coexist with Silver Ring");
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 1,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()), lookup),
                "Recovery Rune must coexist with the scroll slot upgrade");
        helper.assertTrue(rifle.getEnabledCalibrationScrollSlotCount(stack, lookup) == 2
                        && FullautoRapidcastSpellrifle.hasSilverRing(stack, lookup),
                "Other adjustments must retain their effects");
        var restored = ItemStack.parseOptional(lookup, (CompoundTag) stack.saveOptional(lookup));
        helper.assertTrue(FullautoRapidcastSpellrifle.hasRecoveryRune(restored, lookup), "Recovery Rune must survive serialization");
        var rule = rifle.getCalibrationAdjustmentProfile(stack).rules().stream()
                .filter(candidate -> candidate.accepts(rune)).findFirst().orElseThrow();
        assertTranslatableKey(helper, rule.effectLines().getFirst(),
                "jei.apprenticecodex.spell_calibration_bench.effect.remove_recoil", "JEI must explain recoil removal");
        var lines = new ArrayList<Component>();
        rifle.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), lines, TooltipFlag.Default.NORMAL);
        assertTranslatableKey(helper, lines.get(1), "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_2.no_recoil",
                "Adjusted controls must not claim ADS reduces recoil");
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 2, ItemStack.EMPTY, lookup), "Recovery Rune must be removable");
        helper.assertFalse(FullautoRapidcastSpellrifle.hasRecoveryRune(stack, lookup), "Removing Recovery Rune must restore recoil");
        lines.clear();
        rifle.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), lines, TooltipFlag.Default.NORMAL);
        assertTranslatableKey(helper, lines.get(1), "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_2",
                "Removing Recovery Rune must restore the ADS recoil description");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRecoilDistributesAndOverlapsWithoutLosingAngle(GameTestHelper helper) {
        var recoil = new FullautoRapidcastSpellrifleRecoil();
        recoil.addImpulse(3.0F, 0);
        helper.assertTrue(recoil.advanceImpulses(0) == 0, "Recoil must not jump when a shot arrives");
        var firstQuarter = recoil.advanceImpulses(25_000_000);
        helper.assertTrue(Math.abs(firstQuarter - 1.3125F) < 0.001F, "Ease-out must apply 43.75 percent of the angle at 25 ms");
        var firstHalf = firstQuarter + recoil.advanceImpulses(50_000_000);
        helper.assertTrue(Math.abs(firstHalf - 2.25F) < 0.001F, "Ease-out must apply 75 percent of the angle at 50 ms");
        recoil.addImpulse(1.5F, 50_000_000);
        helper.assertTrue(recoil.advanceImpulses(50_000_000) == 0, "Repeated sampling must not duplicate recoil");
        var total = firstHalf + recoil.advanceImpulses(100_000_000) + recoil.advanceImpulses(150_000_000);
        helper.assertTrue(Math.abs(total - 4.5F) < 0.001F, "Overlapping shots must preserve their combined angle");
        helper.assertTrue(recoil.advanceImpulses(200_000_000) == 0, "Completed recoil must not move or restore the view");
        recoil.addImpulse(3.0F, 200_000_000);
        helper.assertTrue(Math.abs(recoil.advanceImpulses(350_000_000) - 3.0F) < 0.001F,
                "Sparse frames must preserve the same total angle");
        recoil.addImpulse(3.0F, 350_000_000);
        recoil.advanceImpulses(375_000_000);
        recoil.clearImpulses();
        helper.assertTrue(recoil.advanceImpulses(500_000_000) == 0, "Discarded recoil must not resume after a pause or item switch");
        recoil.addImpulse(3.0F, 500_000_000);
        var previousDelta = Float.MAX_VALUE;
        for (var sample = 1; sample <= 4; sample++) {
            var delta = recoil.advanceImpulses(500_000_000L + sample * 25_000_000L);
            helper.assertTrue(delta >= 0 && delta < previousDelta,
                    "Ease-out must decelerate without moving the view backward");
            previousDelta = delta;
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRecoilBuildsRecoversAndReducesWhileAiming(GameTestHelper helper) {
        var hip = new FullautoRapidcastSpellrifleRecoil();
        var ads = new FullautoRapidcastSpellrifleRecoil();
        helper.assertTrue(Math.abs(hip.fire(0, false) - 1.5F) < 0.001F, "First hip shot must kick by 1.5 degrees");
        helper.assertTrue(Math.abs(ads.fire(0, true) - 0.675F) < 0.001F, "ADS must reduce the first kick");
        var hipSecond = hip.fire(3, false);
        var adsSecond = ads.fire(3, true);
        helper.assertTrue(hipSecond > 1.5F && adsSecond < hipSecond * 0.45F,
                "ADS must reduce both kick and buildup");
        for (var tick = 6; tick <= 60; tick += 3) {
            helper.assertTrue(hip.fire(tick, false) <= 3.0F, "Continuous hip fire must cap at 3 degrees");
        }
        helper.assertTrue(Math.abs(hip.fire(63, false) - 3.0F) < 0.001F, "Sustained fire must reach maximum kick");
        helper.assertTrue(Math.abs(hip.fire(67, false) - 3.0F) < 0.001F, "Buildup must be held for four ticks");
        var recovered = hip.fire(77, false);
        helper.assertTrue(recovered > 1.5F && recovered < 3.0F, "Buildup must recover gradually");
        helper.assertTrue(Math.abs(hip.fire(93, false) - 1.5F) < 0.001F, "Sixteen idle ticks must fully recover buildup");
        helper.assertTrue(Math.abs(hip.fire(0, false) - 1.5F) < 0.001F, "Clock reset must discard previous buildup");
        helper.succeed();
    }

    private static final String LEGACY_NEXT_SPECIAL_CAST_TICK_TAG =
            "ApprenticeCodexFullautoRapidcastSpellrifleNextSpecialCastTick";

    private FullautoRapidcastSpellrifleGameTestScenarios() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleKeepsExpectedStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));

            helper.assertTrue(modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty(),
                    "Fullauto Rapidcast Spellrifle should not add attack damage modifiers");
            helper.assertTrue(modifiers.get(Attributes.ATTACK_SPEED).isEmpty(),
                    "Fullauto Rapidcast Spellrifle should not add attack speed modifiers");
            helper.assertTrue(modifiers.isEmpty(), "Unenchanted rifle must not grant attributes");

        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleTooltipShowsControlsBeforeShiftHint(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);

            helper.assertTrue(tooltipLines.size() >= 4,
                    "Fullauto Rapidcast Spellrifle tooltip should include controls, spacer, and shift hint");
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(0),
                    "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_1",
                    "Fullauto Rapidcast Spellrifle should show left-click control first"
            );
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(1),
                    "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_2",
                    "Fullauto Rapidcast Spellrifle should show right-click control second"
            );
            helper.assertTrue(tooltipLines.get(2).getString().isEmpty(),
                    "Fullauto Rapidcast Spellrifle should separate controls from the shift hint with a blank line");
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(3),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    "Fullauto Rapidcast Spellrifle should show shift hint after controls"
            );
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleSpecialCooldownPolicyMatchesDefaults(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (FullautoRapidcastSpellrifle) ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get();
            helper.assertTrue(item.resolveSpecialCooldownTicks(20 * 5) == 0,
                    "Fullauto Rapidcast Spellrifle should remove cooldowns at the default bypass threshold");
            helper.assertTrue(item.resolveSpecialCooldownTicks(101) == 20,
                    "Fullauto Rapidcast Spellrifle should not reduce longer cooldowns below the default minimum");
            helper.assertTrue(item.resolveSpecialCooldownTicks(20 * 60) == 20 * 50,
                    "Fullauto Rapidcast Spellrifle should subtract the default 10 seconds from long cooldowns");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleRateLimitIgnoresLegacyPersistentNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_rate_limit_test");
            FullautoRapidcastSpellrifleRateLimiter.clear(player);
            try {
                player.getPersistentData().putLong(LEGACY_NEXT_SPECIAL_CAST_TICK_TAG, Long.MAX_VALUE);

                helper.assertTrue(FullautoRapidcastSpellrifleRateLimiter.canAttemptSpecialCast(player),
                        "Fullauto Rapidcast Spellrifle should ignore legacy persistent next-cast NBT");
                helper.assertFalse(FullautoRapidcastSpellrifleRateLimiter.canAttemptSpecialCast(player),
                        "Fullauto Rapidcast Spellrifle should still rate-limit repeated same-tick attempts");

                FullautoRapidcastSpellrifleCastEvent.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));
                helper.assertTrue(FullautoRapidcastSpellrifleRateLimiter.canAttemptSpecialCast(player),
                        "Fullauto Rapidcast Spellrifle rate limit should be cleared on logout");
            } finally {
                FullautoRapidcastSpellrifleRateLimiter.clear(player);
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_ammo_policy_test");

            helper.assertTrue(item.getAmmoItem(stack) == ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(),
                    "Fullauto Rapidcast Spellrifle should use Full-auto Spell Casting Round");
            helper.assertTrue(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get() instanceof SpellcasterRoundItem,
                    "Full-auto Spell Casting Round should be a SpellcasterRoundItem");
            var roundItem = (SpellcasterRoundItem) ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get();
            helper.assertTrue(roundItem.getEmptyCasingItem() == ItemRegistry.EMPTY_FULLAUTO_SPELL_CASTING_CASING.get(),
                    "Full-auto Spell Casting Round should return Empty Full-auto Spell Casting Casing");
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.2F,
                    "Fullauto Rapidcast Spellrifle should use 20% empty casing return chance without Spellcaster Ammo Pouch");

            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()));
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.9F,
                    "Fullauto Rapidcast Spellrifle should use 90% empty casing return chance with Spellcaster Ammo Pouch");
            var ammo = new ItemStack(roundItem, 2);
            var casing = new ItemStack(ItemRegistry.EMPTY_FULLAUTO_SPELL_CASTING_CASING.get());
            helper.assertTrue(SpellcasterAmmoPouch.storeInEquippedPouches(player, ammo) == 2,
                    "Ammo pouch should store Full-auto Spell Casting Rounds");
            helper.assertTrue(SpellcasterAmmoPouch.consumeAmmoFromAccessiblePouches(player, roundItem),
                    "Full-auto ammunition should be consumable from an equipped pouch");
            helper.assertTrue(SpellcasterAmmoPouch.countAmmoInAccessiblePouches(player, roundItem) == 1,
                    "Pouch consumption should remove exactly one Full-auto round");
            helper.assertTrue(casing.is(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS),
                    "Full-auto casing should be classified as an empty casing");
            helper.assertTrue(SpellcasterAmmoPouch.storeInEquippedPouches(player, casing) == 1,
                    "Ammo pouch should store Empty Full-auto Spell Casting Casings");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_recast_ammo_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var ammoStack = new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 1);
            player.getInventory().add(ammoStack);

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, true)) {
                FullautoRapidcastSpellrifleCastEvent.onSpellCast(new SpellOnCastEvent(
                        player,
                        spell.getSpellId(),
                        1,
                        spell.getManaCost(1),
                        spell.getSchoolType(),
                        CastSource.SWORD
                ));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close Fullauto Rapidcast Spellrifle test context.", exception);
            }

            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    item.getAmmoItem(stack)
            ) == 1, "Fullauto Rapidcast Spellrifle recast should not consume Full-auto Spell Casting Round");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleKeepsNormalManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_mana_policy_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 1));

            helper.assertFalse(stack.getItem() instanceof ManaBypassSpellItem,
                    "Fullauto Rapidcast Spellrifle should not bypass mana consumption");

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var manaCost = spell.getManaCost(1);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            magicData.setMana(0.0F);

            try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                helper.assertFalse(SpellgunCastContext.shouldBypassManaCheck(spell, player),
                        "Fullauto Rapidcast Spellrifle context must not enable Spellgun mana bypass");
                helper.assertFalse(spell.canBeCastedBy(1, CastSource.SWORD, magicData, player).isSuccess(),
                        "Fullauto Rapidcast Spellrifle should remain mana-gated when mana is insufficient");
                helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                                player,
                                player.getInventory(),
                                item.getAmmoItem(stack)
                        ) == 1,
                        "Rejected Fullauto Rapidcast Spellrifle cast should not consume ammunition");

                var event = new SpellOnCastEvent(
                        player,
                        spell.getSpellId(),
                        1,
                        manaCost,
                        spell.getSchoolType(),
                        CastSource.SWORD
                );
                ItemManaBypassCastEvent.onSpellCast(event);
                helper.assertTrue(event.getManaCost() == manaCost,
                        "Fullauto Rapidcast Spellrifle should keep normal mana cost: " + event.getManaCost());

                magicData.setMana(manaCost);
                helper.assertTrue(spell.canBeCastedBy(1, CastSource.SWORD, magicData, player).isSuccess(),
                        "Fullauto Rapidcast Spellrifle should cast when normal mana requirements are met");
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close Fullauto Rapidcast Spellrifle mana policy test context.", exception);
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_instant_policy_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 1));

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            FullautoRapidcastSpellrifleCastContext.rememberPending(
                    player.getUUID(),
                    stack,
                    spell,
                    false,
                    helper.getLevel().getGameTime()
            );

            FullautoRapidcastSpellrifleCastEvent.onSpellCast(new SpellOnCastEvent(
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
                    item.getAmmoItem(stack)
            ) == 0, "Fullauto Rapidcast Spellrifle instant cast should consume Full-auto Spell Casting Round");

            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    20 * 5,
                    spell,
                    player,
                    CastSource.SWORD
            );
            FullautoRapidcastSpellrifleCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 0,
                    "Fullauto Rapidcast Spellrifle instant cast should bypass cooldowns at the threshold: "
                            + cooldownEvent.getEffectiveCooldown());
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoCalibrationPreservesDisabledScrolls(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_calibration");
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1));
        scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Owned scroll"));
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 1, "Rifle must start with one scroll slot");
        helper.assertFalse(rifle.isSneakSelectionUiEnabled(stack), "Single-slot rifle must not open selection UI");
        var upgrade = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        for (var slot = 1; slot <= 3; slot++) {
            helper.assertTrue(menu.getSlot(slot).mayPlace(upgrade), "Lesser upgrades must be repeatable");
            menu.getSlot(slot).set(upgrade.copy());
        }
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 4, "Three upgrades must enable four scroll slots");
        helper.assertTrue(rifle.isSneakSelectionUiEnabled(stack), "Expanded rifle must enable selection UI");
        helper.assertFalse(rifle.canPlaceCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()), lookup),
                "Scrollwoven Parchment must not be accepted");
        for (var slot = 4; slot < 8; slot++) {
            helper.assertTrue(menu.getSlot(slot).mayPlace(scroll), "Enabled scroll slot must accept a scroll");
            menu.getSlot(slot).set(scroll.copy());
        }
        rifle.setSneakSelectionIndex(stack, 3);
        helper.assertTrue(rifle.getSneakSelectionIndex(stack) == 3, "Expanded selection must select the requested slot");
        for (var slot = 1; slot <= 3; slot++) menu.getSlot(slot).set(ItemStack.EMPTY);
        helper.assertTrue(rifle.getSneakSelectionIndex(stack) == 0, "Shrinking must normalize selection");
        helper.assertFalse(rifle.isSneakSelectionIndexSelectable(stack, 3), "Disabled selection must be rejected");
        helper.assertFalse(rifle.isSneakSelectionIndexSelectable(stack, -1), "Negative selection must be rejected");
        for (var slot = 5; slot < 8; slot++) {
            helper.assertFalse(menu.getSlot(slot).mayPlace(scroll), "Disabled slots must reject insertion");
            helper.assertTrue(menu.getSlot(slot).mayPickup(player), "Disabled slots must remain extractable");
            helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, menu.getSlot(slot).remove(1)), "Scroll ownership must survive shrink");
            helper.assertTrue(FullautoRapidcastSpellrifleScrollStorage.get(stack, slot - 4, lookup).isEmpty(), "Extraction must not duplicate scrolls");
        }
        var restored = ItemStack.parseOptional(lookup, (CompoundTag) stack.saveOptional(lookup));
        helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, FullautoRapidcastSpellrifleScrollStorage.get(restored, 0, lookup)),
                "Scroll components must survive serialization");
        helper.assertFalse(ISpellContainer.isSpellContainer(restored), "Rifle must not project spells into a spell container");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoLongSupportAndCooldownOrder(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_long");
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        var spell = SpellRegistry.FIREBALL_SPELL.get();
        helper.assertTrue(spell.getCastType() == CastType.LONG, "Test fixture must use a LONG spell");
        var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1));
        var state = rifle.evaluateCalibrationImbue(stack, 0, new SpellData(spell, 1), lookup);
        helper.assertTrue(state.canInsert() && !state.isUsable(), "LONG scroll must be storable but initially unusable");
        FullautoRapidcastSpellrifleScrollStorage.set(stack, 0, scroll, lookup);
        helper.assertTrue(FullautoRapidcastSpellrifle.getSelectedSpellData(stack, lookup) == SpellData.EMPTY, "Unsupported LONG must not be selected");
        var ring = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
        helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 0, ring, lookup), "Silver Ring must be accepted");
        helper.assertFalse(rifle.trySetCalibrationAdjustment(stack, 1, ring, lookup), "Duplicate Silver Ring must be rejected");
        var continuous = rifle.evaluateCalibrationImbue(stack, 0, new SpellData(SpellRegistry.FIRE_BREATH_SPELL.get(), 1), lookup);
        helper.assertFalse(continuous.canInsert(), "CONTINUOUS insertion must be rejected even with Silver Ring");
        helper.assertTrue(FullautoRapidcastSpellrifle.getSelectedSpellData(stack, lookup).getSpell() == spell, "Silver Ring must enable LONG");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setPlayerCastingItem(stack);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.initiateCast(spell, 1, 0, CastSource.SWORD, "mainhand");
        var event = new SpellCooldownAddedEvent.Pre(100, spell, player, CastSource.SWORD);
        try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
            FullautoRapidcastSpellrifleCastEvent.onSpellCooldownAdded(event);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close LONG test context", exception);
        }
        var expected = rifle.resolveSpecialCooldownTicks(100 + spell.getEffectiveCastTime(1, player));
        helper.assertTrue(event.getEffectiveCooldown() == expected && expected >= 20,
                "LONG cast duration must be added before bypass and reduction");
        var castSpeed = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
        var originalSpeed = castSpeed.getBaseValue();
        castSpeed.setBaseValue(originalSpeed + 0.5);
        try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
            var fasterEvent = new SpellCooldownAddedEvent.Pre(1000, spell, player, CastSource.SWORD);
            FullautoRapidcastSpellrifleCastEvent.onSpellCooldownAdded(fasterEvent);
            helper.assertTrue(fasterEvent.getEffectiveCooldown() == rifle.resolveSpecialCooldownTicks(1000 + spell.getEffectiveCastTime(1, player)),
                    "LONG surcharge must respect effective cast speed");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close cast speed test context", exception);
        } finally {
            castSpeed.setBaseValue(originalSpeed);
        }
        rifle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY, lookup);
        helper.assertTrue(FullautoRapidcastSpellrifle.getSelectedSpellData(stack, lookup) == SpellData.EMPTY, "Removing ring must disable LONG again");
        helper.assertFalse(FullautoRapidcastSpellrifleScrollStorage.get(stack, 0, lookup).isEmpty(), "Removing ring must preserve scroll");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoEnchantmentsAndIndependentDenylist(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var allowed = Set.of(Enchantments.ALACRITY, Enchantments.REFLUX, Enchantments.RESERVOIR, Enchantments.TENSE,
                Enchantments.WISDOM, Enchantments.PLUNDER, Enchantments.TRANSCENDENCE);
        enchantments.listElements().forEach(enchantment -> {
            var id = enchantment.key().location();
            helper.assertTrue(rifle.supportsEnchantment(stack, enchantment)
                            == (allowed.contains(enchantment.key()) || id.toString().equals("malum:spirit_plunder")),
                    "Unexpected enchantment support: " + id);
        });
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        stack.enchant(enchantments.getOrThrow(Enchantments.TRANSCENDENCE), 1);
        // GameTest 環境でも Iron's の設定上限を尊重する。付与そのものの成否と上限処理を分けて検証する。
        helper.assertTrue(Enchantments.getLevel(stack, Enchantments.TRANSCENDENCE) == 1, "Transcendence must be present");
        helper.assertTrue(FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(stack, new SpellData(spell, 1)) == Math.min(2, spell.getMaxLevel()),
                "Transcendence must add one level without exceeding the configured maximum");
        try (var ignored = ApprenticeCodexServerConfig.useFullautoRapidcastSpellrifleSpellDenylistOverrideForGameTest(List.of(spell.getSpellId()))) {
            helper.assertTrue(FullautoRapidcastSpellrifle.isSpecialCastSpellDenied(spell), "New denylist must affect new rifle");
            helper.assertFalse(MultipurposeStaffrifle.isSpecialCastSpellDenied(spell),
                    "New denylist must not affect old rifle");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoMagiAgentSuitPreservesAmmoAndManaBenefits(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_suit");
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()));
        player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 1));
        MagicData.getPlayerMagicData(player).setPlayerCastingItem(stack);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (var skipMana : List.of(false, true)) {
            try (var config = ApprenticeCodexServerConfig.useMagiAgentSuitAmmoConfigOverrideForGameTest(1, skipMana);
                 var context = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                var event = new SpellOnCastEvent(player, spell.getSpellId(), 1, spell.getManaCost(1), spell.getSchoolType(), CastSource.SWORD);
                FullautoRapidcastSpellrifleCastEvent.onSpellCast(event);
                helper.assertTrue(event.getManaCost() == (skipMana ? 0 : spell.getManaCost(1)), "Suit mana benefit must follow its setting");
                helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get()) == 1,
                        "Suit ammo benefit must preserve the round");
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close suit test context", exception);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoCastsOnlyStoredSpellAndRejectsOffhand(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_cast");
        var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
        var rifle = (FullautoRapidcastSpellrifle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(rifle.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
                "Offhand use must fail");
        helper.assertTrue(rifle.resolveSneakSelectionStack(player, InteractionHand.OFF_HAND).isEmpty(), "Offhand selection must be disabled");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().add(new ItemStack(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(), 2));
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(1000);
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, false), "Empty rifle must not cast a selected spellbook spell");
        FullautoRapidcastSpellrifleScrollStorage.set(stack, 0,
                SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.FIREBALL_SPELL.get(), 1)), lookup);
        var manaBeforeRejectedCast = magic.getMana();
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, false), "Stored LONG spell must not cast without SilverRing");
        helper.assertTrue(magic.getMana() == manaBeforeRejectedCast, "Rejected LONG spell must not consume mana");
        helper.assertFalse(magic.isCasting(), "Rejected LONG spell must not start casting");
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 2,
                "Rejected LONG spell must not consume ammunition");
        FullautoRapidcastSpellrifleScrollStorage.set(stack, 0,
                SpellCalibrationImbueHelper.createScroll(new SpellData(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1)), lookup);
        helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "Stored INSTANT spell must cast");
        // FakePlayer は通常の player tick を受けないため、開始後の完了処理を明示して進める。
        SpellRegistry.MAGIC_MISSILE_SPELL.get().castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 1,
                "Successful stored spell must consume one round");
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, true), "Immediate repeated packet must be rate limited");
        helper.succeed();
    }
}
