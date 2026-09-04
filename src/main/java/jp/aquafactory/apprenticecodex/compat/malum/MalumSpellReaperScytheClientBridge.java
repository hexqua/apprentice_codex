package jp.aquafactory.apprenticecodex.compat.malum;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class MalumSpellReaperScytheClientBridge {
    private MalumSpellReaperScytheClientBridge() {
    }

    static boolean hasEnoughMana(int manaCost) {
        // player capabilityのMagicDataはclientでは0を返すため、Iron'sがpacketで同期するclient専用値を操作予測に使う。
        // この判定を改造clientが迂回しても、server側のMagicDataによる消費・攻撃・cooldown判定は迂回できない。
        return ClientMagicData.getPlayerMana() >= manaCost;
    }
}
