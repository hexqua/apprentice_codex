package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class ClientSpellCastHelperMixin {

    // spell gun 発動時だけ開始アニメーションを差し替え、音や入力抑制など他の前処理は元の spell 実装へ委ねる.
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
        var castingStack = resolveCastingStack(player, castingSlot);
        if (!(castingStack.getItem() instanceof AbstractSpellGunItem spellGunItem)) {
            return;
        }

        if (!spellGunItem.shouldOverrideSpellGunCastStartAnimation(castingStack, spell)) {
            return;
        }

        spellGunItem.getSpellGunCastStartAnimation(castingStack, spell, spellLevel)
                .getForPlayer()
                .ifPresent(resourceLocation -> ClientSpellCastHelper.animatePlayerStart(player, resourceLocation));
        spell.onClientPreCast(player.level(), spellLevel, player, player.getUsedItemHand(), null);
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

        var castingStack = resolveSpellGunAnimationStack(player, spell);
        if (!(castingStack.getItem() instanceof AbstractSpellGunItem spellGunItem)) {
            return spell.getCastFinishAnimation();
        }

        return spellGunItem.shouldSuppressSpellGunCastFinishAnimation(castingStack, spell)
                ? AnimationHolder.pass()
                : spell.getCastFinishAnimation();
    }

    private static ItemStack resolveCastingStack(net.minecraft.world.entity.player.Player player, String castingSlot) {
        if (SpellSelectionManager.MAINHAND.equals(castingSlot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(castingSlot)) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack resolveSpellGunAnimationStack(net.minecraft.world.entity.player.Player player, AbstractSpell spell) {
        var mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof AbstractSpellGunItem spellGunItem
                && (spellGunItem.shouldOverrideSpellGunCastStartAnimation(mainHand, spell)
                || spellGunItem.shouldSuppressSpellGunCastFinishAnimation(mainHand, spell))) {
            return mainHand;
        }

        var offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof AbstractSpellGunItem spellGunItem
                && (spellGunItem.shouldOverrideSpellGunCastStartAnimation(offHand, spell)
                || spellGunItem.shouldSuppressSpellGunCastFinishAnimation(offHand, spell))) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}
