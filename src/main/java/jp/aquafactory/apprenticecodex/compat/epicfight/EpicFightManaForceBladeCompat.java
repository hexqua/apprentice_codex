package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeGuardLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EpicFightManaForceBladeCompat {
    public static final String MOD_ID = "epicfight";

    private static final UUID GUARD_EVENT_UUID = UUID.fromString("b1dc3c56-6e3d-4f37-85c5-b2c476559c46");
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();

    private EpicFightManaForceBladeCompat() {
    }

    public static void install(ServerPlayer player) {
        if (!player.isAlive() || INSTALLED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerpatch -> {
            // Epic Fight の通常ガード後にも反撃を差し込めるよう、cancel 済みでも動く -1 priority に置く。
            playerpatch.getEventListener().addEventListener(
                    EventType.TAKE_DAMAGE_EVENT_ATTACK,
                    GUARD_EVENT_UUID,
                    EpicFightManaForceBladeCompat::onTakeDamage,
                    -1
            );
            INSTALLED_PLAYERS.add(player.getUUID());
        });
    }

    public static void clear(ServerPlayer player) {
        INSTALLED_PLAYERS.remove(player.getUUID());
    }

    public static boolean isGuarding(ServerPlayer player) {
        return EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .map(EpicFightManaForceBladeCompat::isGuardingWithManaForceBlade)
                .orElse(false);
    }

    private static void onTakeDamage(TakeDamageEvent.Attack event) {
        var playerpatch = event.getPlayerPatch();
        var guardContext = resolveGuardContext(playerpatch);
        if (guardContext == null) {
            return;
        }
        if (!isDamageSourceInFront(playerpatch, event)) {
            return;
        }

        var player = playerpatch.getOriginal();
        var stack = player.getMainHandItem();
        var wasAlreadyBlockedByEpicFight = event.isCanceled();
        var previousResult = event.getResult();
        var previousParried = event.isParried();
        var wasBlockedByThisCompat = false;
        if (!wasAlreadyBlockedByEpicFight) {
            tryGuardWithEpicFight(guardContext, event);
            wasBlockedByThisCompat = event.isCanceled();
        }
        var wasBlockedByEpicFight = wasAlreadyBlockedByEpicFight || wasBlockedByThisCompat;

        if (!ManaForceBladeGuardLogic.tryHandleGuard(
                player,
                stack,
                event.getDamageSource(),
                event.isParried(),
                false
        )) {
            if (wasBlockedByThisCompat) {
                // Epic Fight の guard は先に event を確定するため、マナ/耐久不足では通常被弾へ戻す。
                event.setCanceled(false);
                event.setResult(previousResult);
                event.setParried(previousParried);
            }
            return;
        }

        if (!wasBlockedByEpicFight) {
            markBlockedWithEpicFightGuard(event, guardContext);
        }
    }

    private static GuardContext resolveGuardContext(ServerPlayerPatch playerpatch) {
        ItemStack stack = playerpatch.getOriginal().getMainHandItem();
        if (!ManaForceBlade.isManaForceBlade(stack) || !(playerpatch.getHoldingSkill() instanceof GuardSkill guardSkill)) {
            return null;
        }

        return new GuardContext(
                guardSkill,
                playerpatch.getSkill(guardSkill),
                playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND)
        );
    }

    private static boolean isGuardingWithManaForceBlade(ServerPlayerPatch playerpatch) {
        return resolveGuardContext(playerpatch) != null;
    }

    private static void tryGuardWithEpicFight(GuardContext guardContext, TakeDamageEvent.Attack event) {
        var damageSource = event.getDamageSource();
        if (damageSource instanceof EpicFightDamageSource epicfightDamageSource
                && epicfightDamageSource.is(EpicFightDamageTypeTags.GUARD_PUNCTURE)) {
            return;
        }

        guardContext.guardSkill().guard(
                guardContext.skillContainer(),
                guardContext.itemCapability(),
                event,
                resolveGuardKnockback(damageSource),
                resolveGuardImpact(damageSource),
                false
        );
    }

    private static float resolveGuardKnockback(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (damageSource instanceof EpicFightDamageSource epicfightDamageSource) {
            return 0.25F + Math.min(epicfightDamageSource.calculateImpact() * 0.1F, 1.0F);
        }

        return 0.25F;
    }

    private static float resolveGuardImpact(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (damageSource instanceof EpicFightDamageSource epicfightDamageSource) {
            return epicfightDamageSource.calculateImpact();
        }

        return 0.5F;
    }

    private static boolean isDamageSourceInFront(ServerPlayerPatch playerpatch, TakeDamageEvent.Attack event) {
        var sourcePosition = event.getDamageSource().getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }

        var player = playerpatch.getOriginal();
        var viewVector = player.getViewVector(1.0F);
        viewVector = viewVector.subtract(0.0D, viewVector.y, 0.0D).normalize();
        var toSourcePosition = sourcePosition.subtract(player.position()).normalize();
        return toSourcePosition.dot(viewVector) > 0.0D;
    }

    private static void markBlockedWithEpicFightGuard(TakeDamageEvent.Attack event, GuardContext guardContext) {
        playEpicFightGuardFeedback(event, guardContext);
        markBlocked(event);
    }

    private static void playEpicFightGuardFeedback(TakeDamageEvent.Attack event, GuardContext guardContext) {
        var playerpatch = event.getPlayerPatch();
        playerpatch.playSound(EpicFightSounds.CLASH.get(), -0.05F, 0.1F);

        var offender = resolveOffender(event);
        if (offender != null) {
            EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(
                    playerpatch.getOriginal().serverLevel(),
                    HitParticleType.FRONT_OF_EYES,
                    HitParticleType.ZERO,
                    playerpatch.getOriginal(),
                    offender
            );
        }

        var animation = guardContext.itemCapability()
                .getGuardMotion(guardContext.guardSkill(), GuardSkill.BlockType.GUARD, playerpatch);
        if (animation != null) {
            // ManaForceBlade 固有の projectile ガードでも、アドオンが item capability 側へ差し込んだ guard motion は優先して使う。
            playerpatch.playAnimationSynchronized(animation, 0.0F);
        }
    }

    private static Entity resolveOffender(TakeDamageEvent.Attack event) {
        var directEntity = event.getDamageSource().getDirectEntity();
        if (directEntity instanceof Projectile) {
            return directEntity;
        }

        return event.getDamageSource().getEntity();
    }

    private static void markBlocked(TakeDamageEvent.Attack event) {
        event.setCanceled(true);
        event.setResult(AttackResult.ResultType.BLOCKED);
        event.getPlayerPatch().countHurtTime(event.getDamage());

        var attacker = event.getDamageSource().getEntity();
        if (attacker != null) {
            EpicFightCapabilities.getUnparameterizedEntityPatch(attacker, LivingEntityPatch.class)
                    .ifPresent(attackerpatch -> attackerpatch.setLastAttackEntity(event.getPlayerPatch().getOriginal()));
        }

        var directEntity = event.getDamageSource().getDirectEntity();
        if (directEntity instanceof LivingEntity livingEntity) {
            EpicFightCapabilities.<LivingEntity, LivingEntityPatch<LivingEntity>>getParameterizedEntityPatch(
                            livingEntity,
                            LivingEntity.class,
                            LivingEntityPatch.class
                    )
                    .ifPresent(entitypatch -> entitypatch.onAttackBlocked(event.getDamageSource(), event.getPlayerPatch()));
        }
    }

    private record GuardContext(
            GuardSkill guardSkill,
            SkillContainer skillContainer,
            CapabilityItem itemCapability
    ) {
    }
}
