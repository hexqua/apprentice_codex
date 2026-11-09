package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CastingMovementTestSupport {
    private CastingMovementTestSupport() {}

    public static final UUID CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID =
            UUID.fromString("04a46352-a09b-44fb-b504-92ab5f69f969");

    public static void assertCastingMoveSpeedAdjustment(
            GameTestHelper helper,
            double externalBonus,
            double expectedAmount,
            String message
    ) {
        var actualAmount = CastingMoveSpeedAdjustment.computeAvailableBonus(externalBonus);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount + " for external bonus " + externalBonus);
    }

    public static void assertCastingMoveSpeedModifierAmount(
            GameTestHelper helper,
            AttributeInstance attributeInstance,
            @Nullable UUID excludedModifierId,
            double expectedAmount,
            String message
    ) {
        var actualAmount = attributeInstance.getModifiers().stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .filter(modifier -> excludedModifierId == null || !excludedModifierId.equals(modifier.getId()))
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + attributeInstance.getModifiers());
    }
}
