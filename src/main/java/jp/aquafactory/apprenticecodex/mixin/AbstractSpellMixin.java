package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowStartSoundContext;
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

import java.util.Optional;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {
    @Redirect(
            method = "onClientPreCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastStartSound()Ljava/util/Optional;"
            )
    )
    private Optional<SoundEvent> apprentice_codex$redirectClientFocusStaffbowStartSound(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity entity,
            InteractionHand hand,
            @Nullable MagicData playerMagicData
    ) {
        return FocusStaffbowStartSoundContext.isSuppressed(entity) ? Optional.empty() : spell.getCastStartSound();
    }

    @Redirect(
            method = "onServerPreCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastStartSound()Ljava/util/Optional;"
            )
    )
    private Optional<SoundEvent> apprentice_codex$redirectServerFocusStaffbowStartSound(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        return FocusStaffbowStartSoundContext.isSuppressed(entity) ? Optional.empty() : spell.getCastStartSound();
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

        var castingItem = playerMagicData.getPlayerCastingItem();
        if (castingItem.getItem() instanceof FocusStaffbow focusStaffbow) {
            focusStaffbow.triggerCastCompletionAnimation(serverPlayer, castingItem, cancelled);
        }
    }
}
