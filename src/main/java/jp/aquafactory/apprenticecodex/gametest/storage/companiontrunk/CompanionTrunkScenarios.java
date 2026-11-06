package jp.aquafactory.apprenticecodex.gametest.storage.companiontrunk;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.InventoryTestSupport.containsItem;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareSummonedEntityIsolationArea;

final class CompanionTrunkScenarios {
    private CompanionTrunkScenarios() {}

    static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.DIAMOND));
            trunk.moveTo(player.getX() + 5.0, player.getY(), player.getZ(), 0.0f, 0.0f);

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.isAlive(), "Companion Trunk should stay active when recalled with items inside");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 1
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 1,
                    "Companion Trunk should be recalled within one block of the caster");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should keep its items after recall");
        });
    }

    static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.EMERALD));
            trunk.moveTo(player.getX() + 1.0, player.getY(), player.getZ(), 0.0f, 0.0f);
            var before = trunk.position();

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.position().distanceTo(before) < 0.01,
                    "Companion Trunk should not move when recast while already within two blocks");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should stay loaded after the failed dismiss");
        });
    }

    static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
            var trunkPos = new BlockPos(0, 2, 0);
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.DIAMOND, 3));

            trunk.dropAllContentsAndDiscard();

            var chestPos = findCompanionTrunkChest(helper, trunkPos);
            helper.assertTrue(chestPos != null, "Companion Trunk should create a vanilla chest when there is space nearby");
            helper.assertBlockPresent(Blocks.CHEST, chestPos);

            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
            helper.assertTrue(blockEntity instanceof ChestBlockEntity, "Death chest should use the vanilla chest block entity");
            helper.assertTrue(blockEntity instanceof ChestBlockEntity chest && containsItem(chest, Items.DIAMOND, 3),
                    "Death chest should receive the Companion Trunk inventory");
        });
    }

    static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
        var trunkPos = new BlockPos(0, 2, 0);
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    helper.setBlock(trunkPos.offset(x, y, z), Blocks.STONE);
                }
            }
        }

        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 4, 0));
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.EMERALD, 2));

            trunk.dropAllContentsAndDiscard();

            helper.assertTrue(findCompanionTrunkChest(helper, trunkPos) == null,
                    "Companion Trunk should not create a chest when every candidate position is blocked");
            helper.assertItemEntityPresent(Items.EMERALD, trunkPos, 2.5);
        });
    }

    static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        var trunkPos = new BlockPos(0, 2, 0);
        helper.setBlock(trunkPos, Blocks.LAVA);
        var player = createCompanionTrunkPlayer(helper, new BlockPos(1, 2, 0));
        var trunk = createCompanionTrunk(helper, player, trunkPos);
        trunk.setCompanionMaxHealth(10.0f);
        var initialHealth = trunk.getHealth();

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(trunk.isInLava(),
                    "Companion Trunk should be touching lava during the fire immunity test");
            helper.assertTrue(Math.abs(trunk.getHealth() - initialHealth) < 0.0001f,
                    "Companion Trunk should ignore environmental lava damage");
            helper.assertFalse(trunk.isOnFire(),
                    "Companion Trunk should not appear ignited while touching lava");
            helper.assertTrue(trunk.getRemainingFireTicks() <= 0,
                    "Companion Trunk should not gain fire ticks from touching lava");
            var belowWorld = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, -2, 0)));
            trunk.moveTo(belowWorld.x, belowWorld.y, belowWorld.z, 0.0f, 0.0f);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.isAlive(), "Companion Trunk should survive lava and void rescue");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 2
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 2,
                    "Companion Trunk should return near its owner after falling below the world");
        });
    }

    static void companionTrunkClimbsOneBlockStepWhenFollowingOwner(GameTestHelper helper) {
        // GameTest の管理範囲外に tick 必須のエンティティを生成すると、隣接 chunk が entity-ticking にならず、
        // tickCount=0 のまま timeout する flaky テストになり得る。対処はエンティティと移動経路を
        // template が管理する水平範囲の正の相対座標へ収めることであり、setChunkForced 等の
        // chunk 強制 load は使用しない。
        // 強制 load は誤ったテスト配置でも動作させて症状を隠し、本来のテスト環境と異なる条件を作るためである。
        var trunkPos = new BlockPos(0, 12, 2);
        var ownerPos = new BlockPos(4, 13, 2);
        for (var x = 0; x <= 4; ++x) {
            for (var z = 1; z <= 3; ++z) {
                helper.setBlock(new BlockPos(x, 11, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 12, z), x >= 2 ? Blocks.STONE : Blocks.AIR);
                helper.setBlock(new BlockPos(x, 13, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 14, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 15, z), Blocks.AIR);
            }
        }

        var owner = createCompanionTrunkPlayer(helper, ownerPos);
        var trunk = createCompanionTrunk(helper, owner, trunkPos);
        var absoluteOwnerPos = helper.absolutePos(ownerPos);

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(),
                    "Companion Trunk should land on the raised step after following its owner: "
                            + describeCompanionTrunkMovement(trunk));
            helper.assertTrue(trunk.blockPosition().getY() >= absoluteOwnerPos.getY(),
                    "Companion Trunk should climb onto the one block step while following its owner: "
                            + describeCompanionTrunkMovement(trunk));
        });
    }

    static void companionTrunkLandingDoesNotTrampleFarmland(GameTestHelper helper) {
        var farmlandPos = new BlockPos(0, 11, 0);
        var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 12, 1));
        helper.setBlock(farmlandPos, Blocks.FARMLAND);
        helper.setBlock(farmlandPos.above(), Blocks.AIR);
        helper.setBlock(farmlandPos.above(2), Blocks.AIR);
        helper.setBlock(farmlandPos.above(3), Blocks.AIR);
        var trunk = createCompanionTrunk(helper, owner, farmlandPos.above(4));

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(), "Companion Trunk should land on the farmland test block");
            helper.assertBlockPresent(Blocks.FARMLAND, farmlandPos);
        });
    }

    static FakePlayer createCompanionTrunkPlayer(GameTestHelper helper, BlockPos pos) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "companion_trunk_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static void castCompanionTrunk(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.COMPANION_TRUNK.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static CompanionTrunkEntity createCompanionTrunk(GameTestHelper helper, FakePlayer owner, BlockPos pos) {
        var trunk = new CompanionTrunkEntity(EntityRegistry.COMPANION_TRUNK.get(), helper.getLevel(), owner);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        trunk.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(trunk);
        return trunk;
    }

    static String describeCompanionTrunkMovement(CompanionTrunkEntity trunk) {
        return "blockPos=" + trunk.blockPosition()
                + ", position=" + trunk.position()
                + ", delta=" + trunk.getDeltaMovement()
                + ", onGround=" + trunk.onGround()
                + ", tickCount=" + trunk.tickCount;
    }

    static CompanionTrunkEntity getSingleCompanionTrunk(GameTestHelper helper, FakePlayer owner) {
        var trunks = helper.getLevel().getEntitiesOfClass(
                CompanionTrunkEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                trunk -> owner.getUUID().equals(trunk.getOwnerUuid())
        );
        helper.assertTrue(trunks.size() == 1, "Expected exactly one Companion Trunk but found " + trunks.size());
        return trunks.get(0);
    }

    static BlockPos findCompanionTrunkChest(GameTestHelper helper, BlockPos center) {
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    var candidate = center.offset(x, y, z);
                    if (helper.getBlockState(candidate).is(Blocks.CHEST)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }
}
