package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class SkyEdge extends AbstractSpell {
    @SuppressWarnings("removal") private final ResourceLocation spellId = new ResourceLocation(ApprenticeCodex.MODID, "sky_edge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(10)
            .build();

    public SkyEdge() {
        // スペルパワー100 = 1ダメージ.
        baseSpellPower = 500;
        spellPowerPerLevel = 0;
        manaCostPerLevel = 20;
        baseManaCost = 100;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount(spellLevel, caster)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getProjectileCount(int spellLevel, LivingEntity entity){
        // 基本はレベル+3本飛ぶ.
        var baseCount = spellLevel + 3;
        var spellPower = getSpellPower(spellLevel, entity);

        // レベル4以上は一定パワーで+1、レベル5は更に一定パワーで+2まで許容.
        if (spellLevel >= 4 && spellPower >= 700) {
            baseCount += 1;
        }
        if (spellLevel >= 5 && spellPower >= 900) {
            baseCount += 1;
        }

        return baseCount;
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
        // todo:効果音選定.
        return super.getCastStartSound();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // todo:効果音選定.
        return super.getCastFinishSound();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CHARGED_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            var item = new ItemStack(Items.GOLDEN_SWORD);

            for(var count = 0; count < getProjectileCount(spellLevel, entity); ++count){
                var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, entity, item);
                var dimensions = entity.getDimensions(entity.getPose());
                var spawnPosition = pickSpawnPosition(level, entity, projectile, dimensions, level.random);

                // 視線先の対象を狙うようにする.
                var result = RaycastTools.raycastFromEye(entity, 48);
                var velocity = result.hitPosition().subtract(spawnPosition).normalize();
                var delay = Math.round(level.random.nextFloat() * 5) + 20;
                var speed = lerp(2.4f, 2.5f, level.random.nextDouble());
                projectile.setDamage(getDamage(spellLevel, entity));
                projectile.setPos(spawnPosition);
                projectile.setProjectileVelocity(velocity, speed);
                projectile.setStandbyTicks(delay);
                level.addFreshEntity(projectile);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
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
        // 自分の少し後ろ上空に出す(身体向き基準)
        var yaw = caster.getYRot();
        var origin = caster.getEyePosition();

        var forward = Vec3.directionFromRotation(0.0f, yaw).normalize();
        var back = forward.scale(-1.0);
        var right = new Vec3(back.z, 0, -back.x).normalize();

        // 極座標で出す(角度は240度範囲内、始点下方向)
        var backDist = lerp(0.5, 1.0, rand.nextDouble());
        var angle = Math.PI * (1.0 + lerp((60.0 / 180.0), (300.0 / 180.0), rand.nextDouble()));
        var radius = lerp(1.25, 2.0, rand.nextDouble());

        var up = Math.cos(angle) * radius;
        var side = Math.sin(angle) * radius;
        return origin.add(back.scale(backDist)).add(0, up, 0).add(right.scale(side));
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
