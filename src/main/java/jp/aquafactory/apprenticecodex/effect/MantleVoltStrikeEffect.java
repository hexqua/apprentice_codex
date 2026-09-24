package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.effect.VoltStrikeEffect;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import java.util.Comparator;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleElementalDash;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/** 通常詠唱のeffectとは別IDにし、解除と接触の所有者を外套へ限定する。 */
public final class MantleVoltStrikeEffect extends VoltStrikeEffect {
    public MantleVoltStrikeEffect() { super(MobEffectCategory.BENEFICIAL, 0xffaa55); }

    @Override
    public @NotNull String getDescriptionId() { return "spell.irons_spellbooks.volt_strike"; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        if (!MantleElementalDash.canTick(entity, MantleElementalDash.LIGHTNING)) return false;
        var player = (ServerPlayer) entity;
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        Vec3 current = player.position();
        Vec3 previous = dash.takeContactPosition(player);
        // 実際に移動した区間を細分化する。entity本体のboxを広げると床まで衝突するため、攻撃用だけ下へ伸ばす。
        Vec3 displacement = current.subtract(previous);
        int samples = Math.max(1, (int) Math.ceil(displacement.length() / .2));
        // 転送等の不連続な移動を攻撃経路として採用しない。
        if (samples > 40 && displacement.length() > Math.max(8, dash.motion().length() + 1)) {
            previous = current;
            displacement = Vec3.ZERO;
            samples = 1;
        }
        AABB body = player.getBoundingBox().move(previous.subtract(current));
        for (int i = 0; i <= samples; i++) {
            Vec3 offset = displacement.scale((double) i / samples);
            AABB at = body.move(offset);
            if (!player.level().noBlockCollision(player, at.deflate(.1))) {
                impact(player, amplifier, null, at.getCenter());
                return false;
            }
            AABB attack = new AABB(at.minX - .25, at.minY - 1.5, at.minZ - .25,
                    at.maxX + .25, at.maxY + .5, at.maxZ + .25);
            Vec3 origin = at.getCenter();
            var candidates = player.level().getEntities(player, attack);
            candidates.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(origin)));
            for (var target : candidates) {
                if (Utils.hasLineOfSight(player.level(), origin, target.getBoundingBox().getCenter(), true)
                        && DamageSources.applyDamage(target, amplifier, SpellRegistry.VOLT_STRIKE_SPELL.get().getDamageSource(player))) {
                    target.invulnerableTime = 20;
                    impact(player, amplifier, target, origin);
                    return false;
                }
            }
        }
        // 壁直前で物理移動が止まった場合も命中扱いとし、上流同様に次の移動先まで確認する。
        Vec3 movement = player.getDeltaMovement();
        int wallSamples = Math.max(1, (int) Math.ceil(movement.length() / .2));
        wallSamples = Math.min(40, wallSamples);
        for (int i = 1; i <= wallSamples; i++) {
            if (!player.level().noBlockCollision(player, player.getBoundingBox().move(movement.scale((double) i / wallSamples)).deflate(.1))) {
                impact(player, amplifier, null, player.getBoundingBox().getCenter());
                return false;
            }
        }
        player.fallDistance = 0;
        return true;
    }

    private static void impact(ServerPlayer player, int amplifier, Entity directHit, Vec3 origin) {
        // 上流VoltStrikeの半径・減衰・反動を維持する。直接命中した相手へ爆発を重複適用しない。
        var world = player.level();
        Vec3 blast = world.clip(new ClipContext(origin, origin.add(0, .15, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        for (var target : world.getEntities(player, new AABB(blast, blast).inflate(4))) {
            double distanceSquared = target.position().distanceToSqr(blast);
            if (target != directHit && distanceSquared < 16 && target.canBeHitByProjectile()
                    && Utils.hasLineOfSight(world, blast, target.getBoundingBox().getCenter(), true)) {
                DamageSources.applyDamage(target, (float) (amplifier * (1 - distanceSquared / 16) * .5),
                        SpellRegistry.VOLT_STRIKE_SPELL.get().getDamageSource(player));
            }
        }
        player.setDeltaMovement(player.getDeltaMovement().normalize().scale(-.5).add(0, .5, 0));
        player.hurtMarked = true;
        MagicManager.spawnParticles(world, ParticleHelper.ELECTRIC_SPARKS, blast.x, blast.y, blast.z, 25, .08, .08, .08, .3, false);
        MagicManager.spawnParticles(world, ParticleHelper.ELECTRICITY, blast.x, blast.y, blast.z, 75, .1, .1, .1, .5, false);
        MagicManager.spawnParticles(world, new BlastwaveParticleOptions(new Vector3f(.7f, 1, 1), 8),
                blast.x, blast.y + .15, blast.z, 1, 0, 0, 0, 0, true);
        world.playSound(null, blast.x, blast.y, blast.z, SoundEvents.TRIDENT_THUNDER.value(), player.getSoundSource(), 4, .8f);
        ShootingStarMantleRuntime.state(player).elemental.contact(player);
    }

    @Override
    public void onEffectRemoved(LivingEntity entity, int amplifier) {
        super.onEffectRemoved(entity, amplifier);
        MantleElementalDash.restoreSpin(entity);
    }
}
