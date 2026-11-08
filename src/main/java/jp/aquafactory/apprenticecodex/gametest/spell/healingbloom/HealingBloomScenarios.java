package jp.aquafactory.apprenticecodex.gametest.spell.healingbloom;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.countFreshItemDrops;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareElevatedStonePlatform;

public final class HealingBloomScenarios {
    private HealingBloomScenarios() {}

    static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState();
            var pos = new BlockPos(0, 2, 0);
            var shape = state.getShape(level, pos, CollisionContext.empty());
            helper.assertTrue(shape.isEmpty(),
                    "Healing Bloom light outline should be empty so it cannot be removed by hand");
            helper.assertTrue(state.getLightEmission(level, pos) == 11,
                    "Healing Bloom light should now emit light level 11");
        });
    }

    static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var pos = new BlockPos(0, 2, 0);
            helper.setBlock(pos, BlockRegistry.HEALING_BLOOM_LIGHT.get());

            for (int i = 0; i < 25; ++i) {
                if (level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get())) {
                    var blockEntity = helper.getBlockEntity(pos);
                    helper.assertTrue(blockEntity instanceof HealingBloomLightBlockEntity,
                            "Healing Bloom light is missing its block entity");
                    HealingBloomLightBlockEntity.serverTick(
                            level,
                            pos,
                            level.getBlockState(pos),
                            (HealingBloomLightBlockEntity) blockEntity
                    );
                }
            }

            helper.assertTrue(!level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get()),
                    "Healing Bloom light should self-clean without a bloom");
        });
    }

    static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_owner_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(new BlockPos(0, 2, 0));
            helper.assertTrue(bloom.shouldBeSaved(), "Healing Bloom should now be saved with the world");
            helper.assertTrue(bloom.hurt(level.damageSources().playerAttack(owner), 2.0f),
                    "Healing Bloom should now accept damage from its owner");
        });
    }

    static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
        var level = helper.getLevel();
        var relativeAnchorPos = new BlockPos(0, 2, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_root_loss_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(bloom);

        helper.runAtTickTime(1, () -> helper.setBlock(relativeAnchorPos.below(), Blocks.AIR));
        helper.runAtTickTime(3, () -> {
            var blooms = level.getEntitiesOfClass(HealingBloomEntity.class, new AABB(anchorPos).inflate(1.5));
            helper.assertTrue(!blooms.isEmpty(),
                    "Healing Bloom should remain as a dead entity for a short time instead of silently disappearing when its root is lost");
            helper.assertTrue(blooms.stream().allMatch(entity -> !entity.isAlive() || entity.isDeadOrDying()),
                    "Healing Bloom should enter its death state when its root is lost");
            helper.succeed();
        });
    }

    static void healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeAnchorPos = new BlockPos(0, 2, 0);
            var anchorPos = helper.absolutePos(relativeAnchorPos);
            helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_death_drop_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(anchorPos);
            bloom.setBloomMaxHealth(10.0f);
            setHealingBloomFruitCount(bloom, 3);
            bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(bloom);

            killHealingBloom(level, bloom);

            helper.assertTrue(!level.getBlockState(anchorPos).is(BlockRegistry.COMFORT_BERRY_BUSH.get()),
                    "Healing Bloom death should not plant a Comfort Berry Bush");
            helper.assertTrue(countFreshItemDrops(level, ItemRegistry.COMFORT_BERRIES.get(), anchorPos, 1.5D) == 3,
                    "Healing Bloom death should drop exactly the stored Comfort Berries");
        });
    }

    static void healingBloomImmediateDeathDropsNothingAndPlantsNoBush(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeAnchorPos = new BlockPos(0, 2, 0);
            var anchorPos = helper.absolutePos(relativeAnchorPos);
            helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_immediate_death_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(anchorPos);
            bloom.setBloomMaxHealth(10.0f);
            bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(bloom);

            killHealingBloom(level, bloom);

            helper.assertTrue(!level.getBlockState(anchorPos).is(BlockRegistry.COMFORT_BERRY_BUSH.get()),
                    "Immediate Healing Bloom death should not plant a Comfort Berry Bush");
            helper.assertTrue(countFreshItemDrops(level, ItemRegistry.COMFORT_BERRIES.get(), anchorPos, 1.5D) == 0,
                    "Immediate Healing Bloom death should not drop Comfort Berries before fruit has grown");
        });
    }

    static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        // 同 batch の他 Healing Bloom から再生オーラを受けないよう、高所へ隔離する。
        var relativeAnchorPos = new BlockPos(0, 20, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        prepareElevatedStonePlatform(helper, relativeAnchorPos);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_regen_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.setHealth(5.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(bloom);

        helper.runAtTickTime(45, () -> {
            helper.assertFalse(bloom.hasEffect(MobEffects.REGENERATION),
                    "Healing Bloom should not grant its own regeneration effect to itself");
            helper.assertTrue(Math.abs(bloom.getHealth() - 5.0f) < 0.01f,
                    "Healing Bloom should not recover before its low-speed natural heal ticks");
        });
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(Math.abs(bloom.getHealth() - 6.0f) < 0.01f,
                    "Healing Bloom should recover exactly one point from low-speed natural healing after 80 ticks: "
                            + bloom.getHealth());
            helper.succeed();
        });
    }

    static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB);

            castHealingBloom(helper, owner, 1, anchorPos, false);

            var bloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(Math.abs(bloom.getY() - (helper.absolutePos(anchorPos).below().getY() + 0.5)) < 0.01,
                    "Healing Bloom should now sit on top of a slab support instead of refusing the placement");
        });
    }

    static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_recast_same_owner_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, owner, 1, firstAnchor, false);
            var firstBloom = getSingleLivingHealingBloom(helper, owner);

            castHealingBloom(helper, owner, 1, secondAnchor, false);

            var blooms = getOwnedHealingBlooms(helper, owner);
            helper.assertTrue(blooms.size() == 1,
                    "Healing Bloom should still allow only one active bloom for the same owner");
            helper.assertTrue(blooms.get(0) == firstBloom,
                    "Healing Bloom should keep the original bloom when recast without sneaking");
            helper.assertTrue(firstBloom.blockPosition().equals(helper.absolutePos(firstAnchor)),
                    "Healing Bloom should remain at the original anchor after a blocked recast");
        });
    }

    static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, firstOwner).size() == 1,
                    "The first owner should keep exactly one Healing Bloom");
            helper.assertTrue(getOwnedHealingBlooms(helper, secondOwner).size() == 1,
                    "A different owner should be able to place a separate Healing Bloom");
        });
    }

    static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_missing_state_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);

            var spellData = Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null,
                    "Healing Bloom stale-state test could not resolve spell data capability");
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, state -> state.setBloomUuid(UUID.randomUUID()));

            castHealingBloom(helper, owner, 1, anchorPos, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, owner).size() == 1,
                    "A missing managed Healing Bloom should not block recasting for the same owner");
        });
    }

    static void healingBloomOfflineDeathDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_offline_death_recast_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            var deadBloomUuid = prepareHealingBloomDeadWhileOwnerOffline(helper, owner, firstAnchor);

            castHealingBloom(helper, owner, 1, secondAnchor, false);

            var currentBloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(secondAnchor)),
                    "A Healing Bloom that died while its owner was offline should not block normal recasting");
            assertManagedHealingBloomUuid(helper, owner, currentBloom.getUUID(),
                    "Healing Bloom stale state should be replaced by the newly placed bloom");
            helper.assertTrue(!deadBloomUuid.equals(currentBloom.getUUID()),
                    "The recast Healing Bloom should not reuse the dead bloom UUID");
        });
    }

    static void healingBloomSneakCastRecoversOfflineDeathState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_offline_death_sneak_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var replacementAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(replacementAnchor.below(), Blocks.STONE);

            var deadBloomUuid = prepareHealingBloomDeadWhileOwnerOffline(helper, owner, firstAnchor);

            castHealingBloom(helper, owner, 1, replacementAnchor, true);

            var currentBloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(replacementAnchor)),
                    "Sneak casting should recover a stale Healing Bloom state left by offline death");
            assertManagedHealingBloomUuid(helper, owner, currentBloom.getUUID(),
                    "Sneak casting should replace the stale Healing Bloom UUID with the new bloom UUID");
            helper.assertTrue(!deadBloomUuid.equals(currentBloom.getUUID()),
                    "The replacement Healing Bloom should not reuse the dead bloom UUID");
        });
    }

    static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_force_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_force_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var replacementAnchor = new BlockPos(2, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(replacementAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            var previousBloom = getSingleLivingHealingBloom(helper, firstOwner);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);
            var otherOwnersBloom = getSingleLivingHealingBloom(helper, secondOwner);

            castHealingBloom(helper, firstOwner, 1, replacementAnchor, true);

            var currentBloom = getSingleLivingHealingBloom(helper, firstOwner);
            helper.assertTrue(currentBloom != previousBloom,
                    "Sneak casting should create a new Healing Bloom for the owner");
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(replacementAnchor)),
                    "The replacement Healing Bloom should appear at the new anchor");
            helper.assertTrue(!previousBloom.isAlive() || previousBloom.isDeadOrDying(),
                    "The previous Healing Bloom should enter its death state after the replacement bloom is created");
            helper.assertTrue(otherOwnersBloom.isAlive() && otherOwnersBloom.blockPosition().equals(helper.absolutePos(secondAnchor)),
                    "Replacing your own Healing Bloom should not affect blooms owned by other players");
        });
    }

    public static void setHealingBloomFruitCount(HealingBloomEntity bloom, int fruitCount) {
        var tag = new CompoundTag();
        bloom.addAdditionalSaveData(tag);
        tag.putInt("FruitCount", fruitCount);
        bloom.readAdditionalSaveData(tag);
    }

    static UUID prepareHealingBloomDeadWhileOwnerOffline(GameTestHelper helper, FakePlayer owner, BlockPos anchorPos) {
        castHealingBloom(helper, owner, 1, anchorPos, false);
        var bloom = getSingleLivingHealingBloom(helper, owner);
        var bloomUuid = bloom.getUUID();
        assertManagedHealingBloomUuid(helper, owner, bloomUuid,
                "Healing Bloom offline-death setup should start with a managed bloom");

        // オフライン中の死亡では owner を ServerPlayer として引けず、即時解除されない state が残る。
        clearHealingBloomCachedOwner(bloom);
        killHealingBloom(helper.getLevel(), bloom);
        assertManagedHealingBloomUuid(helper, owner, bloomUuid,
                "Healing Bloom state should still contain the dead bloom UUID until the owner recasts");
        return bloomUuid;
    }

    static void clearHealingBloomCachedOwner(HealingBloomEntity bloom) {
        var tag = new CompoundTag();
        bloom.addAdditionalSaveData(tag);
        bloom.readAdditionalSaveData(tag);
    }

    static void assertManagedHealingBloomUuid(GameTestHelper helper, FakePlayer owner, UUID expectedUuid, String message) {
        var spellData = Capabilities.getSpellDataOrNull(owner);
        helper.assertTrue(spellData != null, "Healing Bloom state assertion could not resolve spell data capability");
        helper.assertTrue(expectedUuid.equals(spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE).getBloomUuid()),
                message);
    }

    static void killHealingBloom(ServerLevel level, HealingBloomEntity bloom) {
        bloom.setHealth(0.0f);
        bloom.die(level.damageSources().genericKill());
    }

    public static FakePlayer createHealingBloomPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    public static void castHealingBloom(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos, boolean forceReplace) {
        var spell = (HealingBloom) SpellRegistry.HEALING_BLOOM.get();
        var castData = new HealingBloom.HealingBloomCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        tag.putBoolean("ForceReplace", forceReplace);
        castData.deserializeNBT(helper.getLevel().registryAccess(), tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    public static List<HealingBloomEntity> getOwnedHealingBlooms(GameTestHelper helper, FakePlayer owner) {
        var blooms = new ArrayList<HealingBloomEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof HealingBloomEntity bloom
                    && bloom.isAlive()
                    && owner.getUUID().equals(bloom.getOwnerUuid())) {
                blooms.add(bloom);
            }
        }
        return blooms;
    }

    public static HealingBloomEntity getSingleLivingHealingBloom(GameTestHelper helper, FakePlayer owner) {
        var blooms = getOwnedHealingBlooms(helper, owner);
        helper.assertTrue(blooms.size() == 1, "Expected exactly one living Healing Bloom but found " + blooms.size());
        return blooms.get(0);
    }
}
