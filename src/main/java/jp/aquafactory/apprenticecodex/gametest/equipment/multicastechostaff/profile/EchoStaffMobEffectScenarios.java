package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.profile;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EchoStaffMobEffectScenarios {
    private EchoStaffMobEffectScenarios() {}

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
}
