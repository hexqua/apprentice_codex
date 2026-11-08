package jp.aquafactory.apprenticecodex.gametest.spell.earthforge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EarthForgeScenarios {
    private EarthForgeScenarios() {}

    static void earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(GameTestHelper helper) {
        var centerPos = new BlockPos(2, 3, 2);
        var sourceWaterPos = centerPos;
        var flowingWaterPos = centerPos.east();
        var seagrassPos = centerPos.west();
        var waterloggedStairPos = centerPos.north();
        var lavaPos = centerPos.offset(2, 0, 2);
        var changedToLavaPos = centerPos.offset(2, 0, -2);

        for (var x = -1; x <= 5; x++) {
            for (var z = -1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, centerPos.getY() - 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, centerPos.getY(), z), Blocks.STONE);
            }
        }
        helper.setBlock(seagrassPos.below(), Blocks.DIRT);
        helper.setBlock(sourceWaterPos, Blocks.WATER);
        helper.setBlock(flowingWaterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        helper.setBlock(seagrassPos, Blocks.SEAGRASS);
        helper.setBlock(changedToLavaPos, Blocks.WATER);
        helper.setBlock(
                waterloggedStairPos,
                Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.WATERLOGGED, true)
        );
        helper.setBlock(lavaPos, Blocks.LAVA);

        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 4, 1), "earth_forge_water_replace_test");
        helper.runAtTickTime(1, () -> castEarthForge(helper, player, centerPos, Direction.UP, 3));
        helper.runAtTickTime(2, () -> helper.setBlock(changedToLavaPos, Blocks.LAVA));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.DIRT, sourceWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, flowingWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, seagrassPos);
            helper.assertBlockPresent(Blocks.OAK_STAIRS, waterloggedStairPos);
            helper.assertBlockProperty(waterloggedStairPos, StairBlock.WATERLOGGED, true);
            helper.assertBlockPresent(Blocks.LAVA, lavaPos);
            helper.assertBlockPresent(Blocks.LAVA, changedToLavaPos);
        });
    }

    static void castEarthForge(GameTestHelper helper, FakePlayer player, BlockPos centerPos, Direction effectDirection, int radius) {
        var spell = (EarthForge) SpellRegistry.EARTH_FORGE.get();
        var absoluteCenterPos = helper.absolutePos(centerPos);
        var tag = new CompoundTag();
        tag.putInt("CenterX", absoluteCenterPos.getX());
        tag.putInt("CenterY", absoluteCenterPos.getY());
        tag.putInt("CenterZ", absoluteCenterPos.getZ());
        tag.putInt("EffectDirection", effectDirection.get3DDataValue());
        tag.putInt("Radius", radius);

        var castData = new EarthForge.EarthForgeCastData();
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
    }
}
