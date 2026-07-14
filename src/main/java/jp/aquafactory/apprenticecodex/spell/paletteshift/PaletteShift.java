package jp.aquafactory.apprenticecodex.spell.paletteshift;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.pastelstaff.PastelStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class PaletteShift extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "palette_shift");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(false)
            .build();

    public PaletteShift() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public boolean requiresLearning() {
        // エルドリッチだけどアイテム専用なので初期解禁.
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.PALETTE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (hasPastelStaffInHands(entity)) {
            return true;
        }

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.not_hold_pastel_staff").withStyle(ChatFormatting.RED)
            ));
        }
        return false;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        entity.addEffect(new MobEffectInstance(EffectRegistry.PALETTE_RECEPTION.get(), 20 * 30));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static boolean hasPastelStaffInHands(LivingEntity entity) {
        return PastelStaff.isPastelStaff(entity.getMainHandItem())
                || PastelStaff.isPastelStaff(entity.getOffhandItem());
    }
}
