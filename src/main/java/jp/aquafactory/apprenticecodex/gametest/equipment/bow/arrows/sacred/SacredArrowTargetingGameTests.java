package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowCastData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SacredArrowTargetingGameTests {
    private SacredArrowTargetingGameTests() {
    }

    private static final String TEMPLATE = "gametest/arrow_combat";
    private static final String BATCH = "apprenticecodex.sacred_arrow";

    @GameTest(template = "gametest/sacred_arrow_target", batch = BATCH)
    public static void selectionIsNearestAndFrozenAtStart(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var near = s.zombie(4, 0);
            var far = s.zombie(8, 0);
            near.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT, 300));
            far.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            s.owner.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            var data = s.preCast();
            h.assertTrue(((SacredArrowCastData) data.getAdditionalCastData()).targetId().equals(near.getUUID()), "Must select the nearest valid target across both effects, excluding self");
            near.removeEffect(MobEffectRegistry.GUIDING_BOLT);
            // 射程外への移動とチャンク非表示化を混同しないよう、同じ水平チャンク内で離す。
            near.setPos(near.position().add(0, 50, 0));
            h.assertTrue(h.getLevel().getEntity(near.getUUID()) == near, "Relocated target must remain loaded for the lock retention test");
            s.owner.setYRot(90);
            s.owner.setXRot(90);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrow().isChasing(), "Losing the mark, leaving the search area or turning after selection must not cancel the lock");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void noTargetDoesNotReselectAndCancellationClearsState(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var target = s.zombie(5, 0);
            var data = s.preCast();
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(!s.castArrow().isChasing(), "An empty selection must stay empty during casting");
            data = s.preCast();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, data, true);
            h.assertTrue(!(data.getAdditionalCastData() instanceof SacredArrowCastData), "Cancellation must clear the selection");
            data = s.preCast();
            target.discard();
            s.castArrow().discard();
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(!s.castArrow().isChasing(), "A removed target must fall back to normal mode");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/sacred_arrow_target", batch = BATCH)
    public static void searchBoundsAlliesAndDirectCast(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
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
            } finally {
                board.removePlayerTeam(team);
            }
            var data = MagicData.getPlayerMagicData(s.owner);
            data.setAdditionalCastData(null);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(s.castArrow().isChasing(), "A direct cast must select a target when no cast snapshot exists");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void captureConeIncludesYawAndPitchBoundaries(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var target = s.zombie(8, 0);
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            for (boolean vertical : List.of(false, true)) {
                for (double degrees : new double[]{-60.1, -60, -59.9, 0, 59.9, 60, 60.1, 180}) {
                    double angle = Math.toRadians(degrees);
                    var offset = new Vec3(Math.cos(angle), vertical ? Math.sin(angle) : 0,
                            vertical ? 0 : Math.sin(angle)).scale(8);
                    target.setPos(s.owner.getEyePosition().add(offset).subtract(0, target.getBbHeight() * 0.5, 0));
                    var data = (SacredArrowCastData) s.preCast().getAdditionalCastData();
                    h.assertTrue((data.targetId() != null) == (Math.abs(degrees) <= 60),
                            "Capture cone boundary mismatch: vertical=" + vertical + ", degrees=" + degrees);
                }
            }
            target.setPos(s.origin.add(8, 0, 0));
            s.owner.setXRot(90);
            h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId() == null,
                    "Looking down must exclude a distant ground-level target");
            target.setPos(s.owner.getEyePosition().add(0, -8, 0).subtract(0, target.getBbHeight() * 0.5, 0));
            h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId().equals(target.getUUID()),
                    "Looking down must include targets below the caster");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void captureConeKeepsDistancePriorityAndNormalFallback(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var near = s.zombie(3, 3);
            var center = s.zombie(8, 0);
            var behind = s.zombie(-2, 0);
            near.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT, 300));
            center.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            behind.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            h.assertTrue(((SacredArrowCastData) s.preCast().getAdditionalCastData()).targetId().equals(near.getUUID()),
                    "Distance must win over alignment, while closer targets behind the caster are excluded");
            near.discard();
            center.discard();
            var data = s.preCast();
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(!s.castArrow().isChasing(), "Only out-of-cone marks must produce a normal arrow");
        }
        h.succeed();
    }
}
