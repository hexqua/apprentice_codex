package jp.aquafactory.apprenticecodex.gametest.equipment.broom.control;

import java.util.ArrayList;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomRushAttack;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.broom.HoverrideBroomItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.magicData;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnHoverrideBroom;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.trackEntity;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class HoverrideRushGameTests {
    private HoverrideRushGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void hoverrideTwilightGaleCalibrationAndRushMathMatchContract(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        var twilightGale = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get());
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        stack, 0, twilightGale),
                "Hoverride Broom should accept Twilight Gale calibration");
        helper.assertTrue(HoverrideBroomItem.isRushStyleEnabled(stack),
                "Twilight Gale calibration should enable the rush style");
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        stack, 1, twilightGale),
                "Hoverride Broom should reject duplicate Twilight Gale calibration");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.baseDamage(0.5D, 4.0D, 8.0D) - 4.0F) < 1.0e-6F,
                "Rush minimum speed should use minimum damage");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.baseDamage(0.7D, 4.0D, 8.0D) - 8.0F) < 1.0e-6F,
                "Rush damage should cap at maximum speed");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.knockbackStrength(0.5D) - 0.5D) < 1.0e-9D
                        && Math.abs(HoverrideBroomRushAttack.knockbackStrength(0.7D) - 1.0D) < 1.0e-9D,
                "Rush knockback should scale from 0.5 to 1.0");

        var broomBox = new AABB(0.1D, 0.0D, -0.4D, 0.9D, 0.5D, 0.4D);
        var movement = new Vec3(0.5D, 0.0D, 0.0D);
        var grazingTarget = new AABB(0.0D, 0.0D, 0.8D, 0.6D, 1.8D, 1.4D);
        var outsideTarget = new AABB(0.0D, 0.0D, 1.0D, 0.6D, 1.8D, 1.6D);
        helper.assertTrue(HoverrideBroomRushAttack.intersectsPath(broomBox, movement, grazingTarget),
                "Rush contact padding should accept a target 1.1 blocks from the travel centerline");
        helper.assertFalse(HoverrideBroomRushAttack.intersectsPath(broomBox, movement, outsideTarget),
                "Rush contact padding should reject a target 1.3 blocks from the travel centerline");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushDamageRespectsIframeAndUsesExplicitKnockback(GameTestHelper helper) {
        var config = rushTestConfig(0.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_iframe");
            var magicData = magicData(helper, player);
            magicData.setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush test rider should mount");
            broom.tick();

            var target = spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, 0.0D));
            var initialHealth = target.getHealth();
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();

            helper.assertTrue(target.getHealth() < initialHealth,
                    "A validated 0.5 block rush should damage a contact target");
            helper.assertTrue(target.getDeltaMovement().x > 0.0D,
                    "Successful rush damage should knock the target along travel direction");
            helper.assertTrue(broom.isRushAttackActive() && !broom.isPushable(),
                    "An active rush should disable ordinary entity pushing");
            helper.assertTrue(Math.abs(magicData.getMana() - 9.5F) < 1.0e-4F,
                    "An active rush should consume its configured mana cost");

            var healthAfterFirstHit = target.getHealth();
            target.setDeltaMovement(Vec3.ZERO);
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(Math.abs(target.getHealth() - healthAfterFirstHit) < 1.0e-4F,
                    "Rush damage should respect the target's normal invulnerability frames");
            helper.assertTrue(target.getDeltaMovement().lengthSqr() < 1.0e-8D,
                    "A rejected iframe hit must not apply explicit rush knockback");

            var blocker = spawnRushTarget(helper, broom.position().add(0.8D, 0.0D, 0.0D));
            helper.assertFalse(broom.canCollideWith(blocker),
                    "An active rush should ignore entity collision shapes");
            var beforeCollisionMove = broom.getX();
            broom.move(MoverType.SELF, new Vec3(0.3D, 0.0D, 0.0D));
            helper.assertTrue(broom.getX() - beforeCollisionMove > 0.299D,
                    "An active rush should not lose travel distance when crossing a mob");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushRequiresFullCombinedManaAndRejectsAnomalousTravel(GameTestHelper helper) {
        var config = rushTestConfig(1.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_mana");
            var magicData = magicData(helper, player);
            magicData.setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush mana test rider should mount");
            broom.tick();

            var target = spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, 0.0D));
            var initialHealth = target.getHealth();
            magicData.setMana(1.4F);
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-4F,
                    "Rush attack should require the full movement and rush mana cost");
            helper.assertTrue(broom.isManaDepleted() && !broom.isRushAttackActive(),
                    "Insufficient combined mana should deplete propulsion without activating rush");

            magicData.setMana(50.0F);
            broom.tick();
            // 回復確認の静止tickを含む2tick観測なので、平均0.5 block/tickとなる距離を使う。
            var exactCostTarget = spawnRushTarget(helper, broom.position().add(0.8D, 0.0D, 0.0D));
            magicData.setMana(1.5F);
            broom.setPos(broom.getX() + 1.0D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(exactCostTarget.getHealth() < exactCostTarget.getMaxHealth(),
                    "An exact combined mana payment should still apply rush damage");
            helper.assertTrue(broom.isManaDepleted() && broom.isRushAttackActive(),
                    "An exact combined mana payment should attack before entering depleted mode");

            magicData.setMana(50.0F);
            broom.tick();
            var manaBeforeAnomaly = magicData.getMana();
            broom.setPos(broom.getX() + 5.0D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertFalse(broom.isRushAttackActive(),
                    "An anomalously long server-observed displacement must not activate rush");
            helper.assertTrue(Math.abs(magicData.getMana() - (manaBeforeAnomaly - 1.0F)) < 1.0e-4F,
                    "Rejected rush travel should only retain the ordinary movement cost");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushCapsTargetsPerTick(GameTestHelper helper) {
        var config = rushTestConfig(0.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_target_cap");
            magicData(helper, player).setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush target cap rider should mount");
            broom.tick();

            var targets = new ArrayList<Zombie>();
            for (var index = 0; index < 5; ++index) {
                targets.add(spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, index * 0.05D)));
            }
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            var damagedTargets = targets.stream().filter(target -> target.getHealth() < target.getMaxHealth()).count();
            helper.assertTrue(damagedTargets == HoverrideBroomRushAttack.MAX_TARGETS_PER_TICK,
                    "Rush should damage at most four targets per tick");
        }
        helper.succeed();
    }

    private static HoverrideBroomServerConfig.Values rushTestConfig(double forwardManaCost) {
        return new HoverrideBroomServerConfig.Values(
                forwardManaCost,
                0.0D,
                50.0D,
                forwardManaCost,
                0.0D,
                100.0D,
                0.5D,
                20.0D,
                4.0D,
                8.0D,
                0.5D
        );
    }

    private static void installRushStyle(HoverrideBroomEntity broom) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get())
        )) {
            throw new IllegalStateException("Hoverride rush test could not install Twilight Gale");
        }
        broom.setBroomItemStack(stack);
    }

    private static Zombie spawnRushTarget(GameTestHelper helper, Vec3 position) {
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        if (target == null) {
            throw new IllegalStateException("Hoverride rush test could not create a zombie target");
        }
        target.setNoAi(true);
        target.setPos(position);
        trackEntity(helper, target);
        helper.assertTrue(helper.getLevel().addFreshEntity(target), "Broom fixture entity must be registered");
        return target;
    }
}
