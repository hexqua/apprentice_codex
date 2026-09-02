package jp.aquafactory.apprenticecodex.item.curios.undyingemblem;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UndyingEmblemClientState {
    private static int remainingCooldownTicks;
    private static long receivedGameTime;

    private UndyingEmblemClientState() {
    }

    public static void set(int remainingTicks, long serverGameTime) {
        remainingCooldownTicks = Math.max(0, remainingTicks);
        receivedGameTime = serverGameTime;
    }

    public static int getRemainingCooldownTicks() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return remainingCooldownTicks;
        }
        var elapsed = Math.max(0L, level.getGameTime() - receivedGameTime);
        return Math.max(0, remainingCooldownTicks - (int) Math.min(Integer.MAX_VALUE, elapsed));
    }

    public static void reset() {
        remainingCooldownTicks = 0;
        receivedGameTime = 0;
    }
}
