package jp.aquafactory.apprenticecodex.spell.sacredarrow;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.AABB;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import java.util.Comparator;

import java.util.List;
import java.util.Optional;

public class SacredArrow extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sacred_arrow");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(8)
            .build();

    public SacredArrow() {
        baseSpellPower = 800;
        spellPowerPerLevel = 150;
        baseManaCost = 30;
        manaCostPerLevel = 5;
        castTime = 20;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.sacred_arrow.chasing_penetrate_damage_multiplier", getPenetrateDamagePercent()),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SACRED_ARROW);
    }

    private int getPenetrateDamagePercent(){
        return Math.round(100 * getPenetrateDamageMultiplier());
    }

    private float getPenetrateDamageMultiplier(){
        // 固定25%.
        return 0.25f;
    }

    public int getDuration() {
        // 固定15秒.
        return 20 * 15;
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
        return Optional.of(SoundRegistry.SUNBEAM_WINDUP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.GUIDING_BOLT_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        super.onServerPreCast(level, spellLevel, entity, magicData);
        if (level instanceof ServerLevel && magicData != null) {
            magicData.setAdditionalCastData(selectTarget(entity));
        }
    }

    private SacredArrowCastData selectTarget(LivingEntity caster) {
        var origin = caster.position();
        var target = caster.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(origin, origin).inflate(48), candidate ->
                                SacredArrowEntity.isLiveTarget(candidate, caster)
                                        && (candidate.hasEffect(EffectRegistry.SACRED_SIGN)
                                        || candidate.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.GUIDING_BOLT)))
                .stream().min(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(caster))
                        .thenComparingInt(LivingEntity::getId)).orElse(null);
        if (target != null && caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ui.apprenticecodex.sacred_arrow.target_found",
                    target.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
        }
        return new SacredArrowCastData(target == null ? null : target.getUUID());
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData magicData, boolean cancelled) {
        super.onServerCastComplete(level, spellLevel, entity, magicData, cancelled);
        if (magicData != null && magicData.getAdditionalCastData() instanceof SacredArrowCastData) {
            magicData.setAdditionalCastData(null);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            // 開始処理を持たない発動だけを補完し、選出済みの「対象なし」は尊重する。
            var data = playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof SacredArrowCastData stored
                    ? stored : selectTarget(entity);
            var target = data.targetId() == null ? null : server.getEntity(data.targetId());
            var arrow = new SacredArrowEntity(EntityRegistry.SACRED_ARROW.get(), level);
            arrow.launch(entity, entity.getEyePosition(), entity.getLookAngle(), getDamage(spellLevel, entity),
                    getPenetrateDamageMultiplier(), getDuration(), SacredArrowEntity.isLiveTarget(target, entity) ? (LivingEntity) target : null);
            level.addFreshEntity(arrow);
            if (playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof SacredArrowCastData) {
                playerMagicData.setAdditionalCastData(null);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
