package jp.aquafactory.apprenticecodex.spell.grindrunner;

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
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class GrindRunner extends AbstractSummonWeaponSpell<GrindRunnerWheelEntity> {
    private static final double SUMMON_RANGE = 8.0;
    private static final double SUMMON_ENTITY_HITBOX_WIDTH = 0.6;
    private static final double SUMMON_BLOCK_SIDE_OFFSET = 0.6;
    private static final double GROUND_SEARCH_DISTANCE = 4.0;
    private static final double SUMMON_GROUND_Y_EPSILON = 1.0E-3;
    private static final double SUMMON_DROP_HEIGHT = 4.0;
    private static final int SUMMON_DROP_DURATION_TICK = 5;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public GrindRunner() {
        super(GrindRunnerWheelEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 20;
        manaCostPerLevel = 5;
        castTime = 100;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.process_item_per_second", Utils.stringTruncation(getGrindItemPerSecond(spellLevel, caster), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 2f * getSpellPower(spellLevel, entity) /100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.GRIND_RUNNER);
    }

    private float getGrindItemPerSecond(int spellLevel, LivingEntity entity) {
        return 8 * getSpellPower(spellLevel, entity) / 100.0f;
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
        // todo:音を後で付ける.
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // todo:音を後で付ける.
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (findSummonGroundPosition(level, entity).isEmpty()) {
            sendCantSpawnInAirMessage(entity);
            return false;
        }
        return true;
    }

    @Override
    public GrindRunnerWheelEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonPos = findSummonGroundPosition(level, entity);
        if (summonPos.isEmpty()) {
            sendCantSpawnInAirMessage(entity);
        }

        var summonWeapon = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), level, entity);
        summonWeapon.setSummonSettings(summonPos.orElse(entity.position()), SUMMON_DROP_HEIGHT, SUMMON_DROP_DURATION_TICK);
        summonWeapon.setLaunchSettings(getLaunchSpeed(spellLevel, entity), getLaunchSlowdownStartTick(spellLevel, entity), getLaunchSlowdownFactor(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull GrindRunnerWheelEntity weapon) {
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull GrindRunnerWheelEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }

    private double getLaunchSpeed(int spellLevel, LivingEntity entity) {
        return 1.15 + 0.15 * getSpellPower(spellLevel, entity) / 100.0;
    }

    private int getLaunchSlowdownStartTick(int spellLevel, LivingEntity entity) {
        return Math.max(8, Math.round(18f - 5f * getSpellPower(spellLevel, entity) / 100.0f));
    }

    private double getLaunchSlowdownFactor(int spellLevel, LivingEntity entity) {
        return 0.90;
    }

    private Optional<Vec3> findSummonGroundPosition(Level level, LivingEntity caster) {
        var basePosition = resolveSummonBasePosition(level, caster);
        var downHit = level.clip(new ClipContext(
                basePosition,
                basePosition.add(0, -GROUND_SEARCH_DISTANCE, 0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        if (downHit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        var hitBlockPos = downHit.getBlockPos();
        var collisionShape = level.getBlockState(hitBlockPos).getCollisionShape(level, hitBlockPos);
        if (collisionShape.isEmpty()) {
            return Optional.empty();
        }

        // 境界値でわずかに沈むケースを避けるため、衝突形状の上面 Y を下限として使う.
        var collisionTopY = hitBlockPos.getY() + collisionShape.max(Direction.Axis.Y);
        var groundY = Math.max(downHit.getLocation().y, collisionTopY) + SUMMON_GROUND_Y_EPSILON;
        return Optional.of(new Vec3(basePosition.x, groundY, basePosition.z));
    }

    private Vec3 resolveSummonBasePosition(Level level, LivingEntity caster) {
        var look = caster.getViewVector(1.0F);
        if (look.lengthSqr() < 1.0E-6) {
            look = new Vec3(0, 0, 1);
        }
        look = look.normalize();

        var target = RaycastTools.raycast(caster, look, SUMMON_RANGE, SUMMON_RANGE, SUMMON_ENTITY_HITBOX_WIDTH, e -> e != caster);
        if (target.hitEntity() != null) {
            // モブ対象時は足元座標ではなく命中点を基準にして接地計算の安定性を上げる.
            return target.hitPosition();
        }

        if (target.hitType() == RaycastTools.TargetType.BLOCK && target.hitBlock() != null) {
            var blockCenter = Vec3.atCenterOf(target.hitBlock());
            var sideNormal = resolveBlockSideNormal(blockCenter, target.hitPosition(), look);
            return blockCenter.add(sideNormal.scale(SUMMON_BLOCK_SIDE_OFFSET));
        }

        return caster.getEyePosition(1.0F).add(look.scale(SUMMON_RANGE));
    }

    private static Vec3 resolveBlockSideNormal(Vec3 blockCenter, Vec3 hitPos, Vec3 fallbackLook) {
        var offset = hitPos.subtract(blockCenter);
        if (offset.lengthSqr() < 1.0E-6) {
            return axisAlignedNormal(fallbackLook.scale(-1));
        }
        return axisAlignedNormal(offset);
    }

    private static Vec3 axisAlignedNormal(Vec3 vec) {
        var absX = Math.abs(vec.x);
        var absY = Math.abs(vec.y);
        var absZ = Math.abs(vec.z);

        if (absX >= absY && absX >= absZ) {
            return new Vec3(signOrOne(vec.x), 0, 0);
        }
        if (absY >= absX && absY >= absZ) {
            return new Vec3(0, signOrOne(vec.y), 0);
        }
        return new Vec3(0, 0, signOrOne(vec.z));
    }

    private static double signOrOne(double value) {
        return value < 0.0 ? -1.0 : 1.0;
    }

    private void sendCantSpawnInAirMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_spawn_in_air").withStyle(ChatFormatting.RED)
            ));
        }
    }
}
