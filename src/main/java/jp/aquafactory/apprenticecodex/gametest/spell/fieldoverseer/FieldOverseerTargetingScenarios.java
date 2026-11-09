package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondLegacyScenarios.createFieldOverseerTestEntity;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class FieldOverseerTargetingScenarios {
    private FieldOverseerTargetingScenarios() {}

    static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "field_overseer_attack_test");
            var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN.get());
            if (manaRegen != null) {
                manaRegen.setBaseValue(0.0D);
            }
            var ownerMagicData = MagicData.getPlayerMagicData(owner);
            ownerMagicData.setMana(75.0F);
            var anchorPos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
            var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
            // 並列実行中の別構造内の敵を拾わないよう、このテストでは構造内に収まる射程へ絞る.
            staff.configure(anchorPos, 4.0F, 4.0D, 40, 100.0F, 20);

            // 日光や構造外への落下で攻撃対象から外れないよう、床内にハスクを配置する.
            var highHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 2, 2));
            var mediumHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
            var lowHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 2, 2));
            highHealthTarget.setHealth(20.0F);
            mediumHealthTarget.setHealth(10.0F);
            lowHealthTarget.setHealth(5.0F);

            helper.runAfterDelay(52, () -> {
                helper.assertTrue(highHealthTarget.getHealth() < 20.0F,
                        "FieldOverseer should strike the highest-current-health target first");
                helper.assertTrue(Math.abs(mediumHealthTarget.getHealth() - 10.0F) < 0.0001F,
                        "FieldOverseer second strike should wait three ticks");
                helper.assertTrue(Math.abs(lowHealthTarget.getHealth() - 5.0F) < 0.0001F,
                        "FieldOverseer should not strike the lower-health target before its sequence turn");
            });
            helper.runAfterDelay(58, () -> {
                helper.assertTrue(mediumHealthTarget.getHealth() < 10.0F,
                        "FieldOverseer should perform the second strike three ticks after the first");
            });
            helper.runAfterDelay(62, () -> {
                helper.assertTrue(lowHealthTarget.getHealth() < 5.0F,
                        "FieldOverseer should perform the third strike six ticks after the first");
                helper.assertTrue(Math.abs(staff.getCurrentMana() - 100.0F) < 0.0001F,
                        "FieldOverseer should receive two 20 mana transfers after spending one attack cost");
                helper.assertTrue(Math.abs(ownerMagicData.getMana() - 35.0F) < 0.0001F,
                        "FieldOverseer should drain the same amount from the owner: " + ownerMagicData.getMana());
                helper.succeed();
            });
        });
    }
}
