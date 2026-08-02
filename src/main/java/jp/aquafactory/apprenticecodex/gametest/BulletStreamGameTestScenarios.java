package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class BulletStreamGameTestScenarios {
    private static final int SPIN_UP_DELAY_TICKS = 40;
    private static final int RELEASE_DURATION_TICKS = 10;
    private static final ResourceLocation CAST_TIME_REDUCTION_TEST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bullet_stream_cast_time_test");

    private BulletStreamGameTestScenarios() {
    }

    static void waitsForSpinUpThenFiresEveryTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 0), "bullet_stream_spin_up_test"
            );
            prepareShootingLane(helper);
            faceForward(player);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(0, 2, 8));
            target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
            target.setHealth(200.0F);
            var initialHealth = target.getHealth();

            var spell = (BulletStream) SpellRegistry.BULLET_STREAM.get();
            var weapon = spell.onCastNoWeapon(level, 1, player, MagicData.getPlayerMagicData(player));
            for (var tick = 0; tick < SPIN_UP_DELAY_TICKS - 1; tick++) {
                weapon.tickOnServer(level);
            }
            helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0E-6F,
                    "Bullet Stream should not damage targets before spin-up completes");
            helper.assertFalse(weapon.getIsRecoilTick(),
                    "Bullet Stream should not recoil before spin-up completes");

            weapon.tickOnServer(level);
            helper.assertTrue(target.getHealth() < initialHealth,
                    "Bullet Stream should fire its first shot when spin-up completes");
            helper.assertTrue(weapon.getIsRecoilTick(),
                    "Bullet Stream should recoil while firing");
            var healthAfterFirstShot = target.getHealth();
            target.invulnerableTime = 20;
            target.setDeltaMovement(Vec3.ZERO);

            weapon.tickOnServer(level);
            helper.assertTrue(target.getHealth() < healthAfterFirstShot,
                    "Bullet Stream should fire every tick and bypass damage i-frames");
            var movement = target.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x) < 1.0E-6D && Math.abs(movement.z) < 1.0E-6D,
                    "Bullet Stream shots should not knock targets back");
            weapon.discard();
        });
    }

    static void releaseKeepsWeaponForTenTicksAndOnlyFinishesAfterFiring(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 0), "bullet_stream_release_test"
            );
            faceForward(player);
            var spell = (BulletStream) SpellRegistry.BULLET_STREAM.get();

            var interruptedDuringSpinUp = spell.onCastNoWeapon(
                    level, 1, player, MagicData.getPlayerMagicData(player)
            );
            interruptedDuringSpinUp.tickOnServer(level);
            interruptedDuringSpinUp.releaseWeapon();
            helper.assertFalse(interruptedDuringSpinUp.isSpinningDown(),
                    "Bullet Stream should not play spin-finish when interrupted before firing");
            assertTenTickRelease(helper, interruptedDuringSpinUp);

            var interruptedAfterFiring = spell.onCastNoWeapon(
                    level, 1, player, MagicData.getPlayerMagicData(player)
            );
            for (var tick = 0; tick < SPIN_UP_DELAY_TICKS; tick++) {
                interruptedAfterFiring.tickOnServer(level);
            }
            helper.assertTrue(interruptedAfterFiring.getIsRecoilTick(),
                    "Bullet Stream should be firing before its finish sequence is tested");
            interruptedAfterFiring.releaseWeapon();
            helper.assertTrue(interruptedAfterFiring.isSpinningDown(),
                    "Bullet Stream should play spin-finish after firing has begun");
            helper.assertFalse(interruptedAfterFiring.getIsRecoilTick(),
                    "Released Bullet Stream should stop recoiling immediately");
            assertTenTickRelease(helper, interruptedAfterFiring);
        });
    }

    static void castDurationIgnoresCastTimeReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 0), "bullet_stream_cast_time_test"
            );
            var castTimeReduction = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
            helper.assertTrue(castTimeReduction != null,
                    "Bullet Stream test should resolve the cast-time reduction attribute");
            if (castTimeReduction == null) {
                return;
            }
            castTimeReduction.addTransientModifier(new AttributeModifier(
                    CAST_TIME_REDUCTION_TEST_MODIFIER_ID,
                    0.5D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            var spell = (BulletStream) SpellRegistry.BULLET_STREAM.get();
            var fixedCastTime = spell.getCastTime(1);
            helper.assertTrue(spell.getEffectiveCastTime(1, player) == fixedCastTime,
                    "Bullet Stream cast duration should ignore cast-time reduction");
        });
    }

    private static void assertTenTickRelease(
            GameTestHelper helper,
            jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity weapon
    ) {
        for (var tick = 0; tick < RELEASE_DURATION_TICKS - 1; tick++) {
            weapon.tickOnServer(helper.getLevel());
        }
        helper.assertFalse(weapon.isRemoved(),
                "Released Bullet Stream should remain for the first nine finish ticks");
        weapon.tickOnServer(helper.getLevel());
        helper.assertTrue(weapon.isRemoved(),
                "Released Bullet Stream should disappear on the tenth finish tick");
    }

    private static void faceForward(net.neoforged.neoforge.common.util.FakePlayer player) {
        player.setYRot(0.0F);
        player.setYBodyRot(0.0F);
        player.setYHeadRot(0.0F);
        player.setXRot(0.0F);
    }

    private static void prepareShootingLane(GameTestHelper helper) {
        for (var x = -1; x <= 1; x++) {
            for (var z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.AIR);
            }
        }
    }
}
