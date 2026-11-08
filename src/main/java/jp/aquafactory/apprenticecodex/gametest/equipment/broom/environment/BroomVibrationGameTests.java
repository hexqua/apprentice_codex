package jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.prepareVibrationIsolation;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomVibrationGameTests {
    private BroomVibrationGameTests() {}

    static final BlockPos VIBRATION_SENSOR_POS = new BlockPos(4, 1, 1);

    @GameTest(template = TEMPLATE, timeoutTicks = 90)
    public static void mountedFloatmountRepeatedlyEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "floatmount_vibration_rider");
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Floatmount riding should emit an Elytra flight vibration");
            helper.runAfterDelay(50, () -> {
                assertVibrationSensorActive(helper,
                        "Continuous Floatmount riding should emit another vibration after the sensor resets");
                player.stopRiding();
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mountedHoverrideEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnHoverrideBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "hoverride_vibration_rider");
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Hoverride riding should emit an Elytra flight vibration");
            player.stopRiding();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unoccupiedBroomDoesNotEmitFlightVibration(GameTestHelper helper) {
        spawnBroom(helper, 1.5D);
        var eventListener = new LocalElytraGlideListener(helper.absolutePos(TEST_POS));
        var dynamicListener = new DynamicGameEventListener<GameEventListener>(eventListener);
        dynamicListener.add(helper.getLevel());

        helper.runAfterDelay(25, () -> {
            dynamicListener.remove(helper.getLevel());
            // 実スカルクセンサーは並列配置された近隣GameTestの飛行振動も拾うため、この箒の近傍だけを監視する。
            helper.assertFalse(eventListener.received(),
                    "An unoccupied broom must not emit an Elytra flight vibration");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.broom_deep_sensor_isolated", timeoutTicks = 100)
    public static void deepSensorSilencesMountedBroomFlightVibration(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> {
            prepareVibrationIsolation(helper);
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper, "floatmount_deep_sensor_rider");
            var senseSensor = EffectRegistry.SENSE_SENSOR.get();
            player.addEffect(new MobEffectInstance(senseSensor, 60, 0));
            mountForVibrationTest(helper, broom, player);
            placeVibrationSensor(helper);

            var vibrations = new ArrayList<String>();
            for (int tick = 1; tick <= 35; tick++) {
                int observedTick = tick;
                helper.runAfterDelay(tick, () -> {
                    var entity = helper.getBlockEntity(VIBRATION_SENSOR_POS);
                    if (entity instanceof SculkSensorBlockEntity sensor) {
                        var info = sensor.getVibrationData().getCurrentVibration();
                        if (info != null) vibrations.add("tick=" + observedTick + ", event="
                                + BuiltInRegistries.GAME_EVENT.getKey(info.gameEvent()) + ", pos=" + info.pos()
                                + ", source=" + info.getEntity(helper.getLevel()).orElse(null));
                    }
                });
            }
            helper.runAfterDelay(35, () -> {
                helper.assertFalse(isVibrationSensorActive(helper),
                        "Deep Sensor should silence player-sourced broom flight vibrations: observed=" + vibrations
                                + ", effect=" + player.getEffect(senseSensor) + ", riding=" + player.isPassenger());
                player.removeEffect(senseSensor);
                helper.runAfterDelay(15, () -> {
                    helper.assertTrue(isVibrationSensorActive(helper),
                            "The isolated sensor must still receive broom flight after Deep Sensor is removed");
                    player.stopRiding();
                    helper.succeed();
                });
            });
        });
    }

    static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            String profileName
    ) {
        return mountForVibrationTest(helper, broom, serverRider(helper, profileName));
    }

    static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            ServerPlayer player
    ) {
        player.setPos(broom.getX(), broom.getY(), broom.getZ());
        helper.assertTrue(player.startRiding(broom, true), "Broom vibration test rider should mount directly");
        return player;
    }

    static void placeVibrationSensor(GameTestHelper helper) {
        // 騎乗後に配置し、ENTITY_MOUNTではなく継続する飛行振動だけを検証する。
        helper.setBlock(VIBRATION_SENSOR_POS, Blocks.SCULK_SENSOR);
    }

    static boolean isVibrationSensorActive(GameTestHelper helper) {
        var state = helper.getBlockState(VIBRATION_SENSOR_POS);
        return state.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE
                && state.getValue(SculkSensorBlock.POWER) > 0;
    }

    static void assertVibrationSensorActive(GameTestHelper helper, String message) {
        helper.assertTrue(isVibrationSensorActive(helper), message);
    }

    static final class LocalElytraGlideListener implements GameEventListener {
        private final PositionSource positionSource;
        private boolean received;

        private LocalElytraGlideListener(BlockPos position) {
            positionSource = new BlockPositionSource(position);
        }

        @Override
        public @NotNull PositionSource getListenerSource() {
            return positionSource;
        }

        @Override
        public int getListenerRadius() {
            return 2;
        }

        @Override
        public boolean handleGameEvent(
                @NotNull ServerLevel level,
                @NotNull GameEvent gameEvent,
                @NotNull GameEvent.Context context,
                @NotNull Vec3 sourcePosition
        ) {
            if (gameEvent.equals(GameEvent.ELYTRA_GLIDE)) {
                received = true;
            }
            return true;
        }

        private boolean received() {
            return received;
        }
    }
}
