package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.datagen.spell.RemoteOwnerCastSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexRemoteOwnerCastGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String CONFIG_BATCH = "apprenticecodex.remote_owner_cast_config";

    private ApprenticeCodexRemoteOwnerCastGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastProfileResolvesAllowedOrigin(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource();
        var profile = new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                false
        );

        try (var ignored = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(spellId, profile))) {
            helper.assertTrue(RemoteOwnerCastProfileManager.getUsableProfile(
                            jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(),
                            RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT
                    ).isPresent(),
                    "Remote Owner Cast profile should resolve for an allowed origin.");
            helper.assertTrue(RemoteOwnerCastProfileManager.getUsableProfile(
                            jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(),
                            RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
                    ).isEmpty(),
                    "Remote Owner Cast profile should not resolve for a disallowed origin.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = CONFIG_BATCH)
    public static void remoteOwnerCastConfigDenylistBlocksFollowcast(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource();
        var profile = RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY;

        try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(spellId, profile));
             var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                     true,
                     false,
                     List.of(),
                     List.of(spellId.toString()),
                     true,
                     true
             )) {
            var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
            helper.assertFalse(amulet.canImbueSpell(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(), 1),
                    "Remote Owner Cast denylist should block Satellite Followcast Amulet imbue.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = CONFIG_BATCH)
    public static void remoteOwnerCastConfigDowngradesGeometryBeforeCast(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource();

        try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                spellId,
                RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY
        )); var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                true,
                true,
                List.of(),
                List.of(),
                true,
                true
        )) {
            var profile = RemoteOwnerCastProfileManager.getUsableProfile(
                    jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(),
                    RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
            );
            helper.assertTrue(profile.isPresent() && profile.get().castMode() == RemoteOwnerCastMode.PROXY_OWNER_MAGIC,
                    "forceProxyOwnerMagic should downgrade remote_player_geometry before casting.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastFollowcastRejectsRecastSpell(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        helper.assertFalse(amulet.canImbueSpell(SpellRegistry.RAISE_DEAD_SPELL.get(), 1),
                "Satellite Followcast Amulet should keep rejecting recast spells.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastDatagenIncludesRaiseDeadImpactProfile(GameTestHelper helper) {
        var raiseDeadId = requireId(SpellRegistry.RAISE_DEAD_SPELL.getId());
        var profile = RemoteOwnerCastSpellProfileDataGenerator.createProfileDefinitions().stream()
                .filter(definition -> definition.spell().equals(raiseDeadId))
                .map(definition -> definition.profile())
                .findFirst();

        helper.assertTrue(profile.isPresent(), "Remote Owner Cast datagen should include Raise Dead.");
        helper.assertTrue(profile.get().allowsOrigin(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT),
                "Raise Dead should resolve for Charged Twin Blade Staff impact.");
        helper.assertTrue(profile.get().allowInitialRecast(),
                "Raise Dead should inherit the initial recast allowance from the staff profile.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastDatagenUsesAnchorOwnerForConeProfiles(GameTestHelper helper) {
        for (var spellId : List.of(
                requireId(SpellRegistry.DRAGON_BREATH_SPELL.getId()),
                requireId(SpellRegistry.FIRE_BREATH_SPELL.getId()),
                requireId(SpellRegistry.CONE_OF_COLD_SPELL.getId()),
                requireId(SpellRegistry.ELECTROCUTE_SPELL.getId()),
                requireId(SpellRegistry.POISON_BREATH_SPELL.getId())
        )) {
            var profile = RemoteOwnerCastSpellProfileDataGenerator.createProfileDefinitions().stream()
                    .filter(definition -> definition.spell().equals(spellId))
                    .map(definition -> definition.profile())
                    .findFirst();
            helper.assertTrue(profile.isPresent(),
                    "Remote Owner Cast datagen should include cone spell: " + spellId);
            helper.assertTrue(profile.get().castMode() == RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC,
                    "Cone spell should use Remote Owner anchor owner mode: " + spellId);
        }

        helper.succeed();
    }

    private static ResourceLocation requireId(ResourceLocation id) {
        if (id == null) {
            throw new IllegalStateException("Missing spell id");
        }
        return id;
    }
}
