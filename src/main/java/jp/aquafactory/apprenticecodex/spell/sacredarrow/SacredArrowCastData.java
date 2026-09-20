package jp.aquafactory.apprenticecodex.spell.sacredarrow;

import io.redspace.ironsspellbooks.api.spells.ICastData;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

// nullも「詠唱開始時に対象なし」を表し、発射時の再探索と区別する。
public record SacredArrowCastData(@Nullable UUID targetId) implements ICastData {
    @Override public void reset() {}
}
