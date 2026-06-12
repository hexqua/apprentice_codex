package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MirageAvoidanceEvents {
    public static final int EFFECT_DURATION_TICKS = 25;
    public static final int INVULNERABLE_TICKS = 15;
    public static final int FREEZE_TICKS = 2;
    public static final int SLIDE_TICKS = 18;
    public static final int VULNERABLE_RECOVERY_START_TICK = 20;
    public static final double RUN_SPEED_PER_TICK = 0.42D;
    private static final double SLOW_FALL_SPEED = -0.08D;
    private static final double INPUT_EPSILON_SQ = 1.0E-6D;
    private static final DustParticleOptions MIRAGE_DENSE_SMOKE =
            new DustParticleOptions(new Vector3f(0.58F, 0.22F, 0.95F), 0.85F);
    private static final DustParticleOptions MIRAGE_FAINT_SMOKE =
            new DustParticleOptions(new Vector3f(0.74F, 0.46F, 1.0F), 0.65F);

    private MirageAvoidanceEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START && event.phase != TickEvent.Phase.END) {
            return;
        }

        var player = event.player;
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var level = player.level();
        var state = spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE);
        if (!isActive(level, state)) {
            tickFallDamageSuppression(spellData, player, state);
            return;
        }

        if (!player.isAlive() || player.isPassenger()) {
            deactivate(spellData, player, state, true);
            return;
        }

        var elapsedTicks = getElapsedTicks(level, state);
        if (event.phase == TickEvent.Phase.START) {
            applyMovement(player, state, elapsedTicks);
        } else {
            stabilizePostPhysicsMovement(player, elapsedTicks);
        }
        player.fallDistance = 0.0F;

        if (event.phase == TickEvent.Phase.START && !level.isClientSide) {
            spawnTrailParticles(player, elapsedTicks);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE);
        if (isInvulnerable(player.level(), state) || shouldSuppressFallDamage(player, state, event.getSource())) {
            event.setCanceled(true);
            player.fallDistance = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isInputLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static boolean isActive(Level level, MirageAvoidanceState state) {
        return state.activeUntilGameTime > level.getGameTime();
    }

    public static boolean isInputLocked(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        return spellData != null && isActive(player.level(), spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
    }

    private static boolean isInvulnerable(Level level, MirageAvoidanceState state) {
        return state.invulnerableUntilGameTime > level.getGameTime();
    }

    private static boolean shouldSuppressFallDamage(Player player, MirageAvoidanceState state, net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypes.FALL)
                && (isActive(player.level(), state) || state.suppressFallDamageUntilGround);
    }

    private static int getElapsedTicks(Level level, MirageAvoidanceState state) {
        return Math.max(0, (int) (level.getGameTime() - state.startGameTime));
    }

    private static void applyMovement(Player player, MirageAvoidanceState state, int elapsedTicks) {
        if (elapsedTicks < FREEZE_TICKS || elapsedTicks >= VULNERABLE_RECOVERY_START_TICK) {
            player.setDeltaMovement(Vec3.ZERO);
            markMovementChanged(player);
            return;
        }

        var slideTick = Mth.clamp(elapsedTicks - FREEZE_TICKS, 0, SLIDE_TICKS);
        var speedScale = 1.0D - slideTick / (double) SLIDE_TICKS;
        var horizontal = resolveCurrentDirection(player, state).scale(RUN_SPEED_PER_TICK * speedScale);
        var current = player.getDeltaMovement();
        var y = current.y;
        if (y < SLOW_FALL_SPEED) {
            y = SLOW_FALL_SPEED;
        }

        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        markMovementChanged(player);
    }

    private static void stabilizePostPhysicsMovement(Player player, int elapsedTicks) {
        var movement = player.getDeltaMovement();
        if (elapsedTicks < FREEZE_TICKS || elapsedTicks >= VULNERABLE_RECOVERY_START_TICK) {
            if (movement.lengthSqr() > 1.0E-6D) {
                player.setDeltaMovement(Vec3.ZERO);
                markMovementChanged(player);
            }
            return;
        }

        if (movement.y < SLOW_FALL_SPEED) {
            player.setDeltaMovement(movement.x, SLOW_FALL_SPEED, movement.z);
            markMovementChanged(player);
        }
    }

    private static Vec3 resolveCurrentDirection(Player player, MirageAvoidanceState state) {
        var forward = getFlatForward(player.getYRot());
        var right = new Vec3(-forward.z, 0.0D, forward.x);
        var direction = forward.scale(state.movementForward).add(right.scale(state.movementStrafe));
        if (direction.lengthSqr() <= INPUT_EPSILON_SQ) {
            return forward;
        }

        return direction.normalize();
    }

    private static Vec3 getFlatForward(float yaw) {
        var yawRad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
    }

    private static void markMovementChanged(Player player) {
        player.hasImpulse = true;
        if (!player.level().isClientSide) {
            player.hurtMarked = true;
        }
    }

    private static void tickFallDamageSuppression(CodexSpellData spellData, Player player, MirageAvoidanceState state) {
        if (state.activeUntilGameTime == 0L && !state.suppressFallDamageUntilGround) {
            return;
        }

        if (state.suppressFallDamageUntilGround) {
            player.fallDistance = 0.0F;
        }
        if (state.suppressFallDamageUntilGround && !player.onGround()) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, MirageAvoidanceState::reset);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            MirageAvoidanceSync.syncToClient(serverPlayer, spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
        }
    }

    private static void deactivate(CodexSpellData spellData, Player player, MirageAvoidanceState state, boolean stopMovement) {
        if (state.activeUntilGameTime == 0L && !state.suppressFallDamageUntilGround) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.reset();
            s.suppressFallDamageUntilGround = true;
        });
        player.fallDistance = 0.0F;
        if (stopMovement) {
            player.setDeltaMovement(Vec3.ZERO);
            markMovementChanged(player);
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            MirageAvoidanceSync.syncToClient(serverPlayer, spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
        }
    }

    private static void spawnTrailParticles(Player player, int elapsedTicks) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var invulnerable = elapsedTicks < INVULNERABLE_TICKS;
        var recovery = elapsedTicks >= VULNERABLE_RECOVERY_START_TICK;
        if (recovery && player.tickCount % 2 != 0) {
            return;
        }

        var random = player.getRandom();
        var box = player.getBoundingBox();
        var particleCount = invulnerable ? 4 : recovery ? 1 : 2;
        var smoke = invulnerable ? MIRAGE_DENSE_SMOKE : MIRAGE_FAINT_SMOKE;
        for (var i = 0; i < particleCount; ++i) {
            var x = Mth.lerp(random.nextDouble(), box.minX, box.maxX);
            var y = player.getY() + 0.1D + random.nextDouble() * Math.min(1.1D, box.getYsize() * 0.7D);
            var z = Mth.lerp(random.nextDouble(), box.minZ, box.maxZ);
            serverLevel.sendParticles(smoke, x, y, z, 1, 0.10D, 0.04D, 0.10D, 0.004D);
            if (invulnerable || random.nextBoolean()) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.06D, 0.03D, 0.06D, 0.01D);
            }
            if (!recovery && random.nextFloat() < 0.65F) {
                serverLevel.sendParticles(createMirageSpark(invulnerable), x, y, z, 1,
                        0.07D, 0.04D, 0.07D, 0.018D);
            }
        }
    }

    private static AdditiveGlowParticleOptions createMirageSpark(boolean invulnerable) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                invulnerable ? 0.13F : 0.09F,
                0.82F,
                0.40F,
                1.0F,
                invulnerable ? 2 : 1,
                invulnerable ? 12 : 8,
                4,
                0.65F,
                1.25F,
                invulnerable ? 0.64F : 0.38F,
                invulnerable ? 0.96F : 0.70F,
                0.08F,
                0.48F,
                0.45F,
                true
        );
    }
}
