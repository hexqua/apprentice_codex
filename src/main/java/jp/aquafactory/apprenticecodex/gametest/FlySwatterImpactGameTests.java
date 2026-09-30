package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
// GameTestの登録はアノテーション走査によるため、通常のJava参照はない。
@SuppressWarnings("unused")
public final class FlySwatterImpactGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.fly_swatter_impact";
    private static final float DAMAGE = 4;
    private static final float EPSILON = 1.0e-4f;

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void areaDamageIsUniformAndOrdinaryWallsBlockIt(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var near = s.zombie(s.origin.add(0, 0, 0.8));
            var corner = s.zombie(s.origin.add(-2.8, 0, 2.8));
            var blocked = s.zombie(s.origin.add(2, 0, 0));
            var outside = s.zombie(s.origin.add(4.5, 0, -2));
            s.owner.setPos(s.origin.add(0, 0, -1));
            float ownerHealth = s.owner.getHealth();
            s.wall(1);
            s.missile().impactBlock(s.origin);
            h.assertTrue(near.getHealth() < 20 && Math.abs(near.getHealth() - corner.getHealth()) < EPSILON,
                    "Unobstructed targets must receive equal damage even at cube corners beyond the old sphere");
            h.assertTrue(blocked.getHealth() == 20 && outside.getHealth() == 20,
                    "Walls and the area boundary must reject ordinary splash targets");
            h.assertTrue(s.owner.getHealth() == ownerHealth, "Area damage must protect its owner");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartSplashIgnoresWallsAndDamagesOnlyOnce(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var dragon = s.dragon(s.origin.add(15, 0, 0));
            dragon.getParts()[0].setPos(s.origin.add(0.5, 0, 0));
            dragon.getParts()[1].setPos(s.origin.add(1.5, 0, 0));
            s.wall(1);
            var missile = s.missile();
            float expected = s.singleDragonDamage(missile);
            missile.impactBlock(s.origin);
            h.assertTrue(Math.abs(200 - dragon.getHealth() - expected) < EPSILON,
                    "Exposed multipart hitboxes must damage their parent once despite walls toward its eyes");
            h.assertTrue(dragon.getDeltaMovement().lengthSqr() == 0,
                    "Multipart targets must receive no damage or blast knockback");
            // 別のミサイルのロックまで重複排除してしまわないことも保証する。
            s.missile().impactBlock(s.origin);
            h.assertTrue(Math.abs(200 - dragon.getHealth() - expected * 2) < EPSILON,
                    "Separate missiles must each apply one hit to the same multipart target");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartParentBoxDoesNotCreatePhantomSplashHits(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var dragon = s.dragon(s.origin.add(5, 0, 0));
            for (var part : dragon.getParts()) part.setPos(s.origin.add(20, 0, 0));
            s.missile().impactBlock(s.origin);
            h.assertTrue(dragon.getHealth() == 200,
                    "A multipart parent's broad bounding box must not count as a real splash hitbox");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void directPartHitAndBurstLifetimeNeverRepeatDamage(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var dragon = s.dragon(s.origin.add(15, 0, 0));
            var part = dragon.getParts()[0];
            part.setPos(s.origin);
            var missile = s.missile();
            missile.setRadius(0);
            float expected = s.singleDragonDamage(missile);
            missile.impactEntity(s.origin.add(0, 0.5, 0), part);
            h.assertTrue(Math.abs(200 - dragon.getHealth() - expected) < EPSILON,
                    "A direct part hit must apply one parent hit even with zero splash radius");
            var center = missile.position();
            float health = dragon.getHealth();
            missile.impactEntity(center, part);
            missile.onAntiMagic(MagicData.getPlayerMagicData(s.owner));
            for (int tick = 0; tick < 13; ++tick) s.tick(missile);
            h.assertTrue(missile.isBursting() && !missile.isRemoved() && missile.position().equals(center)
                            && dragon.getHealth() == health,
                    "Burst must remain stationary without repeated damage or an anti-magic restart");
            s.tick(missile);
            h.assertTrue(missile.isRemoved(), "Burst must end after fourteen ticks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void entityContactUsesIntersectionAndDetectsStartInside(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(s.origin.add(3, 0, 0));
            var missile = s.missile();
            var start = s.origin.add(0, 1, 0);
            missile.setPos(start);
            missile.setDeltaMovement(new Vec3(4, 0, 0));
            s.tick(missile);
            h.assertTrue(missile.isBursting() && missile.position().x < target.getX() - 0.4
                            && Math.abs(missile.position().y - start.y) < 1.0e-6,
                    "Impact center must stay at the entry point instead of teleporting to target feet");
        }
        try (var s = new Scene(h)) {
            s.zombie(s.origin);
            var missile = s.missile();
            var start = s.origin.add(0, 1, 0);
            missile.setPos(start);
            missile.setDeltaMovement(new Vec3(1, 0, 0));
            s.tick(missile);
            h.assertTrue(missile.isBursting() && missile.position().equals(start),
                    "A projectile already inside a moving hitbox must impact at its current position");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void dragonFlightAimsAtRealPartAndRetainsCrystalDamage(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var dragon = s.dragon(s.origin.add(12, 0, 0));
            for (var part : dragon.getParts()) part.setPos(s.origin.add(30, 0, 0));
            dragon.getParts()[2].setPos(dragon.position());
            var missile = s.missile();
            var start = s.origin.add(0, 1.5, 0);
            missile.launch(start, start, new Vec3(1, 0, 0), dragon, 0);
            int deadline = missile.getArrivalTicks();
            for (int tick = 0; tick < deadline && !missile.isBursting() && !missile.isRemoved(); ++tick) s.tick(missile);
            h.assertTrue(missile.isBursting() && dragon.getHealth() < 200,
                    "Flight must hit an actual dragon part before the deadline instead of aiming above its body");
        }
        try (var s = new Scene(h)) {
            var crystal = Objects.requireNonNull(EntityType.END_CRYSTAL.create(h.getLevel()));
            crystal.setPos(s.origin.add(1, 0, 0));
            h.getLevel().addFreshEntity(crystal);
            s.entities.add(crystal);
            s.missile().impactBlock(s.origin);
            h.assertTrue(crystal.isRemoved(), "Area damage must retain End Crystal support");
        }
        h.succeed();
    }

    private static final class ImpactProjectile extends FlySwatterProjectileEntity {
        ImpactProjectile(Scene scene) {
            super(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), scene.helper.getLevel(), scene.owner);
            setDamage(DAMAGE);
            setRadius(3);
        }

        void impactBlock(Vec3 center) {
            onHitBlock(new BlockHitResult(center, Direction.UP, BlockPos.containing(center), false));
        }

        void impactEntity(Vec3 center, Entity target) {
            onHitEntity(new EntityHitResult(target, center));
        }
    }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

        Scene(GameTestHelper helper) {
            this.helper = helper;
            origin = helper.absoluteVec(new Vec3(2.5, 30.5, 2.5));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "fly_impact_test"));
            owner.setPos(origin.add(-10, 0, 0));
            owner.setNoGravity(true);
            helper.getLevel().addFreshEntity(owner);
            entities.add(owner);
        }

        Zombie zombie(Vec3 position) {
            var zombie = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
            zombie.setNoAi(true);
            zombie.setNoGravity(true);
            Objects.requireNonNull(zombie.getAttribute(Attributes.ARMOR)).setBaseValue(0);
            zombie.setPos(position);
            helper.getLevel().addFreshEntity(zombie);
            entities.add(zombie);
            return zombie;
        }

        EnderDragon dragon(Vec3 position) {
            var dragon = Objects.requireNonNull(EntityType.ENDER_DRAGON.create(helper.getLevel()));
            dragon.setNoAi(true);
            dragon.setPos(position);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            helper.getLevel().addFreshEntity(dragon);
            for (var part : dragon.getParts()) part.setPos(position);
            entities.add(dragon);
            return dragon;
        }

        float singleDragonDamage(ImpactProjectile missile) {
            var control = dragon(origin.add(35, 0, 0));
            var source = CombatOwnerResolver.createDamageSource(helper.getLevel(), missile, owner,
                    missile.getCombatOwnerUuid(), DamageTypes.FLY_SWATTER);
            // CombatToolsの既知の胴体補正は維持し、その一回分と比較する。
            CombatTools.applyDamage(control, DAMAGE, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK);
            float damage = 200 - control.getHealth();
            helper.assertTrue(damage > 0, "Control dragon must accept one Fly Swatter hit");
            control.discard();
            return damage;
        }

        ImpactProjectile missile() {
            var missile = new ImpactProjectile(this);
            missile.setPos(origin);
            entities.add(missile);
            return missile;
        }

        void wall(int x) {
            for (int y = -1; y <= 3; ++y) for (int z = -2; z <= 2; ++z) {
                var pos = BlockPos.containing(origin.add(x, y, z));
                blocks.putIfAbsent(pos, helper.getLevel().getBlockState(pos));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
        }

        void tick(FlySwatterProjectileEntity missile) {
            ++missile.tickCount;
            missile.tick();
        }

        @Override public void close() {
            entities.forEach(Entity::discard);
            blocks.forEach((pos, state) -> helper.getLevel().setBlockAndUpdate(pos, state));
        }
    }
}
