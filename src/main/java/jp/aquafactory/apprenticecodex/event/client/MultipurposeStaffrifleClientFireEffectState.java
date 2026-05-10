package jp.aquafactory.apprenticecodex.event.client;

import net.minecraft.client.Minecraft;

public final class MultipurposeStaffrifleClientFireEffectState {
    private static final float RECOIL_DURATION_TICKS = 8.0F;
    private static final float RECOIL_HOLD_TICKS = 2.0F;

    private static long lastFireGameTime = Long.MIN_VALUE;

    private MultipurposeStaffrifleClientFireEffectState() {
    }

    public static void beginRecoil() {
        lastFireGameTime = resolveGameTime();
    }

    public static float getRecoilAmount(float partialTick) {
        var gameTime = resolveGameTime();
        if (gameTime < 0L || lastFireGameTime == Long.MIN_VALUE) {
            return 0.0F;
        }

        var age = gameTime + partialTick - lastFireGameTime;
        if (age < 0.0F || age >= RECOIL_DURATION_TICKS) {
            return 0.0F;
        }

        if (age <= RECOIL_HOLD_TICKS) {
            return 1.0F;
        }

        var restoreProgress = (age - RECOIL_HOLD_TICKS) / (RECOIL_DURATION_TICKS - RECOIL_HOLD_TICKS);
        return 1.0F - restoreProgress;
    }

    private static long resolveGameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? -1L : level.getGameTime();
    }
}
