package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CombatToolsGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private CombatToolsGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void failedNoKnockbackDamageDoesNotSuppressLaterKnockback(GameTestHelper helper) {
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        target.setInvulnerable(true);

        var applied = CombatTools.applyDamage(
                target,
                2.0F,
                helper.getLevel().damageSources().generic(),
                null,
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );
        helper.assertFalse(applied, "Invulnerable target should reject test damage");

        target.setInvulnerable(false);
        target.setDeltaMovement(Vec3.ZERO);
        target.knockback(0.5D, 1.0D, 0.0D);
        helper.assertTrue(
                target.getDeltaMovement().horizontalDistanceSqr() > 1.0e-8D,
                "Failed no-knockback damage should not suppress a later knockback"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void successfulNoKnockbackDamageSuppressesOnlyDamageKnockback(GameTestHelper helper) {
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        target.setDeltaMovement(Vec3.ZERO);

        var applied = CombatTools.applyDamage(
                target,
                2.0F,
                helper.getLevel().damageSources().generic(),
                null,
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );
        helper.assertTrue(applied, "Damage should be applied to the test target");
        helper.assertTrue(
                target.getDeltaMovement().horizontalDistanceSqr() <= 1.0e-8D,
                "No-knockback damage should suppress its automatic knockback"
        );

        target.knockback(0.5D, 1.0D, 0.0D);
        helper.assertTrue(
                target.getDeltaMovement().horizontalDistanceSqr() > 1.0e-8D,
                "No-knockback damage should allow an explicit follow-up knockback"
        );
        helper.succeed();
    }
}
