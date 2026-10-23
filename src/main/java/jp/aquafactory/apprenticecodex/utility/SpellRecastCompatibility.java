package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellRecastCompatibility {
    private static final Set<Class<? extends AbstractSpell>> WARNED_SPELL_CLASSES = ConcurrentHashMap.newKeySet();

    private SpellRecastCompatibility() {
    }

    public static boolean hasNoRecastWithoutCaster(AbstractSpell spell, int spellLevel) {
        try {
            return spell.getRecastCount(spellLevel, null) <= 0;
        } catch (RuntimeException exception) {
            // 外部MODには使用者の装備を必須とする実装がある。回数不明を0とみなすと
            // 再詠唱禁止を突破するため拒否し、JEIの全レベル走査でも警告はクラスごとに一度に抑える。
            if (WARNED_SPELL_CLASSES.add(spell.getClass())) {
                ApprenticeCodex.LOGGER.warn(
                        "Cannot determine recast count without a caster for {}. Rejecting uses that require no recasts.",
                        spell.getClass().getName(), exception);
            }
            // レベルや設定で結果が変わり得るため、判定結果そのものはキャッシュしない。
            return false;
        }
    }
}
