package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.packet.ScytheRecallEffectPacket;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
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
import java.util.Set;
import java.util.UUID;

public final class ScytheThrowEntity extends Projectile implements GeoEntity {
    public static final int DEFAULT_TRAIL_COLOR = 0xB4DCFF;
    private static final EntityDataAccessor<Integer> TRAIL_COLOR = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOVERING = SynchedEntityData.defineId(ScytheThrowEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation THROW = RawAnimation.begin().thenLoop("throw_normal");
    private final Set<UUID> outwardContacts = new HashSet<>();
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
    private float physical;
    private float magic;

    public ScytheThrowEntity(EntityType<? extends ScytheThrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void prepare(Player owner, ItemStack stack, int slot, double distance) {
        this.player = owner;
        this.original = stack;
        this.weapon = stack.copy();
        var school = MagicTools.getImbuedSpellSchool(stack);
        entityData.set(TRAIL_COLOR, school == null ? DEFAULT_TRAIL_COLOR : MagicTools.resolveSchoolTintColor(school));
        this.slot = slot;
        setOwner(owner);
        physical = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        magic = MalumSpellReaperScytheBridge.throwMagicDamage(owner);
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

    public boolean isHovering() { return entityData.get(HOVERING); }
    public int getTrailColor() { return entityData.get(TRAIL_COLOR); }

    public void checkOwner() {
        if (ending || isRemoved()) return;
        if (player == null || !player.isAlive() || player.isSpectator() || player.isRemoved() || player.level() != level()) {
            discard();
        } else if (player.getInventory().selected != slot || !getUUID().equals(ScytheThrowManager.token(player.getMainHandItem()))) {
            recall();
        } else if (distanceToSqr(player) >= 24 * 24) {
            recall();
        }
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) return;
        checkOwner();
        if (isRemoved()) return;
        if (!isHovering()) {
            double t = Math.min(1, ++age / (double) travelTicks);
            var next = start.lerp(destination, 1 - (1 - t) * (1 - t));
            var clipped = clipDestination(position(), next);
            var blocked = clipped.distanceToSqr(next) > 1.0e-8;
            var from = position();
            setPos(clipped);
            sweep(from, clipped, outwardContacts, false, false);
            if (blocked || age >= travelTicks) entityData.set(HOVERING, true);
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
        ending = true;
        try {
            if (player != null && player.isAlive() && player.level() == level()) {
                var end = handPosition(player);
                sweep(position(), end, new HashSet<>(), false, true);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, new ScytheRecallEffectPacket(position(), end, getTrailColor()));
            }
        } finally {
            discard();
        }
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

    @Override public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide) ScytheThrowManager.forget(this, player, original);
        super.remove(reason);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL_COLOR, DEFAULT_TRAIL_COLOR);
        builder.define(HOVERING, false);
    }
    // セッション限定。保存済みEntityが復元されてもowner無しとして即時破棄する。
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public @NotNull AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(3); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "throw", 0, state -> state.setAndContinue(THROW)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
