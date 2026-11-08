package jp.aquafactory.apprenticecodex.gametest.spell.compoundphial;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

final class CompoundPhialScenarios {
    private CompoundPhialScenarios() {}

    static void compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var owner = EntityType.SHEEP.create(level);
        helper.assertTrue(owner != null, "Failed to create Compound Phial owner target");
        owner.setNoAi(true);
        var ownerPos = helper.absoluteVec(new Vec3(2.0D, 2.0D, 3.0D));
        owner.moveTo(ownerPos.x, ownerPos.y, ownerPos.z, 0.0F, 0.0F);
        level.addFreshEntity(owner);

        var fullDamageTarget = EntityType.SHEEP.create(level);
        helper.assertTrue(fullDamageTarget != null, "Failed to create Compound Phial full-damage target");
        fullDamageTarget.setNoAi(true);
        var fullDamageTargetPos = helper.absoluteVec(new Vec3(3.2D, 2.0D, 2.5D));
        fullDamageTarget.moveTo(fullDamageTargetPos.x, fullDamageTargetPos.y, fullDamageTargetPos.z, 0.0F, 0.0F);
        level.addFreshEntity(fullDamageTarget);

        var falloffTarget = EntityType.SHEEP.create(level);
        helper.assertTrue(falloffTarget != null, "Failed to create Compound Phial falloff target");
        falloffTarget.setNoAi(true);
        var falloffTargetPos = helper.absoluteVec(new Vec3(4.7D, 2.0D, 2.5D));
        falloffTarget.moveTo(falloffTargetPos.x, falloffTargetPos.y, falloffTargetPos.z, 0.0F, 0.0F);
        level.addFreshEntity(falloffTarget);

        var ownerHealth = owner.getHealth();
        var fullDamageTargetHealth = fullDamageTarget.getHealth();
        var falloffTargetHealth = falloffTarget.getHealth();

        var projectilePos = helper.absoluteVec(new Vec3(2.5D, 4.0D, 2.5D));
        var projectile = new CompoundPhialProjectileEntity(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), level, owner);
        projectile.setPos(projectilePos.x, projectilePos.y, projectilePos.z);
        projectile.setDeltaMovement(0.0D, -0.8D, 0.0D);
        projectile.setDamage(4.0F);
        projectile.setSplashRadius(2.0F);
        projectile.setPotionColorRandom(level);
        level.addFreshEntity(projectile);

        helper.runAtTickTime(8, () -> {
            helper.assertTrue(projectile.isRemoved(), "Compound Phial projectile did not impact during the test");

            var ownerTaken = ownerHealth - owner.getHealth();
            var fullDamageTaken = fullDamageTargetHealth - fullDamageTarget.getHealth();
            var falloffTaken = falloffTargetHealth - falloffTarget.getHealth();

            helper.assertTrue(ownerTaken > 0.0F, "Compound Phial should still hit its owner inside the splash");
            helper.assertTrue(Math.abs(fullDamageTaken - ownerTaken) < 0.1F,
                    "Compound Phial full-damage band should apply equal damage to nearby targets: target="
                            + fullDamageTaken + ", owner=" + ownerTaken);
            helper.assertTrue(falloffTaken >= fullDamageTaken * 0.6F - 0.1F,
                    "Compound Phial falloff target should keep at least 60% damage: target="
                            + falloffTaken + ", full=" + fullDamageTaken);
            helper.assertTrue(falloffTaken < fullDamageTaken,
                    "Compound Phial falloff target should still take less than full-band damage");
            helper.assertTrue(Math.abs(falloffTaken - Math.round(falloffTaken)) > 0.05F,
                    "Compound Phial falloff damage should not be rounded to whole damage steps: " + falloffTaken);
            helper.succeed();
        });
    }
}
