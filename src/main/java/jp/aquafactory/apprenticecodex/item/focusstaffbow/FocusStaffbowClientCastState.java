package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class FocusStaffbowClientCastState {
    private static final int LABEL_COLOR_CYAN = 0x55FFFF;
    private static final int LABEL_COLOR_RED = 0xFF5555;
    private static final float MANA_UI_SAFE_MARGIN = 0.001F;
    private static final long STALE_VISIBLE_STATE_TICKS = 20L;
    private static final CastBarRenderState HIDDEN_CAST_BAR = new CastBarRenderState(false, 0.0F, "", "", 0xFFFFFF, 0xFFFFFF);

    private static String castMode = "";
    private static String spellId = "";
    private static long startedGameTime;
    private static int requiredCastTicks;
    private static int chargeBaselineTicks;
    private static int baseManaCost;
    private static boolean hasSyncedCastState;
    private static boolean hasConfirmedActiveUseState;
    private static long lastVisibleGameTime;

    private FocusStaffbowClientCastState() {
    }

    public static void applySyncedState(@Nullable CompoundTag data) {
        if (data == null || data.isEmpty()) {
            clear();
            return;
        }

        castMode = data.contains("castMode") ? data.getString("castMode") : "";
        spellId = data.getString("spellId");
        startedGameTime = data.getLong("startedGameTime");
        requiredCastTicks = Math.max(0, data.getInt("requiredCastTicks"));
        chargeBaselineTicks = data.contains("chargeBaselineTicks")
                ? Math.max(0, data.getInt("chargeBaselineTicks"))
                : requiredCastTicks;
        if (data.contains("chargeUpdateIntervalTicks")) {
            data.getInt("chargeUpdateIntervalTicks");
        }
        baseManaCost = data.contains("baseManaCost") ? Math.max(0, data.getInt("baseManaCost")) : 0;
        hasSyncedCastState = true;
        hasConfirmedActiveUseState = false;
        lastVisibleGameTime = 0L;
        if (spellId.isEmpty()) {
            clear();
        }
    }

    public static void clear() {
        castMode = "";
        spellId = "";
        startedGameTime = 0L;
        requiredCastTicks = 0;
        chargeBaselineTicks = 0;
        baseManaCost = 0;
        hasSyncedCastState = false;
        hasConfirmedActiveUseState = false;
        lastVisibleGameTime = 0L;
    }

    public static boolean hasPendingCast(@Nullable LocalPlayer player) {
        return resolveCastBarState(player).visible();
    }

    public static CastBarRenderState resolveCastBarState(@Nullable LocalPlayer player) {
        if (shouldClearImmediately(player)) {
            clear();
            return HIDDEN_CAST_BAR;
        }
        if (!canRenderFor(player)) {
            return HIDDEN_CAST_BAR;
        }
        if (player == null) {
            return HIDDEN_CAST_BAR;
        }

        var currentGameTime = player.level().getGameTime();
        var activelyUsing = isActivelyUsingFocusStaffbow(player);
        if (activelyUsing) {
            hasConfirmedActiveUseState = true;
            lastVisibleGameTime = currentGameTime;
        } else if (hasConfirmedActiveUseState && !isWithinVisibleGrace(currentGameTime)) {
            clear();
            return HIDDEN_CAST_BAR;
        }

        // ワールド生成直後は use 状態同期より cast state 同期が先に届くことがあるため、
        // 初回表示は main hand に FocusStaffbow を持っている限り許可し、1フレームで捨てない。
        long elapsedTicks = Math.max(0L, currentGameTime - startedGameTime);
        if (isContinuous()) {
            var appliedMultiplier = FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(elapsedTicks);
            return new CastBarRenderState(
                    true,
                    FocusStaffbowChargeLogic.computeContinuousChargeProgress(elapsedTicks),
                    String.format(Locale.ROOT, "x%.1f", appliedMultiplier),
                    "",
                    0xFFFFFF,
                    0xFFFFFF
            );
        }

        if (requiredCastTicks > 0 && elapsedTicks < requiredCastTicks) {
            float completion = Mth.clamp(elapsedTicks / (float) requiredCastTicks, 0.0F, 1.0F);
            var remainingLabel = Utils.timeFromTicks(Math.max(0.0F, requiredCastTicks - elapsedTicks), 1);
            return new CastBarRenderState(true, completion, remainingLabel, "", 0xFFFFFF, 0xFFFFFF);
        }

        var appliedMultiplier = FocusStaffbowChargeLogic.computePendingChargeMultiplier(elapsedTicks, chargeBaselineTicks);

        var plannedManaCost = resolveDisplayedManaCost(player, appliedMultiplier);
        var currentMana = resolveCurrentMana(player);
        var manaLabelColor = plannedManaCost > currentMana + MANA_UI_SAFE_MARGIN ? LABEL_COLOR_RED : LABEL_COLOR_CYAN;
        return new CastBarRenderState(
                true,
                1.0F,
                String.format(Locale.ROOT, "x%.1f", appliedMultiplier),
                String.format(Locale.ROOT, " (%d)", plannedManaCost),
                0xFFFFFF,
                manaLabelColor
        );
    }

    private static int resolveDisplayedManaCost(LocalPlayer player, double appliedMultiplier) {
        if (player.getAbilities().instabuild) {
            return 0;
        }

        return FocusStaffbowChargeLogic.computeScaledManaCost(baseManaCost, appliedMultiplier);
    }

    private static float resolveCurrentMana(LocalPlayer player) {
        return Math.max(0, ClientMagicData.getPlayerMana());
    }

    private static boolean isContinuous() {
        return "continuous".equals(castMode);
    }

    private static boolean shouldClearImmediately(@Nullable LocalPlayer player) {
        return !hasCastState()
                || player == null
                || !player.isAlive()
                || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof FocusStaffbow);
    }

    private static boolean canRenderFor(LocalPlayer player) {
        return hasCastState() && player.getMainHandItem().getItem() instanceof FocusStaffbow;
    }

    private static boolean isActivelyUsingFocusStaffbow(LocalPlayer player) {
        return player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && ItemStack.isSameItemSameComponents(player.getUseItem(), player.getMainHandItem());
    }

    private static boolean isWithinVisibleGrace(long currentGameTime) {
        return currentGameTime - lastVisibleGameTime <= STALE_VISIBLE_STATE_TICKS;
    }

    private static boolean hasCastState() {
        return hasSyncedCastState && !spellId.isEmpty();
    }

    public record CastBarRenderState(
            boolean visible,
            float completionPercent,
            String primaryLabelText,
            String secondaryLabelText,
            int primaryLabelColor,
            int secondaryLabelColor
    ) {
    }
}
