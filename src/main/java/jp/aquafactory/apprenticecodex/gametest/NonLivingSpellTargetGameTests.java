package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
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
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class NonLivingSpellTargetGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private NonLivingSpellTargetGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void moonLightChargeCutDestroysCrystal(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
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
                    ++cut.tickCount;
                    cut.tick();
                }
            }
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void arcaneBeamDestroysCrystalAndCombatFilterRejectsItems(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
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
            ++beam.tickCount;
            beam.tick();
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void magicSpearBlastDestroysCrystal(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var crystal = scene.crystal();
            var spear = new ImpactSpear(scene);
            spear.setPos(crystal.position());
            spear.setup(1, new Vec3(1, 0, 0), new Vec3(0, 0, 1), null);
            spear.setDeltaMovement(Vec3.ZERO);
            scene.add(spear);
            spear.impact(crystal);
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterDrillDestroysCrystal(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var crystal = scene.crystal();
            var drill = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(),
                    helper.getLevel(), scene.owner);
            drill.setDamage(1);
            drill.setPos(scene.origin);
            scene.add(drill);
            drill.updateOwnerTarget(helper.getLevel(), new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY, crystal.getBoundingBox().getCenter(), crystal, null));
            for (int tick = 0; tick < 16 && !crystal.isRemoved(); tick++) drill.tickOnServer(helper.getLevel());
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grindRunnerContactDestroysCrystal(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var crystal = scene.crystal();
            var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(),
                    helper.getLevel(), scene.owner);
            wheel.setDamage(1);
            wheel.setSummonSettings(crystal.position(), 0, 1);
            scene.add(wheel);
            wheel.tickOnServer(helper.getLevel());
            wheel.tickOnServer(helper.getLevel());
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bloodBrandBurstDestroysCrystalWithoutHealingCaster(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var crystal = scene.crystal();
            var origin = scene.sheep(crystal.position().add(-1, 0, 0));
            scene.owner.setHealth(10);
            BloodBrandBurst.burst(helper.getLevel(), origin, scene.owner,
                    new BloodBrandState(scene.owner.getUUID(), 1, 3), false);
            scene.assertDestroyed(crystal);
            helper.assertTrue(scene.owner.getHealth() == 10,
                    "Destroying a crystal must not count as absorbed living health");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inscribeIceBurstDestroysCrystal(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var crystal = scene.crystal();
            var origin = scene.sheep(crystal.position().add(-1, 0, 0));
            InscribeIceBurst.burstFromDagger(helper.getLevel(), origin, scene.owner, scene.owner, 1);
            scene.assertDestroyed(crystal);
        }
        helper.succeed();
    }

    private static final class ImpactSpear extends MagicSpearMissileEntity {
        private ImpactSpear(Scene scene) {
            super(EntityRegistry.MAGIC_SPEAR_MISSILE.get(), scene.helper.getLevel(), scene.owner);
        }

        private void impact(Entity target) {
            onHitEntity(new EntityHitResult(target));
        }
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final Vec3 origin;
        private final FakePlayer owner;
        private final List<Entity> entities = new ArrayList<>();

        private Scene(GameTestHelper helper) {
            this.helper = helper;
            // 同期的に完了・後片付けし、クリスタルの爆発が構造や隣のテストへ届かない高度に置く。
            // 1.20.1のGameTest原点は地下のため、射線と爆風が地形に遮られない上空へ置く。
            origin = helper.absoluteVec(new Vec3(2.5, 260, 2.5));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "crystal_spell_test"));
            owner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            owner.setPos(origin.add(-16, 0, 0));
            owner.setNoGravity(true);
            owner.setYRot(-90);
            add(owner);
        }

        private void add(Entity entity) {
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        private EndCrystal crystal() {
            var crystal = Objects.requireNonNull(EntityType.END_CRYSTAL.create(helper.getLevel()));
            crystal.setPos(origin.add(4, 0, 0));
            add(crystal);
            return crystal;
        }

        private Sheep sheep(Vec3 position) {
            var sheep = Objects.requireNonNull(EntityType.SHEEP.create(helper.getLevel()));
            sheep.setNoAi(true);
            sheep.setNoGravity(true);
            sheep.setPos(position);
            add(sheep);
            return sheep;
        }

        private void assertDestroyed(EndCrystal crystal) {
            helper.assertTrue(crystal.isRemoved(), "Spell damage must destroy an End Crystal");
        }

        @Override
        public void close() {
            entities.forEach(Entity::discard);
        }
    }
}
