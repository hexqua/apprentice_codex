package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

public final class ChargecastCatalystbookPresentationResolver {
    private ChargecastCatalystbookPresentationResolver() {
    }

    public static AnimationHolder resolveCompletionAnimation(AbstractSpell spell) {
        var finishAnimation = spell.getCastFinishAnimation();
        if (finishAnimation.getForPlayer().isPresent()) {
            return finishAnimation;
        }
        // none は完了時に停止する明示指定なので、通常の INSTANT が返す pass の場合だけ開始側へ戻す。
        if (!finishAnimation.isPass) {
            return AnimationHolder.none();
        }

        var startAnimation = spell.getCastStartAnimation();
        return startAnimation.getForPlayer().isPresent() ? startAnimation : AnimationHolder.none();
    }

    public static boolean shouldDeferStartSound(AbstractSpell spell) {
        // INSTANT の効果音を Start 側だけに置く魔法は、追加詠唱の完了まで効果音を遅らせる。
        return spell.getCastStartSound().isPresent() && spell.getCastFinishSound().isEmpty();
    }
}
