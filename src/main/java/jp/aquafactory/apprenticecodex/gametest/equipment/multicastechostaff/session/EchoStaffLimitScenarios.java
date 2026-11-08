package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.session;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.completeMulticastEchoStaffCast;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EchoStaffLimitScenarios {
    private EchoStaffLimitScenarios() {}

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
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
                MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
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
                var effect = player.getEffect(EffectRegistry.ECHO_SPELL.get());
                helper.assertTrue(effect != null && effect.getAmplifier() == 0,
                        "Echo Cast should store the maximum amplifier for a one-cast limit");
                helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "Echo Cast should reject casts while already at the configured maximum");
            }
        });
    }
}
