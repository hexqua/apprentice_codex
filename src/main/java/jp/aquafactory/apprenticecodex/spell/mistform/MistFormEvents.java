package jp.aquafactory.apprenticecodex.spell.mistform;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MistFormEvents {
    private static final double MAX_FALL_SPEED = -0.08D;
    private static final double FLUID_SURFACE_EPSILON = 0.02D;
    private static final int FLUID_STANDING_REENABLE_DELAY_TICKS = 20;
    private static final double FLUID_SCAN_BELOW = 0.18D;
    private static final double FLUID_SCAN_ABOVE = 0.08D;
    private static final double FLUID_SNAP_UP_LIMIT = 0.35D;
    private static final double FLUID_SNAP_DOWN_LIMIT = 0.65D;
    private static final DustParticleOptions MIST_PARTICLE =
            new DustParticleOptions(new Vector3f(0.88F, 0.98F, 1.0F), 0.85F);
    private static final Map<Player, Integer> FLUID_STANDING_DISABLED_UNTIL_TICK =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MistFormEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        if (!player.hasEffect(EffectRegistry.MIST_FORM)) {
            FLUID_STANDING_DISABLED_UNTIL_TICK.remove(player);
            return;
        }

        applySlowFalling(player);
        applyFluidStanding(player);
        spawnMistParticles(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)) {
            return;
        }
        if (attacker == event.getEntity() || !attacker.hasEffect(EffectRegistry.MIST_FORM)) {
            return;
        }

        attacker.removeEffect(EffectRegistry.MIST_FORM);
    }

    public static boolean canStandOnFluid(Player player) {
        if (player.isShiftKeyDown() || player.isSpectator() || player.isPassenger()
                || !player.hasEffect(EffectRegistry.MIST_FORM)) {
            return false;
        }

        if (isTouchingFluid(player)) {
            disableFluidStanding(player);
            return false;
        }

        return !isFluidStandingDisabled(player);
    }

    private static void applySlowFalling(Player player) {
        if (player.onGround() || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()) {
            return;
        }

        var movement = player.getDeltaMovement();
        if (movement.y < MAX_FALL_SPEED) {
            player.setDeltaMovement(movement.x, MAX_FALL_SPEED, movement.z);
        }
        player.fallDistance = 0.0F;
    }

    private static void applyFluidStanding(Player player) {
        if (!canStandOnFluid(player)) {
            return;
        }

        var support = findFluidSupport(player.level(), player);
        if (support == null) {
            return;
        }

        var targetY = support.surfaceY() + FLUID_SURFACE_EPSILON;
        var currentY = player.getY();
        if (currentY < targetY - FLUID_SNAP_UP_LIMIT || currentY > targetY + FLUID_SNAP_DOWN_LIMIT) {
            return;
        }

        if (currentY < targetY - 1.0E-5D) {
            player.setPos(player.getX(), targetY, player.getZ());
        }

        var movement = player.getDeltaMovement();
        if (movement.y < 0.0D) {
            player.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
        player.setOnGround(true);
        player.fallDistance = 0.0F;
    }

    private static void disableFluidStanding(Player player) {
        FLUID_STANDING_DISABLED_UNTIL_TICK.put(player, player.tickCount + FLUID_STANDING_REENABLE_DELAY_TICKS);
    }

    private static boolean isFluidStandingDisabled(Player player) {
        var disabledUntilTick = FLUID_STANDING_DISABLED_UNTIL_TICK.get(player);
        if (disabledUntilTick == null) {
            return false;
        }

        if (player.tickCount <= disabledUntilTick) {
            return true;
        }

        FLUID_STANDING_DISABLED_UNTIL_TICK.remove(player);
        return false;
    }

    private static boolean isTouchingFluid(Player player) {
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
                    if (fluidState.isEmpty()) {
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
                    var state = level.getBlockState(mutablePos);
                    var fluidState = state.getFluidState();
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
        return !level.getBlockState(pos)
                .getCollisionShape(level, pos, CollisionContext.empty())
                .isEmpty();
    }

    private static void spawnMistParticles(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel) || player.tickCount % 2 != 0) {
            return;
        }

        var random = player.getRandom();
        var box = player.getBoundingBox();
        for (var i = 0; i < 2; ++i) {
            var x = lerp(random.nextDouble(), box.minX, box.maxX);
            var y = player.getY() + 0.1D + random.nextDouble() * Math.min(1.1D, box.getYsize() * 0.65D);
            var z = lerp(random.nextDouble(), box.minZ, box.maxZ);
            serverLevel.sendParticles(MIST_PARTICLE, x, y, z, 1, 0.08D, 0.03D, 0.08D, 0.002D);
        }
    }

    private static double lerp(double amount, double from, double to) {
        return from + (to - from) * amount;
    }

    private record FluidSupport(double surfaceY) {
    }
}
