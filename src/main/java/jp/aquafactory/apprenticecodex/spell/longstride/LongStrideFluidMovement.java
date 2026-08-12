package jp.aquafactory.apprenticecodex.spell.longstride;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class LongStrideFluidMovement {
    private static final double FLUID_SURFACE_EPSILON = 0.02D;
    private static final double FLUID_SCAN_BELOW = 0.18D;
    private static final double FLUID_SCAN_ABOVE = 0.08D;
    private static final double FLUID_SNAP_UP_LIMIT = 0.35D;
    private static final double FLUID_SNAP_DOWN_LIMIT = 0.65D;
    private static final double FLUID_RISE_ACCELERATION = 0.03D;
    private static final double MAX_FLUID_RISE_SPEED = 0.1D;

    private LongStrideFluidMovement() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        apply(event.player);
    }

    public static boolean canStandOnFluid(Player player, FluidState fluidState) {
        return !fluidState.isEmpty()
                && canControlFluidMovement(player)
                && !isTouchingWalkableFluid(player);
    }

    public static void apply(Player player) {
        if (!canControlFluidMovement(player)) {
            return;
        }

        if (isTouchingWalkableFluid(player)) {
            applyBuoyancy(player);
            return;
        }

        applyFluidStanding(player);
    }

    private static boolean canControlFluidMovement(Player player) {
        return player.hasEffect(EffectRegistry.LONG_STRIDE_MOBILITY.get())
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && !player.isFallFlying();
    }

    private static void applyBuoyancy(Player player) {
        var movement = player.getDeltaMovement();
        var vertical = Math.min(MAX_FLUID_RISE_SPEED, movement.y + FLUID_RISE_ACCELERATION);
        player.setDeltaMovement(movement.x, vertical, movement.z);
        player.setOnGround(false);
        player.fallDistance = 0.0F;
    }

    private static void applyFluidStanding(Player player) {
        var support = findFluidSupport(player.level(), player);
        if (support == null) {
            return;
        }

        var targetY = support.surfaceY() + FLUID_SURFACE_EPSILON;
        var currentY = player.getY();
        if (currentY < targetY - FLUID_SNAP_UP_LIMIT || currentY > targetY + FLUID_SNAP_DOWN_LIMIT) {
            return;
        }

        var movement = player.getDeltaMovement();
        if (currentY > targetY && movement.y > MAX_FLUID_RISE_SPEED) {
            return;
        }

        player.setPos(player.getX(), targetY, player.getZ());
        player.setDeltaMovement(movement.x, 0.0D, movement.z);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }

    private static boolean isTouchingWalkableFluid(Player player) {
        var level = player.level();
        var box = player.getBoundingBox().deflate(1.0E-4D);
        var minX = net.minecraft.util.Mth.floor(box.minX);
        var maxX = net.minecraft.util.Mth.floor(box.maxX);
        var minY = net.minecraft.util.Mth.floor(box.minY);
        var maxY = net.minecraft.util.Mth.floor(box.maxY);
        var minZ = net.minecraft.util.Mth.floor(box.minZ);
        var maxZ = net.minecraft.util.Mth.floor(box.maxZ);

        var mutablePos = new BlockPos.MutableBlockPos();
        for (var y = minY; y <= maxY; ++y) {
            for (var x = minX; x <= maxX; ++x) {
                for (var z = minZ; z <= maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    var fluidState = level.getFluidState(mutablePos);
                    if (fluidState.isEmpty() || hasSolidCollision(level, mutablePos)) {
                        continue;
                    }

                    var fluidTop = y + fluidState.getHeight(level, mutablePos);
                    if (box.maxY > y && box.minY < fluidTop) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    private static FluidSupport findFluidSupport(Level level, Player player) {
        var box = player.getBoundingBox();
        var minX = net.minecraft.util.Mth.floor(box.minX + 0.05D);
        var maxX = net.minecraft.util.Mth.floor(box.maxX - 0.05D);
        var minZ = net.minecraft.util.Mth.floor(box.minZ + 0.05D);
        var maxZ = net.minecraft.util.Mth.floor(box.maxZ - 0.05D);
        var minY = net.minecraft.util.Mth.floor(box.minY - FLUID_SCAN_BELOW);
        var maxY = net.minecraft.util.Mth.floor(box.minY + FLUID_SCAN_ABOVE);

        FluidSupport best = null;
        var mutablePos = new BlockPos.MutableBlockPos();
        for (var y = minY; y <= maxY; ++y) {
            for (var x = minX; x <= maxX; ++x) {
                for (var z = minZ; z <= maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    var fluidState = level.getFluidState(mutablePos);
                    if (fluidState.isEmpty() || hasSolidCollision(level, mutablePos)) {
                        continue;
                    }

                    var surfaceY = y + 1.0D;
                    if (best == null || surfaceY > best.surfaceY()) {
                        best = new FluidSupport(surfaceY);
                    }
                }
            }
        }

        return best;
    }

    private static boolean hasSolidCollision(CollisionGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return true;
        }
        return !state.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty();
    }

    private record FluidSupport(double surfaceY) {
    }
}
