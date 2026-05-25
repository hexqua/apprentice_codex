package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmuletCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
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
    public static void satelliteFollowcastAmuletAcceptsDispenserProfileAndRejectsSharedDenylists(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();

        helper.assertTrue(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                "Satellite Followcast Amulet should accept profiled Spell Dispenser spells.");
        helper.assertTrue(amulet.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                "Satellite Followcast Amulet should accept profiled continuous Spell Dispenser spells.");
        helper.assertFalse(amulet.canImbueSpell(SpellRegistry.LONG_STRIDE.get(), 1),
                "Satellite Followcast Amulet should reject continuous spells without a Spell Dispenser profile.");
        helper.assertFalse(amulet.canImbueSpell(SpellRegistry.AUTO_MAGNET.get(), 1),
                "Satellite Followcast Amulet should reject Spell Dispenser data-driven denylisted spells.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = DENYLIST_CONFIG_BATCH)
    public static void satelliteFollowcastAmuletServerDenylistBlocksImbue(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useSatelliteFollowcastAmuletSpellDenylistOverrideForGameTest(
                List.of(SpellRegistry.MAGE_LIGHT.get().getSpellId())
        )) {
            var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
            helper.assertFalse(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                    "Satellite Followcast Amulet server denylist should block otherwise supported spells.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletLesserUpgradeStopsAtTwoSlots(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        var stack = new ItemStack(amulet);
        amulet.initializeSpellContainer(stack);

        var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
        var upgraded = amulet.createSpellSlotUpgradeResult(stack, upgradeItem);
        helper.assertFalse(upgraded.isEmpty(), "Satellite Followcast Amulet should accept the first lesser slot upgrade.");
        helper.assertTrue(ISpellContainer.get(upgraded).getMaxSpellCount() == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                "Satellite Followcast Amulet should upgrade to exactly two spell slots.");

        var rejected = amulet.createSpellSlotUpgradeResult(upgraded, upgradeItem);
        helper.assertTrue(rejected.isEmpty(), "Satellite Followcast Amulet should reject upgrades beyond two spell slots.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void satelliteFollowcastAmuletContinuousRuntimeSkipsOnlyActiveCrystal(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "satellite_followcast_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Satellite Followcast Amulet continuous test could not resolve player mana data.");
        magicData.setMana(200.0F);

        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        var amuletStack = new ItemStack(amulet);
        var fireBreath = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var mageLight = SpellRegistry.MAGE_LIGHT.get();
        magicData.getSyncedData().learnSpell(fireBreath, false);
        magicData.getSyncedData().learnSpell(mageLight, false);

        var spells = ISpellContainer.create(SatelliteFollowcastAmulet.MAX_SPELL_SLOTS, false, false).mutableCopy();
        spells.addSpellAtIndex(fireBreath, 1, 0, false);
        spells.addSpellAtIndex(mageLight, 1, 1, false);
        ISpellContainer.set(amuletStack, spells.toImmutable());
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, amuletStack);

        helper.runAtTickTime(1, () -> {
            SatelliteFollowcastAmuletCastEvent.onSpellPreCast(createSpellPreCastEvent(player, mageLight));
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

            SatelliteFollowcastAmuletCastEvent.onSpellPreCast(createSpellPreCastEvent(player, mageLight));
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(mageLight),
                    "Satellite Followcast Amulet should skip the active continuous crystal and cast the other crystal.");
        });

        helper.runAtTickTime(20, () -> {
            var searchCenter = SatelliteFollowcastAmulet.getCrystalPosition(
                    player,
                    0,
                    SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
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

    private static SpellPreCastEvent createSpellPreCastEvent(FakePlayer player, io.redspace.ironsspellbooks.api.spells.AbstractSpell spell) {
        return new SpellPreCastEvent(player, spell.getSpellId(), 1, spell.getSchoolType(), CastSource.SPELLBOOK);
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
