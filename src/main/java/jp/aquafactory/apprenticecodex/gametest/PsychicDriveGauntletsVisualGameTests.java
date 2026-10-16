package jp.aquafactory.apprenticecodex.gametest;

import com.google.gson.JsonParser;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.MotionData;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import jp.aquafactory.apprenticecodex.particle.PsychicDriveGauntletsSparkMotion;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録される。
public final class PsychicDriveGauntletsVisualGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsVisualGameTests() {}

    private static Snapshot arm(Phase phase, float charge, Impact impact, int hold, long launch, double started) {
        return new Snapshot(phase, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, new Vec3(0, 0, 1),
                launch, (long) started, charge, (long) started, 0, impact, (long) started, hold,
                new MotionData(started, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 1, 0));
    }

    private static Snapshot drill(long launch, double started) {
        return arm(Phase.HOLDING, 1, Impact.ENTITY, PsychicDriveGauntletsArm.DRILL_TICKS, launch, started);
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-6, message + ": " + actual + " != " + expected);
    }

    @GameTest(template = TEMPLATE)
    public static void auraContinuesThroughFlightAndFadesOnceForEveryTerminalPhase(GameTestHelper helper) {
        for (var terminal : List.of(Phase.HOLDING, Phase.FOLLOW_THROUGH, Phase.RETURNING, Phase.IDLE)) {
            var state = new PsychicDriveGauntletsVisualState();
            var charge = arm(Phase.CHARGING, 1, Impact.NONE, 2, 0, 0);
            var flight = arm(Phase.OUTBOUND, 1, Impact.NONE, 2, 20, 20);
            near(helper, state.aura(charge, 19), 0.08, "Full charge must have its existing aura strength");
            near(helper, state.aura(flight, 20), 0.08, "Launch must preserve the charged aura");
            var contact = arm(terminal, 1, Impact.ENTITY, 12, 20, 23);
            near(helper, state.aura(contact, 23), 0.08, "Terminal state must start fading from the flight strength");
            near(helper, state.aura(contact, 23.5), 0.06, "Repeated rendering must not extend the fade");
            near(helper, state.aura(contact, 24), 0.04, "Aura must be half strength after one tick");
            near(helper, state.aura(contact, 25), 0, "Aura must disappear after two ticks");
            near(helper, state.aura(contact, 30), 0, "Late snapshots must not revive a finished aura");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void partialChargeCancellationAndIndependentArmsDoNotLeakAura(GameTestHelper helper) {
        var left = new PsychicDriveGauntletsVisualState();
        var right = new PsychicDriveGauntletsVisualState();
        var partial = arm(Phase.OUTBOUND, 0.5F, Impact.NONE, 2, 10, 10);
        near(helper, left.aura(partial, 10), 0.0525, "Partial launch must retain charge-proportional aura");
        near(helper, right.aura(arm(Phase.OUTBOUND, 0, Impact.NONE, 2, 10, 10), 10), 0,
                "An ordinary attack on the other arm must not acquire an aura");
        near(helper, left.aura(null, 11), 0.0525, "Cancellation must start the same short fade");
        near(helper, left.aura(null, 13), 0, "Cleared snapshots must not keep the aura alive");
        near(helper, new PsychicDriveGauntletsVisualState().aura(drill(10, 20), 20), 0,
                "A newly tracked impact must not invent a flight aura");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void continuousSparksFillOnlyTheGapsAndNeverCatchUp(GameTestHelper helper) {
        var state = new PsychicDriveGauntletsVisualState();
        var sample = drill(100, 103);
        var emitted = new ArrayList<Integer>();
        for (int elapsed = 0; elapsed <= 12; elapsed++) {
            if (state.emitContinuousSpark(sample, 103 + elapsed)) emitted.add(elapsed);
            helper.assertTrue(!state.emitContinuousSpark(sample, 103 + elapsed),
                    "Duplicate snapshots must not emit twice in the same tick");
        }
        helper.assertTrue(emitted.equals(List.of(2, 6, 10)), "Continuous sparks must omit intermediate hit ticks");
        helper.assertTrue(PsychicDriveGauntletsVisualState.INITIAL_SPARKS
                        + 2 * PsychicDriveGauntletsVisualState.INTERMEDIATE_SPARKS + emitted.size() == 11,
                "A complete drill must emit eleven new sparks before its finisher");
        var delayed = new PsychicDriveGauntletsVisualState();
        helper.assertTrue(!delayed.emitContinuousSpark(sample, 106), "A late first sample must not replay the two-tick spark");
        helper.assertTrue(delayed.emitContinuousSpark(sample, 109), "A future scheduled gap may still emit");
        helper.assertTrue(!delayed.emitContinuousSpark(sample, 114), "Skipped gaps must not be emitted in a burst");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationBlocksAndNewAttacksKeepEmittersIndependent(GameTestHelper helper) {
        var state = new PsychicDriveGauntletsVisualState();
        helper.assertTrue(state.emitContinuousSpark(drill(100, 103), 105), "Drill fixture must emit its first gap");
        for (var sample : List.of(
                arm(Phase.RETURNING, 1, Impact.ENTITY, 12, 100, 103),
                arm(Phase.HOLDING, 1, Impact.BLOCK, 2, 100, 103),
                arm(Phase.HOLDING, 0.5F, Impact.ENTITY, 2, 100, 103),
                arm(Phase.HOLDING, 1, Impact.NONE, 12, 100, 103))) {
            helper.assertTrue(!state.emitContinuousSpark(sample, 109),
                    "Only a successful full-charge entity drill may continuously emit");
        }
        helper.assertTrue(!state.emitContinuousSpark(null, 110), "Missing arms must stop emission");
        helper.assertTrue(state.emitContinuousSpark(drill(200, 203), 205), "A new launch must get an independent schedule");
        var other = new PsychicDriveGauntletsVisualState();
        helper.assertTrue(other.emitContinuousSpark(drill(200, 203), 205), "The opposite arm must not share emission history");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sparksBounceOnceOnRealTerrainWithoutExtendingLife(GameTestHelper helper) {
        var floor = new BlockPos(2, 2, 2);
        helper.setBlock(floor, Blocks.STONE);
        var world = helper.absolutePos(floor);
        var box = new AABB(world.getX() + 0.4, world.getY() + 1, world.getZ() + 0.4,
                world.getX() + 0.5, world.getY() + 1.1, world.getZ() + 0.5);
        var falling = new Vec3(0.10, -0.2, 0.12);
        var actual = Entity.collideBoundingBox(null, falling, box, helper.getLevel(), List.of());
        near(helper, actual.y, 0, "Fixture must collide with the floor");
        var first = PsychicDriveGauntletsSparkMotion.collide(falling, actual, 7, 14, false);
        helper.assertTrue(first.bounced() && !first.removed() && first.lifetime() == 11,
                "First landing must permit one bounce and cap remaining life at four ticks");
        near(helper, first.velocity().x, 0.05, "Landing must halve horizontal speed");
        near(helper, first.velocity().y, 0.05, "Landing must return one quarter of falling speed");
        near(helper, first.velocity().z, 0.06, "Landing must halve horizontal speed on both axes");
        var late = PsychicDriveGauntletsSparkMotion.collide(falling, actual, 9, 10, false);
        helper.assertTrue(late.lifetime() == 10, "A late landing must never extend the original lifetime");
        var second = PsychicDriveGauntletsSparkMotion.collide(falling, actual, 10, 11, true);
        helper.assertTrue(second.removed() && second.velocity().equals(Vec3.ZERO), "Second landing must remove the spark");
        var ceiling = PsychicDriveGauntletsSparkMotion.collide(new Vec3(0.1, 0.2, 0.1), new Vec3(0, 0, 0.1), 3, 12, false);
        helper.assertTrue(!ceiling.bounced() && !ceiling.removed() && ceiling.velocity().equals(new Vec3(0, 0, 0.1)),
                "Walls and ceilings must clip velocity without consuming the ground bounce");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sparkRegistrationReusesTheExistingTexture(GameTestHelper helper) throws Exception {
        var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "psychic_drive_gauntlets_spark");
        helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.get(id) == ParticleRegistry.PSYCHIC_DRIVE_GAUNTLETS_SPARK.get(),
                "The original spark type must be registered");
        try (var stream = Objects.requireNonNull(PsychicDriveGauntletsVisualGameTests.class.getResourceAsStream(
                "/assets/apprenticecodex/particles/psychic_drive_gauntlets_spark.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var textures = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("textures");
            helper.assertTrue(textures.size() == 1 && textures.get(0).getAsString().equals("apprenticecodex:glow_square"),
                    "The new spark must reuse the existing glow texture");
        }
        try (var texture = PsychicDriveGauntletsVisualGameTests.class.getResourceAsStream(
                "/assets/apprenticecodex/textures/particle/glow_square.png")) {
            helper.assertTrue(texture != null, "The referenced particle texture must exist");
        }
        helper.succeed();
    }
}
