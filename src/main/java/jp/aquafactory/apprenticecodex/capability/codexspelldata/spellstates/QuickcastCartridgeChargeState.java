package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.nbt.CompoundTag;

/** 収納アイテムと独立した、プレイヤー共有の回復待ちだけを保存する。 */
public final class QuickcastCartridgeChargeState implements ICodexSpellState {
    private long recoveryUntil;
    private long recoveryDuration;
    private boolean restored;

    public long recoveryUntil() { return recoveryUntil; }
    public long recoveryDuration() { return recoveryDuration; }
    public boolean available(long now) { return recoveryUntil <= now; }

    public void repairAfterLoad(long now) {
        if (!restored) return;
        restored = false;
        // 通常の再ログインでは終了時刻を変えず、巻き戻り時だけ保存期間以内に補正する。
        long safeDuration = Math.min(recoveryDuration, Long.MAX_VALUE - Math.max(0, now));
        recoveryUntil = PersistentGameTimeSanitizer.repairPersistedFutureUntilWithKnownMax(
                now, recoveryUntil, safeDuration);
    }

    public void consume(long now, long duration) {
        recoveryDuration = Math.max(1, duration);
        recoveryUntil = addTime(now, recoveryDuration);
    }

    public void recover() {
        recoveryUntil = 0;
        recoveryDuration = 0;
    }

    public static long addTime(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    public static long recoveryTicks(int cooldown, double multiplier, int minimum) {
        double scaled = Math.ceil(Math.max(0, cooldown) * multiplier);
        return Math.max(minimum, scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) scaled);
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("recoveryUntil", recoveryUntil);
        tag.putLong("recoveryDuration", recoveryDuration);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        recoveryUntil = Math.max(0, tag.getLong("recoveryUntil"));
        recoveryDuration = Math.max(0, tag.getLong("recoveryDuration"));
        restored = true;
    }
}
