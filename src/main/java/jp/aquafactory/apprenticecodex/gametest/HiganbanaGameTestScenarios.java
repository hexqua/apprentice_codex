package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.higanbana.Higanbana;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class HiganbanaGameTestScenarios {
    private static final double POSITION_EPSILON = 1.0E-6D;
    private static final float VALUE_EPSILON = 1.0E-4F;

    private HiganbanaGameTestScenarios() {
    }

    static void higanbanaHasNoRecastAndKeepsSlashCounts(GameTestHelper helper) {
        var spell = (Higanbana) SpellRegistry.HIGANBANA.get();
        helper.assertTrue(spell.getRecastCount(1, null) == 0,
                "Higanbana should no longer expose recasts");

        helper.assertTrue(spell.getSlashCount() == 4,
                "Higanbana should keep its fixed four-slash sequence");

        helper.succeed();
    }

    static void higanbanaAutomaticallySlashesWithoutFollowingOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "higanbana_stationary_test");
        owner.setYRot(30.0F);
        var weapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
        weapon.setRemainingSlashCount(2);
        weapon.setFirstSlashStandby(5);
        level.addFreshEntity(weapon);

        var initialPosition = weapon.position();
        var initialYaw = weapon.getYRot();
        owner.setPos(owner.getX() + 4.0D, owner.getY(), owner.getZ() + 4.0D);
        owner.setYRot(initialYaw + 90.0F);

        tickWeapon(level, weapon, 4);
        helper.assertTrue(weapon.getRemainingSlashCount() == 2,
                "Higanbana should wait for canSlash before its first slash");
        assertStationary(helper, weapon, initialPosition, initialYaw);

        tickWeapon(level, weapon, 1);
        helper.assertTrue(weapon.getRemainingSlashCount() == 1,
                "Higanbana should consume one slash after the first standby");

        tickWeapon(level, weapon, 5);
        helper.assertTrue(weapon.getRemainingSlashCount() == 0,
                "Higanbana should automatically consume its final slash");
        helper.assertFalse(weapon.isRemoved(),
                "Higanbana should keep the final slash effect visible before removal");
        assertStationary(helper, weapon, initialPosition, initialYaw);

        tickWeapon(level, weapon, 10);
        helper.assertFalse(weapon.isRemoved(),
                "Higanbana should preserve the full final slash effect duration");
        tickWeapon(level, weapon, 1);
        helper.assertTrue(weapon.isRemoved(),
                "Higanbana should remove itself after the final slash effect");

        owner.discard();
        helper.succeed();
    }

    static void higanbanaDamageHasNoKnockbackAndHealsHalf(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "higanbana_damage_test");
        owner.setHealth(owner.getMaxHealth() - 10.0F);
        var weapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        weapon.setRemainingSlashCount(1);
        level.addFreshEntity(weapon);

        var target = createZombie(level, weapon.position().add(weapon.getLookAngle().normalize().scale(0.75D)));
        var initialTargetHealth = target.getHealth();
        var initialOwnerHealth = owner.getHealth();
        var initialMovement = new Vec3(0.125D, 0.0D, -0.25D);
        target.setDeltaMovement(initialMovement);

        weapon.slash(level);

        var dealtDamage = initialTargetHealth - target.getHealth();
        helper.assertTrue(dealtDamage > 0.0F, "Higanbana should damage a valid target");
        helper.assertTrue(target.getDeltaMovement().distanceTo(initialMovement) < POSITION_EPSILON,
                "Higanbana damage should not knock the target back");
        helper.assertTrue(Math.abs(owner.getHealth() - (initialOwnerHealth + dealtDamage * 0.5F)) < VALUE_EPSILON,
                "Higanbana should heal exactly 50% of the damage dealt");

        weapon.discard();
        target.discard();
        owner.discard();
        helper.succeed();
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(new Vec3(2.5D, 2.0D, 2.5D));
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create Higanbana test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void tickWeapon(ServerLevel level, HiganbanaKatanaEntity weapon, int ticks) {
        for (var tick = 0; tick < ticks && !weapon.isRemoved(); ++tick) {
            weapon.tickOnServer(level);
        }
    }

    private static void assertStationary(GameTestHelper helper, HiganbanaKatanaEntity weapon,
                                         Vec3 expectedPosition, float expectedYaw) {
        helper.assertTrue(weapon.position().distanceTo(expectedPosition) < POSITION_EPSILON,
                "Higanbana should remain at its summoned position");
        helper.assertTrue(Math.abs(weapon.getYRot() - expectedYaw) < VALUE_EPSILON,
                "Higanbana should retain its summoned direction");
    }
}
