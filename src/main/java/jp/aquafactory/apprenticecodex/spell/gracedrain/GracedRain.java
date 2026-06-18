package jp.aquafactory.apprenticecodex.spell.gracedrain;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class GracedRain extends AbstractSummonWeaponSpell<GracedRainCloudEntity> implements IClientBlockTargetingSpell, ICraftsmansDelightAffectedSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "graced_rain");
    private static final String MESSAGE_CANT_PLACE = "ui.apprenticecodex.cant_place";

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public GracedRain() {
        super(GracedRainCloudEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 15;
        manaCostPerLevel = 5;
        castTime = 400;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.healing_per_second", Math.round(getHealAmount(spellLevel, caster) * 2.0f)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getEffectRadiusBlocks(spellLevel, caster), 1)),
                Component.translatable("ui.apprenticecodex.growth_speed_rank",
                        Component.translatable(getGrowthSpeedRankKey(getGrowthIntervalTicks(spellLevel, caster))))
        );
    }

    private float getHealAmount(int spellLevel, LivingEntity entity) {
        return 2f * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private double getTargetRange() {
        return 16.0;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getTargetRange();
    }

    private int getEffectRadiusBlocks(int spellLevel, LivingEntity entity) {
        return 1 + (int) Math.floor(getSpellPower(spellLevel, entity) / 200.0f);
    }

    private int getGrowthIntervalTicks(int spellLevel, LivingEntity entity) {
        return Math.max(1, 5 - Math.round(getSpellPower(spellLevel, entity) / 100.0f));
    }

    private String getGrowthSpeedRankKey(int growthIntervalTicks) {
        if (growthIntervalTicks <= 1) {
            return "ui.apprenticecodex.growth_speed_rank.fastest";
        }
        if (growthIntervalTicks <= 3) {
            return "ui.apprenticecodex.growth_speed_rank.fast";
        }
        return "ui.apprenticecodex.growth_speed_rank.normal";
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
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var result = RaycastTools.raycastFromEye(entity, getTargetRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, entity));
        if (result.hitEntity() != null) {
            return true;
        }

        var blockTarget = resolveBlockTarget(level, spellLevel, entity, result, false);
        if (blockTarget.isPresent() && GracedRainCloudEntity.findBlockAnchorCloudPosition(level, blockTarget.get()).isEmpty()) {
            sendCantPlaceMessage(entity);
            return false;
        }

        return true;
    }

    @Override
    public GracedRainCloudEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var result = RaycastTools.raycastFromEye(entity, getTargetRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, entity));
        var cloud = new GracedRainCloudEntity(EntityRegistry.GRACED_RAIN_CLOUD.get(), level, entity);
        cloud.setEffectRadiusBlocks(getEffectRadiusBlocks(spellLevel, entity));
        cloud.setGrowthIntervalTicks(getGrowthIntervalTicks(spellLevel, entity));
        cloud.setHealAmount(getHealAmount(spellLevel, entity));
        if (result.hitEntity() != null) {
            cloud.setFollowTarget(result.hitEntity());
        } else {
            Vec3 basePos;
            // 植物のような collider を持たない対象へ合わせるため、クライアントで見えていた枠線対象を優先復元する。
            var blockTarget = resolveBlockTarget(level, spellLevel, entity, result, true);
            if (blockTarget.isPresent()) {
                cloud.setAnchorBlock(level, blockTarget.get());
                level.addFreshEntity(cloud);
                return cloud;
            } else {
                basePos = result.hitPosition();
            }
            cloud.setAnchorPosition(GracedRainCloudEntity.toCloudPosition(basePos));
        }

        level.addFreshEntity(cloud);
        return cloud;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull GracedRainCloudEntity weapon) {
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull GracedRainCloudEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }

    private Optional<BlockPos> resolveBlockTarget(Level level, int spellLevel, LivingEntity entity, RaycastTools.TargetResult result, boolean consumeOutlinedTarget) {
        var outlinedTarget = resolveOutlinedTarget(level, spellLevel, entity, consumeOutlinedTarget);
        if (outlinedTarget.isPresent()) {
            return outlinedTarget;
        }

        if (result.hitType() == RaycastTools.TargetType.BLOCK && result.hitBlock() != null) {
            return Optional.of(result.hitBlock());
        }

        return Optional.empty();
    }

    private Optional<BlockPos> resolveOutlinedTarget(Level level, int spellLevel, LivingEntity entity, boolean consume) {
        var target = consume
                ? BlockTargetingHelper.getValidatedPendingTarget(level, entity, getSpellResource(), getClientBlockTargetingRange(spellLevel, entity))
                : BlockTargetingHelper.peekValidatedPendingTarget(level, entity, getSpellResource(), getClientBlockTargetingRange(spellLevel, entity));
        return target
                .map(targetData -> targetData.getHitBlockPos().immutable());
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(MESSAGE_CANT_PLACE, this.getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }
}
