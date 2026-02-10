package jp.aquafactory.apprenticecodex.common.spells.gracedrain;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.spells.AbstractFirearmSpell;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
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

public class GracedRain extends AbstractFirearmSpell<GracedRainCloudEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "graced_rain");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public GracedRain() {
        super(GracedRainCloudEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 5;
        baseManaCost = 20;
        manaCostPerLevel = 4;
        castTime = 400;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) / 10.0f;
    }

    private double getTargetRange() {
        return 16.0;
    }

    private double getTargetWidth() {
        return 0.5;
    }

    private float getCloudRadius(int spellLevel, LivingEntity entity) {
        return 2.5f;
    }

    private float getCloudThickness(int spellLevel, LivingEntity entity) {
        return 0.8f;
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
        return Optional.empty();
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
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public GracedRainCloudEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var result = RaycastTools.raycastFromEye(entity, getTargetRange(), getTargetWidth(), e -> true);
        var cloud = new GracedRainCloudEntity(EntityRegistry.GRACED_RAIN_CLOUD.get(), level, entity);
        cloud.setCloudRadius(getCloudRadius(spellLevel, entity));
        cloud.setCloudThickness(getCloudThickness(spellLevel, entity));
        if (result.hitEntity() != null) {
            cloud.setFollowTarget(result.hitEntity());
        } else {
            Vec3 basePos;
            if (result.hitType() == RaycastTools.TargetType.BLOCK && result.hitBlock() != null) {
                basePos = Vec3.atCenterOf(result.hitBlock());
            } else {
                basePos = result.hitPosition();
            }
            cloud.setAnchorPosition(GracedRainCloudEntity.toCloudPosition(basePos));
        }

        level.addFreshEntity(cloud);
        return cloud;
    }

    @Override
    public TickCastTypes onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull GracedRainCloudEntity weapon) {
        return TickCastTypes.KEEP_CASTING;
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull GracedRainCloudEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
