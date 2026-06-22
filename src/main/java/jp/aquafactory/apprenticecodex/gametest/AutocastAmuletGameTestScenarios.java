package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

final class AutocastAmuletGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private AutocastAmuletGameTestScenarios() {
    }

    static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var spellContainer = ISpellContainer.get(stack);
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var necklaceTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT)
            );

            helper.assertTrue(spellContainer != null, "Autocast Amulet default spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Autocast Amulet default slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Autocast Amulet should stay hidden from the spell wheel");
            helper.assertTrue(stack.is(necklaceTag),
                    "Autocast Amulet should be tagged as curios:necklace");
            helper.assertTrue(item.canImbueSpell(apprenticeSpell, 1),
                    "Autocast Amulet should allow sense_evil by default");
            helper.assertTrue(item.canImbueSpell(ironsHeal, 1),
                    "Autocast Amulet should allow Iron's heal by default");
            helper.assertFalse(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Autocast Amulet should reject non-allowlisted spells");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(apprenticeSpell.getSpellResource()),
                    "Autocast Amulet allowlist should contain sense_evil");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(ironsHeal.getSpellResource()),
                    "Autocast Amulet allowlist should contain Iron's heal");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().size() == 19,
                    "Autocast Amulet default allowlist size mismatch: " + AutocastAmuletSpellListManager.getAllowlist().size());
        });
    }
    static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var mutable = ISpellContainer.create(5, false, false).mutableCopy();

            helper.assertTrue(mutable.addSpellAtIndex(apprenticeSpell, 1, 0, false),
                    "Failed to prepare allowlisted sense_evil for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 1, false),
                    "Failed to prepare blocked magic_missile for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsHeal, 1, 2, false),
                    "Failed to prepare allowlisted heal for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsGreaterHeal, 1, 3, false),
                    "Failed to prepare allowlisted greater_heal for Autocast Amulet normalization test");
            ISpellContainer.set(stack, mutable.toImmutable());

            item.normalizeImbuedSpellContainer(stack);

            var normalized = ISpellContainer.get(stack);
            helper.assertTrue(normalized != null, "Autocast Amulet normalized spell container is null");
            helper.assertTrue(normalized != null && normalized.getMaxSpellCount() == 3,
                    "Autocast Amulet normalization should clamp slot count to 3 but got " + (normalized == null ? -1 : normalized.getMaxSpellCount()));
            helper.assertTrue(normalized != null && normalized.getActiveSpellCount() == 3,
                    "Autocast Amulet normalization should keep only 3 allowlisted spells but got " + (normalized == null ? -1 : normalized.getActiveSpellCount()));
            assertSpellData(helper, normalized, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet normalization should preserve the first allowlisted spell");
            assertSpellData(helper, normalized, 1, ironsHeal, 1, false,
                    "Autocast Amulet normalization should compact later allowlisted spells");
            assertSpellData(helper, normalized, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet normalization should preserve allowlisted order after filtering");

            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(1) - 1.0D) < 1.0e-9D,
                    "Autocast Amulet single-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(2) - 1.44D) < 1.0e-9D,
                    "Autocast Amulet two-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(3) - 1.96D) < 1.0e-9D,
                    "Autocast Amulet three-spell mana multiplier regression");
            helper.assertTrue(AutocastAmulet.getScaledManaCost(ironsHeal, 1, 3) == 59,
                    "Autocast Amulet scaled mana cost should round heal to 59 at 3 active spells");
        });
    }
    static void autocastAmuletSpellSlotUpgradeStopsAtThreeAndKeepsOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(apprenticeSpell, 1)
            );

            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the first lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsHeal, 1));
            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the second lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsGreaterHeal, 1));

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Autocast Amulet upgraded spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 3,
                    "Autocast Amulet spell slot upgrade should stop at 3 slots");
            assertSpellData(helper, spellContainer, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet slot upgrade should preserve the first spell");
            assertSpellData(helper, spellContainer, 1, ironsHeal, 1, false,
                    "Autocast Amulet slot upgrade should preserve the second spell");
            assertSpellData(helper, spellContainer, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet slot upgrade should append the third spell at the tail");
            helper.assertTrue(item.createSpellSlotUpgradeResult(stack, upgradeItem).isEmpty(),
                    "Autocast Amulet should reject a fourth spell slot upgrade");
        });
    }
    static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    3,
                    new SpellData(apprenticeSpell, 1),
                    new SpellData(ironsHeal, 1),
                    new SpellData(ironsGreaterHeal, 1)
            );
            var spellContainer = ISpellContainer.get(stack);

            helper.assertTrue(spellContainer != null, "Autocast Amulet workbench extraction spell container is null");
            var extractionIndex = item.getWorkbenchSpellExtractionIndex(stack, spellContainer);
            helper.assertTrue(extractionIndex == 2,
                    "Autocast Amulet workbench extraction should target the last filled slot but got " + extractionIndex);
            helper.assertTrue(item.canRemoveWorkbenchSpell(stack, spellContainer, extractionIndex, spellContainer.getSpellAtIndex(extractionIndex)),
                    "Autocast Amulet should allow removing its tail spell in Spellcaster Workbench");

            var mutable = spellContainer.mutableCopy();
            helper.assertTrue(mutable.removeSpellAtIndex(extractionIndex),
                    "Autocast Amulet tail spell should be removable from the mutable container");
            ISpellContainer.set(stack, mutable.toImmutable());
            item.normalizeImbuedSpellContainer(stack);

            var remaining = ISpellContainer.get(stack);
            helper.assertTrue(remaining != null, "Autocast Amulet remaining spell container is null after extraction");
            helper.assertTrue(remaining != null && remaining.getMaxSpellCount() == 3,
                    "Autocast Amulet should preserve max slot count after extraction");
            helper.assertTrue(remaining != null && remaining.getActiveSpellCount() == 2,
                    "Autocast Amulet should keep the first two spells after tail extraction");
            assertSpellData(helper, remaining, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet should keep the first spell after tail extraction");
            assertSpellData(helper, remaining, 1, ironsHeal, 1, false,
                    "Autocast Amulet should keep the second spell after tail extraction");
            helper.assertTrue(remaining != null && remaining.getSpellAtIndex(2) == SpellData.EMPTY,
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
            var expensiveSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var fallbackSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var expensiveCost = AutocastAmulet.getScaledManaCost(expensiveSpell, 1, 2);
            var fallbackCost = AutocastAmulet.getScaledManaCost(fallbackSpell, 1, 2);
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
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            equipNecklaceCurio(player, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(0.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            runAutocastAmuletServerTick(player, 20);
            var cooldownInstance = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    getEquippedAutocastAmulet(player)
            );
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet creative test should still cast greater_heal with zero mana");
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet creative LONG cast should still complete immediately");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Autocast Amulet creative cast should not consume mana but got " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet creative cast should still add spell cooldown");
            helper.assertTrue(cooldownInstance != null && cooldownInstance.getSpellCooldown() == expectedCooldown,
                    "Autocast Amulet creative cast should store the helper cooldown amount but got "
                            + (cooldownInstance == null ? "null" : cooldownInstance.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }
    static void autocastAmuletCooldownUsesHelperAmountWithoutSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_cooldown_test");
            var stack = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Autocast Amulet cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    stack
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Autocast Amulet cooldown event should use the helper cooldown amount but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            var swordCooldownMultiplier = io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue();
            if (swordCooldownMultiplier != 1.0F) {
                helper.assertTrue(
                        cooldownEvent.getEffectiveCooldown() != io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                                spell,
                                player,
                                CastSource.SWORD
                        ),
                        "Autocast Amulet cooldown event should diverge from Iron's sword multiplier path when the config multiplier is not 1"
                );
            }
        });
    }
    static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_long_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
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
                            AutocastAmulet.getScaledManaCost(spell, 1, 1)
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
}
