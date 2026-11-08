package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleAdsMovement;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRateLimiter;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTranslatableKey;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FullautoRapidcastSpellrifleGameTestScenarios {
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

    private static final String LEGACY_NEXT_SPECIAL_CAST_TICK_TAG =
            "ApprenticeCodexFullautoRapidcastSpellrifleNextSpecialCastTick";

    private FullautoRapidcastSpellrifleGameTestScenarios() {
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
            helper.assertTrue(spell.getSpellCooldown() > ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleCooldownBypassThresholdTicks(),
                    "This fixture must exceed the unmodified cooldown threshold");
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 10,
                    "A reduced effective cooldown at the threshold must use subtraction, not bypass: "
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
    public static void fullautoLongStorageSurvivesCalibrationChanges(GameTestHelper helper) {
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
        rifle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY, lookup);
        helper.assertTrue(FullautoRapidcastSpellrifle.getSelectedSpellData(stack, lookup) == SpellData.EMPTY, "Removing ring must disable LONG again");
        helper.assertFalse(FullautoRapidcastSpellrifleScrollStorage.get(stack, 0, lookup).isEmpty(), "Removing ring must preserve scroll");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoEnchantmentSupportAndTranscendence(GameTestHelper helper) {
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
        // 通常上限と無関係に、付与された場合だけ固定で一段階上昇する。
        helper.assertTrue(Enchantments.getLevel(stack, Enchantments.TRANSCENDENCE) == 1, "Transcendence must be present");
        helper.assertTrue(FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(stack, new SpellData(spell, 1)) == 2,
                "Transcendence must add exactly one level");
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
