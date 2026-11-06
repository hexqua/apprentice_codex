package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import java.util.Map;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static jp.aquafactory.apprenticecodex.gametest.spell.senseevil.SenseEvilScenarios.spawnPositionedZombie;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareWideSearchIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class MulticastEchoStaffScenarios {
    private MulticastEchoStaffScenarios() {}

    static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_delay_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Multicast Echo Staff should consume EchoSpell after the normal cast succeeds");
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Normal cast should consume mana before delayed multicast: " + magicData.getMana());
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Normal cast cooldown should be suppressed until multicast finishes");
        });

        helper.runAtTickTime(2, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Multicast Echo Staff should not fire before the configured delay");
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost * 2.0F)) < 1.0e-4F,
                    "Delayed multicast should consume mana once: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Multicast Echo Staff should apply the final penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffCreativeCastSkipsFinalCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_creative_cooldown_test");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> completeMulticastEchoStaffCast(
                helper.getLevel(),
                player,
                staffStack,
                spell,
                spellLevel,
                0,
                manaCost * 3.0F
        ));
        helper.runAtTickTime(2, () ->
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player))
        );
        helper.runAtTickTime(4, () -> {
            var originalCreativeCooldown = ServerConfigs.CREATIVE_COOLDOWN.get();
            try {
                ServerConfigs.CREATIVE_COOLDOWN.set(false);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            } finally {
                ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
                ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            }
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Multicast Echo Staff creative finalization should respect disabled creative cooldowns");
            helper.succeed();
        });
    }

    static void multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_mana_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 1;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Insufficient mana interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Insufficient mana interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffItemChangeEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_item_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Item change interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Item change interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLogoutEndsWithPenaltyCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_logout_penalty_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            MulticastEchoStaffCastHelper.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));

            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Logout interruption should not run an additional multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Logout interruption should keep the full amplifier penalty cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_cast_time_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.ARCANE_BLAST.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Multicast Echo Staff should add skipped cast time to final cooldown: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_fortify_indicator_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FORTIFY_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));

            var magicData = MagicData.getPlayerMagicData(player);
            var targetAreas = helper.getLevel().getEntitiesOfClass(
                    TargetedAreaEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    targetArea -> !targetArea.isRemoved()
            );

            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Repeated Fortify multicast should clear temporary cast data after sending cast data to the client");
            helper.assertTrue(targetAreas.isEmpty(),
                    "Repeated Fortify multicast should discard temporary target area indicators: " + targetAreas.size());
            helper.succeed();
        });
    }

    static void multicastEchoStaffInvalidInstantCastKeepsEchoSpell(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_invalid_instant_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.ACUPUNCTURE_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        player.setYRot(-90.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 3.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Acupuncture should find the target before it is removed");
            target.discard();
            player.setYRot(90.0F);

            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Invalid instant casts should not consume EchoSpell");
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should only pay the normal cast mana cost: " + magicData.getMana());
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should not start delayed multicast: " + magicData.getMana());
            helper.succeed();
        });
    }

    static void multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 40, 0);
        prepareWideSearchIsolationArea(helper, playerPos);
        var player = createEquipmentTestPlayer(helper, playerPos, "multicast_echo_staff_invalid_long_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 41.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should find the elevated target before it is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Long cast test should remove EchoSpell before the normal cast completes");
            target.moveTo(helper.absoluteVec(new Vec3(-12.5D, 41.0D, 0.5D)));
            target.discard();
            player.setYRot(90.0F);
            player.setYHeadRot(90.0F);
            player.setYBodyRot(90.0F);
        });

        helper.runAtTickTime(3, () -> {
            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
        });

        helper.runAtTickTime(6, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should not spend mana on delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Invalid long cast should keep the normal cooldown instead of the multicast penalty: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    static void multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 40, 0);
        prepareWideSearchIsolationArea(helper, playerPos);
        var player = createEquipmentTestPlayer(helper, playerPos, "multicast_echo_staff_long_context_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
            spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 41.0D, 0.5D)));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should store a valid target before EchoSpell is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Long cast context test should remove EchoSpell before the normal cast completes");

            finishStartedSpellCast(helper.getLevel(), player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Valid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
            magicData.setAdditionalCastData(null);
        });

        helper.succeedWhen(() -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost * 2.0F)) < 1.0e-4F,
                    "Valid long cast should use the start EchoSpell context for delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Valid long cast should apply the multicast penalty cooldown from the start context: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Delayed multicast pre-cast data should be restored after the repeated cast");
        });
    }

    static void multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_duration_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    MulticastEchoStaffMobEffectProfile.DEFAULT_DURATION_EXTENSION
            ))) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 0))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getDuration() == 120,
                    "Duplicate multicast mob effect should extend duration by 50% of attempted duration: "
                            + (result == null ? "null" : result.getDuration()));
        });
    }

    static void multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_amp_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            var profile = new MulticastEchoStaffMobEffectProfile(0.0D, 0, 6000, 0.5D, 0, 2);
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 1))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getAmplifier() == 2,
                    "Duplicate multicast mob effect should stack amplifier from attempted level: "
                            + (result == null ? "null" : result.getAmplifier()));
        });
    }

    static void multicastEchoStaffMobEffectProfileIgnoresMissingProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_effect_missing_profile_test");
            var spell = SpellRegistry.SHOCK.get();
            var effect = MobEffects.MOVEMENT_SPEED;
            player.addEffect(new MobEffectInstance(effect, 100, 0));

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
                    true,
                    true,
                    true,
                    true,
                    false,
                    0,
                    false,
                    0
            ); var ignoredProfile = MulticastEchoStaffMobEffectProfileManager.useProfilesForGameTest(Map.of())) {
                MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> player.addEffect(new MobEffectInstance(effect, 40, 0))
                );
            }

            var result = player.getEffect(effect);
            helper.assertTrue(result != null && result.getDuration() == 100,
                    "Missing multicast mob effect profile should leave vanilla duplicate handling unchanged: "
                            + (result == null ? "null" : result.getDuration()));
        });
    }

    static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_direct_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.SHOCK.get();
            var profile = new MulticastEchoStaffAttackProfile(0.25D, true, false, true, 0, 1);
            var source = CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.SHOCK);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> {
                            target.invulnerableTime = 20;
                            CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                        }
                );
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 4.0F,
                    "Direct CombatTools multicast damage should use the profile multiplier before resistance: " + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Direct CombatTools multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }

    static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_projectile_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.MANA_SLASH.get();
            var profile = new MulticastEchoStaffAttackProfile(0.5D, true, true, false, 20, 1);
            var projectile = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), helper.getLevel(), player);
            var source = CombatTools.getDamageSource(helper.getLevel(), projectile, player, DamageTypes.MANA_SLASH);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> helper.getLevel().addFreshEntity(projectile)
                );
                target.invulnerableTime = 20;
                CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 6.0F,
                    "Tracked projectile multicast damage should use the profile multiplier after cast context closes: "
                            + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Tracked projectile multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }

    static void multicastEchoStaffCooldownCapLimitsAdjustedCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_cap_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        var cooldownCapTicks = 300;
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            override[0] = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    cooldownCapTicks,
                    10
            );
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            try {
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                var magicData = MagicData.getPlayerMagicData(player);
                var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());

                helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == cooldownCapTicks,
                        "Multicast Echo Staff adjusted cooldown should be capped: "
                                + (cooldown == null ? "null" : cooldown.getSpellCooldown()));
                helper.succeed();
            } finally {
                if (override[0] != null) {
                    override[0].close();
                }
            }
        });
    }

    static void multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_base_above_cap_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SHOCK.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        var cooldownCapTicks = 10;
        var override = new ApprenticeCodexServerConfig.GameTestConfigOverride[1];
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            override[0] = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    cooldownCapTicks,
                    10
            );
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, initialMana);
        });

        helper.runAtTickTime(4, () -> {
            try {
                MulticastEchoStaffCastHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                var magicData = MagicData.getPlayerMagicData(player);
                var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
                var expectedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);

                helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                        "Multicast Echo Staff should preserve original cooldowns above the cap: "
                                + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                                + " / expected " + expectedCooldown);
                helper.succeed();
            } finally {
                if (override[0] != null) {
                    override[0].close();
                }
            }
        });
    }

    static void echoCastStopsAtConfiguredMulticastLimit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useMulticastEchoStaffConfigOverrideForGameTest(
                    2,
                    1.2D,
                    1.0D,
                    12000,
                    1
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "echo_cast_max_limit_test");
                var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
                var spell = SpellRegistry.ECHO_CAST.get();
                var magicData = MagicData.getPlayerMagicData(player);
                player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

                helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "Echo Cast should allow the cast that reaches the configured maximum");
                spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
                var effect = player.getEffect(EffectRegistry.ECHO_SPELL);
                helper.assertTrue(effect != null && effect.getAmplifier() == 0,
                        "Echo Cast should store the maximum amplifier for a one-cast limit");
                helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "Echo Cast should reject casts while already at the configured maximum");
            }
        });
    }

    static void prepareMulticastEchoStaffCast(
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.setPlayerCastingItem(staffStack);
        magicData.setMana(mana);
        player.addEffect(new MobEffectInstance(EffectRegistry.ECHO_SPELL, 200, amplifier));
    }

    public static void completeMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        beginMulticastEchoStaffCast(level, player, staffStack, spell, spellLevel, amplifier, mana);
        finishStartedSpellCast(level, player, spell, spellLevel);
    }

    public static void beginMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        prepareMulticastEchoStaffCast(player, staffStack, spell, spellLevel, amplifier, mana);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(staffStack);
        spell.onServerPreCast(level, spellLevel, player, magicData);
    }

    public static void finishStartedSpellCast(
            ServerLevel level,
            FakePlayer player,
            AbstractSpell spell,
            int spellLevel
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        spell.castSpell(level, spellLevel, player, CastSource.SPELLBOOK, true);
        spell.onServerCastComplete(level, spellLevel, player, magicData, false);
    }

    static int expectedMulticastEchoStaffCooldown(
            AbstractSpell spell,
            ServerPlayer player,
            CastSource castSource,
            int amplifier
    ) {
        var baseCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        var cooldownCapTicks = ApprenticeCodexServerConfig.multicastEchoStaffCooldownCapTicks();
        if (baseCooldown > cooldownCapTicks) {
            return baseCooldown;
        }

        var cooldownComponent = (amplifier + 2)
                * ApprenticeCodexServerConfig.multicastEchoStaffCooldownMultiplier()
                * baseCooldown;
        var castTimeComponent = (amplifier + 1)
                * ApprenticeCodexServerConfig.multicastEchoStaffCastTimeCooldownMultiplier()
                * spell.getEffectiveCastTime(1, player);
        return Math.min(cooldownCapTicks, (int) Math.ceil(cooldownComponent + castTimeComponent));
    }
}
