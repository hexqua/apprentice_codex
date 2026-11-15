package jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton;

import java.util.TreeMap;

public final class ExoskeletonInputState {
    private long nextSequence;
    private final TreeMap<Long, Integer> pendingCosts = new TreeMap<>();

    public long reserve(int cost) {
        long sequence = ++nextSequence;
        pendingCosts.put(sequence, cost);
        return sequence;
    }

    public long availableMana(int syncedMana) {
        return Math.max(0L, (long) syncedMana - pendingCosts.values().stream().mapToLong(Integer::longValue).sum());
    }

    public boolean acknowledge(long sequence) {
        if (!pendingCosts.containsKey(sequence)) return false;
        pendingCosts.headMap(sequence, true).clear();
        return true;
    }

    public void reset() {
        pendingCosts.clear();
        // 古いworldから遅れて届く応答と、新しい入力の番号を再利用しない。
    }
}
