package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HeavenlyFist extends AbstractSpell implements ICraftsmansDelightAffectedSpell {
    private static final double TARGET_RANGE = 32.0D;
    private static final double TARGET_RAYCAST_WIDTH = 0.6D;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "heavenly_fist");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public HeavenlyFist() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 80;
        manaCostPerLevel = 20;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        var lines = new ArrayList<MutableComponent>();
        lines.add(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
        lines.add(Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 2)));
        if (HeavenlyFistPressingProcessor.canProcessItems()) {
            lines.add(Component.translatable("ui.apprenticecodex.process_capacity", getPressProcessCapacity(spellLevel, caster)));
        }
        return lines;
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 4 + 6 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.HEAVENLY_FIST);
    }

    private float getRadius(int spellLevel, LivingEntity entity) {
        return 2.5f + 2.0f * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getPressProcessCapacity(int spellLevel, LivingEntity entity) {
        var baseProcessSpeed = 8 * getSpellPower(spellLevel, entity) / 100.0f;
        if (!isCraftsmansDelightProcessSpeedBonusEnabled()) {
            return Mth.floor(baseProcessSpeed);
        }

        return Mth.floor(CraftsmansDelight.applyProcessSpeedBonus(baseProcessSpeed, entity));
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) {
            return true;
        }

        var result = resolveTarget(level, entity).isPresent();
        if (!result) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.cast_error_target", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
        }

        return result;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            resolveTarget(level, entity).ifPresent(target -> {
                var fist = new HeavenlyFistFistEntity(
                        EntityRegistry.HEAVENLY_FIST_FIST.get(),
                        level,
                        entity,
                        target.center(),
                        getDamage(spellLevel, entity),
                        getRadius(spellLevel, entity),
                        getPressProcessCapacity(spellLevel, entity)
                );
                fist.setYRot(entity.getYRot());
                fist.setXRot(entity.getXRot());
                level.addFreshEntity(fist);
            });
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<LockedTarget> resolveTarget(Level level, LivingEntity caster) {
        var look = caster.getViewVector(1.0F);
        if (look.lengthSqr() < 1.0E-6) {
            return Optional.empty();
        }
        look = look.normalize();

        var eye = caster.getEyePosition(1.0F);
        var maxEnd = eye.add(look.scale(TARGET_RANGE));
        var blockHit = level.clip(new ClipContext(
                eye,
                maxEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        var effectiveEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxEnd;

        var combatHit = findEntityHit(level, caster, eye, effectiveEnd, look, entity -> CombatTools.isValidCombatTarget(entity, caster));
        if (combatHit != null) {
            return Optional.of(new LockedTarget(CombatTools.resolutePartEntity(combatHit.getEntity()).getBoundingBox().getCenter()));
        }

        if (HeavenlyFistPressingProcessor.canProcessItems()) {
            var itemHit = findEntityHit(level, caster, eye, effectiveEnd, look, entity -> entity instanceof ItemEntity);
            if (itemHit != null) {
                return Optional.of(new LockedTarget(itemHit.getEntity().position()));
            }
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return Optional.of(new LockedTarget(resolveAdjacentBlockCenter(blockHit, look)));
        }

        return Optional.empty();
    }

    private static EntityHitResult findEntityHit(Level level, LivingEntity caster, Vec3 start, Vec3 end, Vec3 look,
                                                 Predicate<Entity> predicate) {
        var distance = start.distanceTo(end);
        if (distance <= 1.0E-6) {
            return null;
        }

        var searchBox = caster.getBoundingBox()
                .expandTowards(look.scale(distance))
                .inflate(TARGET_RAYCAST_WIDTH / 2.0D);
        return ProjectileUtil.getEntityHitResult(
                level,
                caster,
                start,
                end,
                searchBox,
                entity -> entity != caster && entity.isAlive() && predicate.test(entity)
        );
    }

    private static Vec3 resolveAdjacentBlockCenter(BlockHitResult blockHit, Vec3 look) {
        var side = blockHit.getDirection();
        if (side == null) {
            var blockCenter = Vec3.atCenterOf(blockHit.getBlockPos());
            return blockCenter.add(axisAlignedNormal(look.scale(-1)));
        }

        return blockHit.getBlockPos().relative(side).getCenter();
    }

    private static Vec3 axisAlignedNormal(Vec3 vec) {
        var absX = Math.abs(vec.x);
        var absY = Math.abs(vec.y);
        var absZ = Math.abs(vec.z);

        if (absX >= absY && absX >= absZ) {
            return new Vec3(vec.x < 0.0D ? -1.0D : 1.0D, 0.0D, 0.0D);
        }
        if (absY >= absX && absY >= absZ) {
            return new Vec3(0.0D, vec.y < 0.0D ? -1.0D : 1.0D, 0.0D);
        }
        return new Vec3(0.0D, 0.0D, vec.z < 0.0D ? -1.0D : 1.0D);
    }

    private record LockedTarget(Vec3 center) {
    }
}
