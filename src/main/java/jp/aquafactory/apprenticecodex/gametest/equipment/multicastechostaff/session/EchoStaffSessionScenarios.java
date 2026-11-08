package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.session;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.completeMulticastEchoStaffCast;
import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.expectedMulticastEchoStaffCooldown;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EchoStaffSessionScenarios {
    private EchoStaffSessionScenarios() {}

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
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL.get()),
                    "Multicast Echo Staff should consume EchoSpell after the normal cast succeeds");
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Normal cast should consume mana before delayed multicast: " + magicData.getMana());
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Normal cast cooldown should be suppressed until multicast finishes");
        });

        helper.runAtTickTime(2, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Multicast Echo Staff should not fire before the configured delay");
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player))
        );
        helper.runAtTickTime(4, () -> {
            var originalCreativeCooldown = ServerConfigs.CREATIVE_COOLDOWN.get();
            try {
                ServerConfigs.CREATIVE_COOLDOWN.set(false);
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            } finally {
                ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
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
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
}
