package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.mixin.LivingEntityAccessor;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncMantleDashPacket;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 通常詠唱の追加データを占有せず、外套の突進だけを管理する。 */
public final class MantleElementalDash {
    public static final int FIRE = 1;
    public static final int LIGHTNING = 2;
    private int kind;
    private int level;
    private boolean hover;
    private long end;
    private long revision;
    private Vec3 motion = Vec3.ZERO;
    private Vec3 contactPosition;
    private boolean depletionPending;
    private boolean held;
    private boolean blocked;
    private boolean wasFlying;
    private float forward;
    private float strafe;
    private long inputSequence = -1;
    private long inputTime = Long.MIN_VALUE;
    private long lastActivation = Long.MIN_VALUE;

    public int kind() { return kind; }
    public int level() { return level; }
    public boolean hover() { return hover; }
    public boolean active() { return kind != 0; }
    public Vec3 motion() { return motion; }
    public long end() { return end; }
    public boolean paidHover() { return active() && hover; }
    public boolean shouldRenderHoverDashSpin(Player player) {
        // この状態は外套専用。共通の回転フラグだけでは激流や通常詠唱を識別できない。
        return paidHover() && player.level().getGameTime() < end && player.isAutoSpinAttack();
    }
    public boolean paidFlight() { return kind == FIRE && !hover; }
    public boolean defersDepletion() { return active() && depletionPending; }
    public void deferDepletion() { depletionPending = true; }

    public Vec3 takeContactPosition(Player player) {
        Vec3 previous = contactPosition == null ? player.position() : contactPosition;
        contactPosition = player.position();
        return previous;
    }

    public boolean invulnerable(ServerPlayer player) {
        return paidHover() && player.level().getGameTime() < end
                && ShootingStarMantleRuntime.isHovering(player) && canTick(player, kind)
                && player.hasEffect(effect(kind));
    }

    public static AbstractSpell spell(int kind) {
        return kind == FIRE ? SpellRegistry.BURNING_DASH_SPELL.get() : SpellRegistry.VOLT_STRIKE_SPELL.get();
    }

    public static Holder<MobEffect> effect(int kind) {
        return kind == FIRE ? EffectRegistry.MANTLE_BURNING_DASH : EffectRegistry.MANTLE_VOLT_STRIKE;
    }

    public static boolean normalDash(LivingEntity entity) {
        return entity.hasEffect(MobEffectRegistry.BURNING_DASH) || entity.hasEffect(MobEffectRegistry.VOLT_STRIKE);
    }

    public void input(ServerPlayer player, long sequence, boolean jump, float forwardInput, float strafeInput) {
        if (sequence < 0 || sequence <= inputSequence || !Float.isFinite(forwardInput) || !Float.isFinite(strafeInput)) return;
        inputSequence = sequence;
        inputTime = player.level().getGameTime();
        boolean pressed = jump && !held;
        held = jump;
        forward = Mth.clamp(forwardInput, -1, 1);
        strafe = Mth.clamp(strafeInput, -1, 1);
        if (!jump) blocked = false;
        var state = ShootingStarMantleRuntime.state(player);
        if (pressed) {
            if (!blocked && (!state.hovering || !active())) begin(player);
            // 拒否時も予測を確定状態へ戻す。保持packetには応答せず帯域を抑える。
            sync(player);
        }
    }

