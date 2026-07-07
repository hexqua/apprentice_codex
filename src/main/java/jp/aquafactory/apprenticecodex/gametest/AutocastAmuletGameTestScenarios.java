package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class AutocastAmuletGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private AutocastAmuletGameTestScenarios() {
    }

    static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var apprenticeLongSpell = SpellRegistry.MANTIS_LEAP.get();
            var continuousSpell = SpellRegistry.MANA_CHARGE.get();
            var necklaceTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT)
            );

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Autocast Amulet should not expose Iron's SpellContainer by default");
            helper.assertTrue(jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(
                            stack,
                            createSpellScroll(apprenticeSpell)
                    ),
                    "Autocast Amulet should not be handled as an Arcane Anvil imbue target");
            helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == AutocastAmulet.MIN_SPELL_SLOTS,
                    "Autocast Amulet should start with one enabled spell slot");
            helper.assertTrue(stack.is(necklaceTag),
                    "Autocast Amulet should be tagged as curios:necklace");
            helper.assertTrue(item.canImbueSpell(apprenticeSpell, 1),
                    "Autocast Amulet should allow instant no-recast spells by default");
            helper.assertTrue(item.canImbueSpell(apprenticeLongSpell, 1),
                    "Autocast Amulet should allow long no-recast spells to be imbued");
            helper.assertFalse(item.canImbueSpell(continuousSpell, 1),
                    "Autocast Amulet should reject continuous spells");
            helper.assertFalse(item.canAutoCastSpell(stack, apprenticeLongSpell, 1),
                    "Autocast Amulet should not auto-cast long spells without Silver Ring adjustment");
            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            helper.assertTrue(item.canAutoCastSpell(stack, apprenticeLongSpell, 1),
                    "Autocast Amulet should allow long no-recast spells with Silver Ring adjustment");
        });
    }

    static void autocastAmuletDeletesPersistedFutureRetryTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = ((AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get()).getDefaultInstance();
            var tag = stack.getOrCreateTag();
            tag.putLong("apprenticecodex:autocast_retry_sequence_tick", 72000L);
            tag.putInt("apprenticecodex:autocast_retry_skip_slot", 2);

            helper.assertFalse(AutocastAmulet.isRetrySequenceCoolingDown(stack, 0L),
                    "Autocast Amulet should delete impossible future retry sequence ticks");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(stack) == -1L,
                    "Autocast Amulet retry sequence tick should be removed after sanitizing");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(stack) == -1,
                    "Autocast Amulet retry skip slot should be removed together with the retry tick");
        });
    }

    static void autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "autocast_amulet_wisdom_adjustment_test");
            var stack = ((AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get()).getDefaultInstance();
            var wisdomShard = new ItemStack(ItemRegistry.WISDOM_SHARD.get());
            var silverRing = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());

            helper.assertTrue(AutocastAmulet.isCalibrationAdjustmentItem(wisdomShard),
                    "Wisdom Shard should be accepted as an Autocast Amulet adjustment item");
            helper.assertFalse(AutocastAmulet.isCalibrationSlotUpgrade(wisdomShard),
                    "Wisdom Shard should not count as an Autocast Amulet slot upgrade");
            helper.assertFalse(AutocastAmulet.isSilverRing(wisdomShard),
                    "Wisdom Shard should not count as a Silver Ring adjustment");

            AutocastAmulet.setCalibrationAdjustment(stack, 0, wisdomShard);

            helper.assertTrue(AutocastAmulet.hasWisdomShardAdjustment(stack),
                    "Autocast Amulet should detect an installed Wisdom Shard");
            helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == AutocastAmulet.MIN_SPELL_SLOTS,
                    "Wisdom Shard should not increase Autocast Amulet spell slots");
            helper.assertFalse(AutocastAmulet.hasSilverRingAdjustment(stack),
                    "Wisdom Shard should not enable Silver Ring long-cast support");

            var menuStack = ((AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get()).getDefaultInstance();
            var menu = createSpellCalibrationBenchMenuWithTarget(player, menuStack);
            var adjustmentSlot = menu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START);
            helper.assertTrue(adjustmentSlot.mayPlace(wisdomShard),
                    "Spell Calibration Bench should accept Wisdom Shard for Autocast Amulet adjustments");
            helper.assertTrue(adjustmentSlot.mayPlace(silverRing),
                    "Spell Calibration Bench should accept Silver Ring for Autocast Amulet adjustments");
            helper.assertFalse(adjustmentSlot.mayPlace(fireRune),
                    "Spell Calibration Bench should reject school runes for Autocast Amulet adjustments");

            var playerInventoryMenuSlot = SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START
                    + ScrollcasterGauntlet.CALIBRATION_SCROLL_SLOT_COUNT;
            player.getInventory().setItem(9, wisdomShard.copy());
            var quickMovedWisdomShard = menu.quickMoveStack(player, playerInventoryMenuSlot);
            helper.assertTrue(quickMovedWisdomShard.is(ItemRegistry.WISDOM_SHARD.get()),
                    "Shift-clicked Wisdom Shard should move while Autocast Amulet is the target");
            helper.assertTrue(adjustmentSlot.getItem().is(ItemRegistry.WISDOM_SHARD.get()),
                    "Shift-clicked Wisdom Shard should enter an Autocast Amulet adjustment slot");
        });
    }

    static void autocastAmuletWisdomShardBlocksUnprofiledAutoCast(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_wisdom_unprofiled_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.CHARGE_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            AutocastAmulet.setCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            equipNecklaceCurio(player, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(200.0F);

            try (var ignored = jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                    .useProfilesForGameTest(Map.of())) {
                runAutocastAmuletServerTick(player, 20);
            }

            helper.assertFalse(magicData.isCasting(),
                    "Wisdom Shard should block auto-cast when the spell has no profile");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Wisdom Shard should not start cooldown for unprofiled blocked spells");
            helper.succeed();
        });
    }

    static void autocastAmuletWisdomShardProfileConditionsUseAndSemantics(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_wisdom_conditions_test");
            var chargeSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.CHARGE_SPELL.get();
            var fortifySpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FORTIFY_SPELL.get();
            var chargeData = new SpellData(chargeSpell, 1);
            var fortifyData = new SpellData(fortifySpell, 1);
            var chargeEffectId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charged");
            var fortifyEffectId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fortify");
            var profiles = Map.of(
                    chargeSpell.getSpellResource(),
                    new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfile(
                            List.of(new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletMobEffectCondition(
                                    chargeEffectId,
                                    60
                            )),
                            Optional.of(0.5F)
                    ),
                    fortifySpell.getSpellResource(),
                    new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfile(
                            List.of(new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletMobEffectCondition(
                                    fortifyEffectId,
                                    0
                            )),
                            Optional.empty()
                    )
            );

            try (var ignored = jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                    .useProfilesForGameTest(profiles)) {
                helper.assertFalse(jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                                .canCastWithWisdomShard(player, chargeData),
                        "Wisdom Shard profile should require every configured condition");

                player.setHealth(player.getMaxHealth() * 0.5F);
                player.addEffect(new MobEffectInstance(
                        io.redspace.ironsspellbooks.registries.MobEffectRegistry.CHARGED.get(),
                        61
                ));
                helper.assertFalse(jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                                .canCastWithWisdomShard(player, chargeData),
                        "Wisdom Shard mob effect profile should reject one tick above the threshold");

                player.removeEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.CHARGED.get());
                player.addEffect(new MobEffectInstance(
                        io.redspace.ironsspellbooks.registries.MobEffectRegistry.CHARGED.get(),
                        60
                ));
                helper.assertTrue(jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                                .canCastWithWisdomShard(player, chargeData),
                        "Wisdom Shard mob effect profile should accept exactly the configured threshold");

                player.removeEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.FORTIFY.get());
                helper.assertTrue(jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager
                                .canCastWithWisdomShard(player, fortifyData),
                        "Wisdom Shard mob effect profile should treat missing effects as 0 ticks");
            }
        });
    }

    static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var remoteEye = SpellRegistry.REMOTE_EYE.get();
            var mutable = ISpellContainer.create(5, false, false).mutableCopy();

            helper.assertTrue(mutable.addSpellAtIndex(apprenticeSpell, 1, 0, false),
                    "Failed to prepare sense_evil for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(mageLight, 1, 1, false),
                    "Failed to prepare mage_light for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(remoteEye, 1, 2, false),
                    "Failed to prepare remote_eye for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(SpellRegistry.MANA_CHARGE.get(), 1, 3, false),
                    "Failed to prepare blocked mana_charge for Autocast Amulet normalization test");
            ISpellContainer.set(stack, mutable.toImmutable());

            item.initializeSpellContainer(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Autocast Amulet legacy conversion should remove Iron's SpellContainer");
            assertAutocastSpellData(helper, stack, 0, apprenticeSpell, 1,
                    "Autocast Amulet conversion should preserve the first valid spell");
            assertAutocastSpellData(helper, stack, 1, mageLight, 1,
                    "Autocast Amulet conversion should preserve the second valid spell");
            assertAutocastSpellData(helper, stack, 2, remoteEye, 1,
                    "Autocast Amulet conversion should preserve the third valid spell");
            helper.assertTrue(AutocastAmulet.getSpellDataAt(stack, 3) == SpellData.EMPTY,
                    "Autocast Amulet conversion should drop blocked spells");
        });
    }
    static void autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var remoteEye = SpellRegistry.REMOTE_EYE.get();
            var charge = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(apprenticeSpell, 1)
            );

            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            AutocastAmulet.setCalibrationScroll(stack, 1, createSpellScroll(mageLight));
            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            AutocastAmulet.setCalibrationScroll(stack, 2, createSpellScroll(remoteEye));
            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    2,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            AutocastAmulet.setCalibrationScroll(stack, 3, createSpellScroll(charge));

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Autocast Amulet spell storage should not expose Iron's SpellContainer");
            helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == 4,
                    "Autocast Amulet spell slot upgrade should enable 4 slots");
            assertAutocastSpellData(helper, stack, 0, apprenticeSpell, 1,
                    "Autocast Amulet slot upgrade should preserve the first spell");
            assertAutocastSpellData(helper, stack, 1, mageLight, 1,
                    "Autocast Amulet slot upgrade should preserve the second spell");
            assertAutocastSpellData(helper, stack, 2, remoteEye, 1,
                    "Autocast Amulet slot upgrade should append the third spell at the tail");
            assertAutocastSpellData(helper, stack, 3, charge, 1,
                    "Autocast Amulet slot upgrade should append the fourth spell at the tail");
        });
    }

    static void autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var remoteEye = SpellRegistry.REMOTE_EYE.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    3,
                    new SpellData(apprenticeSpell, 1),
                    new SpellData(mageLight, 1),
                    new SpellData(remoteEye, 1)
            );

            AutocastAmulet.setCalibrationAdjustment(stack, 1, ItemStack.EMPTY);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Autocast Amulet should keep scroll storage outside Iron's SpellContainer after removing an upgrade");
            helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == 2,
                    "Autocast Amulet should disable the removed upgrade slot");
            assertAutocastSpellData(helper, stack, 0, apprenticeSpell, 1,
                    "Autocast Amulet should keep the first enabled spell");
            assertAutocastSpellData(helper, stack, 1, mageLight, 1,
                    "Autocast Amulet should keep the second enabled spell");
            assertAutocastSpellData(helper, stack, 2, remoteEye, 1,
                    "Autocast Amulet should keep the spell stored in the disabled slot");
            helper.assertFalse(AutocastAmulet.isEnabledSpellSlot(stack, 2),
                    "Autocast Amulet should treat the stored third spell as disabled after removing an upgrade");
        });
    }
    static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var remoteEye = SpellRegistry.REMOTE_EYE.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    3,
                    new SpellData(apprenticeSpell, 1),
                    new SpellData(mageLight, 1),
                    new SpellData(remoteEye, 1)
            );
            var removedScroll = AutocastAmulet.getCalibrationScroll(stack, 2);
            helper.assertTrue(removedScroll.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Autocast Amulet should expose its stored tail scroll");
            AutocastAmulet.setCalibrationScroll(stack, 2, ItemStack.EMPTY);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Autocast Amulet should still not expose Iron's SpellContainer after scroll extraction");
            assertAutocastSpellData(helper, stack, 0, apprenticeSpell, 1,
                    "Autocast Amulet should keep the first spell after tail extraction");
            assertAutocastSpellData(helper, stack, 1, mageLight, 1,
                    "Autocast Amulet should keep the second spell after tail extraction");
            helper.assertTrue(AutocastAmulet.getSpellDataAt(stack, 2) == SpellData.EMPTY,
                    "Autocast Amulet should clear only the tail spell slot after extraction");
        });
    }
    static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_first_interval_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(200.0F);
            equipNecklaceCurio(player, stack);

            runAutocastAmuletServerTick(player, 19);
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet should stay idle before the first 20 tick interval");
            runAutocastAmuletServerTick(player, 20);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet should start casting charge on the first castable interval after equip");
            helper.assertTrue(spell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet should start the imbued charge spell on the first castable interval");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet should cast charge at the imbued spell level");
            helper.succeed();
        });
    }
    static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_mana_retry_test");

        helper.runAtTickTime(1, () -> {
            var expensiveSpell = SpellRegistry.SENSE_EVIL.get();
            var fallbackSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var expensiveCost = expensiveSpell.getManaCost(1);
            var fallbackCost = fallbackSpell.getManaCost(1);
            helper.assertTrue(expensiveCost > fallbackCost,
                    "Autocast Amulet mana retry test requires the first spell to cost more mana than the fallback spell");

            var stack = createAutocastAmuletStack(
                    helper,
                    2,
                    new SpellData(expensiveSpell, 1),
                    new SpellData(fallbackSpell, 1)
            );
            equipNecklaceCurio(player, stack);
            var equippedStack = getEquippedAutocastAmulet(player);

            var magicData = MagicData.getPlayerMagicData(player);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 8.0F));
            magicData.getSyncedData().learnSpell(expensiveSpell, false);
            magicData.getSyncedData().learnSpell(fallbackSpell, false);
            magicData.setMana(fallbackCost);

            runAutocastAmuletServerTick(player, 20);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should stop immediately when the first spell lacks mana");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell in the blocked mana sequence");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) == 80L,
                    "Autocast Amulet mana retry should wait exactly 60 ticks after the failed sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) == 0,
                    "Autocast Amulet mana retry should skip the errored slot once on the delayed retry");

            runAutocastAmuletServerTick(player, 40);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should not retry again before the delayed retry sequence");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell before the delayed retry sequence");

            runAutocastAmuletServerTick(player, 80);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet delayed retry should skip the errored slot and cast the fallback spell");
            helper.assertTrue(fallbackSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet delayed retry should cast the next spell after skipping the errored slot");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet delayed retry should cast charge at the imbued spell level");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) < 0L,
                    "Autocast Amulet should clear the delayed retry state after consuming the one-shot skipped sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) < 0,
                    "Autocast Amulet should clear the skipped slot marker after the delayed retry sequence");
            helper.succeed();
        });
    }
    static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_creative_mana_test");

        helper.runAtTickTime(1, () -> {
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            AutocastAmulet.setCalibrationScroll(stack, 0, createSpellScroll(spell));
            equipNecklaceCurio(player, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(0.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            runAutocastAmuletServerTick(player, 20);
            var cooldownInstance = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedAutocastCooldownWithoutSwordMultiplier(
                    spell,
                    player,
                    CastSource.SWORD
            ) + spell.getEffectiveCastTime(1, player);
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet creative test should still cast greater_heal with zero mana");
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet creative LONG cast should still complete immediately");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Autocast Amulet creative cast should not consume mana but got " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet creative cast should still add spell cooldown");
            helper.assertTrue(cooldownInstance != null && cooldownInstance.getSpellCooldown() == expectedCooldown,
                    "Autocast Amulet creative cast should store the cooldown without the sword multiplier but got "
                            + (cooldownInstance == null ? "null" : cooldownInstance.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void autocastAmuletCooldownIgnoresSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_cooldown_test");
            var stack = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Autocast Amulet cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var expectedCooldown = expectedAutocastCooldownWithoutSwordMultiplier(
                    spell,
                    player,
                    CastSource.SWORD
            );
            var ironsSwordCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    ironsSwordCooldown,
                    spell,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Autocast Amulet cooldown event should ignore the sword cooldown multiplier but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() != ironsSwordCooldown,
                    "Autocast Amulet cooldown event should differ from Iron's sword multiplier path");

            var originalSwordCooldownMultiplier = io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.get();
            try {
                io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.set(0.0D);
                var zeroMultiplierSwordCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                        spell,
                        player,
                        CastSource.SWORD
                );
                var zeroMultiplierCooldownEvent = new SpellCooldownAddedEvent.Pre(
                        zeroMultiplierSwordCooldown,
                        spell,
                        player,
                        CastSource.SWORD
                );
                jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onSpellCooldownAdded(
                        zeroMultiplierCooldownEvent
                );
                helper.assertTrue(zeroMultiplierSwordCooldown == 0,
                        "Iron's sword cooldown should be zero with a zero sword multiplier but got "
                                + zeroMultiplierSwordCooldown);
                helper.assertTrue(zeroMultiplierCooldownEvent.getEffectiveCooldown() == expectedCooldown,
                        "Autocast Amulet cooldown event should restore cooldown even with a zero sword multiplier but got "
                                + zeroMultiplierCooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
            } finally {
                io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.set(originalSwordCooldownMultiplier);
            }
        });
    }
    static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_long_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            AutocastAmulet.setCalibrationAdjustment(
                    stack,
                    0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
            );
            AutocastAmulet.setCalibrationScroll(stack, 0, createSpellScroll(spell));
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(300.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            helper.assertTrue(invokeAutocastBeginCast(
                            player,
                            magicData,
                            stack,
                            new SpellData(spell, 1),
                            1,
                            "necklace_0",
                            spell.getManaCost(1)
                    ),
                    "Autocast Amulet should start greater_heal from the auto-cast path");
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet should resolve greater_heal immediately from the auto-cast path");
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(),
                    "Autocast Amulet LONG cast should complete immediately instead of leaving the player casting");
            helper.succeed();
        });
    }
    static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");

            controller.queueCooldownCast(100L, spellId, icon, 1300);

            var active = controller.getActiveNotification();
            helper.assertTrue(active != null, "Autocast Amulet notification controller should show the cast notification immediately");
            if (active != null) {
                helper.assertTrue(active.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST,
                        "Autocast Amulet cast notification should use the CAST kind");
                helper.assertTrue(active.displaySeconds() == 65,
                        "Autocast Amulet cast notification should display the rounded cooldown seconds");
                helper.assertTrue("65s".equals(active.displayText()),
                        "Autocast Amulet cast notification text should include the seconds suffix");
            }

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 3,
                    "Autocast Amulet 65 second cooldown should schedule 60/30/10 notifications but got " + scheduled.size());
            if (scheduled.size() == 3) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 200L && scheduled.get(0).entry().displaySeconds() == 60,
                        "Autocast Amulet 60 second notification should trigger when 60 seconds remain");
                helper.assertTrue(scheduled.get(1).triggerTick() == 800L && scheduled.get(1).entry().displaySeconds() == 30,
                        "Autocast Amulet 30 second notification should trigger when 30 seconds remain");
                helper.assertTrue(scheduled.get(2).triggerTick() == 1200L && scheduled.get(2).entry().displaySeconds() == 10,
                        "Autocast Amulet 10 second notification should trigger when 10 seconds remain");
            }
        });
    }
    static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");

            controller.queueCooldownCast(0L, spellId, icon, 500);

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 1,
                    "Autocast Amulet 25 second cooldown should only schedule the 10 second notification but got " + scheduled.size());
            if (scheduled.size() == 1) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 300L,
                        "Autocast Amulet 25 second cooldown should trigger the 10 second notification after 15 seconds");
                helper.assertTrue(scheduled.get(0).entry().displaySeconds() == 10,
                        "Autocast Amulet short cooldown should keep the 10 second label");
            }
        });
    }

    static void autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "arcane_blast");
            var icon = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "textures/spells/arcane_blast.png");

            controller.queueCooldownCast(0L, spellId, icon, 99);

            helper.assertTrue(controller.getActiveNotification() == null,
                    "Autocast Amulet cooldowns under 5 seconds should not create a cast notification");
            helper.assertTrue(controller.getPendingQueueSize() == 0,
                    "Autocast Amulet cooldowns under 5 seconds should not queue a cast notification");
            helper.assertTrue(controller.getScheduledNotifications().isEmpty(),
                    "Autocast Amulet cooldowns under 5 seconds should not schedule threshold notifications");

            controller.queueCooldownCast(1L, spellId, icon, 100);
            var active = controller.getActiveNotification();
            helper.assertTrue(active != null
                            && active.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST
                            && active.displaySeconds() == 5,
                    "Autocast Amulet cooldowns of 5 seconds should still create a cast notification");
        });
    }

    static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var healId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var healIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");
            var chargeId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var chargeIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");
            var manaLowId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heal");
            var manaLowIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/heal.png");
            var delayedId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath");
            var delayedIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/fire_breath.png");

            controller.queueCooldownCast(0L, healId, healIcon, 1300);
            controller.queueCooldownCast(1L, chargeId, chargeIcon, 800);
            helper.assertTrue(controller.getPendingQueueSize() == 1,
                    "Autocast Amulet overlapping cast notifications should queue instead of drawing together");

            controller.advance(30L);
            var secondCast = controller.getActiveNotification();
            helper.assertTrue(secondCast != null && secondCast.spellId().equals(chargeId),
                    "Autocast Amulet queued cast notification should appear after the first cast display finishes");

            controller.queueManaLow(30L, manaLowId, manaLowIcon);
            controller.advance(60L);
            var manaLow = controller.getActiveNotification();
            helper.assertTrue(manaLow != null
                            && manaLow.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.MANA_LOW
                            && "MP!".equals(manaLow.displayText()),
                    "Autocast Amulet mana-low notification should use the dedicated minimal overlay text");

            controller.queueCooldownCast(85L, delayedId, delayedIcon, 400);
            controller.advance(100L);
            var stillBlockedByQueue = controller.getActiveNotification();
            helper.assertTrue(stillBlockedByQueue != null
                            && stillBlockedByQueue.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST
                            && stillBlockedByQueue.spellId().equals(delayedId),
                    "Autocast Amulet threshold notification should wait until earlier queued notifications finish");

            controller.advance(130L);
            var delayedThreshold = controller.getActiveNotification();
            helper.assertTrue(delayedThreshold != null
                            && delayedThreshold.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.THRESHOLD
                            && "60s".equals(delayedThreshold.displayText()),
                    "Autocast Amulet delayed threshold notification should keep the original 60 second label");
        });
    }

    static void autocastAmuletNotificationControllerUpdatesLinearBuildRemaining(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var linearController = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var linearId = ResourceLocation.fromNamespaceAndPath("apprenticecodex", "linear_build");
            var castId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var castIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");

            linearController.updateLinearBuildRemaining(0L, linearId, new ItemStack(Items.FERN), "10");
            linearController.updateLinearBuildRemaining(5L, linearId, new ItemStack(Items.FERN), "9");
            linearController.advance(34L);

            var activeLinear = linearController.getActiveNotification();
            helper.assertTrue(activeLinear != null
                            && activeLinear.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.LINEAR_BUILD_REMAINING
                            && "9".equals(activeLinear.displayText()),
                    "Linear Build remaining notification should update the active entry and refresh its display duration");

            var queuedController = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            queuedController.queueCooldownCast(0L, castId, castIcon, 1200);
            queuedController.updateLinearBuildRemaining(1L, linearId, new ItemStack(Items.FERN), "10");
            queuedController.updateLinearBuildRemaining(2L, linearId, new ItemStack(Items.FERN), "9");
            helper.assertTrue(queuedController.getPendingQueueSize() == 1,
                    "Linear Build remaining notifications should keep only the latest pending entry");

            queuedController.advance(30L);
            var queuedLinear = queuedController.getActiveNotification();
            helper.assertTrue(queuedLinear != null
                            && queuedLinear.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.LINEAR_BUILD_REMAINING
                            && "9".equals(queuedLinear.displayText()),
                    "Linear Build remaining notification should show the latest queued value after earlier notifications finish");
        });
    }

    private static void assertAutocastSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = AutocastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
    }

    private static int expectedAutocastCooldownWithoutSwordMultiplier(
            AbstractSpell spell,
            net.minecraft.world.entity.player.Player player,
            CastSource castSource
    ) {
        return jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldownWithoutSwordMultiplier(
                spell,
                player,
                castSource
        );
    }
}
