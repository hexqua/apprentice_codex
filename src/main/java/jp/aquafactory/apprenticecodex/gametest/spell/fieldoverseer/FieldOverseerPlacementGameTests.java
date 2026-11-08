package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerPlacementGameTests {
    private FieldOverseerPlacementGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_isolated", timeoutTicks = 80)
    public static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_fall_test");
        var anchorPos = DeployedSpellTestFixture.of(helper).absolute(new BlockPos(0, 2, 0));
        DeployedSpellTestFixture.of(helper).setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = FieldOverseerTestSupport.create(helper, owner, anchorPos, 100.0F, 40);
        var initialY = staff.getY();

        helper.runAtTickTime(5, () -> DeployedSpellTestFixture.of(helper).setBlock(new BlockPos(0, 1, 0), Blocks.AIR));
        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isAlive() && !staff.isRemoved(),
                    "FieldOverseer staff should remain alive after losing support");
            helper.assertTrue(staff.getY() < initialY - 0.05D,
                    "FieldOverseer staff should fall after losing support: " + staff.getY());
        });
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_isolated")
    public static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_owner_damage_test");
        var anchorPos = DeployedSpellTestFixture.of(helper).absolute(new BlockPos(0, 2, 0));
        DeployedSpellTestFixture.of(helper).setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = FieldOverseerTestSupport.create(helper, owner, anchorPos, 100.0F, 40);
        var initialHealth = staff.getHealth();

        var damaged = staff.hurt(owner.damageSources().playerAttack(owner), 5.0F);
        helper.assertFalse(damaged, "FieldOverseer staff should reject owner damage");
        helper.assertTrue(Math.abs(staff.getHealth() - initialHealth) < 0.0001F,
                "FieldOverseer staff health changed after owner damage");
        helper.succeed();
    }
}
