package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorObservationBuffer;
import jp.aquafactory.apprenticecodex.spell.deepsensor.SenseSensorVibrationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DeepSensorGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    // 隣接するGameTest構造はスカルクセンサーの受信範囲に入るため、振動を発生させるテスト同士を分離する。
    private static final String SCULK_STEP_ISOLATED_BATCH = "apprenticecodex.deep_sensor_sculk_step_isolated";
    private static final String SCULK_PROJECTILE_ISOLATED_BATCH = "apprenticecodex.deep_sensor_sculk_projectile_isolated";
    private static final String SCULK_SNEAKING_ISOLATED_BATCH = "apprenticecodex.deep_sensor_sculk_sneaking_isolated";
    private static final String SCULK_WOOL_ISOLATED_BATCH = "apprenticecodex.deep_sensor_sculk_wool_isolated";
    private static final ResourceLocation STEP_EVENT = ResourceLocation.withDefaultNamespace("step");
    private static final BlockPos SENSOR_POS = new BlockPos(1, 2, 1);
    private static final BlockPos VIBRATION_POS = new BlockPos(3, 2, 1);
    private static final List<Holder<GameEvent>> SILENCED_GAME_EVENTS = List.of(
            GameEvent.STEP,
            GameEvent.SWIM,
            GameEvent.HIT_GROUND,
            GameEvent.SPLASH,
            GameEvent.ELYTRA_GLIDE,
            GameEvent.UNEQUIP,
            GameEvent.ENTITY_DISMOUNT,
            GameEvent.EQUIP,
            GameEvent.ENTITY_MOUNT,
            GameEvent.ENTITY_DAMAGE
    );
    private static final List<Holder<GameEvent>> EXPLICITLY_AUDIBLE_GAME_EVENTS = List.of(
            GameEvent.PROJECTILE_SHOOT,
            GameEvent.INSTRUMENT_PLAY,
            GameEvent.DRINK,
            GameEvent.EAT,
            GameEvent.CONTAINER_CLOSE,
            GameEvent.CONTAINER_OPEN,
            GameEvent.BLOCK_DESTROY,
            GameEvent.BLOCK_PLACE
    );

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

    @GameTest(template = TEMPLATE)
    public static void activeEffectCancelsConfiguredMovementGameEvents(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_silenced_events");
        addSenseSensorEffect(player);

        for (var gameEvent : SILENCED_GAME_EVENTS) {
            assertCancellation(helper, gameEvent, player, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void activeEffectKeepsExplicitNonMovementGameEvents(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_audible_events");
        addSenseSensorEffect(player);

        for (var gameEvent : EXPLICITLY_AUDIBLE_GAME_EVENTS) {
            assertCancellation(helper, gameEvent, player, false);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void silencingRequiresActiveEffectAndLivingSource(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_inactive_effect");
        assertCancellation(helper, GameEvent.STEP, player, false);
        assertCancellation(helper, GameEvent.STEP, null, false);

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        addSenseSensorEffect(zombie);
        assertCancellation(helper, GameEvent.STEP, zombie, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = SCULK_STEP_ISOLATED_BATCH, timeoutTicks = 20)
    public static void activeEffectKeepsSculkSensorInactiveForStep(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_sculk_step");
        addSenseSensorEffect(player);
        assertSculkSensorResponse(helper, GameEvent.STEP, GameEvent.Context.of(player), false);
    }

    @GameTest(template = TEMPLATE, batch = SCULK_PROJECTILE_ISOLATED_BATCH, timeoutTicks = 20)
    public static void activeEffectLetsSculkSensorReceiveProjectileShoot(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_sculk_projectile");
        addSenseSensorEffect(player);
        assertSculkSensorResponse(helper, GameEvent.PROJECTILE_SHOOT, GameEvent.Context.of(player), true);
    }

    @GameTest(template = TEMPLATE, batch = SCULK_SNEAKING_ISOLATED_BATCH, timeoutTicks = 20)
    public static void sneakingStillSuppressesAudibleGameEvent(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_sculk_sneaking");
        player.setShiftKeyDown(true);
        assertSculkSensorResponse(helper, GameEvent.PROJECTILE_SHOOT, GameEvent.Context.of(player), false);
    }

    @GameTest(template = TEMPLATE, batch = SCULK_WOOL_ISOLATED_BATCH, timeoutTicks = 20)
    public static void woolStillSuppressesStepWithoutEffect(GameTestHelper helper) {
        var player = createTestPlayer(helper, "deep_sensor_sculk_wool");
        assertSculkSensorResponse(
                helper,
                GameEvent.STEP,
                GameEvent.Context.of(player, Blocks.WHITE_WOOL.defaultBlockState()),
                false
        );
    }

    private static net.neoforged.neoforge.common.util.FakePlayer createTestPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var profile = new GameProfile(
                UUID.nameUUIDFromBytes(profileName.getBytes(StandardCharsets.UTF_8)),
                profileName
        );
        var player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(VIBRATION_POS));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static void addSenseSensorEffect(net.minecraft.world.entity.LivingEntity entity) {
        var senseSensor = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.SENSE_SENSOR.get());
        entity.addEffect(new MobEffectInstance(senseSensor, 40, 0));
    }

    private static void assertCancellation(
            GameTestHelper helper,
            Holder<GameEvent> gameEvent,
            Entity source,
            boolean expectedCanceled
    ) {
        var event = new VanillaGameEvent(
                helper.getLevel(),
                gameEvent,
                helper.absoluteVec(Vec3.atCenterOf(VIBRATION_POS)),
                GameEvent.Context.of(source)
        );
        SenseSensorVibrationEvent.onVanillaGameEvent(event);
        var eventId = gameEvent.unwrapKey()
                .map(net.minecraft.resources.ResourceKey::location)
                .orElseGet(() -> BuiltInRegistries.GAME_EVENT.getKey(gameEvent.value()));
        helper.assertTrue(
                event.isCanceled() == expectedCanceled,
                "Deep Sensor cancellation mismatch for " + eventId + ": expected " + expectedCanceled
        );
    }

    private static void assertSculkSensorResponse(
            GameTestHelper helper,
            Holder<GameEvent> gameEvent,
            GameEvent.Context context,
            boolean expectedActive
    ) {
        helper.setBlock(SENSOR_POS, Blocks.SCULK_SENSOR);
        var eventPosition = helper.absoluteVec(Vec3.atCenterOf(VIBRATION_POS));

        // 動的listenerの登録と振動の伝播を、固定tickで分けて実環境と同じ経路を通す。
        helper.runAfterDelay(2, () -> {
            helper.getLevel().gameEvent(gameEvent, eventPosition, context);
            helper.runAfterDelay(5, () -> {
                var state = helper.getBlockState(SENSOR_POS);
                var isActive = state.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE
                        && state.getValue(SculkSensorBlock.POWER) > 0;
                helper.assertTrue(
                        isActive == expectedActive,
                        "Sculk Sensor response mismatch for " + gameEvent.unwrapKey()
                                .map(net.minecraft.resources.ResourceKey::location)
                                .orElseGet(() -> BuiltInRegistries.GAME_EVENT.getKey(gameEvent.value()))
                                + ": expected active=" + expectedActive
                );
                helper.succeed();
            });
        });
    }
}
