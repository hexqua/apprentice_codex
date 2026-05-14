package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.particle.SmashcastTremorBlockParticleOptions;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SmashcastScepterAttackEvent {
    private static final Map<ServerLevel, Map<UUID, PendingSmash>> PENDING_SMASHES = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<UUID, Long>> EPIC_FIGHT_SMASHCAST_IMPACT_TICKS = new WeakHashMap<>();
    private static final int MACE_SMASH_LEVEL_EVENT = 2013;
    private static final int MACE_SMASH_LEVEL_EVENT_DATA = 750;
    private static final int TREMOR_BLOCK_LIMIT = 16;
    private static final double SMASH_TREMOR_RADIUS = 3.0D;
    private static final float TREMOR_IMPULSE_MULTIPLIER = 1.75F;

    private SmashcastScepterAttackEvent() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SmashcastScepter) || !SmashcastScepter.isSmashAttack(player)) {
            return;
        }

        if (!canRegisterSmashcast(player, stack)) {
            return;
        }

        PENDING_SMASHES.computeIfAbsent(player.serverLevel(), ignored -> new HashMap<>())
                .put(player.getUUID(), new PendingSmash(
                        target.getUUID(),
                        player.serverLevel().getGameTime(),
                        player.fallDistance
                ));
    }

    public static boolean canRegisterSmashcast(Player player, ItemStack stack) {
        return stack.getItem() instanceof SmashcastScepter scepter && scepter.canStartSmashcast(player, stack);
    }

    public static float registerEpicFightSmashcastImpact(
            ServerPlayer player,
            LivingEntity target,
            DamageSource source,
            float fallDistance
    ) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SmashcastScepter scepter)) {
            return 0.0F;
        }

        var effectiveFallDistance = Math.max(0.0F, fallDistance);
        var bonusDamage = SmashcastScepter.calculateSmashBonusDamage(
                stack,
                effectiveFallDistance,
                player.serverLevel(),
                target,
                source
        );

        var level = player.serverLevel();
        var gameTime = level.getGameTime();
        var lastImpactTick = EPIC_FIGHT_SMASHCAST_IMPACT_TICKS
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .put(player.getUUID(), gameTime);
        if (lastImpactTick != null && lastImpactTick == gameTime) {
            return bonusDamage;
        }

        scepter.triggerSmashAnimation(player, stack);
        scepter.tryCastSmashSpell(player, stack, effectiveFallDistance);
        target.invulnerableTime = 0;
        applyImpulseFallDamageProtection(player);
        applyEpicFightPostAttackEnchantmentEffects(player, target, source, stack, effectiveFallDistance);
        applyAreaKnockback(player, target);
        playSmashVisualEffects(player, target, effectiveFallDistance);
        playSmashSound(player, effectiveFallDistance);
        return bonusDamage;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        var player = resolveDirectPlayerAttack(event.getSource());
        if (player == null || !(player.getMainHandItem().getItem() instanceof SmashcastScepter scepter)) {
            return;
        }

        var pending = removePending(player);
        if (pending == null || !pending.matches(event.getEntity(), player.serverLevel().getGameTime())) {
            return;
        }

        var stack = player.getMainHandItem();
        scepter.triggerSmashAnimation(player, stack);
        scepter.tryCastSmashSpell(player, stack, pending.fallDistance());

        event.getEntity().invulnerableTime = 0;
        applyImpulseFallDamageProtection(player);
        applyAreaKnockback(player, event.getEntity());
        playSmashVisualEffects(player, event.getEntity(), pending.fallDistance());
        playSmashSound(player, pending.fallDistance());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        var smashes = PENDING_SMASHES.get(serverLevel);
        if (smashes == null || smashes.isEmpty()) {
            cleanupEpicFightSmashcastImpacts(serverLevel);
            return;
        }

        var expireBefore = serverLevel.getGameTime() - 1L;
        smashes.entrySet().removeIf(uuidPendingSmashEntry -> uuidPendingSmashEntry.getValue().gameTime() < expireBefore);
        if (smashes.isEmpty()) {
            PENDING_SMASHES.remove(serverLevel);
        }
        cleanupEpicFightSmashcastImpacts(serverLevel);
    }

    private static void cleanupEpicFightSmashcastImpacts(ServerLevel serverLevel) {
        var impacts = EPIC_FIGHT_SMASHCAST_IMPACT_TICKS.get(serverLevel);
        if (impacts == null || impacts.isEmpty()) {
            return;
        }

        var expireBefore = serverLevel.getGameTime() - 1L;
        impacts.entrySet().removeIf(entry -> entry.getValue() < expireBefore);
        if (impacts.isEmpty()) {
            EPIC_FIGHT_SMASHCAST_IMPACT_TICKS.remove(serverLevel);
        }
    }

    private static ServerPlayer resolveDirectPlayerAttack(net.minecraft.world.damagesource.DamageSource source) {
        if (!(source.getDirectEntity() instanceof ServerPlayer player)) {
            return null;
        }
        if (!(source.is(DamageTypes.PLAYER_ATTACK) || "player".equals(source.getMsgId()))) {
            return null;
        }
        return player;
    }

    private static PendingSmash removePending(ServerPlayer player) {
        var smashes = PENDING_SMASHES.get(player.serverLevel());
        if (smashes == null) {
            return null;
        }

        var pending = smashes.remove(player.getUUID());
        if (smashes.isEmpty()) {
            PENDING_SMASHES.remove(player.serverLevel());
        }
        return pending;
    }

    public static void applyImpulseFallDamageProtection(ServerPlayer player) {
        if (player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos != null) {
            if (player.currentImpulseImpactPos.y > player.position().y) {
                player.currentImpulseImpactPos = player.position();
            }
        } else {
            player.currentImpulseImpactPos = player.position();
        }

        player.setIgnoreFallDamageFromCurrentImpulse(true);
        player.setDeltaMovement(player.getDeltaMovement().with(Direction.Axis.Y, SmashcastScepter.WIND_BURST_MOTION_EPSILON));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void applyEpicFightPostAttackEnchantmentEffects(
            ServerPlayer player,
            LivingEntity target,
            DamageSource source,
            ItemStack stack,
            float fallDistance
    ) {
        var originalFallDistance = player.fallDistance;
        player.fallDistance = Math.max(originalFallDistance, fallDistance);
        try {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(player.serverLevel(), target, source, stack);
        } finally {
            player.fallDistance = originalFallDistance;
        }
    }

    private static void applyAreaKnockback(ServerPlayer player, LivingEntity impactTarget) {
        var center = impactTarget.position();
        var area = AABB.ofSize(center, SmashcastScepter.SMASH_KNOCKBACK_RADIUS * 2.0D,
                SmashcastScepter.SMASH_KNOCKBACK_RADIUS * 2.0D,
                SmashcastScepter.SMASH_KNOCKBACK_RADIUS * 2.0D);
        for (var target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
            var offset = target.position().subtract(center);
            var horizontal = new Vec3(offset.x, 0.0D, offset.z);
            var distance = horizontal.length();
            if (distance > SmashcastScepter.SMASH_KNOCKBACK_RADIUS || distance <= 0.0001D) {
                continue;
            }

            var strength = SmashcastScepter.SMASH_KNOCKBACK_POWER
                    * (1.0D - distance / SmashcastScepter.SMASH_KNOCKBACK_RADIUS);
            var movement = horizontal.normalize().scale(strength).add(0.0D, 0.35D * strength, 0.0D);
            target.push(movement.x, movement.y, movement.z);
            target.hurtMarked = true;
        }
    }

    private static void playSmashVisualEffects(ServerPlayer player, LivingEntity impactTarget, float fallDistance) {
        if (impactTarget.onGround()) {
            player.setSpawnExtraParticlesOnFall(true);
        }
        player.serverLevel().levelEvent(MACE_SMASH_LEVEL_EVENT, impactTarget.getOnPos(), MACE_SMASH_LEVEL_EVENT_DATA);
        spawnTremorBlocks(player.serverLevel(), impactTarget.position(), fallDistance);
    }

    private static void spawnTremorBlocks(ServerLevel level, Vec3 center, float fallDistance) {
        var blocks = new LinkedHashSet<BlockPos>();
        for (int i = 0; i < TREMOR_BLOCK_LIMIT * 2 && blocks.size() < TREMOR_BLOCK_LIMIT; i++) {
            var angle = level.random.nextDouble() * Math.PI * 2.0D;
            var radius = SMASH_TREMOR_RADIUS * Math.sqrt(level.random.nextDouble());
            var x = center.x + Math.cos(angle) * radius;
            var z = center.z + Math.sin(angle) * radius;
            var blockPos = findVisibleGroundBlock(level, x, center.y + 1.0D, z, 4);
            if (blockPos != null) {
                blocks.add(blockPos);
            }
        }

        var impulseBase = fallDistance >= SmashcastScepter.HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD ? 0.32F : 0.24F;
        for (var blockPos : blocks) {
            spawnTremorBlock(level, blockPos, (impulseBase + level.random.nextFloat() * 0.10F) * TREMOR_IMPULSE_MULTIPLIER);
        }
    }

    private static void spawnTremorBlock(ServerLevel level, BlockPos blockPos, float impulseStrength) {
        var above = blockPos.above();
        if (!level.getBlockState(above).isAir() && !level.getBlockState(above.above()).isAir()) {
            return;
        }

        sendTremorParticle(level, blockPos, blockPos, impulseStrength);
        if (!level.getBlockState(above).isAir()) {
            sendTremorParticle(level, above, above, impulseStrength);
        }
    }

    private static void sendTremorParticle(ServerLevel level, BlockPos statePos, BlockPos particlePos, float impulseStrength) {
        var state = level.getBlockState(statePos);
        if (state.isAir()) {
            return;
        }

        level.sendParticles(
                new SmashcastTremorBlockParticleOptions(state, new Vec3(0.0D, impulseStrength, 0.0D)),
                particlePos.getX() + 0.5D,
                particlePos.getY(),
                particlePos.getZ() + 0.5D,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static BlockPos findVisibleGroundBlock(ServerLevel level, double x, double startY, double z, int searchDepth) {
        var start = BlockPos.containing(x, startY, z);
        for (int offset = 0; offset <= searchDepth; offset++) {
            var pos = start.below(offset);
            var state = level.getBlockState(pos);
            if (!state.isAir()
                    && state.getRenderShape() == RenderShape.MODEL
                    && !state.getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static void playSmashSound(ServerPlayer player, float fallDistance) {
        var sound = fallDistance >= SmashcastScepter.HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD
                ? SoundRegistry.SMASHCAST_SCEPTER_SMASH_GROUND_HEAVY.get()
                : SoundRegistry.SMASHCAST_SCEPTER_SMASH_GROUND.get();
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.SMASHCAST_SCEPTER_SMASH_AIR.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    private record PendingSmash(UUID targetUuid, long gameTime, float fallDistance) {
        private boolean matches(LivingEntity target, long currentGameTime) {
            return target.getUUID().equals(targetUuid) && currentGameTime - gameTime <= 1L;
        }
    }
}
