package jp.aquafactory.apprenticecodex.spell.deepsensor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SenseSensorVibrationEvent {
    private static final int BASE_RANGE = 8;
    private static final int MAX_RANGE = 64;
    private static final int ACTIVE_TICKS = 30;
    private static final int COOLDOWN_TICKS = 10;
    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final Map<UUID, ActiveSenseSensor> ACTIVE_SENSORS = new HashMap<>();

    private SenseSensorVibrationEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var senseSensor = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.SENSE_SENSOR.get());
        var effect = player.getEffect(senseSensor);
        if (effect == null || !player.isAlive()) {
            deactivate(player);
            return;
        }

        ACTIVE_SENSORS.compute(player.getUUID(), (uuid, activeSensor) -> {
            if (activeSensor == null) {
                activeSensor = new ActiveSenseSensor(player);
            } else if (activeSensor.level != player.serverLevel()) {
                activeSensor.remove();
                activeSensor = new ActiveSenseSensor(player);
            }

            activeSensor.tick(effect);
            return activeSensor;
        });
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            deactivate(player);
        }
    }

    private static void deactivate(ServerPlayer player) {
        var activeSensor = ACTIVE_SENSORS.remove(player.getUUID());
        if (activeSensor != null) {
            activeSensor.remove();
        }
    }

    private static int getListenerRadius(int amplifier) {
        // Amp=0 をスカルクセンサー基準にしつつ、増幅段階ごとに線形に倍率を上げる。
        return Math.min(BASE_RANGE * (amplifier + 1), MAX_RANGE);
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

    private static void spawnActiveParticle(ServerLevel level, Player player) {
        var center = player.position();
        var y = center.y + player.getBbHeight() * 0.35d;
        var offset = 0.45d;
        Direction direction = switch (level.random.nextInt(4)) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        };
        var x = center.x + (direction.getStepX() == 0 ? 0.5d - level.random.nextDouble() : direction.getStepX() * offset);
        var z = center.z + (direction.getStepZ() == 0 ? 0.5d - level.random.nextDouble() : direction.getStepZ() * offset);
        var velocityY = level.random.nextFloat() * 0.04d;
        level.sendParticles(DustColorTransitionOptions.SCULK_TO_REDSTONE, x, y, z, 1, 0.0d, velocityY, 0.0d, 0.0d);
    }

    private static final class ActiveSenseSensor implements VibrationSystem {
        private final ServerPlayer owner;
        private final ServerLevel level;
        private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
        private final VibrationUser vibrationUser;
        private final DynamicGameEventListener<VibrationSystem.Listener> dynamicListener;
        private int currentAmplifier;
        private int activeTicks;
        private int cooldownTicks;

        private ActiveSenseSensor(ServerPlayer owner) {
            this.owner = owner;
            this.level = owner.serverLevel();
            this.vibrationUser = new VibrationUser();
            this.dynamicListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
            this.dynamicListener.add(this.level);
        }

        private void tick(MobEffectInstance effect) {
            currentAmplifier = effect.getAmplifier();
            dynamicListener.move(level);
            VibrationSystem.Ticker.tick(level, vibrationData, vibrationUser);

            if (activeTicks > 0) {
                if ((activeTicks % PARTICLE_INTERVAL_TICKS) == 0) {
                    spawnActiveParticle(level, owner);
                }

                activeTicks--;
                if (activeTicks == 0) {
                    cooldownTicks = COOLDOWN_TICKS;
                    playStopSound();
                }
            } else if (cooldownTicks > 0) {
                cooldownTicks--;
            }
        }

        private void remove() {
            dynamicListener.remove(level);
        }

        private void onReceiveVibration() {
            activeTicks = ACTIVE_TICKS;
            cooldownTicks = 0;
            playClickSound();
            spawnActiveParticle(level, owner);
        }

        private void playClickSound() {
            var position = owner.position();
            level.playSound(null, position.x, position.y + owner.getBbHeight() * 0.5d, position.z,
                    SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.2f + 0.8f);
        }

        private void playStopSound() {
            var position = owner.position();
            level.playSound(null, position.x, position.y + owner.getBbHeight() * 0.5d, position.z,
                    SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.2f + 0.8f);
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
            private final PositionSource positionSource = new EntityPositionSource(owner, owner.getBbHeight() * 0.5f);

            @Override
            public int getListenerRadius() {
                return SenseSensorVibrationEvent.getListenerRadius(currentAmplifier);
            }

            @Override
            public PositionSource getPositionSource() {
                return positionSource;
            }

            @Override
            public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, GameEvent.Context context) {
                if (activeTicks > 0 || cooldownTicks > 0) {
                    return false;
                }

                return !isOwnVibration(context.sourceEntity(), owner);
            }

            @Override
            public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, @Nullable Entity entity,
                                           @Nullable Entity projectileOwner, float distance) {
                ActiveSenseSensor.this.onReceiveVibration();
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
