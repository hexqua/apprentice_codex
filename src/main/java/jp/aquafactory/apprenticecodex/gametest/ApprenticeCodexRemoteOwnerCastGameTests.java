package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.datagen.spell.RemoteOwnerCastSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SpellDispenserSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressCastEvent;
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
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
    private static final String CHROMATIC_RECORD_CASTING_SLOT = "chromatic_magia_dress_remote_owner_test";
    private static final RemoteOwnerCastProfile CHROMATIC_RECORD_PROFILE = new RemoteOwnerCastProfile(
            RemoteOwnerCastMode.PLAYER_SELF,
            RemoteOwnerOriginMode.PLAYER_SELF,
            RemoteOwnerDirectionMode.PLAYER_LOOK,
            Optional.empty(),
            true
    );

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
    public static void remoteOwnerCastRecordsChromaticMagiaDressInstantLongAndInitialRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createChromaticRecordTestOwner(helper, "chromatic_remote_owner_cast_test");
            var magicData = requireMagicData(helper, owner);
            magicData.setMana(1000.0F);

            var helmet = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
            var chestplate = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var boots = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            owner.setItemSlot(EquipmentSlot.HEAD, helmet);
            owner.setItemSlot(EquipmentSlot.CHEST, chestplate);
            owner.setItemSlot(EquipmentSlot.FEET, boots);

            var bonus = ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();
            var instantSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            assertRemoteOwnerCastSucceeds(helper, owner, new SpellData(instantSpell, 1),
                    "Remote Owner INSTANT Chromatic Magia Dress test cast failed");
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, boots, EquipmentSlot.FEET, instantSpell, bonus,
                    "Chromatic Magia Dress boots should record Remote Owner INSTANT casts");

            var longSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPOUND_PHIAL.get();
            assertRemoteOwnerCastSucceeds(helper, owner, new SpellData(longSpell, 1),
                    "Remote Owner LONG Chromatic Magia Dress test cast failed");
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, helmet, EquipmentSlot.HEAD, longSpell, bonus,
                    "Chromatic Magia Dress helmet should record Remote Owner LONG casts");

            var recastSpell = SpellRegistry.RAISE_DEAD_SPELL.get();
            assertRemoteOwnerCastSucceeds(helper, owner, new SpellData(recastSpell, 1),
                    "Remote Owner initial recast Chromatic Magia Dress test cast failed");
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, bonus,
                    "Chromatic Magia Dress chestplate should record Remote Owner initial recast-capable casts");

            var activeRecastResult = tryRemoteOwnerCast(owner, new SpellData(recastSpell, 1));
            helper.assertTrue(activeRecastResult.handled() && !activeRecastResult.succeeded(),
                    "Remote Owner active recast test should fail before recording a second history");
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, bonus,
                    "Chromatic Magia Dress chestplate should not record Remote Owner active recasts");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerContinuousCastRecordsChromaticMagiaDressOncePerSession(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createChromaticRecordTestOwner(helper, "chromatic_remote_owner_continuous_test");
            var magicData = requireMagicData(helper, owner);
            magicData.setMana(1000.0F);

            var leggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            owner.setItemSlot(EquipmentSlot.LEGS, leggings);

            var bonus = ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();
            var continuousSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
            var spellData = new SpellData(continuousSpell, 1);
            var firstSession = startRemoteOwnerContinuousCast(helper, owner, spellData);
            helper.assertFalse(magicData.isCasting(),
                    "Remote Owner CONTINUOUS should not mark owner MagicData as active casting");
            tickRemoteOwnerContinuousCast(owner, firstSession, 12);
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, bonus,
                    "Chromatic Magia Dress leggings should record the first Remote Owner CONTINUOUS onCast only once");
            RemoteOwnerCastRunner.finishContinuousCast(owner.serverLevel(), owner, firstSession, false);

            magicData.setMana(1000.0F);
            var secondSession = startRemoteOwnerContinuousCast(helper, owner, spellData);
            tickRemoteOwnerContinuousCast(owner, secondSession, 1);
            ApprenticeCodexGameTestScenarios.assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, 2.0D * bonus,
                    "Chromatic Magia Dress leggings should record a new Remote Owner CONTINUOUS session after finish");
            RemoteOwnerCastRunner.finishContinuousCast(owner.serverLevel(), owner, secondSession, false);
        });
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

        for (var schoolType : SchoolRegistry.REGISTRY) {
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
            helper.assertTrue(anchor.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(entry.getKey())) != null,
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
    public static void remoteOwnerCombatOwnerUuidKeepsProjectileAttributionAfterAnchorDiscard(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "remote_owner_combat_uuid_test"));
        level.addFreshEntity(owner);

        var anchor = new RemoteOwnerCastAnchorEntity(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), level);
        anchor.bindOwnerName(owner);
        level.addFreshEntity(anchor);

        var shell = new ArtisanSmashShellEntity(EntityRegistry.ARTISAN_SMASH_SHELL.get(), level, anchor);
        helper.assertTrue(owner.getUUID().equals(shell.getCombatOwnerUuid()),
                "Artisan Smash shell should capture the Remote Owner anchor bound owner UUID.");

        var shellTag = new CompoundTag();
        shell.saveCombatOwnerUuid(shellTag);
        var restoredShell = new ArtisanSmashShellEntity(EntityRegistry.ARTISAN_SMASH_SHELL.get(), level);
        restoredShell.loadCombatOwnerUuid(shellTag);
        helper.assertTrue(owner.getUUID().equals(restoredShell.getCombatOwnerUuid()),
                "Artisan Smash shell should persist combat owner UUID.");

        var flySwatter = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), level, anchor);
        helper.assertTrue(owner.getUUID().equals(flySwatter.getCombatOwnerUuid()),
                "Fly Swatter projectile should capture the Remote Owner anchor bound owner UUID.");

        anchor.discard();
        var source = CombatOwnerResolver.createDamageSource(
                level,
                shell,
                shell.getOwner(),
                shell.getCombatOwnerUuid(),
                DamageTypes.ARTISAN_SMASH
        );
        helper.assertTrue(source.getEntity() == owner,
                "Combat owner UUID should resolve the original player after Remote Owner anchor discard.");

        var ownerlessShell = new ArtisanSmashShellEntity(EntityRegistry.ARTISAN_SMASH_SHELL.get(), level);
        var ownerlessSource = CombatOwnerResolver.createDamageSource(
                level,
                ownerlessShell,
                null,
                UUID.randomUUID(),
                DamageTypes.ARTISAN_SMASH
        );
        helper.assertTrue(ownerlessSource.getEntity() == ownerlessShell,
                "Unresolved combat owner UUID should fall back to ownerless projectile damage.");

        owner.discard();
        shell.discard();
        restoredShell.discard();
        flySwatter.discard();
        ownerlessShell.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerSummonWeaponDamageSourceUsesBoundOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "remote_owner_summon_source_test"));
        level.addFreshEntity(owner);

        var anchor = new RemoteOwnerCastAnchorEntity(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), level);
        anchor.bindOwnerName(owner);
        var anchorPos = helper.absoluteVec(new Vec3(2.5D, 2.0D, 2.5D));
        anchor.moveTo(anchorPos.x, anchorPos.y, anchorPos.z, 0.0F, 0.0F);
        level.addFreshEntity(anchor);

        var weapon = new TestHiganbanaKatanaEntity(level, anchor);
        weapon.setPos(anchorPos.x, anchorPos.y, anchorPos.z);
        level.addFreshEntity(weapon);

        var directOwnerWeapon = new TestHiganbanaKatanaEntity(level, owner);
        level.addFreshEntity(directOwnerWeapon);

        try {
            helper.assertTrue(owner.getUUID().equals(weapon.getCombatOwnerUuid()),
                    "Remote Owner summon weapon should capture the bound owner UUID.");
            var source = weapon.createTestDamageSource();
            helper.assertTrue(source.getEntity() == owner,
                    "Remote Owner summon weapon DamageSource should use the bound owner.");
            helper.assertTrue(source.getDirectEntity() == weapon,
                    "Remote Owner summon weapon DamageSource should keep the weapon as direct entity.");

            var directOwnerSource = directOwnerWeapon.createTestDamageSource();
            helper.assertTrue(directOwnerSource.getEntity() == owner,
                    "Summon weapon DamageSource should preserve a current FakePlayer owner when it is not a proxy.");
            helper.assertTrue(directOwnerSource.getDirectEntity() == directOwnerWeapon,
                    "Summon weapon DamageSource should keep the normal weapon as direct entity.");
        } finally {
            weapon.discard();
            directOwnerWeapon.discard();
            anchor.discard();
            owner.discard();
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void remoteOwnerCastDatagenIncludesRaiseDeadImpactProfile(GameTestHelper helper) {
        var raiseDeadId = requireId(SpellRegistry.RAISE_DEAD_SPELL.get().getSpellResource());
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
                requireId(SpellRegistry.DRAGON_BREATH_SPELL.get().getSpellResource()),
                requireId(SpellRegistry.FIRE_BREATH_SPELL.get().getSpellResource()),
                requireId(SpellRegistry.CONE_OF_COLD_SPELL.get().getSpellResource()),
                requireId(SpellRegistry.ELECTROCUTE_SPELL.get().getSpellResource()),
                requireId(SpellRegistry.POISON_BREATH_SPELL.get().getSpellResource())
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
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

        helper.runAfterDelay(3, () -> {
            var expectedCount = spell.getProjectileCount(5, owner);
            var daggers = level.getEntitiesOfClass(
                    InscribeIceDaggerEntity.class,
                    new AABB(remoteOrigin, remoteOrigin).inflate(16.0D),
                    projectile -> projectile.getOwner() == owner
            );
            helper.assertTrue(daggers.size() == expectedCount,
                    "Inscribe Ice RemoteOwnerCast should finish the short throw job: "
                            + daggers.size() + " / " + expectedCount);
            for (var dagger : daggers) {
                var movement = dagger.getDeltaMovement();
                helper.assertTrue(movement.y > 0.5D,
                        "Inscribe Ice RemoteOwnerCast should keep the remote vertical forward direction after context closes: "
                                + movement);
                helper.assertTrue(Math.abs(movement.z) < InscribeIceDaggerEntity.SPEED * 0.15D,
                        "Inscribe Ice RemoteOwnerCast should keep the cast-time right direction for vertical launches: "
                                + movement);
            }
            helper.succeed();
        });
    }

    private static void setAttributeBaseValue(
            GameTestHelper helper,
            FakePlayer player,
            Attribute attribute,
            double value,
            String context
    ) {
        var instance = player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
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
        var instance = anchor.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
        helper.assertTrue(instance != null, message + ": missing attribute");
        helper.assertTrue(Math.abs(instance.getValue() - expected) < 1.0E-6D,
                message + ": " + instance.getValue() + " / expected " + expected);
    }

    private static FakePlayer createChromaticRecordTestOwner(GameTestHelper helper, String profileName) {
        var owner = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new net.minecraft.core.BlockPos(0, 2, 0),
                profileName);
        helper.getLevel().addFreshEntity(owner);
        return owner;
    }

    private static MagicData requireMagicData(GameTestHelper helper, FakePlayer owner) {
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertTrue(magicData != null, "Remote Owner Chromatic Magia Dress test could not resolve player mana data");
        return magicData;
    }

    private static void assertRemoteOwnerCastSucceeds(GameTestHelper helper, FakePlayer owner, SpellData spellData, String message) {
        var result = tryRemoteOwnerCast(owner, spellData);
        helper.assertTrue(result.handled() && result.succeeded(), message);
    }

    private static RemoteOwnerCastRunner.CastResult tryRemoteOwnerCast(FakePlayer owner, SpellData spellData) {
        return RemoteOwnerCastRunner.tryCast(
                owner.serverLevel(),
                owner,
                ItemStack.EMPTY,
                spellData,
                CHROMATIC_RECORD_PROFILE,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                owner.getEyePosition(),
                owner.getLookAngle(),
                CastSource.SWORD,
                CHROMATIC_RECORD_CASTING_SLOT,
                false
        );
    }

    private static RemoteOwnerCastRunner.ContinuousCastSession startRemoteOwnerContinuousCast(
            GameTestHelper helper,
            FakePlayer owner,
            SpellData spellData
    ) {
        var result = RemoteOwnerCastRunner.tryStartContinuousCast(
                owner.serverLevel(),
                owner,
                ItemStack.EMPTY,
                spellData,
                CHROMATIC_RECORD_PROFILE,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                owner.getEyePosition(),
                owner.getLookAngle(),
                CastSource.SWORD,
                CHROMATIC_RECORD_CASTING_SLOT,
                40,
                false
        );
        helper.assertTrue(result.handled() && result.succeeded() && result.session() != null,
                "Remote Owner CONTINUOUS Chromatic Magia Dress test cast failed");
        return result.session();
    }

    private static void tickRemoteOwnerContinuousCast(
            FakePlayer owner,
            RemoteOwnerCastRunner.ContinuousCastSession session,
            int ticks
    ) {
        for (var i = 0; i < ticks && !session.isFinished(); ++i) {
            RemoteOwnerCastRunner.tickContinuousCast(owner.serverLevel(), owner, session);
            ChromaticMagiaDressCastEvent.onPlayerTick(new PlayerTickEvent.Post(owner));
        }
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

    private static final class TestHiganbanaKatanaEntity extends HiganbanaKatanaEntity {
        private TestHiganbanaKatanaEntity(net.minecraft.world.level.Level level, LivingEntity owner) {
            super(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
        }

        private DamageSource createTestDamageSource() {
            return createCombatDamageSource(DamageTypes.HIGANBANA);
        }
    }
}
