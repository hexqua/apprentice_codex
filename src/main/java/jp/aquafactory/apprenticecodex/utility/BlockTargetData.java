package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BlockTargetData implements ICastDataSerializable {
    private boolean hasTarget;
    private BlockPos hitBlockPos;
    private Direction hitFace;
    private double hitX;
    private double hitY;
    private double hitZ;
    private BlockPos placePos;
    private Direction placeFacing;

    public boolean hasTarget() {
        return hasTarget;
    }

    @Nullable
    public BlockPos getHitBlockPos() {
        return hitBlockPos;
    }

    @Nullable
    public Direction getHitFace() {
        return hitFace;
    }

    public Vec3 getHitLocation() {
        return new Vec3(hitX, hitY, hitZ);
    }

    @Nullable
    public BlockPos getPlacePos() {
        return placePos;
    }

    @Nullable
    public Direction getPlaceFacing() {
        return placeFacing;
    }

    public void setTarget(BlockPos hitBlockPos, Direction hitFace, Vec3 hitLocation, BlockPos placePos, Direction placeFacing) {
        this.hasTarget = true;
        this.hitBlockPos = hitBlockPos.immutable();
        this.hitFace = hitFace;
        this.hitX = hitLocation.x;
        this.hitY = hitLocation.y;
        this.hitZ = hitLocation.z;
        this.placePos = placePos.immutable();
        this.placeFacing = placeFacing;
    }

    public void copyFrom(@Nullable BlockTargetData other) {
        if (other == null || !other.hasTarget()) {
            reset();
            return;
        }

        setTarget(
                other.hitBlockPos,
                other.hitFace,
                other.getHitLocation(),
                other.placePos,
                other.placeFacing
        );
    }

    public BlockTargetData copy() {
        var copy = new BlockTargetData();
        copy.copyFrom(this);
        return copy;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBoolean(hasTarget);
        if (!hasTarget) {
            return;
        }

        friendlyByteBuf.writeBlockPos(hitBlockPos);
        friendlyByteBuf.writeEnum(hitFace);
        friendlyByteBuf.writeDouble(hitX);
        friendlyByteBuf.writeDouble(hitY);
        friendlyByteBuf.writeDouble(hitZ);
        friendlyByteBuf.writeBlockPos(placePos);
        friendlyByteBuf.writeEnum(placeFacing);
    }

    @Override
    public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
        hasTarget = friendlyByteBuf.readBoolean();
        if (!hasTarget) {
            reset();
            return;
        }

        hitBlockPos = friendlyByteBuf.readBlockPos();
        hitFace = friendlyByteBuf.readEnum(Direction.class);
        hitX = friendlyByteBuf.readDouble();
        hitY = friendlyByteBuf.readDouble();
        hitZ = friendlyByteBuf.readDouble();
        placePos = friendlyByteBuf.readBlockPos();
        placeFacing = friendlyByteBuf.readEnum(Direction.class);
    }

    @Override
    public void reset() {
        hasTarget = false;
        hitBlockPos = null;
        hitFace = null;
        hitX = 0;
        hitY = 0;
        hitZ = 0;
        placePos = null;
        placeFacing = null;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putBoolean("HasTarget", hasTarget);
        if (!hasTarget) {
            return tag;
        }

        tag.putInt("HitBlockX", hitBlockPos.getX());
        tag.putInt("HitBlockY", hitBlockPos.getY());
        tag.putInt("HitBlockZ", hitBlockPos.getZ());
        tag.putInt("HitFace", hitFace.get3DDataValue());
        tag.putDouble("HitX", hitX);
        tag.putDouble("HitY", hitY);
        tag.putDouble("HitZ", hitZ);
        tag.putInt("PlaceX", placePos.getX());
        tag.putInt("PlaceY", placePos.getY());
        tag.putInt("PlaceZ", placePos.getZ());
        tag.putInt("PlaceFacing", placeFacing.get3DDataValue());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        hasTarget = nbt.getBoolean("HasTarget");
        if (!hasTarget) {
            reset();
            return;
        }

        hitBlockPos = new BlockPos(nbt.getInt("HitBlockX"), nbt.getInt("HitBlockY"), nbt.getInt("HitBlockZ"));
        hitFace = Direction.from3DDataValue(nbt.getInt("HitFace"));
        hitX = nbt.getDouble("HitX");
        hitY = nbt.getDouble("HitY");
        hitZ = nbt.getDouble("HitZ");
        placePos = new BlockPos(nbt.getInt("PlaceX"), nbt.getInt("PlaceY"), nbt.getInt("PlaceZ"));
        placeFacing = Direction.from3DDataValue(nbt.getInt("PlaceFacing"));
    }
}
