package jp.aquafactory.apprenticecodex.spell.deepsensor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeepSensorObservationBuffer {
    public static final int OBSERVATION_LIFETIME_TICKS = 40;
    public static final int MAX_RETAINED_OBSERVATIONS = 64;
    public static final int MAX_DISPLAYED_OBSERVATIONS = 8;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;

    private final Map<ObservationKey, Observation> observations = new HashMap<>();

    public void record(BlockPos position, float distance, long receivedGameTime,
                       @Nullable UUID sourceUuid, @Nullable UUID projectileOwnerUuid,
                       ResourceLocation gameEventId) {
        var snapshotPosition = position.immutable();
        var key = sourceUuid == null
                ? ObservationKey.forPosition(snapshotPosition)
                : ObservationKey.forEntity(sourceUuid);
        observations.put(key, new Observation(
                snapshotPosition,
                distance,
                receivedGameTime,
                receivedGameTime + OBSERVATION_LIFETIME_TICKS,
                sourceUuid,
                projectileOwnerUuid,
                gameEventId
        ));
        trimToCapacity();
    }

    public void prune(long gameTime) {
        observations.values().removeIf(observation -> observation.expiresAtGameTime() <= gameTime);
    }

    public List<DisplayObservation> selectForDisplay(Vec3 observerCenter, long gameTime) {
        prune(gameTime);

        // Entity由来を含め、同じ座標を指す最新情報だけを表示候補にして重なりを抑える。
        Map<BlockPos, Observation> newestByPosition = new LinkedHashMap<>();
        for (var observation : observations.values()) {
            newestByPosition.merge(observation.position(), observation,
                    (left, right) -> left.receivedGameTime() >= right.receivedGameTime() ? left : right);
        }

        var candidates = new ArrayList<Observation>();
        for (var observation : newestByPosition.values()) {
            if (observation.position().getCenter().distanceToSqr(observerCenter) > MIN_DIRECTION_LENGTH_SQR) {
                candidates.add(observation);
            }
        }
        candidates.sort(Comparator
                .comparingDouble(Observation::distance)
                .thenComparing(Comparator.comparingLong(Observation::receivedGameTime).reversed()));

        return candidates.stream()
                .limit(MAX_DISPLAYED_OBSERVATIONS)
                .map(observation -> new DisplayObservation(
                        observation.position(),
                        observation.distance(),
                        observation.expiresAtGameTime()
                ))
                .toList();
    }

    public boolean isEmpty() {
        return observations.isEmpty();
    }

    public int size() {
        return observations.size();
    }

    public void clear() {
        observations.clear();
    }

    private void trimToCapacity() {
        while (observations.size() > MAX_RETAINED_OBSERVATIONS) {
            var oldest = observations.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().receivedGameTime()))
                    .orElseThrow();
            observations.remove(oldest.getKey());
        }
    }

    public record DisplayObservation(BlockPos position, float distance, long expiresAtGameTime) {
    }

    private record Observation(BlockPos position, float distance, long receivedGameTime, long expiresAtGameTime,
                               @Nullable UUID sourceUuid, @Nullable UUID projectileOwnerUuid,
                               ResourceLocation gameEventId) {
    }

    private record ObservationKey(@Nullable UUID sourceUuid, @Nullable BlockPos position) {
        private static ObservationKey forEntity(UUID sourceUuid) {
            return new ObservationKey(sourceUuid, null);
        }

        private static ObservationKey forPosition(BlockPos position) {
            return new ObservationKey(null, position);
        }
    }
}
