package jp.aquafactory.apprenticecodex.gametest.spell.senseevil;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.createRemoteIsolationOrigin;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.forceLoadChunk;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareAbsoluteIsolationPlatform;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareAbsoluteIsolationTargetPlatform;

public final class SenseEvilScenarios {
    private SenseEvilScenarios() {}

    static final double SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE = 1.5D;

    static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        var level = helper.getLevel();
        var casterPos = createRemoteIsolationOrigin(helper, new BlockPos(0, 14, 0), 768, 96);
        prepareAbsoluteIsolationPlatform(level, casterPos);
        var caster = createSenseEvilPlayer(level, casterPos, "sense_evil_spawner_cube_test");
        var spell = (SenseEvil) SpellRegistry.SENSE_EVIL.get();
        var range = getSenseEvilRange(spell, caster, 1);
        var diagonalOffset = Mth.floor(range * 0.75);

        helper.assertTrue(Math.sqrt(2.0 * diagonalOffset * diagonalOffset) > range,
                "Diagonal test offset must stay outside the old spherical spawner range");

        var zombieCenter = caster.getBoundingBox().getCenter().add(diagonalOffset, 0.0, diagonalOffset);
        prepareAbsoluteIsolationTargetPlatform(level, zombieCenter);
        var zombie = spawnPositionedZombie(level, zombieCenter);
        var spawnerPos = caster.blockPosition().offset(diagonalOffset, 0, diagonalOffset);
        placeZombieSpawner(level, spawnerPos);

        helper.runAtTickTime(5, () -> {
            var highlights = collectSenseEvilHighlights(spell, level, 1, caster);
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(BlockPos.containing(zombie.getBoundingBox().getCenter())), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should still detect entities at the shared diagonal cube offset");
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(spawnerPos), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should detect spawners at the same diagonal cube offset as entities");
            helper.succeed();
        });
    }

    static FakePlayer createSenseEvilPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static FakePlayer createSenseEvilPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        level.addFreshEntity(player);
        return player;
    }

    public static LivingEntity spawnPositionedZombie(ServerLevel level, Vec3 targetCenter) {
        forceLoadChunk(level, BlockPos.containing(targetCenter));
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create zombie for SenseEvil GameTest");
        }
        var floorPos = BlockPos.containing(targetCenter.x, targetCenter.y - zombie.getBbHeight() * 0.5, targetCenter.z);
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floorPos.above(2), Blocks.AIR.defaultBlockState(), 3);
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        zombie.moveTo(targetCenter.x, floorPos.getY() + 1.0, targetCenter.z, 0.0f, 0.0f);
        level.addFreshEntity(zombie);
        return zombie;
    }

    static void placeZombieSpawner(ServerLevel level, BlockPos pos) {
        forceLoadChunk(level, pos);
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
            throw new IllegalStateException("Failed to place spawner for SenseEvil GameTest at " + pos);
        }
        spawner.setEntityId(EntityType.ZOMBIE, level.getRandom());
        spawner.setChanged();
    }

    static double getSenseEvilRange(SenseEvil spell, LivingEntity caster, int spellLevel) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("getRange", int.class, LivingEntity.class);
            method.setAccessible(true);
            return (double) method.invoke(spell, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read SenseEvil range for GameTest", exception);
        }
    }

    @SuppressWarnings("unchecked")
    static List<SenseEvilHighlightsPacket.TargetData> collectSenseEvilHighlights(
            SenseEvil spell,
            ServerLevel level,
            int spellLevel,
            LivingEntity caster
    ) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("collectHighlights", ServerLevel.class, int.class, LivingEntity.class);
            method.setAccessible(true);
            return (List<SenseEvilHighlightsPacket.TargetData>) method.invoke(spell, level, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to collect SenseEvil highlights for GameTest", exception);
        }
    }

    static void assertSenseEvilHighlightPresent(
            GameTestHelper helper,
            List<SenseEvilHighlightsPacket.TargetData> highlights,
            Vec3 expectedPosition,
            double tolerance,
            String message
    ) {
        var found = highlights.stream()
                .anyMatch(target -> target.position().distanceTo(expectedPosition) <= tolerance);
        helper.assertTrue(found, message + " / expected near " + expectedPosition + " but got " + highlights);
    }
}
