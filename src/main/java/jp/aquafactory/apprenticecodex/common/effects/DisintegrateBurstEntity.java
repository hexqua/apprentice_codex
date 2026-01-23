package jp.aquafactory.apprenticecodex.common.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class DisintegrateBurstEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> DATA_DENSITY =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Y =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(DisintegrateBurstEntity.class, EntityDataSerializers.FLOAT);

    public DisintegrateBurstEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setup(int lifetimeTicks, float startRadius, int density, Vec3 dir) {
        entityData.set(DATA_LIFETIME, lifetimeTicks);
        entityData.set(DATA_RADIUS, startRadius);
        entityData.set(DATA_DENSITY, density);

        var d = (dir == null || dir.lengthSqr() < 1.0e-6) ? new Vec3(0, 0, 1) : dir.normalize();
        entityData.set(DATA_DIR_X, (float) d.x);
        entityData.set(DATA_DIR_Y, (float) d.y);
        entityData.set(DATA_DIR_Z, (float) d.z);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_LIFETIME, 8);
        entityData.define(DATA_RADIUS, 0.35f);
        entityData.define(DATA_DENSITY, 18);

        entityData.define(DATA_DIR_X, 0f);
        entityData.define(DATA_DIR_Y, 0f);
        entityData.define(DATA_DIR_Z, 1f);
    }

    @Override
    public void tick() {
        super.tick();

        int lifetime = entityData.get(DATA_LIFETIME);
        if (tickCount > lifetime) {
            discard();
            return;
        }

        @SuppressWarnings("resource") var level = level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        var startRadius = entityData.get(DATA_RADIUS);
        var t = (float) tickCount / (float) Math.max(1, lifetime);
        var radius = lerp(startRadius, 0.05f, clamp01(t));
        var density = entityData.get(DATA_DENSITY);

        var center = position();
        server.sendParticles(
                ParticleTypes.END_ROD,
                center.x, center.y, center.z,
                density,
                radius, radius, radius,
                0.01
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        // no save.
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        // no save.
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
