package jp.aquafactory.apprenticecodex.gametest.spell.targeting.aim;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.equipment.silentassassin.SilentAssassinGameTests.assertClose;

final class BeamOcclusionScenarios {
    private BeamOcclusionScenarios() {}

    static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            helper.setBlock(new BlockPos(2, 3, 1), Blocks.GRASS);
            helper.setBlock(new BlockPos(4, 3, 1), Blocks.STONE);

            var start = helper.absoluteVec(new Vec3(0.5D, 3.5D, 1.5D));
            var arcaneBeam = new ArcaneBeamEntity(EntityRegistry.ARCANE_BEAM.get(), level);
            arcaneBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            arcaneBeam.updateLength(8.0F, level);
            assertClose(helper, arcaneBeam.getLength(), 3.5D, 0.01D,
                    "Arcane Beam should ignore grass collision and stop at stone");

            var phalanxBeam = new PhalanxChargeBeamEntity(EntityRegistry.PHALANX_CHARGE_BEAM.get(), level);
            phalanxBeam.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
            phalanxBeam.setup(level, 8.0F, 0.15F, 1.0F);
            assertClose(helper, phalanxBeam.getLength(), 3.5D, 0.01D,
                    "Phalanx Charge beam should ignore grass collision and stop at stone");
        });
    }
}
