package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    private static ItemStack resolveCastingStack(net.minecraft.world.entity.player.Player player, String castingSlot) {
        if (SpellSelectionManager.MAINHAND.equals(castingSlot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(castingSlot)) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }
}
