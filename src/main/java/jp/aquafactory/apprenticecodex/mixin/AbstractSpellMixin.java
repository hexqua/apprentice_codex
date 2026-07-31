package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookStartSoundContext;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowStartSoundContext;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffPendingAdvance;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {
    @Redirect(
            method = {"getSpellPower", "getEntityPowerMultiplier"},
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/SchoolType;getPowerFor(Lnet/minecraft/world/entity/LivingEntity;)D"
            )
    )
    private double apprentice_codex$useDivinePossessionSchoolPower(SchoolType schoolType, LivingEntity caster) {
        return MagiAgentSuitEffects.resolveSchoolPower((AbstractSpell) (Object) this, schoolType, caster);
    }

    @Inject(method = "getEffectiveCastTime", at = @At("RETURN"), cancellable = true)
    private void apprentice_codex$applyMagiAgentSuitBootsCastTimeReduction(
            int spellLevel,
            LivingEntity entity,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(MagiAgentSuitEffects.applyBootsCastTimeReduction(
                (AbstractSpell) (Object) this,
                cir.getReturnValue(),
                entity
        ));
    }

    @Inject(method = "canBeInterrupted", at = @At("RETURN"), cancellable = true)
    private void apprentice_codex$protectNetheriteSwingcastStaffCast(
            @Nullable net.minecraft.world.entity.player.Player player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player != null && player.getMainHandItem().is(ItemRegistry.NETHERITE_SWINGCAST_STAFF.get())) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "onClientPreCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastStartSound()Ljava/util/Optional;"
            )
    )
    private Optional<SoundEvent> apprentice_codex$redirectClientStartSound(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity entity,
            InteractionHand hand,
            @Nullable MagicData playerMagicData
    ) {
        return (FocusStaffbowStartSoundContext.isSuppressed(entity)
                || ChargecastCatalystbookStartSoundContext.shouldSuppress(spell, entity))
                ? Optional.empty() : spell.getCastStartSound();
    }

    @Redirect(
            method = "onServerPreCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastStartSound()Ljava/util/Optional;"
            )
    )
    private Optional<SoundEvent> apprentice_codex$redirectServerStartSound(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        return (FocusStaffbowStartSoundContext.isSuppressed(entity)
                || ChargecastCatalystbookStartSoundContext.shouldSuppress(spell, entity))
                ? Optional.empty() : spell.getCastStartSound();
    }

    @Inject(method = "castSpell", at = @At("HEAD"))
    private void apprentice_codex$handleMulticastEchoStaffCastSpellStart(
            Level world,
            int spellLevel,
            ServerPlayer serverPlayer,
            CastSource castSource,
            boolean triggerCooldown,
            CallbackInfo ci
    ) {
        var magicData = MagicData.getPlayerMagicData(serverPlayer);
        MulticastEchoStaffCastHelper.onCastSpellStart(
                (AbstractSpell) (Object) this,
                world,
                spellLevel,
                serverPlayer,
                castSource,
                triggerCooldown,
                magicData
        );
    }

    @Inject(method = "onServerPreCast", at = @At("HEAD"))
    private void apprentice_codex$handleMulticastEchoStaffServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData,
            CallbackInfo ci
    ) {
        MulticastEchoStaffCastHelper.onServerPreCast((AbstractSpell) (Object) this, spellLevel, entity, playerMagicData);
    }

    @Inject(method = "onServerCastComplete", at = @At("HEAD"))
    private void apprentice_codex$handleFocusStaffbowCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled,
            CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MulticastEchoStaffCastHelper.onServerCastComplete(
                (AbstractSpell) (Object) this,
                level,
                spellLevel,
                serverPlayer,
                playerMagicData,
                cancelled
        );
        //noinspection DataFlowIssue
        RevolvercastStaffPendingAdvance.onServerCastComplete(
                serverPlayer,
                (AbstractSpell) (Object) this,
                playerMagicData,
                cancelled
        );

        //noinspection DataFlowIssue
        var spell = (AbstractSpell) (Object) this;
        var castingItem = playerMagicData.getPlayerCastingItem();
        if (cancelled || !playerMagicData.getPlayerRecasts().hasRecastForSpell(spell)) {
            MithrilFreecastStaffCastContext.clearPendingCooldownSource(
                    serverPlayer.getUUID(),
                    castingItem,
                    spell
            );
        }
        if (castingItem.getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerCastCompletionAnimation(serverPlayer, castingItem, cancelled);
        }
    }
}
