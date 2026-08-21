package jp.aquafactory.apprenticecodex.spell.deepsensor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.DeepSensorObservationsPacket;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SenseSensorVibrationEvent {
    public static final int LISTENER_RADIUS = 24;
    private static final int HEARTBEAT_INTERVAL_TICKS = 100;
    private static final Map<UUID, SensorState> SENSOR_STATES = new HashMap<>();

    private SenseSensorVibrationEvent() {
    }

    public static int calculateTravelTimeInTicks(float distance) {
        return Mth.clamp(Mth.floor(distance), 1, 3);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive()) {
            deactivate(player, true);
            return;
        }

        var effectActive = player.hasEffect(EffectRegistry.SENSE_SENSOR.get());
        var state = SENSOR_STATES.get(player.getUUID());
        if (state != null && state.level != player.serverLevel()) {
            state.clear(player, true);
            SENSOR_STATES.remove(player.getUUID());
            state = null;
        }

        if (effectActive && state == null) {
            state = new SensorState(player);
            SENSOR_STATES.put(player.getUUID(), state);
        }
        if (state == null) {
            return;
        }

        state.tick(player, effectActive);
        if (!state.listenerActive() && state.observations.isEmpty()) {
            SENSOR_STATES.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivate(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivate(player, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivate(player, true);
        }
    }

    private static void deactivate(ServerPlayer player, boolean notifyPlayer) {
        var state = SENSOR_STATES.remove(player.getUUID());
        if (state != null) {
            state.clear(player, notifyPlayer);
        }
    }

    private static boolean isOwnVibration(@Nullable Entity sourceEntity, ServerPlayer owner) {
        if (sourceEntity == null) {
            return false;
        }
        if (sourceEntity == owner || sourceEntity.getControllingPassenger() == owner) {
            return true;
        }
        if (sourceEntity.getRootVehicle() == owner) {
            return true;
        }
        if (sourceEntity instanceof Projectile projectile && projectile.getOwner() == owner) {
            return true;
        }
        return sourceEntity instanceof ItemEntity itemEntity && itemEntity.getOwner() == owner;
    }

    private static final class SensorState {
        private final ServerLevel level;
        private final DeepSensorObservationBuffer observations = new DeepSensorObservationBuffer();
        private ActiveListener listener;
        private List<DeepSensorObservationBuffer.DisplayObservation> displayed = List.of();
        private long nextHeartbeatGameTime;

        private SensorState(ServerPlayer owner) {
            level = owner.serverLevel();
            listener = new ActiveListener(owner, this);
        }

        private void tick(ServerPlayer owner, boolean effectActive) {
            if (effectActive) {
                if (listener == null) {
                    listener = new ActiveListener(owner, this);
                }
                listener.tick();
            } else if (listener != null) {
                listener.remove();
                listener = null;
            }

            var gameTime = level.getGameTime();
            var nextDisplayed = observations.selectForDisplay(
                    owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D),
                    gameTime
            );
            var wasVisible = !displayed.isEmpty();
            var isVisible = !nextDisplayed.isEmpty();

            // 同tickで周期音と消失が重なる場合は、周期音を省略せず停止音を続けて鳴らす。
            if (wasVisible && gameTime >= nextHeartbeatGameTime) {
                playClickSound(owner);
                nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
            }
            if (!wasVisible && isVisible) {
                playClickSound(owner);
                nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
            }
            if (wasVisible && !isVisible) {
                playStopSound(owner);
            }

            if (!displayed.equals(nextDisplayed)) {
                displayed = nextDisplayed;
                sync(owner);
            }
        }

        private boolean listenerActive() {
            return listener != null;
        }

        private void record(BlockPos position, float distance, GameEvent gameEvent,
                            @Nullable Entity sourceEntity, @Nullable Entity projectileOwner) {
            var eventId = BuiltInRegistries.GAME_EVENT.getKey(gameEvent);
            observations.record(
                    position,
                    distance,
                    level.getGameTime(),
                    sourceEntity == null ? null : sourceEntity.getUUID(),
                    projectileOwner == null ? null : projectileOwner.getUUID(),
                    eventId
            );
        }

        private void clear(ServerPlayer owner, boolean notifyPlayer) {
            if (listener != null) {
                listener.remove();
                listener = null;
            }
            observations.clear();
            if (notifyPlayer && !displayed.isEmpty()) {
                playStopSound(owner);
                displayed = List.of();
                sync(owner);
            }
        }

        private void sync(ServerPlayer owner) {
            Networks.sendToPlayer(owner, new DeepSensorObservationsPacket(level.dimension(), displayed));
        }

        private static void playClickSound(ServerPlayer owner) {
            owner.playNotifySound(SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F,
                    owner.getRandom().nextFloat() * 0.2F + 0.8F);
        }

        private static void playStopSound(ServerPlayer owner) {
            owner.playNotifySound(SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F,
                    owner.getRandom().nextFloat() * 0.2F + 0.8F);
        }
    }

    private static final class ActiveListener implements VibrationSystem {
        private final ServerPlayer owner;
        private final SensorState state;
        private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
        private final VibrationUser vibrationUser;
        private final DynamicGameEventListener<VibrationSystem.Listener> dynamicListener;

        private ActiveListener(ServerPlayer owner, SensorState state) {
            this.owner = owner;
            this.state = state;
            this.vibrationUser = new VibrationUser();
            this.dynamicListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
            dynamicListener.add(state.level);
        }

        private void tick() {
            dynamicListener.move(state.level);
            VibrationSystem.Ticker.tick(state.level, vibrationData, vibrationUser);
        }

        private void remove() {
            dynamicListener.remove(state.level);
        }

        @Override
        public VibrationSystem.Data getVibrationData() {
            return vibrationData;
        }

        @Override
        public VibrationSystem.User getVibrationUser() {
            return vibrationUser;
        }

        private final class VibrationUser implements VibrationSystem.User {
            private final PositionSource positionSource = new EntityPositionSource(owner, owner.getBbHeight() * 0.5F);

            @Override
            public int getListenerRadius() {
                return LISTENER_RADIUS;
            }

            @Override
            public PositionSource getPositionSource() {
                return positionSource;
            }

            @Override
            public boolean canReceiveVibration(ServerLevel level, BlockPos pos, GameEvent gameEvent, GameEvent.Context context) {
                return !isOwnVibration(context.sourceEntity(), owner);
            }

            @Override
            public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent gameEvent, @Nullable Entity entity,
                                           @Nullable Entity projectileOwner, float distance) {
                state.record(pos, distance, gameEvent, entity, projectileOwner);
            }

            @Override
            public int calculateTravelTimeInTicks(float distance) {
                return SenseSensorVibrationEvent.calculateTravelTimeInTicks(distance);
            }

            @Override
            public boolean canTriggerAvoidVibration() {
                return true;
            }

            @Override
            public boolean requiresAdjacentChunksToBeTicking() {
                return true;
            }
        }
    }
}
