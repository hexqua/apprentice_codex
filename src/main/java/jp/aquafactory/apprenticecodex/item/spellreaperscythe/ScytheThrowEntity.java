package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.packet.ScytheRecallEffectPacket;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public final class ScytheThrowEntity extends Projectile implements GeoEntity {
    public enum Mode { NORMAL, REBOUND, NARROW }
    public static final int DEFAULT_TRAIL_COLOR = 0xB4DCFF;
    private static final EntityDataAccessor<Integer> TRAIL_COLOR = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOVERING = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> THROW_YAW = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> MAELSTROM = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation THROW = RawAnimation.begin().thenLoop("throw_normal");
    private static final RawAnimation THROW_VERTICAL = RawAnimation.begin().thenLoop("throw_vertical");
    private final Set<UUID> outwardContacts = new HashSet<>();
    private final Set<UUID> returnContacts = new HashSet<>();
    private Player player;
    private ItemStack original = ItemStack.EMPTY;
    private ItemStack weapon = ItemStack.EMPTY;
    private Vec3 start = Vec3.ZERO;
    private Vec3 destination = Vec3.ZERO;
    private int slot;
    private int travelTicks;
    private int age;
    private int hoverTicks;
    private boolean ending;
    private boolean clientSpinStarted;
    private float physical;
    private float magic;

    public ScytheThrowEntity(EntityType<? extends ScytheThrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void prepare(Player owner, ItemStack stack, int slot, double distance) {
        prepare(owner, stack, slot, distance, Mode.NORMAL);
    }

    public void prepare(Player owner, ItemStack stack, int slot, double distance, Mode mode) {
        this.player = owner;
        this.original = stack;
        this.weapon = stack.copy();
        var school = MagicTools.getImbuedSpellSchool(stack);
        entityData.set(TRAIL_COLOR, school == null ? DEFAULT_TRAIL_COLOR : MagicTools.resolveSchoolTintColor(school));
        this.slot = slot;
        entityData.set(MODE, mode.ordinal());
        entityData.set(MAELSTROM, mode != Mode.NORMAL && MalumSpellReaperScytheBridge.hasMaelstrom(owner));
        entityData.set(THROW_YAW, owner.getYRot());
        setYRot(owner.getYRot());
        yRotO = getYRot();
        setOwner(owner);
        physical = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        magic = MalumSpellReaperScytheBridge.throwMagicDamage(owner);
        if (isNarrow()) {
            // 両版のSpellReaperScytheで共通の倍率。各版のMalum装備属性は命中時に別途適用する。
            physical *= 1.3f;
            magic *= 1.3f;
        }
        var eye = owner.getEyePosition();
        var aim = clipDestination(eye, eye.add(owner.getLookAngle().scale(distance)));
        start = handPosition(owner);
        // 狙い先と溜め時間に対する距離は維持し、低い手元からの経路でも遮蔽物を確認する。
        destination = clipDestination(start, aim);
        travelTicks = Math.max(1, Math.min(20, (int) Math.ceil(eye.distanceTo(destination) * 2)));
        setPos(start);
    }

    private static Vec3 handPosition(Player owner) {
        // 通常姿勢は目線より0.5ブロック下。泳ぎ・匍匐姿勢では下げ幅を縮め、地面へ潜らせない。
        return owner.getEyePosition().add(0, -Math.min(0.5, owner.getEyeHeight() * 0.35), 0);
    }

    private Vec3 clipDestination(Vec3 from, Vec3 to) {
        var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) return to;
        var safe = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.5));
        // 壁に密着していても、補正位置が出発点の後方へ飛び出さないようにする。
        return safe.subtract(from).dot(to.subtract(from)) < 0 ? from : safe;
    }

    public float getPhysicalDamage() { return physical; }
    public float getMagicDamage() { return magic; }
    public ItemStack getWeaponSnapshot() { return weapon.copy(); }

    public boolean isHovering() { return entityData.get(HOVERING); }
    public boolean isMaelstrom() { return entityData.get(MAELSTROM); }
    public boolean isReturning() { return entityData.get(RETURNING); }
    public int getTrailColor() { return entityData.get(TRAIL_COLOR); }
    public Mode getMode() { return Mode.values()[entityData.get(MODE)]; }
    public boolean isRebound() { return getMode() != Mode.NORMAL; }
    public boolean isNarrow() { return getMode() == Mode.NARROW; }
    public float getThrowYaw() { return entityData.get(THROW_YAW); }

    public void checkOwner() {
        if (ending || isRemoved()) return;
        if (player == null || !player.isAlive() || player.isSpectator() || player.isRemoved() || player.level() != level()) {
            discard();
        } else if (player.getInventory().selected != slot || !getUUID().equals(ScytheThrowManager.token(player.getMainHandItem()))) {
            recall();
        } else if (distanceToSqr(player) >= Math.pow(isNarrow() ? 48 : isRebound() ? 32 : 24, 2)) {
            recall();
        }
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isMaelstrom() && !isNarrow()) MalumSpellReaperScytheBridge.tickMaelstrom(this);
            // 同期済みの停滞状態から各clientで一度だけ開始し、音の停止はEntityの寿命へ追従させる。
            if (isHovering() && !clientSpinStarted) {
                clientSpinStarted = true;
                ScytheSpinSound.play(this);
            }
            return;
        }
        if (!(level() instanceof ServerLevel)) return;
        checkOwner();
        if (isRemoved()) return;
        if (isReturning()) {
            var end = handPosition(player);
            var movement = end.subtract(position());
            var from = position();
            setPos(movement.lengthSqr() <= 1 ? end : from.add(movement.normalize()));
            sweep(from, position(), returnContacts, false, true);
            if (isMaelstrom()) MalumSpellReaperScytheBridge.tickMaelstrom(this);
            if (movement.lengthSqr() <= 1) {
                playCatchSound(end);
                discard();
            }
            return;
        }
        if (isMaelstrom() && !isNarrow()) MalumSpellReaperScytheBridge.tickMaelstrom(this);
        if (!isHovering()) {
            double t = Math.min(1, ++age / (double) travelTicks);
            var next = start.lerp(destination, 1 - (1 - t) * (1 - t));
            var clipped = clipDestination(position(), next);
            var blocked = clipped.distanceToSqr(next) > 1.0e-8;
            var from = position();
            setPos(clipped);
            sweep(from, clipped, outwardContacts, false, false);
            if (isRemoved()) return;
            if (blocked || age >= travelTicks) {
                if (isRebound()) recall();
                else entityData.set(HOVERING, true);
            }
        } else {
            int cost = ApprenticeCodexServerConfig.spellReaperScytheConfig().throwManaPerTick();
            if (!ScytheThrowManager.canPay(player, cost)) {
                recall();
                return;
            }
            ScytheThrowManager.pay(player, cost);
            if (++hoverTicks % 3 == 0) sweep(position(), position(), new HashSet<>(), true, false);
        }
    }

    public void recall() {
        if (ending || isRemoved() || !(level() instanceof ServerLevel)) return;
        if (isMaelstrom() && !isNarrow()) {
            entityData.set(RETURNING, true);
            return;
        }
        ending = true;
        try {
            if (player != null && player.isAlive() && player.level() == level()) {
                var end = handPosition(player);
                sweep(position(), end, new HashSet<>(), false, true);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(this,
                        new ScytheRecallEffectPacket(position(), end, getTrailColor(), isNarrow(), getThrowYaw()));
                level().playSound(null, end.x, end.y, end.z,
                        jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_SCYTHE_CATCH.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.65f, 1f);
            }
        } finally {
            discard();
        }
    }

    private void playCatchSound(Vec3 end) {
        level().playSound(null, end.x, end.y, end.z,
                jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_SCYTHE_CATCH.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.65f, 1f);
    }

    public static AABB attackBox(Vec3 position) {
        return new AABB(position.x - 1.5, position.y - 0.05, position.z - 1.5,
                position.x + 1.5, position.y + 0.05, position.z + 1.5);
    }

    public static boolean intersectsSweep(AABB target, Vec3 from, Vec3 to) {
        var expanded = target.inflate(1.5, 0.05, 1.5);
        return expanded.contains(from) || expanded.contains(to) || expanded.clip(from, to).isPresent();
    }

    private void sweep(Vec3 from, Vec3 to, Set<UUID> contacts, boolean continuous, boolean returning) {
        if (isNarrow()) {
            sweepNarrow(from, to, contacts, returning);
            return;
        }
        var level = (ServerLevel) level();
        // 広域AABBは候補抽出専用。斜め帰還時はMinkowski拡張した対象との線分交差で絞る。
        var candidates = level.getEntities(this, attackBox(from).minmax(attackBox(to)), e -> CombatTools.isValidCombatTarget(e, player));
        for (var raw : candidates) {
            if (!intersectsSweep(raw.getBoundingBox(), from, to)) continue;
            var target = CombatTools.resolutePartEntity(raw);
            if (contacts.contains(target.getUUID())) continue;
            if (!returning) {
                var expanded = raw.getBoundingBox().inflate(1.5, 0.05, 1.5);
                var origin = expanded.contains(from) ? from : expanded.clip(from, to).orElse(to);
                var box = raw.getBoundingBox();
                var closest = new Vec3(Math.clamp(origin.x, box.minX, box.maxX),
                        Math.clamp(origin.y, box.minY, box.maxY), Math.clamp(origin.z, box.minZ, box.maxZ));
                var hit = level.clip(new ClipContext(origin, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (hit.getType() != HitResult.Type.MISS) continue;
            }
            contacts.add(target.getUUID());
            if (net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this,
                    new net.minecraft.world.phys.EntityHitResult(raw, to))) continue;
            ScytheThrowDamage.hit(level, this, player, target, weapon, physical, magic, continuous);
        }
    }

    private void sweepNarrow(Vec3 from, Vec3 to, Set<UUID> contacts, boolean returning) {
        var level = (ServerLevel) level();
        var box = narrowBox(from, getThrowYaw());
        var movement = to.subtract(from);
        record Contact(net.minecraft.world.entity.Entity raw, double time) {}
        var hits = new ArrayList<Contact>();
        for (var raw : level.getEntities(this, RaycastTools.movingHorizontalBoxBounds(box, movement),
                e -> CombatTools.isValidCombatTarget(e, player))) {
            var time = RaycastTools.firstMovingHorizontalBoxContact(raw.getBoundingBox(), box, movement);
            if (time.isPresent()) hits.add(new Contact(raw, time.getAsDouble()));
        }
        hits.sort(Comparator.comparingDouble(Contact::time).thenComparingInt(hit -> hit.raw().getId()));
        for (var hit : hits) {
            var raw = hit.raw();
            var target = CombatTools.resolutePartEntity(raw);
            if (contacts.contains(target.getUUID())) continue;
            var origin = from.lerp(to, hit.time());
            if (!returning) {
                var bounds = raw.getBoundingBox();
                var closest = new Vec3(Math.clamp(origin.x, bounds.minX, bounds.maxX),
                        Math.clamp(origin.y, bounds.minY, bounds.maxY), Math.clamp(origin.z, bounds.minZ, bounds.maxZ));
                if (level.clip(new ClipContext(origin, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                        .getType() != HitResult.Type.MISS) continue;
            }
            contacts.add(target.getUUID());
            if (net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this,
                    new net.minecraft.world.phys.EntityHitResult(raw, origin))) continue;
            // 同tick内の候補全員へ当てず、最初の接触地点から帰還する。無敵時間は帰還条件を変えない。
            if (!returning) setPos(origin);
            if (!returning && isMaelstrom()) MalumSpellReaperScytheBridge.placeMaelstrom(this, target);
            ScytheThrowDamage.hit(level, this, player, target, weapon, physical, magic, false);
            if (!returning) {
                recall();
                return;
            }
        }
    }

    public static RaycastTools.HorizontalOrientedBox narrowBox(Vec3 center, float yaw) {
        double radians = Math.toRadians(yaw);
        var forward = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        return new RaycastTools.HorizontalOrientedBox(center.subtract(forward.scale(1.5)), forward, 0.05, 1.5, 3);
    }

    @Override public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide) ScytheThrowManager.forget(this, player, original);
        super.remove(reason);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL_COLOR, DEFAULT_TRAIL_COLOR);
        builder.define(HOVERING, false);
        builder.define(MODE, Mode.NORMAL.ordinal());
        builder.define(THROW_YAW, 0f);
        builder.define(MAELSTROM, false);
        builder.define(RETURNING, false);
    }
    // セッション限定。保存済みEntityが復元されてもowner無しとして即時破棄する。
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public @NotNull AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(3); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "throw", 0, state -> state.setAndContinue(isNarrow() ? THROW_VERTICAL : THROW)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
