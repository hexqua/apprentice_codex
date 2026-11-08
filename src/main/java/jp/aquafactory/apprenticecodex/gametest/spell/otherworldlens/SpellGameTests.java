package jp.aquafactory.apprenticecodex.gametest.spell.otherworldlens;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.otherworld_lens_isolated")
    public static void otherworldLensKeepsOcclusionWithoutEntityCollision(GameTestHelper helper) {
        var pos = new BlockPos(0, 2, 0);
        var state = BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState();
        helper.assertTrue(state.canOcclude(), "OtherworldLens lens must remain an occluding block");
        helper.assertTrue(
                Block.isShapeFullBlock(state.getOcclusionShape(helper.getLevel(), helper.absolutePos(pos))),
                "OtherworldLens lens must keep a full-cube occlusion shape"
        );
        helper.assertTrue(
                state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()).isEmpty(),
                "OtherworldLens lens must not collide with entities"
        );
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.otherworld_lens_isolated", timeoutTicks = 40)
    public static void otherworldLensOrphanSelfCleans(GameTestHelper helper) {
        var relativePos = new BlockPos(0, 2, 0);
        var absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(absolutePos, BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(absolutePos).isAir(),
                    "OtherworldLens orphan lens should remove itself");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.otherworld_lens_isolated", timeoutTicks = 40)
    public static void otherworldLensOrphanTickKeepsReplacement(GameTestHelper helper) {
        var relativePos = new BlockPos(0, 2, 0);
        var absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(absolutePos, BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(absolutePos, Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(absolutePos).is(Blocks.STONE),
                    "OtherworldLens cleanup must not remove a replacement block");
            helper.succeed();
        });
    }
}
