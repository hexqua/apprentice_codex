package jp.aquafactory.apprenticecodex.gametest.effect.castingmobility;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.LongStrideMobility;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.CastingMovementTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CastingMobilityGameTests {
    private CastingMobilityGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCastingMoveSpeedAdjustment(helper, 0.0D, 0.8D, "No external bonus should keep full cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.25D, 0.55D, "Diamond-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.50D, 0.30D, "Netherite-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.75D, 0.05D, "Small remaining headroom should stay positive");
            assertCastingMoveSpeedAdjustment(helper, 0.80D, 0.0D, "Exact cap should stop adding more casting move speed");
            assertCastingMoveSpeedAdjustment(helper, 1.10D, 0.0D, "External overshoot should not become a negative correction");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dynamic_casting_movespeed_rebalance_test");
            var effectHolder = EffectRegistry.LONG_STRIDE_MOBILITY;
            var effect = (LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.value();
            var castingMoveSpeed = player.getAttribute(AttributeRegistry.CASTING_MOVESPEED);
            helper.assertTrue(castingMoveSpeed != null,
                    "Dynamic casting mobility test could not resolve the CASTING_MOVESPEED attribute");

            var baseline = castingMoveSpeed.getValue();
            try {
                player.addEffect(new MobEffectInstance(effectHolder, 200, 0));
                helper.assertTrue(castingMoveSpeed != null, "Dynamic casting mobility test lost CASTING_MOVESPEED after addEffect");
                assertCastingMoveSpeedModifierAmount(
                        helper,
                        castingMoveSpeed,
                        null,
                        0.8D,
                        "Dynamic casting mobility effect should initially fill the full cancellation headroom"
                );

                castingMoveSpeed.addTransientModifier(new AttributeModifier(
                        CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                        0.5D,
                        AttributeModifier.Operation.ADD_VALUE
                ));
                effect.applyEffectTick(player, 0);
                assertCastingMoveSpeedModifierAmount(
                        helper,
                        castingMoveSpeed,
                        CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                        0.3D,
                        "Dynamic casting mobility effect should shrink after an external casting move speed bonus is added"
                );

                castingMoveSpeed.removeModifier(CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID);
                effect.applyEffectTick(player, 0);
                assertCastingMoveSpeedModifierAmount(
                        helper,
                        castingMoveSpeed,
                        null,
                        0.8D,
                        "Dynamic casting mobility effect should recover once the external casting move speed bonus is removed"
                );
                player.removeEffect(effectHolder);
                helper.assertTrue(Math.abs(castingMoveSpeed.getValue() - baseline) < 1.0e-9D,
                        "Removing mobility must restore casting movement speed");
            } finally {
                player.removeEffect(effectHolder);
                castingMoveSpeed.removeModifier(CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID);
            }
        });
    }
}
