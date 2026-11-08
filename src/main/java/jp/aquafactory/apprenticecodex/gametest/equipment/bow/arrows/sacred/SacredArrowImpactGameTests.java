package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred;

import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred.SacredArrowFixture.step;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SacredArrowImpactGameTests {
    private SacredArrowImpactGameTests() {
    }

    private static final String TEMPLATE = "gametest/arrow_combat";
    private static final String BATCH = "apprenticecodex.sacred_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void normalDamageMarksOnlySuccessfulLivingHits(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var target = s.zombie(1.5, 0);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(arrow.isRemoved() && Math.abs(target.getHealth() - 12) < 0.01, "Normal hit must deal full damage and consume the arrow");
            h.assertTrue(target.getEffect(EffectRegistry.SACRED_SIGN).getDuration() == 300, "Normal hit must apply a 15 second mark");
            target.removeEffect(EffectRegistry.SACRED_SIGN);
            target.setInvulnerable(true);
            arrow = s.arrow(null, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(arrow.isRemoved() && !target.hasEffect(EffectRegistry.SACRED_SIGN), "Failed damage must not mark or pierce");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void piercingStopsAtPrimaryAndNeverRefreshesMarks(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var bystander = s.zombie(0.7, 0);
            var target = s.zombie(1.4, 0);
            var behind = s.zombie(1.9, 0);
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 123));
            var arrow = s.arrow(target, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(Math.abs(bystander.getHealth() - 18) < 0.01, "Piercing damage must be 25 percent");
            h.assertTrue(Math.abs(target.getHealth() - 12) < 0.01 && arrow.isRemoved(), "Primary damage must be full and stop flight");
            h.assertTrue(behind.getHealth() == 20, "Entities behind the primary must not be damaged");
            h.assertTrue(target.getEffect(EffectRegistry.SACRED_SIGN).getDuration() == 123 && !bystander.hasEffect(EffectRegistry.SACRED_SIGN), "Homing hits must not apply or refresh marks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void successfulPiercingCannotHitTwice(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var target = s.zombie(12, 0);
            var bystander = s.zombie(1, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0));
            step(arrow);
            bystander.invulnerableTime = 0;
            arrow.setPos(s.origin.add(0, 0.9, 0));
            step(arrow);
            h.assertTrue(bystander.getHealth() == 18, "A pierced target must not receive a second successful hit");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void blocksFluidsImmunityAndGlow(GameTestHelper h) {
        try (var s = new SacredArrowFixture(h)) {
            var target = s.zombie(6, 0);
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                var arrow = s.arrow(null, new Vec3(1, 0, 0));
                step(arrow);
                h.assertTrue(arrow.isRemoved(), "Normal arrows must collide with blocks");
                arrow = s.arrow(target, new Vec3(1, 0, 0));
                step(arrow);
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
                    arrow = s.arrow(null, new Vec3(1, 0, 0));
                    var start = arrow.position();
                    step(arrow);
                    h.assertTrue(!arrow.isRemoved() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6, "Fluids must not alter arrow flight");
                    arrow.discard();
                }
            } finally {
                h.getLevel().setBlockAndUpdate(block, previous);
            }
            h.assertTrue(EntityRegistry.SACRED_ARROW.get().is(ModTags.GUIDING_BOLT_IMMUNE), "Both arrow modes must be immune to Guided steering");
            target.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, 300));
            h.assertTrue(target.isCurrentlyGlowing(), "Sacred Sign must enable glowing");
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300));
            target.removeEffect(EffectRegistry.SACRED_SIGN);
            h.assertTrue(target.isCurrentlyGlowing(), "Removing Sacred Sign must preserve vanilla glowing");
        }
        h.succeed();
    }
}