    public void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (inputTime != Long.MIN_VALUE && now - inputTime > 10) held = false;
        var state = ShootingStarMantleRuntime.state(player);
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        boolean flying = player.isFallFlying() && ShootingStarMantleRuntime.canFly(player);
        boolean enteredFlight = flying && !wasFlying;
        wasFlying = flying;
        if (active() && (MantleCalibration.elementalKind(stack) != kind || hover != state.hovering
                || (!hover && !flying) || normalDash(player))) {
            cancel(player);
        }
        if (active()) {
            // 最終tickの衝突は効果のcallbackで先に終了する。消去・牛乳等を自然終了にしない。
            if (now < end && !player.hasEffect(effect(kind))) cancel(player);
            else if (now >= end) finish(player, false, true);
        } else if (enteredFlight && !blocked) begin(player);
    }

    private void begin(ServerPlayer player) {
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        int selected = MantleCalibration.elementalKind(stack);
        var state = ShootingStarMantleRuntime.state(player);
        long now = player.level().getGameTime();
        if (selected == 0 || selected == LIGHTNING && !state.hovering
                || !MantleEnergy.read(stack).usable() || normalDash(player)
                || ShootingStarMantleRuntime.conflict(player) || ShootingStarMantleRuntime.invalidHoverContext(player)
                || (!ShootingStarMantleRuntime.isHovering(player)
                && !(player.isFallFlying() && ShootingStarMantleRuntime.canFly(player)))
                || lastActivation == now) return;
        lastActivation = now;
        hover = state.hovering;
        kind = selected;
        var spell = spell(kind);
        level = 1;
        int duration = hover ? 5 : 15;
        end = now + duration;
        contactPosition = player.position();
        var remaining = MantleEnergy.read(stack).spend(10);
        remaining.save(stack);
        depletionPending = remaining.energy() == 0;
        if (hover) {
            state.dashTicks = 0;
            state.blink.cancel();
        }
        Vec3 direction = hover ? MantleMovement.direction(forward, strafe, player.getYRot()) : player.getLookAngle();
        float power = spell.getSpellPower(level, player);
        Vec3 impulse;
        if (hover) {
            impulse = direction.scale(2);
        } else {
            impulse = direction.multiply(3, 1, 3).normalize();
            if (!hover) impulse = impulse.add(0, .25, 0);
            impulse = impulse.scale((15 + power) / 12f);
        }
        // 浮遊では入力を正確に反映し、飛行では上流同様に現在速度を25%引き継ぐ。
        motion = hover ? impulse : player.getDeltaMovement().scale(.25).add(impulse.scale(.75));
        player.setDeltaMovement(hover ? new Vec3(motion.x, player.getDeltaMovement().y, motion.z) : motion);
        player.hurtMarked = true;
        player.hasImpulse = true;
        // 弱いレベルで再入力した場合も、以前の強いeffectをhiddenEffectとして残さない。
        player.removeEffect(effect(kind));
        // 浮遊の終了はendで管理する。付与tick中のeffect減算で最終tickの保護が先に消えないよう1tick余裕を持たせる。
        player.addEffect(new MobEffectInstance(effect(kind), duration + (hover ? 1 : 0),
                (int) (5 + power), false, false, false));
        if (!hover) player.invulnerableTime = 20;
        MagicData.getPlayerMagicData(player).getSyncedData().setSpinAttackType(kind == FIRE ? SpinAttackType.FIRE : SpinAttackType.LIGHTNING);
        // Player.playSoundはserverから本人を除外する。予測側で鳴らさず全員へ一度だけ配信する。
        spell.getCastFinishSound().ifPresent(sound -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, player.getSoundSource(), 2, .9f + player.getRandom().nextFloat() * .2f));
        revision++;
        sync(player);
        ShootingStarMantleRuntime.sync(player, false);
    }

    public void contact(ServerPlayer player) {
        if (!active()) return;
        blocked = true;
        // 現在tick中の効果の削除はMobEffectInstanceに任せ、iteratorを壊さない。
        finish(player, true, false);
        ShootingStarMantleRuntime.sync(player, false);
    }

    public void cancel(Player player) {
        blocked = held;
        if (active()) finish(player, false, true);
        wasFlying = player.isFallFlying();
    }

    private void finish(Player player, boolean collision, boolean remove) {
        int previous = kind;
        if (previous == 0) return;
        kind = 0;
        end = 0;
        contactPosition = null;
        boolean depleted = depletionPending;
        depletionPending = false;
        if (!hover && !MantleEnergy.read(ShootingStarMantleRuntime.findEquipped(player)).usable()
                && !ShootingStarMantleRuntime.conflict(player)) player.stopFallFlying();
        if (hover && !(collision && previous == LIGHTNING) && !normalDash(player)) MantleMovement.finishImpulse(player);
        motion = player.getDeltaMovement();
        if (remove) player.removeEffect(effect(previous));
        revision++;
        if (player instanceof ServerPlayer serverPlayer) {
            if (depleted) ShootingStarMantleRuntime.notifyDepleted(serverPlayer);
            sync(serverPlayer);
            ShootingStarMantleRuntime.sync(serverPlayer, false);
        }
    }

    public static boolean canTick(LivingEntity entity, int kind) {
        if (!(entity instanceof ServerPlayer player)) return false;
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        return dash.kind == kind && !normalDash(player)
                && MantleCalibration.elementalKind(ShootingStarMantleRuntime.findEquipped(player)) == kind
                && !ShootingStarMantleRuntime.conflict(player) && !ShootingStarMantleRuntime.invalidHoverContext(player);
    }

    public static void restoreSpin(LivingEntity entity) {
        boolean fire = entity.hasEffect(MobEffectRegistry.BURNING_DASH);
        boolean lightning = entity.hasEffect(MobEffectRegistry.VOLT_STRIKE);
        if (fire || lightning) {
            ((LivingEntityAccessor) entity).setLivingEntityFlagInvoker(4, true);
            if (!entity.level().isClientSide) MagicData.getPlayerMagicData(entity).getSyncedData()
                    .setSpinAttackType(lightning ? SpinAttackType.LIGHTNING : SpinAttackType.FIRE);
        }
    }

    public boolean travel(Player player, ShootingStarMantleRuntime.State state) {
        if (!paidHover()) return false;
        if (player.level().getGameTime() >= end) return false;
        state.movingTicks = MantleMovement.movingTicks(state.lastPosition, player.position(), state.movingTicks);
        state.lastPosition = player.position();
        // 通常の浮遊と同じ高さ制御を使い、突進中も地形へ追従する。
        double y = MantleMovement.hoverVertical(player, state);
        player.setDeltaMovement(motion.x, y, motion.z);
        player.move(MoverType.SELF, player.getDeltaMovement());
        player.fallDistance = 0;
        player.calculateEntityAnimation(false);
        return true;
    }

    public SyncMantleDashPacket packet(Player player) {
        return new SyncMantleDashPacket(player.getId(), kind, level, hover, end, revision,
                active() ? motion : player.getDeltaMovement());
    }

    private void sync(ServerPlayer player) { Networks.sendToTrackingEntityAndSelf(player, packet(player)); }

    public boolean accept(SyncMantleDashPacket packet) {
        boolean changed = packet.revision() != revision || packet.kind() != kind || packet.end() != end;
        kind = packet.kind();
        level = packet.level();
        hover = packet.hover();
        end = packet.end();
        revision = packet.revision();
        motion = packet.motion();
        return changed;
    }

    public void predictHover(Player player, int selected, float forwardInput, float strafeInput) {
        if (active() || normalDash(player)) return;
        kind = selected;
        hover = true;
        level = 1;
        end = player.level().getGameTime() + 5;
        motion = MantleMovement.direction(forwardInput, strafeInput, player.getYRot())
                .scale(2);
        player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
    }
}
