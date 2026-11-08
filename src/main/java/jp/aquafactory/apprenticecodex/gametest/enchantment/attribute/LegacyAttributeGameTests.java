package jp.aquafactory.apprenticecodex.gametest.enchantment.attribute;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.manashield.ManaShieldCharmGameTestScenarios;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.longstride.LongStrideFluidMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.placeAbsoluteFluidTestBasin;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LegacyAttributeGameTests {
    private LegacyAttributeGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        LegacyAttributeScenarios.longStrideMobilityStillAddsBaseMovementSpeedBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideStandsOnWaterLavaAndFlowingFluid(GameTestHelper helper) {
        LegacyAttributeScenarios.longStrideStandsOnWaterLavaAndFlowingFluid(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideFluidMovementRespectsExcludedStatesAndLavaDamage(GameTestHelper helper) {
        LegacyAttributeScenarios.longStrideFluidMovementRespectsExcludedStatesAndLavaDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void configuredAttributeEnchantmentAmountsKeepDefaultsAndScaleLinearly(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.configuredAmountsPerLevelKeepDefaultsAndScaleLinearly(helper);
    }

    public static void longStrideBuoyancyRisesWhileSneakingAndPreservesHorizontalMovement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 3, 0), "long_stride_buoyancy_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), player.blockPosition(), Blocks.WATER.defaultBlockState());
            player.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            player.setShiftKeyDown(true);
            player.setOnGround(true);
            player.fallDistance = 8.0F;
            player.setDeltaMovement(0.12D, -0.05D, -0.08D);

            LongStrideFluidMovement.apply(player);

            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.02D) < 1.0E-9D,
                    "LongStride should accelerate upward while submerged and sneaking: "
                            + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.12D) < 1.0E-9D
                            && Math.abs(player.getDeltaMovement().z + 0.08D) < 1.0E-9D,
                    "LongStride buoyancy should preserve horizontal movement: " + player.getDeltaMovement());
            helper.assertFalse(player.onGround(),
                    "LongStride buoyancy should clear onGround while submerged");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "LongStride buoyancy should reset fall distance");

            for (var i = 0; i < 8; ++i) {
                LongStrideFluidMovement.apply(player);
            }
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - 0.1D) < 1.0E-9D,
                    "LongStride buoyancy should cap upward speed at 0.1: " + player.getDeltaMovement().y);

            var surfacingPlayer = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 0),
                    "long_stride_surface_transition_test");
            var columnBottom = surfacingPlayer.blockPosition();
            helper.getLevel().setBlock(columnBottom.below(), Blocks.STONE.defaultBlockState(), 3);
            for (var offset = 0; offset < 3; ++offset) {
                var fluidPos = columnBottom.above(offset);
                helper.getLevel().setBlock(fluidPos, Blocks.WATER.defaultBlockState(), 3);
                for (var direction : Direction.Plane.HORIZONTAL) {
                    helper.getLevel().setBlock(fluidPos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
                }
            }
            surfacingPlayer.addEffect(new MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY.get(), 200, 0));
            surfacingPlayer.setShiftKeyDown(true);
            surfacingPlayer.setOnGround(false);
            surfacingPlayer.setDeltaMovement(0.0D, -0.15D, 0.0D);

            for (var tick = 0; tick < 80; ++tick) {
                LongStrideFluidMovement.apply(surfacingPlayer);
                if (surfacingPlayer.onGround()) {
                    break;
                }
                surfacingPlayer.move(MoverType.SELF,
                        surfacingPlayer.getDeltaMovement());
            }

            var topFluidPos = columnBottom.above(2);
            var expectedSurfaceY = topFluidPos.getY()
                    + helper.getLevel().getFluidState(topFluidPos).getHeight(helper.getLevel(), topFluidPos)
                    + 0.02D;
            helper.assertTrue(surfacingPlayer.onGround()
                            && Math.abs(surfacingPlayer.getY() - expectedSurfaceY) < 1.0E-9D,
                    "LongStride should rise through a deep fluid column and settle on its surface: y="
                            + surfacingPlayer.getY() + ", expected=" + expectedSurfaceY);
        });
    }
}
