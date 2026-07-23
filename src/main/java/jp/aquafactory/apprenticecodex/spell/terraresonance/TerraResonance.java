package jp.aquafactory.apprenticecodex.spell.terraresonance;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.IClientBlockHitTargetingSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TerraResonance extends AbstractSpell implements IClientBlockHitTargetingSpell {
    private static final double CAST_RANGE = 8.0D;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "terra_resonance");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(10)
            .build();

    public TerraResonance() {
        baseSpellPower = 12;
        spellPowerPerLevel = 8;
        baseManaCost = 80;
        manaCostPerLevel = 20;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, @Nullable LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.terra_resonance.search_size",
                        getRange(spellLevel, caster), getRange(spellLevel, caster), getRange(spellLevel, caster))
        );
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return CAST_RANGE;
    }

    private int getRange(int spellLevel, @Nullable Entity caster) {
        // 範囲が広すぎるとパフォーマンスに致命的な影響が出るのでハードリミットあり.
        var size = Math.min(36 + Math.round(getSpellPower(spellLevel, caster)), 127);

        // 偶数だとどっちにずれるか考える必要があるのでプレイヤー有利側に奇数にする.
        return size % 2 == 0 ? size + 1 : size;
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
        // todo:それっぽい音を作る.
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // todo:それっぽい音を作る.
        return Optional.of(jp.aquafactory.apprenticecodex.registry.SoundRegistry.TERRAIN.get());
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
        return new BlockTargetData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (level.isClientSide) {
            return true;
        }
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        var targetingRange = getClientBlockTargetingRange(spellLevel, entity);
        var target = BlockTargetingHelper.getValidatedPendingHitTarget(
                level, entity, getSpellResource(), targetingRange
        )
                // クライアントの細かな照準位置は尊重しつつ、探索中心だけを射程外へ差し替える送信は拒否する。
                .filter(targetData -> isHitBlockWithinRange(entity, targetData, targetingRange));
        if (target.isEmpty()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("ui.irons_spellbooks.cast_error_target", getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        playerMagicData.setAdditionalCastData(target.get());
        return true;
    }

    private static boolean isHitBlockWithinRange(LivingEntity entity, BlockTargetData targetData, double range) {
        var hitBlockPos = targetData.getHitBlockPos();
        return hitBlockPos != null
                && new AABB(hitBlockPos).distanceToSqr(entity.getEyePosition(1.0F)) <= range * range;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (!(level instanceof ServerLevel serverLevel) || playerMagicData == null
                || !(playerMagicData.getAdditionalCastData() instanceof BlockTargetData targetData)) {
            return;
        }

        TerraResonanceJobManager.startPulsePair(serverLevel, targetData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer serverPlayer
                && playerMagicData.getAdditionalCastData() instanceof BlockTargetData targetData
                && targetData.getHitBlockPos() != null && targetData.getHitFace() != null) {
            var result = TerraResonanceSearch.collect(
                    serverLevel,
                    targetData.getHitBlockPos(),
                    targetData.getHitFace(),
                    getRange(spellLevel, entity)
            );
            serverPlayer.displayClientMessage(
                    Component.translatable("ui.apprenticecodex.terra_resonance.prepare")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            TerraResonanceJobManager.startPulsePair(serverLevel, targetData);
            TerraResonanceJobManager.submitResult(serverLevel, serverPlayer, result);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

}
