package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientMultipurposeStaffrifleInputEvent {
    private static boolean attackLocked;

    private ClientMultipurposeStaffrifleInputEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (!shouldReplaceEpicFightAttackInput(minecraft, InputConstants.Type.MOUSE, event.getButton())) {
            return;
        }

        // Epic Fight の攻撃入力は Forge の InteractionKeyMappingTriggered を経由しないことがある。
        // マウス押下時点で止め、近接の片手攻撃モーションへ入る前に射撃詠唱へ差し替える。
        event.setCanceled(true);
        trySendSingleShot(minecraft);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || !player.isAlive() || player.isSpectator()) {
            clearInputLocks();
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            clearInputLocks();
            return;
        }

        if (isEpicFightBattleMode()) {
            // Staffrifle は近接武器ではないため、Epic Fight の基本攻撃モーションへ渡さず射撃詠唱へ差し替える。
            // 戦闘モードの右クリックは射撃・ADSへ割り当てない。
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.isAttack()) {
                trySendSingleShot(minecraft);
            }
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isUseItem()) {
            return;
        }

        if (!MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)
                && ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(player)) {
            return;
        }

        trySendSingleShot(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || !player.isAlive() || player.isSpectator()) {
            clearInputLocks();
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            clearInputLocks();
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
            attackLocked = false;
        }
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        var minecraft = Minecraft.getInstance();
        if (!(event.getPlayer() instanceof LocalPlayer player)
                || !MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)
                || MultipurposeStaffrifleClientAdsState.isScoped(player)) {
            return;
        }
        // 設定適用済みのFOVへADS倍率を加え、開始・解除の補間はバニラへ任せる。
        var adsModifier = (float) Mth.lerp(minecraft.options.fovEffectScale().get(),
                1.0, MultipurposeStaffrifle.getAdsFovModifier());
        event.setNewFovModifier(event.getNewFovModifier() * adsModifier);
    }

    public static void sendSpecialCast(Minecraft minecraft, boolean aiming) {
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        ClientMultipurposeStaffrifleCastContext.beginPending(player.getUUID(), player.getMainHandItem());
        var targetData = ClientBlockTargetSyncService.captureForEmbeddedCast(resolveSelectedSpellData(player));
        Networks.sendToServer(new ClientMultipurposeStaffrifleCastPacket(aiming, targetData));
    }

    public static void trySendSingleShot(Minecraft minecraft) {
        if (attackLocked) {
            return;
        }

        attackLocked = true;
        sendSpecialCast(minecraft, MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(minecraft.player));
    }

    private static SpellData resolveSelectedSpellData(LocalPlayer player) {
        return MultipurposeStaffrifle.resolveCastSpellData(player, player.getMainHandItem());
    }

    private static void clearInputLocks() {
        attackLocked = false;
    }

    private static boolean shouldReplaceEpicFightAttackInput(Minecraft minecraft, InputConstants.Type type, int value) {
        var player = minecraft.player;
        return minecraft.screen == null
                && player != null
                && !player.isSpectator()
                && player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle
                && isEpicFightBattleMode()
                && (EpicFightClientCompat.matchesAttackInput(type, value)
                        || matchesVanillaAttackInput(minecraft, type, value));
    }

    private static boolean matchesVanillaAttackInput(Minecraft minecraft, InputConstants.Type type, int value) {
        var attackKey = minecraft.options.keyAttack.getKey();
        return attackKey.getType() == type && attackKey.getValue() == value;
    }

    private static boolean isEpicFightBattleMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)
                && EpicFightClientCompat.isBattleMode();
    }
}
