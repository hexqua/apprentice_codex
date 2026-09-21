package jp.aquafactory.apprenticecodex.renderer.item;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.player.AbstractClientPlayer;

public final class SpellrifleCastAnimationVisibility {
    private SpellrifleCastAnimationVisibility() {
    }

    public static boolean ownsArms(AbstractClientPlayer player, float partialTick) {
        // 銃の発射も詠唱状態になるため、通信上の詠唱フラグではなく実際の描画レイヤーを調べる。
        var casting = PlayerAnimationAccess.getPlayerAssociatedData(player).get(SpellAnimations.ANIMATION_RESOURCE);
        return FirstPersonMode.isFirstPersonPass()
                || (casting != null && casting.isActive())
                || PlayerAnimationAccess.getPlayerAnimLayer(player).getFirstPersonMode(partialTick) == FirstPersonMode.THIRD_PERSON_MODEL;
    }
}
