package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.*;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SacredArrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.sacred_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void selectionIsNearestAndFrozenAtStart(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var near = s.zombie(4, 0); var far = s.zombie(8, 0);
            near.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT, 300));
            far.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            s.owner.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            var data = s.preCast();
            h.assertTrue(((SacredArrowCastData) data.getAdditionalCastData()).targetId().equals(near.getUUID()), "Must select the nearest valid target across both effects, excluding self");
            near.removeEffect(MobEffectRegistry.GUIDING_BOLT);
            near.setPos(near.position().add(50, 0, 0));
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrow().isChasing(), "Losing the mark or leaving the search area must not cancel the lock");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void noTargetDoesNotReselectAndCancellationClearsState(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(5, 0);
            var data = s.preCast();
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(!s.castArrow().isChasing(), "An empty selection must stay empty during casting");
            data = s.preCast();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, data, true);
            h.assertTrue(!(data.getAdditionalCastData() instanceof SacredArrowCastData), "Cancellation must clear the selection");
            data = s.preCast(); target.discard();
            s.castArrow().discard();
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(!s.castArrow().isChasing(), "A removed target must fall back to normal mode");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void searchBoundsAlliesAndDirectCast(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(49, 0);
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId() == null, "Outside the 48 block AABB must not lock");
            target.setPos(s.origin.add(47.9, 0, 0));
            h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId().equals(target.getUUID()), "Inside the 48 block AABB must lock");
            var board = h.getLevel().getScoreboard();
            var team = board.addPlayerTeam("sacred_" + UUID.randomUUID().toString().substring(0, 8));
            team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(s.owner.getScoreboardName(), team);
                board.addPlayerToTeam(target.getScoreboardName(), team);
                h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId() == null, "Allies must not lock");
            } finally { board.removePlayerTeam(team); }
            var data = MagicData.getPlayerMagicData(s.owner); data.setAdditionalCastData(null);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(s.castArrow().isChasing(), "A direct cast must select a target when no cast snapshot exists");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void normalDamageMarksOnlySuccessfulLivingHits(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(1.5, 0);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(arrow.isRemoved() && Math.abs(target.getHealth() - 12) < 0.01, "Normal hit must deal full damage and consume the arrow");
            h.assertTrue(target.getEffect(EffectRegistry.SACRED_SIGN).getDuration() == 300, "Normal hit must apply a 15 second mark");
            target.removeEffect(EffectRegistry.SACRED_SIGN);
            target.setInvulnerable(true);
            arrow = s.arrow(null, new Vec3(1, 0, 0)); step(arrow);
            h.assertTrue(arrow.isRemoved() && !target.hasEffect(EffectRegistry.SACRED_SIGN), "Failed damage must not mark or pierce");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void piercingStopsAtPrimaryAndNeverRefreshesMarks(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var bystander = s.zombie(0.7, 0);
            var target = s.zombie(1.4, 0);
            var behind = s.zombie(1.9, 0);
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 123));
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); step(arrow);
            h.assertTrue(Math.abs(bystander.getHealth() - 18) < 0.01, "Piercing damage must be 25 percent");
            h.assertTrue(Math.abs(target.getHealth() - 12) < 0.01 && arrow.isRemoved(), "Primary damage must be full and stop flight");
            h.assertTrue(behind.getHealth() == 20, "Entities behind the primary must not be damaged");
            h.assertTrue(target.getEffect(EffectRegistry.SACRED_SIGN).getDuration() == 123 && !bystander.hasEffect(EffectRegistry.SACRED_SIGN), "Homing hits must not apply or refresh marks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void successfulPiercingCannotHitTwice(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0); var bystander = s.zombie(1, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); step(arrow);
            bystander.invulnerableTime = 0;
            arrow.setPos(s.origin.add(0, 0.9, 0)); step(arrow);
            h.assertTrue(bystander.getHealth() == 18, "A pierced target must not receive a second successful hit");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lostTargetContinuesStraightAndExpires(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(15, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); target.discard(); step(arrow);
            var velocity = arrow.getDeltaMovement();
            var other = s.zombie(3, 0);
            step(arrow);
            h.assertTrue(other.getHealth() == 18 && !arrow.isRemoved(), "Lost-target arrows must retain reduced piercing damage");
            for (int i = 0; i < 58; i++) { arrow.setPos(s.origin.add(0, 10, 0)); step(arrow); }
            h.assertTrue(!arrow.isRemoved() && arrow.getDeltaMovement().distanceTo(velocity) < 1.0e-6, "Lost-target arrows must fly straight without gravity");
            step(arrow); h.assertTrue(arrow.isRemoved(), "Lost-target lifetime must be capped at 60 ticks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void reverseHomingConvergesAndNormalFlightFalls(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0);
            var homing = s.arrow(target, new Vec3(-1, 0, 0));
            for (int i = 0; i < 100 && !homing.isRemoved(); i++) step(homing);
            h.assertTrue(homing.isRemoved() && target.getHealth() < 20, "Even backwards launch must converge onto the target");
            target.discard();
            var arrow = s.arrow(null, new Vec3(0, 0, 1)); var start = arrow.position();
            for (int i = 0; i < 10; i++) step(arrow);
            h.assertTrue(arrow.position().distanceTo(start.add(0, 0, 20)) < 1.0e-6, "First ten movements must travel at two blocks per tick without gravity");
            step(arrow);
            h.assertTrue(Math.abs(arrow.getDeltaMovement().y + 0.05) < 1.0e-6, "Gravity must start after ten complete ticks");
            arrow.tickCount = 199; step(arrow);
            h.assertTrue(arrow.isRemoved(), "All arrows must expire at 200 ticks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void blocksFluidsImmunityAndGlow(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(6, 0);
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                var arrow = s.arrow(null, new Vec3(1, 0, 0)); step(arrow);
                h.assertTrue(arrow.isRemoved(), "Normal arrows must collide with blocks");
                arrow = s.arrow(target, new Vec3(1, 0, 0)); step(arrow);
                // 対象の中心へ上向きにも曲がるため、水平移動が厳密に2になることは要求しない。
                h.assertTrue(!arrow.isRemoved() && arrow.getX() > s.origin.x + 1.9
                        && Math.abs(arrow.getDeltaMovement().length() - 2) < 1.0e-6,
                        "Homing arrows must enter physical blocks without slowing: position=" + arrow.position());
                step(arrow);
                h.assertTrue(!arrow.isRemoved() && arrow.getX() > block.getX() + 1,
                        "Homing arrows must emerge from physical blocks");
                arrow.discard();
                for (var fluid : List.of(Blocks.WATER, Blocks.LAVA)) {
                    h.getLevel().setBlockAndUpdate(block, fluid.defaultBlockState());
                    arrow = s.arrow(null, new Vec3(1, 0, 0)); var start = arrow.position(); step(arrow);
                    h.assertTrue(!arrow.isRemoved() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6, "Fluids must not alter arrow flight");
                    arrow.discard();
                }
            } finally { h.getLevel().setBlockAndUpdate(block, previous); }
            h.assertTrue(EntityRegistry.SACRED_ARROW.get().is(io.redspace.ironsspellbooks.util.ModTags.GUIDING_BOLT_IMMUNE), "Both arrow modes must be immune to Guided steering");
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            h.assertTrue(target.isCurrentlyGlowing(), "Sacred Sign must enable glowing");
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300));
            target.removeEffect(EffectRegistry.SACRED_SIGN);
            h.assertTrue(target.isCurrentlyGlowing(), "Removing Sacred Sign must preserve vanilla glowing");
        }
        h.succeed();
    }

    private static void step(Entity entity) { entity.tickCount++; entity.tick(); }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final SacredArrow spell = (SacredArrow) SpellRegistry.SACRED_ARROW.get();
        final List<Entity> entities = new ArrayList<>();
        Scene(GameTestHelper h) {
            helper = h; origin = h.absoluteVec(new Vec3(2, 30, 2));
            owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "sacred_test"));
            owner.setPos(origin); owner.setNoGravity(true); add(owner);
        }
        void add(Entity e) { entities.add(e); helper.getLevel().addFreshEntity(e); }
        Zombie zombie(double x, double z) {
            var e = EntityType.ZOMBIE.create(helper.getLevel());
            e.setPos(origin.add(x, 0, z)); e.setNoAi(true); e.setNoGravity(true);
            e.getAttribute(Attributes.ARMOR).setBaseValue(0); add(e); return e;
        }
        SacredArrowEntity arrow(LivingEntity target, Vec3 direction) {
            var e = new SacredArrowEntity(EntityRegistry.SACRED_ARROW.get(), helper.getLevel());
            e.launch(owner, origin.add(0, 0.9, 0), direction, 8, 0.25f, 300, target); add(e); return e;
        }
        MagicData preCast() {
            var data = MagicData.getPlayerMagicData(owner);
            spell.onServerPreCast(helper.getLevel(), 1, owner, data); return data;
        }
        SacredArrowEntity castArrow() {
            return helper.getLevel().getEntitiesOfClass(SacredArrowEntity.class, owner.getBoundingBox().inflate(3)).stream()
                    .filter(e -> e.getOwner() == owner && !e.isRemoved()).findFirst().orElseThrow();
        }
        @Override public void close() {
            helper.getLevel().getEntitiesOfClass(SacredArrowEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner).forEach(Entity::discard);
            entities.forEach(Entity::discard);
        }
    }
}
