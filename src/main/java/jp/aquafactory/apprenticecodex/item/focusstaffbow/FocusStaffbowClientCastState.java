package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class FocusStaffbowClientCastState {
    private static final CastBarRenderState HIDDEN_CAST_BAR = new CastBarRenderState(false, 0.0F, "");

    private static String spellId = "";
    private static long startedGameTime;
    private static int requiredCastTicks;
    private static double maxChargeMultiplier = 1.0D;

    private FocusStaffbowClientCastState() {
    }

    public static void applySyncedState(@Nullable CompoundTag data) {
        if (data == null || data.isEmpty()) {
            clear();
            return;
        }

        spellId = data.getString("spellId");
        startedGameTime = data.getLong("startedGameTime");
        requiredCastTicks = data.getInt("requiredCastTicks");
        maxChargeMultiplier = data.contains("maxChargeMultiplier") ? data.getDouble("maxChargeMultiplier") : 1.0D;
        if (spellId.isEmpty() || requiredCastTicks <= 0) {
            clear();
        }
    }

    public static void clear() {
        spellId = "";
        startedGameTime = 0L;
        requiredCastTicks = 0;
        maxChargeMultiplier = 1.0D;
    }

    public static boolean hasPendingCast(@Nullable LocalPlayer player) {
        return resolveCastBarState(player).visible();
    }

    public static CastBarRenderState resolveCastBarState(@Nullable LocalPlayer player) {
        if (!isActiveFor(player)) {
            clear();
            return HIDDEN_CAST_BAR;
        }

        long elapsedTicks = Math.max(0L, player.level().getGameTime() - startedGameTime);
        if (elapsedTicks < requiredCastTicks) {
            float completion = Mth.clamp(elapsedTicks / (float) requiredCastTicks, 0.0F, 1.0F);
            var remainingLabel = Utils.timeFromTicks(Math.max(0.0F, requiredCastTicks - elapsedTicks), 1);
            return new CastBarRenderState(true, completion, remainingLabel);
        }

        var rawMultiplier = FocusStaffbowChargeLogic.computeRawChargeMultiplier(elapsedTicks, requiredCastTicks);
        var appliedMultiplier = FocusStaffbowChargeLogic.clampChargeMultiplier(rawMultiplier, maxChargeMultiplier);
        return new CastBarRenderState(true, 1.0F, String.format(Locale.ROOT, "x%.1f", appliedMultiplier));
    }

    private static boolean isActiveFor(@Nullable LocalPlayer player) {
        return player != null
                && !spellId.isEmpty()
                && requiredCastTicks > 0
                && player.isAlive()
                && !player.isSpectator()
                && player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && ItemStack.isSameItemSameTags(player.getUseItem(), player.getMainHandItem())
                && player.getMainHandItem().getItem() instanceof FocusStaffbow;
    }

    public record CastBarRenderState(
            boolean visible,
            float completionPercent,
            String labelText
    ) {
    }
}
