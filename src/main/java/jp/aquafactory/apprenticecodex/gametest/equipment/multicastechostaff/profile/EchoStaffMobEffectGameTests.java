package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.profile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffFixture.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffFixture.manualPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffMobEffectGameTests {
    private EchoStaffMobEffectGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = manualPlayer(helper, "multicast_echo_staff_effect_duration_test");
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

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = manualPlayer(helper, "multicast_echo_staff_effect_amp_test");
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

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileIgnoresMissingProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = manualPlayer(helper, "multicast_echo_staff_effect_missing_profile_test");
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
