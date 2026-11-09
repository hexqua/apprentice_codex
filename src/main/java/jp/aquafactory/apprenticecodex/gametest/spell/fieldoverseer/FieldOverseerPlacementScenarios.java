package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondLegacyScenarios.createFieldOverseerTestEntity;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class FieldOverseerPlacementScenarios {
    private FieldOverseerPlacementScenarios() {}

    static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_fall_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialY = staff.getY();

        helper.runAtTickTime(5, () -> helper.setBlock(new BlockPos(0, 1, 0), Blocks.AIR));
        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isAlive() && !staff.isRemoved(),
                    "FieldOverseer staff should remain alive after losing support");
            helper.assertTrue(staff.getY() < initialY - 0.05D,
                    "FieldOverseer staff should fall after losing support: " + staff.getY());
        });
    }

    static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_owner_damage_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialHealth = staff.getHealth();

        var damaged = staff.hurt(owner.damageSources().playerAttack(owner), 5.0F);
        helper.assertFalse(damaged, "FieldOverseer staff should reject owner damage");
        helper.assertTrue(Math.abs(staff.getHealth() - initialHealth) < 0.0001F,
                "FieldOverseer staff health changed after owner damage");
        helper.succeed();
    }
}
