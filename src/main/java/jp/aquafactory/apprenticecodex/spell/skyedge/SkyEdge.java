package jp.aquafactory.apprenticecodex.spell.skyedge;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class SkyEdge extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sky_edge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public SkyEdge() {
        baseSpellPower = 500;
        spellPowerPerLevel = 0;
        manaCostPerLevel = 15;
        baseManaCost = 80;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(ApprenticeCodexServerConfig.DamageMultiplierKey.SKY_EDGE);
    }

    private int getProjectileCount(int spellLevel, LivingEntity entity){
        // 基本はレベル+3本飛ぶ.
        var baseCount = spellLevel + 3;
        var spellPower = getSpellPower(spellLevel, entity);

        // レベル4以上は一定パワーで+1、レベル5は更に一定パワーで+2まで許容.
        if (spellLevel >= (getMaxLevel() -1 ) && spellPower >= 700) {
            baseCount += 1;
        }
        if (spellLevel >= getMaxLevel() && spellPower >= 900) {
            baseCount += 1;
        }

        return baseCount;
    }

    private double getRange(){
        return 64;
    }

    private double getInaccuracy(){
        return 0.75;
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
        return CastType.LONG;
    }
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(getSchoolType().getCastSound());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for(var count = 0; count < getProjectileCount(spellLevel, entity); ++count) {
            var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, entity);
            var dimensions = entity.getDimensions(entity.getPose());
            var spawnPosition = pickSpawnPosition(level, entity, projectile, dimensions, level.random);

            // 視線先の対象を狙うようにする.
            var result = RaycastTools.raycastFromEye(entity, getRange(), 1, e -> CombatTools.isValidCombatTarget(e, entity));
            var targetPosition = result.hitPosition().add(generateInaccuracy(level.random).scale(getInaccuracy()));
            var velocity = targetPosition.subtract(spawnPosition).normalize();
            var delay = Math.round(level.random.nextFloat() * 5) + 10;
            var speed = Mth.lerp(level.random.nextDouble(), 2.4f, 2.5f);
            projectile.setDamage(getDamage(spellLevel, entity));
            projectile.setPos(spawnPosition);
            projectile.setProjectileVelocity(velocity, speed);
            projectile.setStandbyTicks(delay);
            level.addFreshEntity(projectile);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Vec3 generateInaccuracy(RandomSource rand) {
        return new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5);
    }

    private static Vec3 pickSpawnPosition(Level level, LivingEntity caster, Entity collisionContext,
                                          EntityDimensions projDims, RandomSource rand) {

        var reference = caster.getEyePosition();
        for (int i = 0; i < 8; i++) {
            var candidate = generateCandidate(caster, rand);

            var aabb = makeAabbAt(projDims, candidate);
            if (isSafeSpawn(level, collisionContext, aabb)) {
                return candidate;
            }

            var pushed = tryPushToSafety(level, collisionContext, projDims, candidate, reference);
            if (pushed != null) {
                return pushed;
            }
        }

        // 失敗時は視線位置にする.
        return reference;
    }

    private static Vec3 generateCandidate(LivingEntity caster, RandomSource rand) {
        // 極座標で出す(角度は240度範囲内、始点下方向)
        var backDist = Mth.lerp(rand.nextDouble(), 0.5, 1.0);
        var angle = Math.PI * (1.0 + Mth.lerp(rand.nextDouble(), (60.0 / 180.0), (300.0 / 180.0)));
        var radius = Mth.lerp(rand.nextDouble(), 1.25, 2.0);
        var up = Math.cos(angle) * radius;
        var side = Math.sin(angle) * radius;

        return RotationTools.calculateBehindPosition(caster, backDist, side, up);
    }

    private static Vec3 tryPushToSafety(Level level, Entity ctx, EntityDimensions dims,
                                        Vec3 candidate, Vec3 reference) {

        var dir = reference.subtract(candidate);
        if (dir.lengthSqr() < 1.0e-8) return null;
        dir = dir.normalize();

        var maxPush = 0.6;
        var step = 0.1;

        for (double t = 0.0; t <= maxPush + 1.0e-9; t += step) {
            Vec3 p = candidate.add(dir.scale(t));
            AABB aabb = makeAabbAt(dims, p);
            if (isSafeSpawn(level, ctx, aabb)) {
                return p;
            }
        }
        return null;
    }

    private static boolean isSafeSpawn(Level level, Entity forCollisionContext, AABB aabb) {
        var test = aabb.inflate(0.05);
        return level.noCollision(forCollisionContext, test);
    }

    private static AABB makeAabbAt(EntityDimensions dims, Vec3 center) {
        var w = dims.width;
        var h = dims.height;

        var hw = w / 2.0;
        return new AABB(
                center.x - hw, center.y, center.z - hw,
                center.x + hw, center.y + h, center.z + hw
        );
    }
}
