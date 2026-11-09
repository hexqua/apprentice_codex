package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;

import static jp.aquafactory.apprenticecodex.gametest.spell.autoturret.AutoTurretTestSupport.createAutoTurretRestockTestEntity;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class AutoTurretRestockScenarios {
    private AutoTurretRestockScenarios() {}

    static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret restock should restore ammo to the initial count: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 13.0F) < 1.0E-4F,
                    "AutoTurret restock should spend configured mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_full_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret full-ammo restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret full-ammo restock should not change ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 30.0F) < 1.0E-4F,
                    "AutoTurret full-ammo restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_insufficient_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(16.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret insufficient-mana restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 2,
                    "AutoTurret insufficient-mana restock should not restore ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 16.0F) < 1.0E-4F,
                    "AutoTurret insufficient-mana restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_discard_delay_test");
        var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 3, 10);
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.setMana(10.0F);
        turret.setDamage(100.0F);
        turret.setRestBulletCount(1);
        level.addFreshEntity(owner);
        helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 4));

        helper.runAtTickTime(60, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret should remain alive during the ammo depletion grace period");
            helper.assertTrue(turret.getRestBulletCount() <= 0,
                    "AutoTurret should have spent its final shot before restock: " + turret.getRestBulletCount());
            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock during discard delay should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 3,
                    "AutoTurret restock should restore ammo during discard delay: " + turret.getRestBulletCount());
        });

        helper.runAtTickTime(140, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret restock should clear the ammo depletion discard timer");
            helper.succeed();
        });
    }
}
