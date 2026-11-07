package jp.aquafactory.apprenticecodex.gametest.spell.targeting.aim;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BeamOcclusionGameTests {
    private BeamOcclusionGameTests() {
    }

    @GameTest(template = "gametest/combat_queries", batch = "apprenticecodex.beam_occlusion_isolated")
    public static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        try (var scope = TargetingTestFixture.of(helper)) {
            var level = helper.getLevel();
            var grassPos = helper.absolutePos(new BlockPos(5, 4, 6));
            var wallPos = helper.absolutePos(new BlockPos(7, 4, 6));
            var soilPos = grassPos.below();
            var previousSoil = level.getBlockState(soilPos);
            var previousGrass = level.getBlockState(grassPos);
            var previousWall = level.getBlockState(wallPos);
            scope.onClose(() -> {
                level.setBlockAndUpdate(grassPos, previousGrass);
                level.setBlockAndUpdate(wallPos, previousWall);
                level.setBlockAndUpdate(soilPos, previousSoil);
            });
            level.setBlockAndUpdate(soilPos, Blocks.DIRT.defaultBlockState());
            helper.setBlock(new BlockPos(5, 4, 6), Blocks.SHORT_GRASS);
            helper.setBlock(new BlockPos(7, 4, 6), Blocks.STONE);

            var grass = level.getBlockState(grassPos);
            helper.assertTrue(grass.is(Blocks.SHORT_GRASS) && grass.canSurvive(level, grassPos)
                            && !grass.getShape(level, grassPos).isEmpty() && grass.getCollisionShape(level, grassPos).isEmpty(),
                    "Occlusion fixture must retain visible grass without a collision shape");
            var start = helper.absoluteVec(new Vec3(3.5D, 4.5D, 6.5D));
            var arcaneBeam = scope.track(new ArcaneBeamEntity(EntityRegistry.ARCANE_BEAM.get(), level));
            arcaneBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            scope.assertInside(arcaneBeam);
            arcaneBeam.updateLength(8.0F, level);
            helper.assertTrue(Math.abs(arcaneBeam.getLength() - 3.5D) < 0.01D,
                    "Arcane Beam should ignore grass collision and stop at stone: " + arcaneBeam.getLength());

            var phalanxBeam = scope.track(new PhalanxChargeBeamEntity(EntityRegistry.PHALANX_CHARGE_BEAM.get(), level));
            phalanxBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            scope.assertInside(phalanxBeam);
            phalanxBeam.setup(level, 8.0F, 0.15F, 1.0F);
            helper.assertTrue(Math.abs(phalanxBeam.getLength() - 3.5D) < 0.01D,
                    "Phalanx Charge beam should ignore grass collision and stop at stone: " + phalanxBeam.getLength());
        }
        helper.succeed();
    }
}