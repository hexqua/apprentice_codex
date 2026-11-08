package jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestCleanup.onTestEnd;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEST_POS;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnBroom;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnHoverrideBroom;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomVibrationGameTests {
    private static final BlockPos VIBRATION_SENSOR_POS = new BlockPos(4, 1, 1);

    private BroomVibrationGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 90)
    public static void mountedFloatmountRepeatedlyEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "floatmount_vibration_rider");
        var events = observeVibration(helper, player);
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Floatmount riding should emit an Elytra flight vibration");
            var firstCount = events.count();
            helper.assertTrue(firstCount > 0, "The observed flight vibration must belong to this rider");
            helper.runAfterDelay(50, () -> {
                assertVibrationSensorActive(helper,
                        "Continuous Floatmount riding should emit another vibration after the sensor resets");
                helper.assertTrue(events.count() > firstCount, "This rider must emit a later flight vibration");
                player.stopRiding();
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mountedHoverrideEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnHoverrideBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "hoverride_vibration_rider");
        var events = observeVibration(helper, player);
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Hoverride riding should emit an Elytra flight vibration");
            helper.assertTrue(events.received(), "The Hoverride flight vibration must belong to this rider");
            player.stopRiding();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unoccupiedBroomDoesNotEmitFlightVibration(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var eventListener = observeVibration(helper, broom);

        helper.runAfterDelay(25, () -> {
            // 実スカルクセンサーは並列配置された近隣GameTestの飛行振動も拾うため、この箒の近傍だけを監視する。
            helper.assertFalse(eventListener.received(),
                    "An unoccupied broom must not emit an Elytra flight vibration");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void deepSensorSilencesMountedBroomFlightVibration(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = serverRider(helper, "floatmount_deep_sensor_rider");
        var senseSensor = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.SENSE_SENSOR.get());
        player.addEffect(new MobEffectInstance(senseSensor, 60, 0));
        mountForVibrationTest(helper, broom, player);
        var events = observeVibration(helper, player);
        placeVibrationSensor(helper);

        helper.runAfterDelay(35, () -> {
            helper.assertFalse(events.received(),
                    "Deep Sensor should silence player-sourced broom flight vibrations");
            player.stopRiding();
            helper.succeed();
        });
    }

    private static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            String profileName
    ) {
        return mountForVibrationTest(helper, broom, serverRider(helper, profileName));
    }

    private static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            ServerPlayer player
    ) {
        player.setPos(broom.getX(), broom.getY(), broom.getZ());
        helper.assertTrue(player.startRiding(broom, true), "Broom vibration test rider should mount directly");
        return player;
    }

    private static void placeVibrationSensor(GameTestHelper helper) {
        // 騎乗後に配置し、ENTITY_MOUNTではなく継続する飛行振動だけを検証する。
        helper.setBlock(VIBRATION_SENSOR_POS, Blocks.SCULK_SENSOR);
    }

    private static boolean isVibrationSensorActive(GameTestHelper helper) {
        var state = helper.getBlockState(VIBRATION_SENSOR_POS);
        return state.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE
                && state.getValue(SculkSensorBlock.POWER) > 0;
    }

    private static void assertVibrationSensorActive(GameTestHelper helper, String message) {
        helper.assertTrue(isVibrationSensorActive(helper), message);
    }

    private static LocalElytraGlideListener observeVibration(GameTestHelper helper, Entity source) {
        var listener = new LocalElytraGlideListener(helper.absolutePos(TEST_POS), source);
        var dynamic = new DynamicGameEventListener<GameEventListener>(listener);
        onTestEnd(helper, () -> dynamic.remove(helper.getLevel()));
        dynamic.add(helper.getLevel());
        return listener;
    }

    private static final class LocalElytraGlideListener implements GameEventListener {
        private final PositionSource positionSource;
        private final Entity expectedSource;
        private int count;

        private LocalElytraGlideListener(BlockPos position, Entity expectedSource) {
            positionSource = new BlockPositionSource(position);
            this.expectedSource = expectedSource;
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
                @NotNull Holder<GameEvent> gameEvent,
                @NotNull GameEvent.Context context,
                @NotNull Vec3 sourcePosition
        ) {
            if (gameEvent.equals(GameEvent.ELYTRA_GLIDE) && context.sourceEntity() == expectedSource) {
                count++;
                return true;
            }
            return false;
        }

        private boolean received() {
            return count > 0;
        }

        private int count() {
            return count;
        }
    }
}
