package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorObservationBuffer;
import jp.aquafactory.apprenticecodex.spell.deepsensor.SenseSensorVibrationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DeepSensorGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final ResourceLocation STEP_EVENT = ResourceLocation.withDefaultNamespace("step");

    private DeepSensorGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void observationsRefreshGroupAndExpire(GameTestHelper helper) {
        var buffer = new DeepSensorObservationBuffer();
        var sourceUuid = UUID.fromString("a66f2761-a157-4ba5-9641-1f3e25fa845c");
        var observerCenter = Vec3.ZERO;

        buffer.record(new BlockPos(10, 0, 0), 10.0F, 0L, sourceUuid, null, STEP_EVENT);
        buffer.record(new BlockPos(5, 0, 0), 5.0F, 1L, sourceUuid, null, STEP_EVENT);
        helper.assertTrue(buffer.size() == 1, "Entity observations should refresh by source UUID");

        buffer.record(new BlockPos(5, 0, 0), 3.0F, 2L, null, null, STEP_EVENT);
        var display = buffer.selectForDisplay(observerCenter, 2L);
        helper.assertTrue(buffer.size() == 2, "Position observations should remain distinct from entity observations");
        helper.assertTrue(display.size() == 1, "Observations at the same position should collapse for display");
        helper.assertTrue(display.get(0).distance() == 3.0F,
                "The newest observation at a shared position should supply display metadata");

        helper.assertTrue(!buffer.selectForDisplay(observerCenter, 41L).isEmpty(),
                "A refreshed observation should remain visible before its 40 tick lifetime ends");
        helper.assertTrue(buffer.selectForDisplay(observerCenter, 42L).isEmpty(),
                "Observations should expire exactly 40 ticks after reception");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void observationsPrioritizeNearestAndBoundMemory(GameTestHelper helper) {
        var buffer = new DeepSensorObservationBuffer();
        for (var i = 1; i <= 70; i++) {
            buffer.record(
                    new BlockPos(i, 0, 0),
                    i,
                    i,
                    UUID.nameUUIDFromBytes(("deep-sensor-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    null,
                    STEP_EVENT
            );
        }

        helper.assertTrue(buffer.size() == DeepSensorObservationBuffer.MAX_RETAINED_OBSERVATIONS,
                "Deep Sensor memory should retain at most 64 observations");
        var display = buffer.selectForDisplay(Vec3.ZERO, 70L);
        helper.assertTrue(display.size() == DeepSensorObservationBuffer.MAX_DISPLAYED_OBSERVATIONS,
                "Deep Sensor should display at most eight observations");
        helper.assertTrue(display.get(0).distance() == 31.0F
                        && display.get(display.size() - 1).distance() == 38.0F,
                "Displayed observations should choose the nearest non-expired retained entries");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void travelTimeKeepsNearEventsAndCapsDistantEvents(GameTestHelper helper) {
        helper.assertTrue(SenseSensorVibrationEvent.calculateTravelTimeInTicks(0.2F) == 1,
                "Sub-block vibrations should take one tick");
        helper.assertTrue(SenseSensorVibrationEvent.calculateTravelTimeInTicks(2.9F) == 2,
                "Near vibrations should preserve vanilla floor travel time");
        helper.assertTrue(SenseSensorVibrationEvent.calculateTravelTimeInTicks(24.0F) == 3,
                "Distant vibrations should cap at three ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void activeEffectInitializesListenerOnFirstPlayerTick(GameTestHelper helper) {
        var profile = new GameProfile(
                UUID.fromString("ebc1b663-4d7c-4533-bdf2-99215ce57654"),
                "deep_sensor_effect_test"
        );
        var player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var senseSensor = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.SENSE_SENSOR.get());
        player.addEffect(new MobEffectInstance(senseSensor, 40, 0));

        try {
            // 保存済み効果を持つプレイヤーの初回tickと、新規発動直後のtickは同じ初期化経路を通る。
            SenseSensorVibrationEvent.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(senseSensor),
                    "Deep Sensor listener initialization should keep the active effect");
        } finally {
            player.removeEffect(senseSensor);
            SenseSensorVibrationEvent.onPlayerTick(new PlayerTickEvent.Post(player));
        }
        helper.succeed();
    }
}
