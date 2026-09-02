package jp.aquafactory.apprenticecodex.spell.idolreconstruction;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemClientState;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemConfigState;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class IdolReconstruction extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "idol_reconstruction");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(10)
            .setAllowCrafting(false)
            .build();

    public IdolReconstruction() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 10;
        manaCostPerLevel = 0;
        castTime = 20 * 60 * 20;
    }

    @Override
    public boolean allowLooting(){
        // アイテム専用化.
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel,@Nullable LivingEntity caster) {
        var emblemItem = ItemRegistry.UNDYING_EMBLEM.get();
        var emblemName = emblemItem.getName(new ItemStack(emblemItem));
        if (caster == null) {
            return List.of(
                    Component.translatable("ui.apprenticecodex.idol_reconstruction.accelerate_repair_speed",
                            emblemName, getAccelerateSpeedPercent(null))
            );
        }

        return List.of(
                Component.translatable("ui.apprenticecodex.idol_reconstruction.accelerate_repair_speed",
                        emblemName, getAccelerateSpeedPercent(caster)),
                Component.translatable("ui.apprenticecodex.idol_reconstruction.remain_repair_time",
                        Utils.timeFromTicks(getEffectiveCastTime(spellLevel, caster), 1))
        );
    }

    private int getAccelerateSpeedPercent(@Nullable LivingEntity entity){
        return 100 * getAccelerateSpeedMultiplier(entity);
    }

    private int getAccelerateSpeedMultiplier(@Nullable LivingEntity entity) {
        if ((entity != null && entity.level().isClientSide) || FMLEnvironment.dist == Dist.CLIENT) {
            return UndyingEmblemConfigState.reconstructionSpeedMultiplier();
        }
        return ApprenticeCodexServerConfig.undyingEmblemReconstructionSpeedMultiplier();
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        if (!(entity instanceof Player player)){
            return getCastTime(spellLevel);
        }
        var remaining = player.level().isClientSide
                ? UndyingEmblemClientState.getRemainingCooldownTicks()
                : UndyingEmblemRuntime.getRemainingCooldownTicks(player);
        if (remaining <= 0) {
            return UndyingEmblemRuntime.CAST_INTERVAL_TICKS;
        }
        var multiplier = getAccelerateSpeedMultiplier(entity);
        var acceleratedTicks = (remaining + multiplier - 1) / multiplier;
        var roundedTicks = ((acceleratedTicks + UndyingEmblemRuntime.CAST_INTERVAL_TICKS - 1)
                / UndyingEmblemRuntime.CAST_INTERVAL_TICKS) * UndyingEmblemRuntime.CAST_INTERVAL_TICKS;
        return Math.min(getCastTime(spellLevel), Math.max(UndyingEmblemRuntime.CAST_INTERVAL_TICKS, roundedTicks));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        var equipped = UndyingEmblemRuntime.isEquipped(player);
        var onCooldown = player.level().isClientSide
                ? UndyingEmblemClientState.getRemainingCooldownTicks() > 0
                : UndyingEmblemRuntime.isOnCooldown(player);
        if (!equipped || !onCooldown) {
            if (player instanceof ServerPlayer serverPlayer) {
                var key = equipped
                        ? "ui.apprenticecodex.idol_reconstruction.no_cool_down"
                        : "ui.apprenticecodex.idol_reconstruction.not_found_emblem";
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable(key).withStyle(ChatFormatting.RED)));
            }
            return false;
        }
        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
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
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer player) {
            if (!UndyingEmblemRuntime.isEquipped(player) || !UndyingEmblemRuntime.isOnCooldown(player)) {
                UndyingEmblemRuntime.cancelReconstructionCast(player);
                return;
            }
            var multiplier = ApprenticeCodexServerConfig.undyingEmblemReconstructionSpeedMultiplier();
            // CONTINUOUSの通常経過分を除いた追加量を10tickごとに進め、合計を設定倍率へ合わせる.
            UndyingEmblemRuntime.advanceCooldown(
                    player,
                    UndyingEmblemRuntime.CAST_INTERVAL_TICKS * Math.max(0, multiplier - 1)
            );
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
