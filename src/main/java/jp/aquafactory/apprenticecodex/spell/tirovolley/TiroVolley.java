package jp.aquafactory.apprenticecodex.spell.tirovolley;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TiroVolley extends AbstractSpell implements ICastHighlightSpell {
    private static final int BASE_FIRE_DELAY_TICKS = 20;
    private static final int MAX_SPAWNS_PER_CAST_CALL = 32;
    private static final double LOOK_TARGET_CHANCE = 2.0 / 3.0;
    private static final double LOOK_TARGET_RANGE = 32.0;
    private static final double NEAREST_TARGET_RANGE = 12.0;
    private static final double RANDOM_TARGET_RANGE = 32.0;
    private static final double RANDOM_TARGET_HALF_WIDTH = 8.0;
    private static final double RANDOM_TARGET_HALF_HEIGHT = 5.0;
    private static final double MIN_SPAWN_DISTANCE_FROM_CASTER = 1.5;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "tiro_volley");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public TiroVolley() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 13;
        manaCostPerLevel = 3;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getDamage(getSpellPower(spellLevel, entity));
    }

    public static float getDamage(float spellPower) {
        // 威力は単発より召喚速度で制御する.
        var rawDamage = 4 + spellPower / 1000.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.TIRO_VOLLEY);
    }

    private float getMusketsPerSecond(int spellLevel, LivingEntity entity) {
        var spellPower = getSpellPower(spellLevel, entity);
        return Mth.clamp(2.0f + (spellPower - 100.0f) * (6.0f / 400.0f), 1.0f, 10.0f);
    }

    @Override
    public int getHighlightColor() {
        return 0xffff00;
    }

    @Override
    @Nullable
    public Entity getHighlightEntity(@NotNull Player player, int skillLevel) {
        return findLookTarget(player).orElse(null);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_NETHERITE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new TiroVolleyCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            var castData = playerMagicData.getAdditionalCastData() instanceof TiroVolleyCastData data
                    ? data
                    : new TiroVolleyCastData();

            if (!castData.isInitialized()) {
                castData.initialize(serverLevel.getGameTime());
                playerMagicData.setAdditionalCastData(castData);
            }

            spawnDueMuskets(serverLevel, spellLevel, entity, castData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData.getAdditionalCastData() instanceof TiroVolleyCastData) {
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void spawnDueMuskets(ServerLevel level, int spellLevel, LivingEntity caster, TiroVolleyCastData castData) {
        var elapsedTick = Math.max(0L, level.getGameTime() - castData.startGameTick);
        var ticksPerMusket = 20.0 / getMusketsPerSecond(spellLevel, caster);
        var spawned = 0;

        // 20TPSでは小数tick間隔を直接表現できないため、理想時刻を整数tickへ丸めて平均間隔を守る。
        while (spawned < MAX_SPAWNS_PER_CAST_CALL) {
            var scheduledTick = calculateScheduledTick(castData.nextSpawnIndex, ticksPerMusket);
            if (scheduledTick > elapsedTick) {
                break;
            }

            var fireDelay = Math.max(1, (int) (BASE_FIRE_DELAY_TICKS + scheduledTick - elapsedTick));
            spawnMusket(level, spellLevel, caster, fireDelay);
            castData.nextSpawnIndex += 1;
            spawned += 1;
        }
    }

    private static long calculateScheduledTick(int index, double ticksPerMusket) {
        return Math.round(index * ticksPerMusket);
    }

    private void spawnMusket(ServerLevel level, int spellLevel, LivingEntity caster, int fireDelay) {
        var musket = new TiroVolleyMusketEntity(EntityRegistry.TIRO_VOLLEY_MUSKET.get(), level, caster);
        var spawnPosition = pickSpawnPosition(level, caster, musket, musket.getDimensions(musket.getPose()), level.random);
        var target = findInitialTarget(level, caster);

        musket.setPos(spawnPosition);
        musket.setup(getDamage(spellLevel, caster), spellLevel, fireDelay, target);
        level.addFreshEntity(musket);
    }

    private static Entity findInitialTarget(ServerLevel level, LivingEntity caster) {
        if (level.random.nextDouble() < LOOK_TARGET_CHANCE) {
            var target = findLookTarget(caster);
            if (target.isPresent()) {
                return target.get();
            }
        }

        return findFallbackTarget(level, caster).orElse(null);
    }

    private static Optional<Entity> findLookTarget(LivingEntity caster) {
        var result = SummonedFirearmTools.resolveAssistedAim(caster, LOOK_TARGET_RANGE,
                e -> CombatTools.isValidCombatTarget(e, caster));
        if (result.hitEntity() == null) {
            return Optional.empty();
        }

        var target = CombatTools.resolutePartEntity(result.hitEntity());
        return CombatTools.isValidCombatTarget(target, caster) ? Optional.of(target) : Optional.empty();
    }

    public static Optional<Entity> findFallbackTarget(Level level, LivingEntity caster) {
        var nearestTarget = findNearestNearbyTarget(level, caster);
        if (nearestTarget.isPresent()) {
            return nearestTarget;
        }

        return findRandomForwardTarget(level, caster);
    }

    private static Optional<Entity> findNearestNearbyTarget(Level level, LivingEntity caster) {
        var searchBox = caster.getBoundingBox().inflate(NEAREST_TARGET_RANGE);
        return level.getEntities(caster, searchBox,
                        e -> e != caster && e.isAlive() && CombatTools.isValidCombatTarget(e, caster))
                .stream()
                .map(CombatTools::resolutePartEntity)
                .filter(target -> CombatTools.isValidCombatTarget(target, caster))
                .filter(target -> target.distanceToSqr(caster) <= NEAREST_TARGET_RANGE * NEAREST_TARGET_RANGE)
                .filter(target -> RaycastTools.hasLineOfSight(level, caster, target))
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(caster)));
    }

    public static Optional<Entity> findRandomForwardTarget(Level level, LivingEntity caster) {
        var forward = caster.getViewVector(1.0F);
        if (forward.lengthSqr() < 1.0e-6) {
            forward = RotationTools.getFlatForward(caster);
        } else {
            forward = forward.normalize();
        }

        var worldUp = new Vec3(0, 1, 0);
        var right = worldUp.cross(forward);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        var up = forward.cross(right).normalize();

        var origin = caster.getEyePosition();
        var searchBox = caster.getBoundingBox().inflate(RANDOM_TARGET_RANGE + RANDOM_TARGET_HALF_WIDTH);
        var candidates = new ArrayList<Entity>();

        for (var candidate : level.getEntities(caster, searchBox,
                e -> e != caster && e.isAlive() && CombatTools.isValidCombatTarget(e, caster))) {
            var resolved = CombatTools.resolutePartEntity(candidate);
            if (!CombatTools.isValidCombatTarget(resolved, caster)) {
                continue;
            }

            var toTarget = RaycastTools.getEntityTargetPosition(resolved).subtract(origin);
            var x = toTarget.dot(right);
            var y = toTarget.dot(up);
            var z = toTarget.dot(forward);

            if (z < 0.0 || z > RANDOM_TARGET_RANGE) {
                continue;
            }
            if (Math.abs(x) > RANDOM_TARGET_HALF_WIDTH || Math.abs(y) > RANDOM_TARGET_HALF_HEIGHT) {
                continue;
            }
            if (!RaycastTools.hasLineOfSight(level, caster, resolved)) {
                continue;
            }

            candidates.add(resolved);
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(level.random.nextInt(candidates.size())));
    }

    private static Vec3 pickSpawnPosition(Level level, LivingEntity caster, Entity collisionContext,
                                          EntityDimensions dims, RandomSource rand) {
        var reference = caster.getEyePosition();
        for (int i = 0; i < 8; i++) {
            var candidate = generateCandidate(caster, rand);
            var aabb = makeAabbAt(dims, candidate);
            if (isSafeSpawn(level, collisionContext, aabb)) {
                return candidate;
            }

            var pushed = tryPushToSafety(level, collisionContext, dims, candidate, reference);
            if (pushed != null) {
                return pushed;
            }
        }

        return reference;
    }

    private static Vec3 generateCandidate(LivingEntity caster, RandomSource rand) {
        for (int i = 0; i < 8; i++) {
            var angle = rand.nextDouble() * Math.PI * 2.0;
            var radius = Math.sqrt(rand.nextDouble()) * 4.0;
            var side = Math.cos(angle) * radius;
            var up = Math.sin(angle) * radius;
            var front = Mth.lerp(rand.nextDouble(), 1.6, 3.6);
            var candidate = RotationTools.calculateBehindPosition(caster, -front, side, up);
            if (candidate.distanceToSqr(caster.getEyePosition()) >= MIN_SPAWN_DISTANCE_FROM_CASTER * MIN_SPAWN_DISTANCE_FROM_CASTER) {
                return candidate;
            }
        }

        var fallbackForward = caster.getViewVector(1.0F);
        if (fallbackForward.lengthSqr() < 1.0e-6) {
            fallbackForward = RotationTools.getFlatForward(caster);
        } else {
            fallbackForward = fallbackForward.normalize();
        }
        return caster.getEyePosition().add(fallbackForward.scale(MIN_SPAWN_DISTANCE_FROM_CASTER + 0.1));
    }

    private static Vec3 tryPushToSafety(Level level, Entity ctx, EntityDimensions dims,
                                        Vec3 candidate, Vec3 reference) {
        var dir = reference.subtract(candidate);
        if (dir.lengthSqr() < 1.0e-8) {
            return null;
        }
        dir = dir.normalize();

        for (double t = 0.0; t <= 0.8 + 1.0e-9; t += 0.1) {
            var pushed = candidate.add(dir.scale(t));
            if (isSafeSpawn(level, ctx, makeAabbAt(dims, pushed))) {
                return pushed;
            }
        }
        return null;
    }

    private static boolean isSafeSpawn(Level level, Entity forCollisionContext, AABB aabb) {
        return level.noCollision(forCollisionContext, aabb.inflate(0.05));
    }

    private static AABB makeAabbAt(EntityDimensions dims, Vec3 center) {
        var hw = dims.width / 2.0;
        return new AABB(
                center.x - hw, center.y, center.z - hw,
                center.x + hw, center.y + dims.height, center.z + hw
        );
    }

    public static class TiroVolleyCastData implements ICastDataSerializable {
        private long startGameTick;
        private int nextSpawnIndex;
        private boolean initialized;

        private boolean isInitialized() {
            return initialized;
        }

        private void initialize(long startGameTick) {
            this.startGameTick = startGameTick;
            this.nextSpawnIndex = 0;
            this.initialized = true;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeLong(startGameTick);
            buffer.writeInt(nextSpawnIndex);
            buffer.writeBoolean(initialized);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            startGameTick = buffer.readLong();
            nextSpawnIndex = buffer.readInt();
            initialized = buffer.readBoolean();
        }

        @Override
        public void reset() {
            startGameTick = 0L;
            nextSpawnIndex = 0;
            initialized = false;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putLong("StartGameTick", startGameTick);
            tag.putInt("NextSpawnIndex", nextSpawnIndex);
            tag.putBoolean("Initialized", initialized);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            startGameTick = nbt.getLong("StartGameTick");
            nextSpawnIndex = nbt.getInt("NextSpawnIndex");
            initialized = nbt.getBoolean("Initialized");
        }
    }
}
