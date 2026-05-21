package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MulticastEchoStaffAttackHandler {
    public record CombatDamageAdjustment(float baseAmount, boolean ignoreIframe, int postHitIframeTicks) {
        public static CombatDamageAdjustment unchanged(float baseAmount) {
            return new CombatDamageAdjustment(baseAmount, false, 0);
        }
    }

    private static final ThreadLocal<CastContext> ACTIVE_CAST = new ThreadLocal<>();
    private static final ConcurrentMap<UUID, TrackedProjectile> TRACKED_PROJECTILES = new ConcurrentHashMap<>();
    private static final Map<DamageSource, PendingPostHitIframe> PENDING_POST_HIT_IFRAMES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MulticastEchoStaffAttackHandler() {
    }

    public static void runRepeatedCast(ServerPlayer caster, AbstractSpell spell, Runnable castAction) {
        var profile = resolveActiveProfile(spell);
        if (profile == null) {
            castAction.run();
            return;
        }

        var previousContext = ACTIVE_CAST.get();
        ACTIVE_CAST.set(new CastContext(caster.getUUID(), spell.getSpellResource(), profile));
        try {
            castAction.run();
        } finally {
            if (previousContext == null) {
                ACTIVE_CAST.remove();
            } else {
                ACTIVE_CAST.set(previousContext);
            }
        }
    }

    public static CombatDamageAdjustment adjustCombatDamage(Entity target, float baseAmount, DamageSource source) {
        var profile = resolveProfileForDamage(source, null);
        if (profile == null) {
            return CombatDamageAdjustment.unchanged(baseAmount);
        }

        var adjustedAmount = applyRepeatDamageMultiplier(baseAmount, profile);
        if (target instanceof LivingEntity livingTarget && profile.ignoreIframe()) {
            livingTarget.invulnerableTime = 0;
            return new CombatDamageAdjustment(adjustedAmount, true, Math.max(0, profile.postHitIframeTicks()));
        }
        return new CombatDamageAdjustment(adjustedAmount, false, 0);
    }

    private static @Nullable MulticastEchoStaffAttackProfile resolveActiveProfile(AbstractSpell spell) {
        if (!ApprenticeCodexServerConfig.multicastEchoStaffAttackProfilesEnabled()) {
            return null;
        }

        return MulticastEchoStaffAttackProfileManager.getProfile(spell).orElse(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellDamage(SpellDamageEvent event) {
        var spellDamageSource = event.getSpellDamageSource();
        var spell = spellDamageSource.spell();
        var spellId = spell == null ? null : spell.getSpellResource();
        var profile = resolveProfileForDamage(spellDamageSource, spellId);
        if (profile == null) {
            return;
        }

        event.setAmount(applyRepeatDamageMultiplier(event.getAmount(), profile));
        if (profile.ignoreIframe()) {
            event.getEntity().invulnerableTime = 0;
            rememberPostHitIframe(spellDamageSource, event.getEntity(), Math.max(0, profile.postHitIframeTicks()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        var pending = removePendingPostHitIframe(event.getSource());
        if (pending == null || !pending.targetId().equals(event.getEntity().getUUID())) {
            return;
        }

        event.getEntity().invulnerableTime = pending.postHitIframeTicks();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        var context = ACTIVE_CAST.get();
        if (context == null
                || !context.profile().projectileTracking()
                || Math.max(0, context.profile().trackingLifetimeTicks()) <= 0
                || event.getLevel().isClientSide()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Projectile projectile)) {
            return;
        }

        var owner = projectile.getOwner();
        if (owner == null || !context.casterId().equals(owner.getUUID())) {
            return;
        }

        var expireGameTime = serverLevel.getGameTime() + context.profile().trackingLifetimeTicks();
        TRACKED_PROJECTILES.put(projectile.getUUID(), new TrackedProjectile(
                context.casterId(),
                context.spellId(),
                serverLevel.dimension(),
                context.profile(),
                expireGameTime
        ));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var dimension = serverLevel.dimension();
        var gameTime = serverLevel.getGameTime();
        TRACKED_PROJECTILES.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(dimension) && entry.getValue().expireGameTime() <= gameTime);

        synchronized (PENDING_POST_HIT_IFRAMES) {
            PENDING_POST_HIT_IFRAMES.entrySet().removeIf(entry -> entry.getValue().expireGameTime() <= gameTime);
        }
    }

    private static @Nullable MulticastEchoStaffAttackProfile resolveProfileForDamage(
            DamageSource source,
            @Nullable ResourceLocation spellId
    ) {
        if (!ApprenticeCodexServerConfig.multicastEchoStaffAttackProfilesEnabled()) {
            return null;
        }

        var activeContext = ACTIVE_CAST.get();
        if (activeContext != null
                && activeContext.profile().directDamageTracking()
                && (spellId == null || activeContext.spellId().equals(spellId))
                && isSourceFromCaster(source, activeContext.casterId())) {
            return activeContext.profile();
        }

        var directEntity = source.getDirectEntity();
        if (directEntity == null) {
            return null;
        }

        var tracked = TRACKED_PROJECTILES.get(directEntity.getUUID());
        if (tracked == null
                || (spellId != null && !tracked.spellId().equals(spellId))
                || !isSourceFromCaster(source, tracked.casterId())) {
            return null;
        }

        return tracked.profile();
    }

    private static boolean isSourceFromCaster(DamageSource source, UUID casterId) {
        var causingEntity = source.getEntity();
        if (causingEntity != null && casterId.equals(causingEntity.getUUID())) {
            return true;
        }

        var directEntity = source.getDirectEntity();
        return directEntity != null && casterId.equals(directEntity.getUUID());
    }

    private static float applyRepeatDamageMultiplier(float amount, MulticastEchoStaffAttackProfile profile) {
        var multiplier = Math.max(0.0D, profile.repeatDamageMultiplier())
                * Math.max(0.0D, ApprenticeCodexServerConfig.multicastEchoStaffRepeatDamageMultiplier());
        return (float) (amount * multiplier);
    }

    private static void rememberPostHitIframe(DamageSource source, LivingEntity target, int postHitIframeTicks) {
        synchronized (PENDING_POST_HIT_IFRAMES) {
            PENDING_POST_HIT_IFRAMES.put(source, new PendingPostHitIframe(
                    target.getUUID(),
                    postHitIframeTicks,
                    target.level().getGameTime() + 1L
            ));
        }
    }

    private static @Nullable PendingPostHitIframe removePendingPostHitIframe(DamageSource source) {
        synchronized (PENDING_POST_HIT_IFRAMES) {
            return PENDING_POST_HIT_IFRAMES.remove(source);
        }
    }

    private record CastContext(
            UUID casterId,
            ResourceLocation spellId,
            MulticastEchoStaffAttackProfile profile
    ) {
    }

    private record TrackedProjectile(
            UUID casterId,
            ResourceLocation spellId,
            ResourceKey<Level> dimension,
            MulticastEchoStaffAttackProfile profile,
            long expireGameTime
    ) {
    }

    private record PendingPostHitIframe(UUID targetId, int postHitIframeTicks, long expireGameTime) {
    }
}
