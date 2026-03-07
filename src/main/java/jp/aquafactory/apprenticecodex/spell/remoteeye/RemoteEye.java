package jp.aquafactory.apprenticecodex.spell.remoteeye;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class RemoteEye extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "remote_eye");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public RemoteEye() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        baseManaCost = 100;
        manaCostPerLevel = 20;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel, caster), 1))
        );
    }

    private int getDuration(int spellLevel, LivingEntity caster) {
        return 20 + Math.round(40 * getSpellPower(spellLevel, caster) / 100.0f);
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
        return Optional.of(SoundRegistry.REMOTE_PREPARE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CAST_T_POSE;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var duration = getDuration(spellLevel, entity);
        Capabilities.withSpellData(entity, data -> {
            data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                state.activeUntilGameTime = level.getGameTime() + duration;
                state.anchorX = entity.getX();
                state.anchorY = entity.getY();
                state.anchorZ = entity.getZ();
                state.anchorYaw = entity.getYRot();
                state.anchorPitch = entity.getXRot();
            });
            if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
                RemoteEyeSync.syncToClient(serverPlayer, data.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE));
            }
        });
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
