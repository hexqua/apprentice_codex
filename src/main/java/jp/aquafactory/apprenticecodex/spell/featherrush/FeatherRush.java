package jp.aquafactory.apprenticecodex.spell.featherrush;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class FeatherRush extends AbstractSummonWeaponSpell<FeatherRushWingEntity> {
    private static final double FIRE_SIDE_OFFSET = 0.5;
    private static final float BACKWARD_YAW_BIAS_DEG = 22.0f;
    private static final float BACKWARD_YAW_RANDOM_DEG = 28.0f;
    private static final float BACKWARD_PITCH_RANDOM_DEG = 6.0f;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "feather_rush");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .build();

    public FeatherRush() {
        super(FeatherRushWingEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 75;
        baseManaCost = 4;
        manaCostPerLevel = 2;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.feather_rpm", Utils.stringTruncation(getRpm(spellLevel), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return 3 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getFireIntervalTicks(int spellLevel){
        return Math.max(2, 2 + (getMaxLevel() - spellLevel) / 2);
    }

    private int getRpm(int spellLevel) {
        // 20tick * 60秒.
        return 1200 / getFireIntervalTicks(spellLevel);
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
        return Optional.of(SoundRegistry.FLAPPED.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public FeatherRushWingEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWing = new FeatherRushWingEntity(EntityRegistry.FEATHER_RUSH_WING.get(), level, entity);
        level.addFreshEntity(summonWing);
        return summonWing;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull FeatherRushWingEntity weapon) {
        if (weapon.tickCount % getFireIntervalTicks(spellLevel) != 0) {
            return;
        }

        var fromRightWing = ((weapon.tickCount / getFireIntervalTicks(spellLevel)) & 1) == 0;
        var projectile = new FeatherRushProjectileEntity(EntityRegistry.FEATHER_RUSH_PROJECTILE.get(), level, entity);
        projectile.setPos(getProjectileSpawnPosition(level, weapon, fromRightWing));
        projectile.setDamage(getDamage(spellLevel, entity));

        var shootAngle = RaycastTools.randomRotateInCone(entity.getLookAngle().normalize(), 15f, level.random);
        projectile.setStraightFlightDirections(
                getBackwardDirection(level, entity, fromRightWing),
                shootAngle,
                level.random.nextInt(1, 4)
        );
        level.addFreshEntity(projectile);
        AudioTools.playSoundFromEntity(level, entity, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull FeatherRushWingEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }

    private Vec3 getProjectileSpawnPosition(Level level, FeatherRushWingEntity wing, boolean fromRightWing) {
        var random = level.random;
        var side = (fromRightWing ? FIRE_SIDE_OFFSET : -FIRE_SIDE_OFFSET) + (random.nextDouble() - 0.5) * 0.125;
        return RotationTools.calculateBehindPosition(wing, 0.0, side, -0.5 + (random.nextDouble() - 0.5) * 0.125);
    }

    private Vec3 getBackwardDirection(Level level, LivingEntity caster, boolean fromRightWing) {
        var backwardFlat = RotationTools.getFlatForward(caster).scale(-1);
        var yawBias = fromRightWing ? -BACKWARD_YAW_BIAS_DEG : BACKWARD_YAW_BIAS_DEG;
        var randomYaw = level.random.nextFloat() * BACKWARD_YAW_RANDOM_DEG - BACKWARD_YAW_RANDOM_DEG / 2;
        var randomPitch = level.random.nextFloat() * BACKWARD_PITCH_RANDOM_DEG - BACKWARD_PITCH_RANDOM_DEG / 2;
        var yawRotated = rotateYaw(backwardFlat, yawBias + randomYaw);
        return rotatePitch(yawRotated, randomPitch).normalize();
    }

    private Vec3 rotateYaw(Vec3 direction, float degree) {
        var rad = degree * Mth.DEG_TO_RAD;
        var cos = Mth.cos(rad);
        var sin = Mth.sin(rad);
        var x = direction.x * cos - direction.z * sin;
        var z = direction.x * sin + direction.z * cos;
        return new Vec3(x, direction.y, z);
    }

    private Vec3 rotatePitch(Vec3 direction, float degree) {
        var axis = new Vec3(direction.z, 0.0, -direction.x).normalize();
        if (axis.lengthSqr() <= 1.0e-6) {
            return direction;
        }

        var rad = degree * Mth.DEG_TO_RAD;
        var cos = Math.cos(rad);
        var sin = Math.sin(rad);
        var term1 = direction.scale(cos);
        var term2 = axis.cross(direction).scale(sin);
        var term3 = axis.scale(axis.dot(direction) * (1.0 - cos));
        return term1.add(term2).add(term3);
    }
}
