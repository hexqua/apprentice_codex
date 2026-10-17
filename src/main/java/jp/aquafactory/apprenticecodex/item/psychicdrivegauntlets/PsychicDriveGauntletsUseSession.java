package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

// clientの押下とserver応答を対応付ける。終了済みの応答では次の押下を変更しない。
public final class PsychicDriveGauntletsUseSession {
    private long id;
    private long started;
    private boolean active;

    public long begin(long time) {
        id++;
        started = time;
        active = true;
        return id;
    }

    public void accept(long session, boolean charging) {
        if (active && session == id && !charging) active = false;
    }

    public void end() { active = false; }
    public boolean active() { return active; }
    public long id() { return id; }
    public long elapsed(long time) { return Math.max(0, time - started); }
}
