package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SelectionOption;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellWheelOverlay.class, remap = false)
public abstract class SpellWheelOverlayMixin {
    @Unique
    private static final ThreadLocal<SelectionOption> apprentice_codex$cooldownSelection = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void apprentice_codex$clearCooldownSelectionAtRenderHead(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        apprentice_codex$cooldownSelection.remove();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void apprentice_codex$clearCooldownSelectionAtRenderReturn(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        apprentice_codex$cooldownSelection.remove();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/magic/SpellSelectionManager$SelectionOption;getCastSource()Lio/redspace/ironsspellbooks/api/spells/CastSource;"
            )
    )
    private CastSource apprentice_codex$rememberCooldownSelection(SelectionOption selectionOption) {
        apprentice_codex$cooldownSelection.set(selectionOption);
        return selectionOption.getCastSource();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/capabilities/magic/MagicManager;getEffectiveSpellCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;)I"
            )
    )
    private int apprentice_codex$redirectSpellWheelCooldown(AbstractSpell spell, Player player, CastSource castSource) {
        var selectionOption = apprentice_codex$cooldownSelection.get();
        apprentice_codex$cooldownSelection.remove();
        if (selectionOption == null) {
            return MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        }
        return WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, castSource, selectionOption.slot);
    }
}
