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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffAttackHandler {
    public record CombatDamageAdjustment(float baseAmount, boolean ignoreIframe, int postHitIframeTicks) {
        public static CombatDamageAdjustment unchanged(float baseAmount) {
            return new CombatDamageAdjustment(baseAmount, false, 0);
        }
    }

    private static final ThreadLocal<CastContext> ACTIVE_CAST = new ThreadLocal<>();
    private static final ConcurrentMap<UUID, TrackedProjectile> TRACKED_PROJECTILES = new ConcurrentHashMap<>();
    private static final Map<Entity, TrackedProjectile> TRACKED_WEAPON_ATTACKS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<DamageSource, PendingPostHitIframe> PENDING_POST_HIT_IFRAMES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MulticastEchoStaffAttackHandler() {
    }

    public static void runRepeatedCast(ServerPlayer caster, AbstractSpell spell, Runnable castAction) {
        try (var ignored = openCast(caster, spell, AttackOrigin.STAFF)) {
            castAction.run();
        }
    }

    // 開始前生成・即時化の完了処理も同じコンテキストに含め、遅延攻撃へ設定元を引き継ぐ。
    public static CastScope openCast(ServerPlayer caster, AbstractSpell spell, AttackOrigin origin) {
        var profile = resolveActiveProfile(spell, origin);
        var previousContext = ACTIVE_CAST.get();
        ACTIVE_CAST.set(profile == null ? null : new CastContext(caster.getUUID(), spell.getSpellResource(), profile));
        return () -> {
            if (previousContext == null) {
                ACTIVE_CAST.remove();
            } else {
                ACTIVE_CAST.set(previousContext);
            }
        };
    }

    // Projectile以外は明示登録する。術者が同じだけの通常攻撃を追加詠唱に巻き込まない。
    public static void trackWeaponAttack(Entity entity) {
        var context = ACTIVE_CAST.get();
        if (context == null || entity == null || !(entity.level() instanceof ServerLevel level)
                || context.profile().trackingLifetimeTicks() <= 0) {
            return;
        }
        TRACKED_WEAPON_ATTACKS.putIfAbsent(entity, new TrackedProjectile(context.casterId(), context.spellId(),
                level.dimension(), context.profile(), level.getGameTime() + context.profile().trackingLifetimeTicks()));
    }

    public static void runWeaponTick(Entity weapon, Runnable action) {
        var tracked = getTrackedWeaponAttack(weapon);
        if (tracked == null) {
            action.run();
            return;
        }
        var previousContext = ACTIVE_CAST.get();
        ACTIVE_CAST.set(new CastContext(tracked.casterId(), tracked.spellId(), tracked.profile()));
        try {
            action.run();
        } finally {
            if (previousContext == null) {
                ACTIVE_CAST.remove();
            } else {
                ACTIVE_CAST.set(previousContext);
            }
            if (weapon.isRemoved()) {
                TRACKED_WEAPON_ATTACKS.remove(weapon);
            }
        }
    }

    private static @Nullable TrackedProjectile getTrackedWeaponAttack(Entity entity) {
        var tracked = TRACKED_WEAPON_ATTACKS.get(entity);
        if (tracked == null) {
            return null;
        }
        if (entity.isRemoved() || !(entity.level() instanceof ServerLevel level)
                || !tracked.dimension().equals(level.dimension())
                || tracked.expireGameTime() <= level.getGameTime()) {
            TRACKED_WEAPON_ATTACKS.remove(entity);
            return null;
        }
        return tracked.profile().origin().enabled() ? tracked : null;
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

    private static @Nullable ConfiguredProfile resolveActiveProfile(AbstractSpell spell, AttackOrigin origin) {
        return origin.enabled() ? MulticastEchoStaffAttackProfileManager.getProfile(spell)
                .map(profile -> new ConfiguredProfile(profile, origin)).orElse(null) : null;
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
    public static void onLivingDamage(LivingDamageEvent.Post event) {
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
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
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

    private static @Nullable ConfiguredProfile resolveProfileForDamage(
            DamageSource source,
            @Nullable ResourceLocation spellId
    ) {
        var activeContext = ACTIVE_CAST.get();
        if (activeContext != null
                && activeContext.profile().origin().enabled()
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
        if (tracked == null) {
            var weaponAttack = getTrackedWeaponAttack(directEntity);
            if (weaponAttack != null && weaponAttack.profile().directDamageTracking()) {
                tracked = weaponAttack;
            }
        }
        if (tracked == null
                || !tracked.profile().origin().enabled()
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

    private static float applyRepeatDamageMultiplier(float amount, ConfiguredProfile profile) {
        var multiplier = Math.max(0.0D, profile.repeatDamageMultiplier())
                * Math.max(0.0D, profile.origin().damageMultiplier());
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

    public interface CastScope extends AutoCloseable {
        @Override
        void close();
    }

    public enum AttackOrigin {
        STAFF, RIFLE;

        public boolean enabled() {
            return this == STAFF ? ApprenticeCodexServerConfig.multicastEchoStaffAttackProfilesEnabled()
                    : ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastEnabled();
        }

        public double damageMultiplier() {
            return this == STAFF ? ApprenticeCodexServerConfig.multicastEchoStaffRepeatDamageMultiplier()
                    : ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastDamageMultiplier();
        }
    }

    private record ConfiguredProfile(MulticastEchoStaffAttackProfile definition, AttackOrigin origin) {
        double repeatDamageMultiplier() { return definition.repeatDamageMultiplier(); }
        boolean ignoreIframe() { return definition.ignoreIframe(); }
        boolean projectileTracking() { return definition.projectileTracking(); }
        boolean directDamageTracking() { return definition.directDamageTracking(); }
        int trackingLifetimeTicks() { return definition.trackingLifetimeTicks(); }
        int postHitIframeTicks() { return definition.postHitIframeTicks(); }
    }

    private record CastContext(
            UUID casterId,
            ResourceLocation spellId,
            ConfiguredProfile profile
    ) {
    }

    private record TrackedProjectile(
            UUID casterId,
            ResourceLocation spellId,
            ResourceKey<Level> dimension,
            ConfiguredProfile profile,
            long expireGameTime
    ) {
    }

    private record PendingPostHitIframe(UUID targetId, int postHitIframeTicks, long expireGameTime) {
    }
}
