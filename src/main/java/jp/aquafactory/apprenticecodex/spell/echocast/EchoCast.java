package jp.aquafactory.apprenticecodex.spell.echocast;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.effect.EchoSpell;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class EchoCast extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "echo_cast");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public EchoCast() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 20;
    }

    @Override
    public boolean requiresLearning() {
        // エルドリッチだけどアイテム専用なので初期解禁.
        return false;
    }

    @Override
    public boolean allowLooting() {
        // エルドリッチはデフォルトfalseだが他の処理でtrueにされる可能性を考慮して明示的にfalseにする.
        return false;
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
        return Optional.of(SoundRegistry.MULTICAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.MULTICAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!hasMulticastEchoStaffInHands(entity)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("ui.apprenticecodex.echo_cast.not_match_staff")
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        if (isEchoAmplifierAtLimit(entity)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("ui.apprenticecodex.echo_cast.cannot_echo_more")
                        .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var existingEffect = entity.getEffect(EffectRegistry.ECHO_SPELL.get());
        var amplifier = existingEffect == null ? 0 : Math.min(existingEffect.getAmplifier() + 1, maxEchoAmplifier());
        entity.addEffect(new MobEffectInstance(EffectRegistry.ECHO_SPELL.get(), EchoSpell.DURATION_TICKS, amplifier));

        if (entity instanceof ServerPlayer serverPlayer) {
            if (amplifier >= maxEchoAmplifier()) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "ui.apprenticecodex.echo_cast.max_reached",
                        amplifier + 1
                ).withStyle(ChatFormatting.GREEN), true);
            } else {
                serverPlayer.displayClientMessage(Component.translatable(
                        "ui.apprenticecodex.echo_cast.current_count",
                        amplifier + 1
                ).withStyle(ChatFormatting.GREEN), true);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static boolean hasMulticastEchoStaffInHands(LivingEntity entity) {
        return MulticastEchoStaff.isMulticastEchoStaff(entity.getMainHandItem())
                || MulticastEchoStaff.isMulticastEchoStaff(entity.getOffhandItem());
    }

    private static boolean isEchoAmplifierAtLimit(LivingEntity entity) {
        var existingEffect = entity.getEffect(EffectRegistry.ECHO_SPELL.get());
        return existingEffect != null && existingEffect.getAmplifier() >= maxEchoAmplifier();
    }

    private static int maxEchoAmplifier() {
        return ApprenticeCodexServerConfig.multicastEchoStaffMaxMulticastCount() - 1;
    }
}
