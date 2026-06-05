package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.datagen.spell.RemoteOwnerCastSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SpellDispenserSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorAttributes;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastContext;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexRemoteOwnerCastGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String CONFIG_BATCH = "apprenticecodex.remote_owner_cast_config";
    private static final String INSCRIBE_ICE_ISOLATED_BATCH =
            "apprenticecodex.remote_owner_cast_inscribe_ice_isolated";

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
    public static void remoteOwnerCastConfigDenylistDoesNotBlockFollowcastImbue(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource();
        var profile = RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY;

        try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(spellId, profile));
             var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                     true,
                     List.of(spellId.toString())
             )) {
            var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
            helper.assertTrue(amulet.canImbueSpell(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(), 1),
                    "Remote Owner Cast denylist should block runtime casts, not profile-based Imbue.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = CONFIG_BATCH)
    public static void remoteOwnerCastGeometryConfigKeepsProfileIdentity(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get().getSpellResource();

        try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                spellId,
                RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY
        )); var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                true,
                List.of()
        )) {
            var profile = RemoteOwnerCastProfileManager.getUsableProfile(
                    jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get(),
                    RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
            );
            helper.assertTrue(profile.isPresent() && profile.get().castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                    "Remote Owner Cast config should not rewrite profile cast modes.");
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
    public static void remoteOwnerCastContextOverridesRotationApis(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "remote_owner_rotation_context_test"));
        player.setYRot(90.0F);
        player.setYHeadRot(90.0F);
        player.setYBodyRot(90.0F);
        player.setXRot(10.0F);

        var remoteForward = new Vec3(1.0D, -0.25D, 0.5D).normalize();
        try (var ignored = RemoteOwnerCastContext.push(
                player,
                new Vec3(3.0D, 5.0D, 7.0D),
                remoteForward,
                RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT
        )) {
            assertVecClose(helper, player.getLookAngle(), remoteForward,
                    "Remote Owner Cast context should override getLookAngle");
            assertVecClose(helper, player.getViewVector(1.0F), remoteForward,
                    "Remote Owner Cast context should override getViewVector");
            assertVecClose(helper, player.getForward(), remoteForward,
                    "Remote Owner Cast context should override getForward");
            assertVecClose(helper, Vec3.directionFromRotation(player.getXRot(), player.getYRot()).normalize(), remoteForward,
                    "Remote Owner Cast context should override getXRot/getYRot consistently");
        }

        helper.assertTrue(Math.abs(player.getYRot() - 90.0F) < 1.0E-4F,
                "Remote Owner Cast context should restore the original yaw after close: " + player.getYRot());
        helper.assertTrue(Math.abs(player.getXRot() - 10.0F) < 1.0E-4F,
                "Remote Owner Cast context should restore the original pitch after close: " + player.getXRot());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastAnchorRegistersAndSyncsIronAttributes(GameTestHelper helper) {
        var owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "remote_owner_anchor_attribute_test"));
        var anchor = new RemoteOwnerCastAnchorEntity(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), helper.getLevel());
        var expectedValues = new LinkedHashMap<Attribute, Double>();

        for (var schoolType : SchoolRegistry.REGISTRY.get().getValues()) {
            var schoolPower = MagicTools.resolveSchoolPowerAttribute(schoolType);
            helper.assertTrue(schoolPower != null,
                    "Remote Owner Cast should resolve school power attribute for " + schoolType.getId());
            var schoolResist = MagicTools.resolveSchoolResistAttribute(schoolType);
            helper.assertTrue(schoolResist != null,
                    "Remote Owner Cast should resolve school resist attribute for " + schoolType.getId());

            expectedValues.putIfAbsent(schoolPower, 1.25D + expectedValues.size() * 0.01D);
            expectedValues.putIfAbsent(schoolResist, 1.25D + expectedValues.size() * 0.01D);
        }

        helper.assertTrue(!expectedValues.isEmpty(),
                "Remote Owner Cast dynamic school attribute test should find registered Iron's schools");
        for (var entry : expectedValues.entrySet()) {
            setAttributeBaseValue(helper, owner, entry.getKey(), entry.getValue(), "owner school attribute");
            helper.assertTrue(anchor.getAttribute(entry.getKey()) != null,
                    "Remote Owner Cast anchor should register dynamic school attribute: " + entry.getKey());
        }

        RemoteOwnerCastAnchorAttributes.syncFromOwner(owner, anchor);
        for (var entry : expectedValues.entrySet()) {
            assertAttributeValue(helper, anchor, entry.getKey(), entry.getValue(),
                    "Remote Owner Cast anchor should copy dynamic school attribute");
        }
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

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastDatagenUsesAnchorOwnerForSummonWeaponProfiles(GameTestHelper helper) {
        var remoteOwnerProfiles = RemoteOwnerCastSpellProfileDataGenerator.createProfileDefinitions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        definition -> definition.spell(),
                        definition -> definition.profile()
                ));
        var spellDispenserProfileSpells = SpellDispenserSpellProfileDataGenerator.createProfileDefinitions().stream()
                .map(definition -> definition.spell())
                .collect(java.util.stream.Collectors.toSet());
        var assertedAny = false;

        for (var spellEntry : jp.aquafactory.apprenticecodex.registry.SpellRegistry.SPELLS.getEntries()) {
            var spell = spellEntry.get();
            var spellId = spell.getSpellResource();
            if (!(spell instanceof AbstractSummonWeaponSpell) || !spellDispenserProfileSpells.contains(spellId)) {
                continue;
            }

            assertedAny = true;
            var profile = Optional.ofNullable(remoteOwnerProfiles.get(spellId));
            helper.assertTrue(profile.isPresent(),
                    "Remote Owner Cast datagen should include summon weapon spell: " + spellId);
            helper.assertTrue(profile.get().castMode() == RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC,
                    "Owner-following summon weapon should use Remote Owner anchor owner mode: " + spellId);
        }

        helper.assertTrue(assertedAny, "Remote Owner Cast datagen test did not find summon weapon profiles.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastDatagenIncludesInscribeIceProfile(GameTestHelper helper) {
        var spellId = jp.aquafactory.apprenticecodex.registry.SpellRegistry.INSCRIBE_ICE.get().getSpellResource();
        var profile = RemoteOwnerCastSpellProfileDataGenerator.createProfileDefinitions().stream()
                .filter(definition -> definition.spell().equals(spellId))
                .map(definition -> definition.profile())
                .findFirst();

        helper.assertTrue(profile.isPresent(), "Remote Owner Cast datagen should include Inscribe Ice.");
        helper.assertTrue(profile.get().castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                "Inscribe Ice should use remote player geometry so the real owner remains the projectile owner.");
        helper.assertTrue(profile.get().allowsOrigin(RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST),
                "Inscribe Ice should be available to Satellite Followcast RemoteOwnerCast.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH, timeoutTicks = 40)
    public static void remoteOwnerCastContextKeepsInscribeIceJobGeometry(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "inscribe_ice_remote_owner_test"));
        owner.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var ownerPos = helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D));
        owner.setPos(ownerPos.x, ownerPos.y, ownerPos.z);
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        level.addFreshEntity(owner);

        var spell = (InscribeIce) jp.aquafactory.apprenticecodex.registry.SpellRegistry.INSCRIBE_ICE.get();
        var remoteOrigin = helper.absoluteVec(new Vec3(2.5D, 4.5D, 2.5D));
        var remoteForward = new Vec3(0.0D, 1.0D, 0.0D);
        var spawnedDaggers = new ArrayList<InscribeIceDaggerEntity>();
        java.util.function.Consumer<EntityJoinLevelEvent> daggerListener = event -> {
            if (event.getLevel() == level
                    && event.getEntity() instanceof InscribeIceDaggerEntity dagger
                    && dagger.getOwner() == owner) {
                spawnedDaggers.add(dagger);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(daggerListener);
        try (var ignored = RemoteOwnerCastContext.push(
                owner,
                remoteOrigin,
                remoteForward,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
        )) {
            spell.onCast(level, 5, owner, CastSource.SWORD, MagicData.getPlayerMagicData(owner));
        }

        owner.setYRot(90.0F);
        owner.setXRot(0.0F);

        helper.runAfterDelay(4, () -> {
            try {
                var expectedCount = spell.getProjectileCount(5, owner);
                helper.assertTrue(spawnedDaggers.size() == expectedCount,
                        "Inscribe Ice RemoteOwnerCast should finish the short throw job: "
                                + spawnedDaggers.size() + " / " + expectedCount);
                for (var dagger : spawnedDaggers) {
                    var movement = dagger.getDeltaMovement();
                    helper.assertTrue(movement.y > 0.5D,
                            "Inscribe Ice RemoteOwnerCast should keep the remote vertical forward direction after context closes: "
                                    + movement);
                    helper.assertTrue(Math.abs(movement.z) < InscribeIceDaggerEntity.SPEED * 0.15D,
                            "Inscribe Ice RemoteOwnerCast should keep the cast-time right direction for vertical launches: "
                                    + movement);
                }
                helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(daggerListener);
            }
        });
    }

    private static void setAttributeBaseValue(
            GameTestHelper helper,
            FakePlayer player,
            Attribute attribute,
            double value,
            String context
    ) {
        var instance = player.getAttribute(attribute);
        helper.assertTrue(instance != null, "Missing " + context + " attribute on test player");
        instance.setBaseValue(value);
    }

    private static void assertAttributeValue(
            GameTestHelper helper,
            RemoteOwnerCastAnchorEntity anchor,
            Attribute attribute,
            double expected,
            String message
    ) {
        var instance = anchor.getAttribute(attribute);
        helper.assertTrue(instance != null, message + ": missing attribute");
        helper.assertTrue(Math.abs(instance.getValue() - expected) < 1.0E-6D,
                message + ": " + instance.getValue() + " / expected " + expected);
    }

    private static void assertVecClose(GameTestHelper helper, Vec3 actual, Vec3 expected, String message) {
        helper.assertTrue(actual.distanceTo(expected) < 2.0E-4D,
                message + ": " + actual + " / expected " + expected);
    }

    private static ResourceLocation requireId(ResourceLocation id) {
        if (id == null) {
            throw new IllegalStateException("Missing spell id");
        }
        return id;
    }
}
