package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class ClientSpellCastHelperMixin {

    // 特殊アイテム発動時だけ開始アニメーションを差し替え、音や入力抑制など他の前処理は元の spell 実装へ委ねる.
    @Inject(
            method = "handleClientBoundOnCastStarted",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void handleClientBoundOnCastStarted(UUID castingEntityId, String spellId, int spellLevel, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var player = minecraft.level.getPlayerByUUID(castingEntityId);
        if (player == null) {
            return;
        }

        var spell = SpellRegistry.getSpell(spellId);
        var castingSlot = ClientMagicData.getSyncedSpellData(player).getCastingEquipmentSlot();
        var castingStack = apprentice_codex$resolveCastingStack(player, castingSlot);
        if (!(castingStack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return;
        }

        if (!animationOverrideItem.shouldOverrideCastStartAnimation(castingStack, spell)) {
            return;
        }

        animationOverrideItem.getCastStartAnimation(castingStack, spell, spellLevel)
                .getForPlayer()
                .ifPresent(resourceLocation -> AnimationHelper.animatePlayerStart(player, resourceLocation));
        spell.onClientPreCast(player.level(), spellLevel, player, apprentice_codex$resolveCastingHand(castingSlot), null);
        ci.cancel();
    }

    // 0tick に短縮した LONG は開始直後に終了パケットも飛ぶため、元 spell の Finish をそのまま使うと二重モーションになる。
    @Redirect(
            method = "handleClientBoundOnCastFinished",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getCastFinishAnimation()Lio/redspace/ironsspellbooks/api/util/AnimationHolder;"
            )
    )
    private static AnimationHolder redirectSpellGunCastFinishAnimation(AbstractSpell spell, UUID castingEntityId, String spellId, boolean cancelled) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return spell.getCastFinishAnimation();
        }

        var player = minecraft.level.getPlayerByUUID(castingEntityId);
        if (player == null) {
            return spell.getCastFinishAnimation();
        }

        var castingStack = apprentice_codex$resolveSpellAnimationStack(player, spell);
        if (!(castingStack.getItem() instanceof CastAnimationOverrideItem animationOverrideItem)) {
            return spell.getCastFinishAnimation();
        }

        return animationOverrideItem.shouldSuppressCastFinishAnimation(castingStack, spell)
                ? AnimationHolder.pass()
                : spell.getCastFinishAnimation();
    }

    @Unique
    private static ItemStack apprentice_codex$resolveCastingStack(net.minecraft.world.entity.player.Player player, String castingSlot) {
        if (SpellSelectionManager.MAINHAND.equals(castingSlot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(castingSlot)) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private static InteractionHand apprentice_codex$resolveCastingHand(String castingSlot) {
        return SpellSelectionManager.OFFHAND.equals(castingSlot) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Unique
    private static ItemStack apprentice_codex$resolveSpellAnimationStack(net.minecraft.world.entity.player.Player player, AbstractSpell spell) {
        var mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof CastAnimationOverrideItem animationOverrideItem
                && (animationOverrideItem.shouldOverrideCastStartAnimation(mainHand, spell)
                || animationOverrideItem.shouldSuppressCastFinishAnimation(mainHand, spell))) {
            return mainHand;
        }

        var offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof CastAnimationOverrideItem animationOverrideItem
                && (animationOverrideItem.shouldOverrideCastStartAnimation(offHand, spell)
                || animationOverrideItem.shouldSuppressCastFinishAnimation(offHand, spell))) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}
