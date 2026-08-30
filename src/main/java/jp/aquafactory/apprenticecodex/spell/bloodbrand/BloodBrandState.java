package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public record BloodBrandState(UUID casterUuid, float burstDamage, double range) {
    private static final String PERSISTENT_TAG = "apprenticecodex:blood_brand_state";
    private static final String TAG_CASTER = "Caster";
    private static final String TAG_BURST_DAMAGE = "BurstDamage";
    private static final String TAG_RANGE = "Range";
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public BloodBrandState {
        Objects.requireNonNull(casterUuid);
        burstDamage = Float.isFinite(burstDamage) ? Math.max(0.0F, burstDamage) : 0.0F;
        range = Double.isFinite(range) ? Math.max(0.0D, range) : 0.0D;
    }

    public static BloodBrandState empty() {
        return new BloodBrandState(EMPTY_UUID, 0.0F, 0.0D);
    }

    public boolean isEmpty() {
        return casterUuid.equals(EMPTY_UUID) || range <= 0.0D;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID(TAG_CASTER, casterUuid);
        tag.putFloat(TAG_BURST_DAMAGE, burstDamage);
        tag.putDouble(TAG_RANGE, range);
        return tag;
    }

    public static BloodBrandState load(CompoundTag tag) {
        if (!tag.hasUUID(TAG_CASTER)) {
            throw new IllegalArgumentException("Blood brand state is missing its caster UUID");
        }
        return new BloodBrandState(
                tag.getUUID(TAG_CASTER),
                tag.getFloat(TAG_BURST_DAMAGE),
                tag.getDouble(TAG_RANGE)
        );
    }

    public static @Nullable BloodBrandState get(LivingEntity entity) {
        var persistentData = entity.getPersistentData();
        return persistentData.contains(PERSISTENT_TAG, Tag.TAG_COMPOUND)
                ? load(persistentData.getCompound(PERSISTENT_TAG))
                : null;
    }

    public static void set(LivingEntity entity, BloodBrandState state) {
        entity.getPersistentData().put(PERSISTENT_TAG, state.save());
    }

    public static void remove(LivingEntity entity) {
        entity.getPersistentData().remove(PERSISTENT_TAG);
    }
}
