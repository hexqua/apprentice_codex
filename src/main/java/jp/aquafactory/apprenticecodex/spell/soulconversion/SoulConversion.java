package jp.aquafactory.apprenticecodex.spell.soulconversion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerEvents;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SoulConversion extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soul_conversion");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(false)
            .build();

    public SoulConversion() {
        config.enabled = MalumStaffChargeBridge.isAvailable();
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 500;
        manaCostPerLevel = 0;
        castTime = 30;
    }

    @Override
    public boolean isEnabled() {
        return MalumStaffChargeBridge.isAvailable() && super.isEnabled();
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
        return Optional.of(SoundRegistry.VANILLA_POWER_TUNING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var failure = failureKey(entity);
        if (failure != null) {
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(failure)));
            }
            return false;
        }
        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    private String failureKey(LivingEntity entity) {
        if (!MalumStaffChargeBridge.isAvailable()) return "ui.apprenticecodex.soul_conversion.not_installed_malum";
        if (!(entity instanceof Player) || !ManaSoulTransducerEvents.isEquipped(entity)) return "ui.apprenticecodex.soul_conversion.not_found_transducer";
        if (!MalumStaffChargeBridge.needsRecovery(entity)) return "ui.apprenticecodex.soul_conversion.already_fully_charged";
        if (!isEnabled()) return "ui.apprenticecodex.soul_conversion.disabled";
        return null;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer player && failureKey(player) != null) {
            Utils.serverSideCancelCast(player, false);
            return;
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean triggerCooldown) {
        // onCastより前にマナが引かれるため、最終tickの自然回復も支払い前に検査する。
        if (failureKey(player) != null) return;
        super.castSpell(level, spellLevel, player, source, triggerCooldown);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer player) || failureKey(player) != null) return;
        MalumStaffChargeBridge.recoverFully(player);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
