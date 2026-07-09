package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmuletCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcessThrowerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSatelliteFollowcastAmuletGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String DENYLIST_CONFIG_BATCH =
            "apprenticecodex.satellite_followcast_amulet_denylist_config";

    private ApprenticeCodexSatelliteFollowcastAmuletGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletAcceptsRemoteOwnerProfiles(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();

        helper.assertTrue(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                "Satellite Followcast Amulet should accept profiled RemoteOwner spells.");
        helper.assertTrue(amulet.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                "Satellite Followcast Amulet should accept profiled continuous RemoteOwner spells.");
        try (var ignoredRemoteProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                SpellRegistry.LONG_STRIDE.get().getSpellResource(),
                RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY
        ))) {
            helper.assertTrue(amulet.canImbueSpell(SpellRegistry.LONG_STRIDE.get(), 1),
                    "Satellite Followcast Amulet should accept RemoteOwner-only continuous spells.");
        }
        try (var ignoredRemoteProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of())) {
            helper.assertFalse(amulet.canImbueSpell(SpellRegistry.LONG_STRIDE.get(), 1),
                    "Satellite Followcast Amulet should reject spells without a RemoteOwner profile.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = DENYLIST_CONFIG_BATCH)
    public static void satelliteFollowcastAmuletServerDenylistDoesNotBlockImbue(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useSatelliteFollowcastAmuletSpellDenylistOverrideForGameTest(
                List.of(SpellRegistry.MAGE_LIGHT.get().getSpellId())
        )) {
            var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
            helper.assertTrue(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                    "Satellite Followcast Amulet server denylist should block runtime casts, not Imbue.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_remote_denylist_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast remote denylist test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var followcastSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(followcastSpell, false);
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(followcastSpell));

        var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                true,
                List.of(followcastSpell.getSpellResource().toString())
        );
        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));

        helper.runAtTickTime(8, () -> {
            try {
                var projectiles = level.getEntitiesOfClass(
                        io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                        new AABB(player.position(), player.position()).inflate(16.0D)
                );
                helper.assertTrue(projectiles.isEmpty(),
                        "Satellite Followcast Amulet should not fall back when Remote Owner Cast is denylisted.");
                helper.succeed();
            } finally {
                ignoredConfig.close();
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletCalibrationUpgradesEnableFourSlotsAndConvertLegacy(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        var stack = new ItemStack(amulet);
        amulet.initializeSpellContainer(stack);

        var upgradeStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        for (var slot = 0; slot < SatelliteFollowcastAmulet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            SatelliteFollowcastAmulet.setCalibrationAdjustment(stack, slot, upgradeStack);
        }
        helper.assertTrue(SatelliteFollowcastAmulet.getEnabledSpellSlotCount(stack) == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                "Satellite Followcast Amulet calibration upgrades should enable four spell slots.");
        helper.assertFalse(stack.getItem() instanceof jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem,
                "Satellite Followcast Amulet should not be upgraded by Arcane Anvil slot upgrades anymore.");

        var legacyStack = new ItemStack(amulet);
        var legacySpells = ISpellContainer.create(2, false, false).mutableCopy();
        legacySpells.addSpellAtIndex(SpellRegistry.MAGE_LIGHT.get(), 1, 0, false);
        legacySpells.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 1, false);
        ISpellContainer.set(legacyStack, legacySpells.toImmutable());
        amulet.initializeSpellContainer(legacyStack);

        helper.assertFalse(ISpellContainer.isSpellContainer(legacyStack),
                "Satellite Followcast Amulet legacy conversion should remove Iron's SpellContainer.");
        helper.assertTrue(SatelliteFollowcastAmulet.getEnabledSpellSlotCount(legacyStack) == 2,
                "Satellite Followcast Amulet legacy two-slot item should convert to one calibration upgrade.");
        helper.assertTrue(SatelliteFollowcastAmulet.isCalibrationSlotUpgrade(
                        SatelliteFollowcastAmulet.getCalibrationAdjustment(legacyStack, 0)),
                "Satellite Followcast Amulet legacy two-slot item should gain a slot upgrade adjustment.");
        assertSatelliteSpellData(helper, legacyStack, 0, SpellRegistry.MAGE_LIGHT.get(), 1,
                "Satellite Followcast Amulet legacy conversion should preserve the first spell.");
        assertSatelliteSpellData(helper, legacyStack, 1, io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1,
                "Satellite Followcast Amulet legacy conversion should preserve the second spell.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletKeepsOriginalSpellManaReserved(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_mana_reserve_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet mana reserve test could not resolve player mana data.");

        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        var followSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        magicData.getSyncedData().learnSpell(followSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(followSpell));

        var originalManaCost = triggerSpell.getManaCost(1);
        var followManaCost = SpellDispenserManaHelper.getSpellManaCost(new io.redspace.ironsspellbooks.api.spells.SpellData(followSpell, 1));
        var initialMana = originalManaCost + followManaCost - 1;
        magicData.setMana(initialMana);

        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell, originalManaCost));

        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(followSpell),
                "Satellite Followcast Amulet should not spend mana needed by the original spell.");
        helper.assertTrue(magicData.getMana() == initialMana,
                "Satellite Followcast Amulet should leave mana untouched when the original spell reserve would be broken.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletConsumesFollowcastWhenOriginalManaRemains(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_mana_success_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet mana success test could not resolve player mana data.");

        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        var followSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        magicData.getSyncedData().learnSpell(followSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(followSpell));

        var originalManaCost = triggerSpell.getManaCost(1);
        var followManaCost = SpellDispenserManaHelper.getSpellManaCost(new io.redspace.ironsspellbooks.api.spells.SpellData(followSpell, 1));
        magicData.setMana(originalManaCost + followManaCost);

        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell, originalManaCost));

        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(followSpell),
                "Satellite Followcast Amulet should cast when mana remains for the original spell.");
        helper.assertTrue(magicData.getMana() == originalManaCost,
                "Satellite Followcast Amulet should only consume the followcast mana before the original spell resolves.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletRetriesAfterUnaffordableSlot(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_retry_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet retry test could not resolve player mana data.");

        var unaffordableSpell = SpellRegistry.PRECISION_JACK.get();
        var fallbackSpell = SpellRegistry.THERMAL_PROCESS.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.setMana(triggerSpell.getManaCost(1) + SpellDispenserManaHelper.getSpellManaCost(
                new io.redspace.ironsspellbooks.api.spells.SpellData(fallbackSpell, 1)
        ));
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        magicData.getSyncedData().learnSpell(unaffordableSpell, false);
        magicData.getSyncedData().learnSpell(fallbackSpell, false);

        var amuletStack = createAmuletStack(unaffordableSpell, fallbackSpell);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, amuletStack);

        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));

        helper.assertTrue(
                SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                        level,
                        player,
                        io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                        0,
                        1
                ),
                "Satellite Followcast Amulet should retry the next crystal after an unaffordable spell."
        );
        helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(unaffordableSpell),
                "Satellite Followcast Amulet should not cast or cool down the unaffordable crystal.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletCastsWhileKeepingOriginalContinuousCastState(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_continuous_state_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet continuous state test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
        var followSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get();
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        magicData.getSyncedData().learnSpell(followSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(followSpell));

        magicData.getSyncedData();
        magicData.initiateCast(
                triggerSpell,
                1,
                triggerSpell.getEffectiveCastTime(1, player),
                CastSource.SWORD,
                SpellSelectionManager.MAINHAND
        );

        try (var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                true,
                List.of()
        )) {
            SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));
        }

        helper.assertTrue(magicData.isCasting(),
                "Satellite Followcast Amulet should not clear the original continuous cast state.");
        helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                "Satellite Followcast Amulet should preserve the original continuous spell id.");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(followSpell),
                "Satellite Followcast Amulet should cast through the RemoteOwner busy fallback.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_busy_cooldown_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet busy cooldown test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
        var followSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get();
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        magicData.getSyncedData().learnSpell(followSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(followSpell));
        io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, followSpell, CastSource.SWORD);

        magicData.initiateCast(
                triggerSpell,
                1,
                triggerSpell.getEffectiveCastTime(1, player),
                CastSource.SWORD,
                SpellSelectionManager.MAINHAND
        );

        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));

        helper.assertTrue(magicData.isCasting(),
                "Satellite Followcast Amulet should not clear the original cast state when the follow spell is cooling down.");
        helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                "Satellite Followcast Amulet should preserve the original spell id when the follow spell is cooling down.");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(followSpell),
                "Satellite Followcast Amulet cooldown setup should remain active.");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void satelliteFollowcastAmuletContinuousRuntimeSkipsOnlyActiveCrystal(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet continuous test could not resolve player mana data.");
        magicData.setMana(200.0F);

        var fireBreath = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var mageLight = SpellRegistry.MAGE_LIGHT.get();
        var amuletStack = createAmuletStack(fireBreath, mageLight);
        magicData.getSyncedData().learnSpell(fireBreath, false);
        magicData.getSyncedData().learnSpell(mageLight, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, amuletStack);

        helper.runAtTickTime(1, () -> {
            SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, mageLight));
            helper.assertTrue(
                    SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                            level,
                            player,
                            io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                            0,
                            0
                    ),
                    "Satellite Followcast Amulet should keep the continuous crystal active."
            );
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fireBreath),
                    "Satellite Followcast Amulet should not apply continuous cooldown at start.");

            SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, mageLight));
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(mageLight),
                    "Satellite Followcast Amulet should skip the active continuous crystal and cast the other crystal.");
        });

        helper.runAtTickTime(20, () -> {
            var searchCenter = SatelliteFollowcastAmulet.getCrystalPosition(
                    player,
                    0,
                    SatelliteFollowcastAmulet.getEnabledSpellSlotCount(amuletStack),
                    0.0F
            );
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(searchCenter, searchCenter).inflate(16.0D));
            var anchorOwner = projectiles.stream()
                    .map(FireBreathProjectile::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "Satellite Followcast Amulet CONTINUOUS casts should use a Remote Owner anchor for Fire Breath owner tracking.");
            helper.assertTrue(anchorOwner.get().getDisplayName().getString().equals(player.getDisplayName().getString()),
                    "Satellite Followcast Remote Owner anchor should expose the player name for death messages.");
        });

        helper.succeedWhen(() -> helper.assertFalse(
                SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                        level,
                        player,
                        io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                        0,
                        0
                ),
                "Satellite Followcast Amulet continuous crystal should stop after its fixed duration."
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletClearsContinuousRuntimeForOwnerStateReset(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_clear_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet clear test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var fireBreath = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(fireBreath, false);
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(fireBreath));

        SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));
        helper.assertTrue(
                SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                        level,
                        player,
                        io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                        0,
                        0
                ),
                "Satellite Followcast Amulet should start a continuous runtime before the clear path is tested."
        );

        SatelliteFollowcastAmuletCastEvent.clearPlayerStateForGameTest(player);

        helper.assertFalse(
                SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                        level,
                        player,
                        io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                        0,
                        0
                ),
                "Satellite Followcast Amulet should clear active continuous runtime for owner state reset."
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void satelliteFollowcastAmuletLongSummonWeaponUsesRemoteOwnerAnchor(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_precision_jack_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Precision Jack test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var precisionJack = SpellRegistry.PRECISION_JACK.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(precisionJack, false);
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(precisionJack));

        var profile = RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY.withCastMode(RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC);
        helper.runAtTickTime(1, () -> {
            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(precisionJack.getSpellResource(), profile));
                 var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                         true,
                         List.of()
                 )) {
                SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));
            }
        });

        helper.runAtTickTime(3, () -> {
            var searchCenter = SatelliteFollowcastAmulet.getCrystalPosition(player, 0, SatelliteFollowcastAmulet.MIN_SPELL_SLOTS, 0.0F);
            var knives = level.getEntitiesOfClass(PrecisionJackKnifeEntity.class, new AABB(player.position(), player.position()).inflate(32.0D));
            var anchorOwner = knives.stream()
                    .map(PrecisionJackKnifeEntity::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "Satellite Followcast Precision Jack should keep the summoned knife owned by a Remote Owner anchor.");
            helper.assertTrue(anchorOwner.get().position().distanceTo(searchCenter) < 2.0D,
                    "Satellite Followcast Precision Jack anchor should remain near the initial crystal position.");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void satelliteFollowcastAmuletContinuousSummonWeaponUsesRemoteOwnerAnchor(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_thermal_process_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Thermal Process test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(thermalProcess, false);
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(thermalProcess));

        var profile = RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY.withCastMode(RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC);
        helper.runAtTickTime(1, () -> {
            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(thermalProcess.getSpellResource(), profile));
                 var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                         true,
                         List.of()
                 )) {
                SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));
            }
        });

        helper.runAtTickTime(25, () -> {
            var searchCenter = SatelliteFollowcastAmulet.getCrystalPosition(player, 0, SatelliteFollowcastAmulet.MIN_SPELL_SLOTS, 0.0F);
            var throwers = level.getEntitiesOfClass(ThermalProcessThrowerEntity.class, new AABB(player.position(), player.position()).inflate(32.0D));
            var anchorOwner = throwers.stream()
                    .map(ThermalProcessThrowerEntity::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "Satellite Followcast Thermal Process should keep the thrower owned by a Remote Owner anchor.");
            helper.assertTrue(anchorOwner.get().position().distanceTo(searchCenter) < 2.0D,
                    "Satellite Followcast Thermal Process anchor should remain near the active crystal position.");
        });

        helper.succeedWhen(() -> helper.assertFalse(
                SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                        level,
                        player,
                        io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                        0,
                        0
                ),
                "Satellite Followcast Thermal Process continuous crystal should finish."
        ));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void satelliteFollowcastAmuletDimensionResetCancelsOldRuntimeWithoutOwner(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_dimension_reset_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast dimension reset test could not resolve player mana data.");
        magicData.setMana(500.0F);

        var thermalProcess = SpellRegistry.THERMAL_PROCESS.get();
        var triggerSpell = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(thermalProcess, false);
        magicData.getSyncedData().learnSpell(triggerSpell, false);
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, createAmuletStack(thermalProcess));

        var profile = RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY.withCastMode(RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC);
        helper.runAtTickTime(1, () -> {
            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(thermalProcess.getSpellResource(), profile));
                 var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                         true,
                         List.of()
                 )) {
                SatelliteFollowcastAmuletCastEvent.onSpellCast(createSpellOnCastEvent(player, triggerSpell));
            }
        });

        helper.runAtTickTime(25, () -> {
            var throwers = level.getEntitiesOfClass(ThermalProcessThrowerEntity.class, new AABB(player.position(), player.position()).inflate(32.0D));
            helper.assertTrue(!throwers.isEmpty(),
                    "Satellite Followcast dimension reset test should have an active continuous summon before reset.");

            SatelliteFollowcastAmuletCastEvent.clearPlayerStateForGameTest(player, null);

            var remainingThrowers = level.getEntitiesOfClass(ThermalProcessThrowerEntity.class, new AABB(player.position(), player.position()).inflate(32.0D));
            helper.assertTrue(!remainingThrowers.isEmpty(),
                    "Old-dimension continuous runtime should be cancelled without owner completion.");
            helper.assertFalse(
                    SatelliteFollowcastAmuletCastEvent.hasActiveContinuousFollowcastForGameTest(
                            level,
                            player,
                            io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT,
                            0,
                            0
                    ),
                    "Satellite Followcast dimension reset should clear old-dimension runtime."
            );
            helper.succeed();
        });
    }

    private static SpellOnCastEvent createSpellOnCastEvent(FakePlayer player, io.redspace.ironsspellbooks.api.spells.AbstractSpell spell) {
        return createSpellOnCastEvent(player, spell, spell.getManaCost(1));
    }

    private static SpellOnCastEvent createSpellOnCastEvent(
            FakePlayer player,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            int manaCost
    ) {
        return new SpellOnCastEvent(player, spell.getSpellId(), 1, manaCost, spell.getSchoolType(), CastSource.SPELLBOOK);
    }

    private static ItemStack createAmuletStack(io.redspace.ironsspellbooks.api.spells.AbstractSpell spell) {
        return createAmuletStack(new io.redspace.ironsspellbooks.api.spells.AbstractSpell[]{spell});
    }

    private static ItemStack createAmuletStack(io.redspace.ironsspellbooks.api.spells.AbstractSpell... spells) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        var amuletStack = new ItemStack(amulet);
        for (var slot = 1; slot < spells.length && slot <= SatelliteFollowcastAmulet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            SatelliteFollowcastAmulet.setCalibrationAdjustment(
                    amuletStack,
                    slot - 1,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
        }
        var needsSilverRing = false;
        for (var slot = 0; slot < spells.length && slot < SatelliteFollowcastAmulet.getStoredSpellSlotCount(); ++slot) {
            var spell = spells[slot];
            if (spell.getCastType() != io.redspace.ironsspellbooks.api.spells.CastType.INSTANT) {
                needsSilverRing = true;
            }
            SatelliteFollowcastAmulet.setCalibrationScroll(amuletStack, slot, createSpellScroll(spell));
        }
        if (needsSilverRing) {
            for (var slot = 0; slot < SatelliteFollowcastAmulet.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
                if (!SatelliteFollowcastAmulet.getCalibrationAdjustment(amuletStack, slot).isEmpty()) {
                    continue;
                }
                SatelliteFollowcastAmulet.setCalibrationAdjustment(
                        amuletStack,
                        slot,
                        new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                );
                break;
            }
        }
        return amuletStack;
    }

    private static ItemStack createSpellScroll(io.redspace.ironsspellbooks.api.spells.AbstractSpell spell) {
        var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scrollStack);
        return scrollStack;
    }

    private static void assertSatelliteSpellData(
            GameTestHelper helper,
            ItemStack stack,
            int slot,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellData = SatelliteFollowcastAmulet.getSpellDataAt(stack, slot);
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                        && spellData.getSpell() == expectedSpell
                        && spellData.getLevel() == expectedLevel,
                message + ": got " + (spellData == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                        ? "empty"
                        : spellData.getSpell().getSpellResource()));
    }

    private static FakePlayer createTrackedEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Satellite Followcast Amulet test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }
}
