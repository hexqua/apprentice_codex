package jp.aquafactory.apprenticecodex.gametest.enchantment.attribute;

import jp.aquafactory.apprenticecodex.effect.LongStrideMobility;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.longstride.LongStrideFluidMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;

import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.placeAbsoluteFluidTestBasin;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class LegacyAttributeScenarios {
    private LegacyAttributeScenarios() {}

    static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var movementSpeedModifier = effect.getAttributeModifiers().get(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeedModifier != null, "LongStride is missing the movement speed attribute modifier");

            var actualAmount = effect.getAttributeModifierValue(0, movementSpeedModifier);
            helper.assertTrue(Math.abs(actualAmount - 0.10D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.10 but got " + actualAmount);
        });
    }

    static void longStrideStandsOnWaterLavaAndFlowingFluid(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterWalker = createEquipmentTestPlayer(helper, new BlockPos(0, 3, 0),
                    "long_stride_water_walk_test");
            var waterPos = waterWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), waterPos, Blocks.WATER.defaultBlockState());
            waterWalker.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            waterWalker.setDeltaMovement(0.1D, 0.1D, -0.05D);

            helper.assertTrue(waterWalker.canStandOnFluid(helper.getLevel().getFluidState(waterPos)),
                    "LongStride should enable vanilla fluid standing on water");
            LongStrideFluidMovement.apply(waterWalker);

            var waterSurfaceY = waterPos.getY()
                    + helper.getLevel().getFluidState(waterPos).getHeight(helper.getLevel(), waterPos)
                    + 0.02D;
            helper.assertTrue(waterWalker.onGround()
                            && Math.abs(waterWalker.getY() - waterSurfaceY) < 1.0E-9D
                            && waterWalker.getDeltaMovement().y == 0.0D,
                    "LongStride should settle the player on the water surface: y="
                            + waterWalker.getY() + ", dy=" + waterWalker.getDeltaMovement().y);
            helper.assertTrue(Math.abs(waterWalker.getDeltaMovement().x - 0.1D) < 1.0E-9D
                            && Math.abs(waterWalker.getDeltaMovement().z + 0.05D) < 1.0E-9D,
                    "LongStride fluid standing should preserve horizontal movement on water");

            var lavaWalker = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 0),
                    "long_stride_lava_walk_test");
            var lavaPos = lavaWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), lavaPos, Blocks.LAVA.defaultBlockState());
            lavaWalker.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            lavaWalker.setDeltaMovement(0.0D, -0.2D, 0.0D);
            LongStrideFluidMovement.apply(lavaWalker);
            helper.assertTrue(lavaWalker.onGround() && !lavaWalker.isInLava(),
                    "LongStride should stand above lava without granting lava contact immunity");

            var flowingWalker = createEquipmentTestPlayer(helper, new BlockPos(4, 3, 0),
                    "long_stride_flowing_walk_test");
            var flowingPos = flowingWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), flowingPos,
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
            flowingWalker.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            flowingWalker.setDeltaMovement(0.08D, -0.2D, 0.0D);
            LongStrideFluidMovement.apply(flowingWalker);
            var flowingSurfaceY = flowingPos.getY()
                    + helper.getLevel().getFluidState(flowingPos).getHeight(helper.getLevel(), flowingPos)
                    + 0.02D;
            helper.assertTrue(flowingWalker.onGround()
                            && Math.abs(flowingWalker.getY() - flowingSurfaceY) < 1.0E-9D
                            && Math.abs(flowingWalker.getDeltaMovement().x - 0.08D) < 1.0E-9D,
                    "LongStride should treat flowing fluid block tops as walkable surfaces");

            var shallowWalker = createEquipmentTestPlayer(helper, new BlockPos(6, 3, 0),
                    "long_stride_shallow_flowing_walk_test");
            var shallowPos = shallowWalker.blockPosition();
            placeAbsoluteFluidTestBasin(helper.getLevel(), shallowPos,
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7));
            shallowWalker.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            shallowWalker.setOnGround(false);
            shallowWalker.setDeltaMovement(0.08D, 0.0D, 0.0D);

            for (var tick = 0; tick < 20; ++tick) {
                LongStrideFluidMovement.apply(shallowWalker);
                if (shallowWalker.onGround()) {
                    break;
                }
                shallowWalker.move(MoverType.SELF,
                        shallowWalker.getDeltaMovement());
            }

            var shallowSurfaceY = shallowPos.getY()
                    + helper.getLevel().getFluidState(shallowPos).getHeight(helper.getLevel(), shallowPos)
                    + 0.02D;
            helper.assertTrue(shallowWalker.onGround()
                            && Math.abs(shallowWalker.getY() - shallowSurfaceY) < 1.0E-9D,
                    "LongStride should rise from shallow flowing water and settle on its actual surface: y="
                            + shallowWalker.getY() + ", expected=" + shallowSurfaceY);

            var jumpingWalker = createEquipmentTestPlayer(helper, new BlockPos(8, 3, 0),
                    "long_stride_fluid_jump_test");
            var jumpSupportPos = jumpingWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), jumpSupportPos, Blocks.WATER.defaultBlockState());
            jumpingWalker.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            var jumpSurfaceY = jumpSupportPos.getY()
                    + helper.getLevel().getFluidState(jumpSupportPos).getHeight(helper.getLevel(), jumpSupportPos)
                    + 0.02D;
            jumpingWalker.setPos(jumpingWalker.getX(), jumpSurfaceY + 0.2D, jumpingWalker.getZ());
            jumpingWalker.setOnGround(false);
            jumpingWalker.setDeltaMovement(0.04D, 0.42D, 0.0D);
            LongStrideFluidMovement.apply(jumpingWalker);
            helper.assertTrue(Math.abs(jumpingWalker.getY() - (jumpSurfaceY + 0.2D)) < 1.0E-9D
                            && Math.abs(jumpingWalker.getDeltaMovement().y - 0.42D) < 1.0E-9D,
                    "LongStride fluid standing should not pull back a player jumping away from the surface");
        });
    }

    static void longStrideFluidMovementRespectsExcludedStatesAndLavaDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterloggedPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 3, 0),
                    "long_stride_waterlogged_test");
            helper.getLevel().setBlock(waterloggedPlayer.blockPosition().below(),
                    Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(
                    waterloggedPlayer.blockPosition(),
                    Blocks.OAK_SLAB.defaultBlockState()
                            .setValue(SlabBlock.WATERLOGGED, true),
                    3
            );
            waterloggedPlayer.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            waterloggedPlayer.setDeltaMovement(0.0D, -0.04D, 0.0D);
            var waterloggedY = waterloggedPlayer.getY();
            LongStrideFluidMovement.apply(waterloggedPlayer);
            helper.assertTrue(Math.abs(waterloggedPlayer.getY() - waterloggedY) < 1.0E-9D,
                    "LongStride should not reposition a player inside a collidable waterlogged block: y="
                            + waterloggedPlayer.getY() + ", expected=" + waterloggedY);
            helper.assertTrue(Math.abs(waterloggedPlayer.getDeltaMovement().y + 0.04D) < 1.0E-9D,
                    "LongStride should not add buoyancy inside a collidable waterlogged block: dy="
                            + waterloggedPlayer.getDeltaMovement().y);

            var flyingPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 0),
                    "long_stride_flying_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), flyingPlayer.blockPosition(), Blocks.WATER.defaultBlockState());
            flyingPlayer.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            flyingPlayer.getAbilities().flying = true;
            flyingPlayer.setDeltaMovement(0.0D, -0.04D, 0.0D);
            LongStrideFluidMovement.apply(flyingPlayer);
            helper.assertTrue(Math.abs(flyingPlayer.getDeltaMovement().y + 0.04D) < 1.0E-9D,
                    "LongStride should not override creative flight movement");

            var lavaPlayer = createEquipmentTestPlayer(helper, new BlockPos(4, 3, 0),
                    "long_stride_lava_damage_test");
            lavaPlayer.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            helper.assertFalse(lavaPlayer.hasEffect(MobEffects.FIRE_RESISTANCE),
                    "LongStride should not grant Fire Resistance");
            helper.assertFalse(lavaPlayer.fireImmune(),
                    "LongStride should not make the player immune to fire damage");
        });
    }
}
