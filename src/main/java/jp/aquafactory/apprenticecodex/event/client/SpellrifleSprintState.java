package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.renderer.item.SpellrifleCastAnimationVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SpellrifleSprintState {
    private static final int SHOT_HOLD_TICKS = 30;
    private static final float BLEND_PER_TICK = 0.25F;
    private static LocalPlayer owner;
    private static ClientLevel level;
    private static ItemStack held = ItemStack.EMPTY;
    private static int slot = -1;
    private static long holdUntil;
    private static float previous;
    private static float progress;

    private SpellrifleSprintState() {
    }

    public static boolean isAiming(LocalPlayer player) {
        return MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)
                || FullautoRapidcastSpellrifleClientAdsState.isLocalAdsKeyHeld(player);
    }

    private static void refreshContext() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var stack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        int selected = player == null ? -1 : player.getInventory().selected;
        // 同じスロットのstackは弾薬・付与データの同期でも置換されるため、参照比較で射撃猶予を消さない。
        if (owner != player || level != minecraft.level || held.getItem() != stack.getItem() || slot != selected) {
            owner = player;
            level = minecraft.level;
            slot = selected;
            holdUntil = 0;
            previous = progress = 0;
        }
        held = stack;
    }

    public static void onShot() {
        refreshContext();
        if (level != null) {
            holdUntil = level.getGameTime() + SHOT_HOLD_TICKS;
        }
        // 射撃時は即座に構え直す。横構えのままマズルだけが前方へ出るフレームを避ける。
        previous = progress = 0;
    }

    private static boolean mustUseReadyPose(float partialTick) {
        var minecraft = Minecraft.getInstance();
        return owner == null || level == null || !owner.isAlive() || owner.isSpectator()
                || minecraft.screen != null || isAiming(owner)
                || minecraft.options.keyAttack.isDown() || owner.isUsingItem()
                || level.getGameTime() < holdUntil
                || SpellrifleCastAnimationVisibility.ownsArms(owner, partialTick);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        refreshContext();
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        if (!(held.getItem() instanceof MultipurposeStaffrifle)
                && !(held.getItem() instanceof FullautoRapidcastSpellrifle)) {
            previous = progress = 0;
            return;
        }
        if (mustUseReadyPose(0)) {
            previous = progress = 0;
            return;
        }
        previous = progress;
        progress = Mth.clamp(progress + (owner.isSprinting() ? BLEND_PER_TICK : -BLEND_PER_TICK), 0, 1);
    }

    public static float amount(float partialTick) {
        refreshContext();
        if (mustUseReadyPose(partialTick)) {
            return 0;
        }
        float value = Mth.lerp(partialTick, previous, progress);
        return value * value * (3 - 2 * value);
    }
}
