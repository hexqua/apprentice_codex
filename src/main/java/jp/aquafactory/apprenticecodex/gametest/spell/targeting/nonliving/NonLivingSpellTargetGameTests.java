package jp.aquafactory.apprenticecodex.gametest.spell.targeting.nonliving;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandBurst;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandState;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceBurst;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class NonLivingSpellTargetGameTests {
    private static final String TEMPLATE = "gametest/crystal_targeting";

    private NonLivingSpellTargetGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void moonLightChargeCutDestroysCrystal(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var cut = new MoonLightChargeCutEntity(EntityRegistry.MOON_LIGHT_CHARGE_CUT.get(),
                    helper.getLevel(), scene.owner);
            cut.setPos(scene.origin);
            cut.setYRot(-90);
            cut.setup(8, 1);
            scene.add(cut);
            for (int tick = 0; tick < MoonLightChargeCutEntity.PROCESS_START_DELAY_TICKS
                    + MoonLightChargeCutEntity.PROCESS_DURATION_TICKS + 1; tick++) {
                if (!cut.isRemoved()) {
                    scene.scope.step(cut, true);
                }
            }
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void arcaneBeamDestroysCrystalAndCombatFilterRejectsItems(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var item = Objects.requireNonNull(EntityType.ITEM.create(helper.getLevel()));
            item.setPos(scene.origin.add(2, 0, 0));
            scene.add(item);
            var hits = RaycastTools.sampleBeamHits(helper.getLevel(), scene.origin, scene.origin.add(8, 0, 0),
                    1, 0.2, target -> CombatTools.isValidCombatTarget(target, scene.owner));
            helper.assertTrue(hits.contains(crystal) && !hits.contains(item),
                    "Combat beam must include crystals and reject dropped items");
            var beam = new ArcaneBeamEntity(EntityRegistry.ARCANE_BEAM.get(), helper.getLevel(), scene.owner);
            beam.setPos(scene.origin);
            beam.setYRot(-90);
            beam.setup(0, 0, 8, 1);
            beam.setDamage(1);
            scene.add(beam);
            scene.scope.step(beam, true);
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void magicSpearBlastDestroysCrystal(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var spear = new ImpactSpear(scene);
            spear.setPos(crystal.position());
            spear.setup(1, new Vec3(1, 0, 0), new Vec3(0, 0, 1), null);
            spear.setDeltaMovement(Vec3.ZERO);
            scene.add(spear);
            scene.scope.run(() -> spear.impact(crystal));
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterDrillDestroysCrystal(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var drill = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(),
                    helper.getLevel(), scene.owner);
            drill.setDamage(1);
            drill.setPos(scene.origin);
            scene.add(drill);
            drill.updateOwnerTarget(helper.getLevel(), new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY, crystal.getBoundingBox().getCenter(), crystal, null));
            for (int tick = 0; tick < 16 && !crystal.isRemoved(); tick++)
                scene.scope.run(() -> drill.tickOnServer(helper.getLevel()));
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grindRunnerContactDestroysCrystal(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(),
                    helper.getLevel(), scene.owner);
            wheel.setDamage(1);
            wheel.setSummonSettings(crystal.position(), 0, 1);
            scene.add(wheel);
            scene.scope.run(() -> wheel.tickOnServer(helper.getLevel()));
            scene.scope.run(() -> wheel.tickOnServer(helper.getLevel()));
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bloodBrandBurstDestroysCrystalWithoutHealingCaster(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var origin = scene.sheep(crystal.position().add(-1, 0, 0));
            scene.owner.setHealth(10);
            scene.scope.run(() -> BloodBrandBurst.burst(helper.getLevel(), origin, scene.owner,
                    new BloodBrandState(scene.owner.getUUID(), 1, 3), false));
            scene.assertDestroyed(crystal);
            helper.assertTrue(scene.owner.getHealth() == 10,
                    "Destroying a crystal must not count as absorbed living health");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inscribeIceBurstDestroysCrystal(GameTestHelper helper) {
        try (var scene = new NonLivingTargetFixture(helper)) {
            var crystal = scene.crystal();
            var origin = scene.sheep(crystal.position().add(-1, 0, 0));
            scene.scope.run(() -> InscribeIceBurst.burstFromDagger(helper.getLevel(), origin, scene.owner, scene.owner, 1));
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    private static final class ImpactSpear extends MagicSpearMissileEntity {
        private ImpactSpear(NonLivingTargetFixture scene) {
            super(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), scene.helper.getLevel(), scene.owner);
        }

        private void impact(Entity target) {
            onHitEntity(new EntityHitResult(target));
        }
    }


}
