package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
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
    private static final float CLIENT_MANA_SAFE_MARGIN = 0.0001F;
    private static boolean adsManaShortageLocked;
    private static boolean nonAdsAttackLocked;

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
        trySendNonAdsSpecialCast(minecraft);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()) {
            clearInputLocks();
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            clearInputLocks();
            return;
        }

        if (isEpicFightBattleMode()) {
            // Staffrifle は近接武器ではないため、Epic Fight の基本攻撃モーションへ渡さず射撃詠唱へ差し替える。
            // 右クリック長押しは client tick 側でフルオートとして処理する。
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.isAttack()) {
                trySendNonAdsSpecialCast(minecraft);
            }
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isUseItem()) {
            return;
        }

        if (MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)) {
            return;
        }

        if (ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(player)) {
            return;
        }

        trySendNonAdsSpecialCast(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()) {
            clearInputLocks();
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            clearInputLocks();
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
            nonAdsAttackLocked = false;
        }

        var epicFightBattleFullAuto = isEpicFightBattleFullAuto(player);
        if (!MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player) && !epicFightBattleFullAuto) {
            clearAdsManaShortageLock();
            return;
        }

        if (!epicFightBattleFullAuto && !minecraft.options.keyAttack.isDown()) {
            return;
        }

        if (adsManaShortageLocked) {
            return;
        }

        if (shouldLockAdsManaShortage(player)) {
            sendSpecialCast(minecraft, true);
            adsManaShortageLocked = true;
            return;
        }

        sendSpecialCast(minecraft, true);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (!MultipurposeStaffrifleClientAdsState.shouldHandleAsAds(event.getPlayer())) {
            return;
        }

        event.setNewFovModifier(event.getFovModifier() * MultipurposeStaffrifle.getAdsFovModifier());
    }

    public static void sendSpecialCast(Minecraft minecraft, boolean adsFullAuto) {
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        ClientMultipurposeStaffrifleCastContext.beginPending(player.getUUID(), player.getMainHandItem());
        var targetData = ClientBlockTargetSyncService.captureForEmbeddedCast(resolveSelectedSpellData(player));
        Networks.sendToServer(new ClientMultipurposeStaffrifleCastPacket(adsFullAuto, targetData));
    }

    public static void trySendNonAdsSpecialCast(Minecraft minecraft) {
        if (nonAdsAttackLocked) {
            return;
        }

        nonAdsAttackLocked = true;
        sendSpecialCast(minecraft, false);
    }

    private static boolean shouldLockAdsManaShortage(LocalPlayer player) {
        if (player.getAbilities().instabuild) {
            return false;
        }

        var spellData = resolveSelectedSpellData(player);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == SpellRegistry.none()) {
            return false;
        }

        var spell = spellData.getSpell();
        if (ClientMagicData.getRecasts().hasRecastForSpell(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        return ClientMagicData.getPlayerMana() + CLIENT_MANA_SAFE_MARGIN < spell.getManaCost(spellLevel);
    }

    private static SpellData resolveSelectedSpellData(LocalPlayer player) {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            selectionManager = new SpellSelectionManager(player);
        }

        var selection = selectionManager.getSelection();
        return selection != null ? selection.spellData : SpellData.EMPTY;
    }

    private static void clearAdsManaShortageLock() {
        adsManaShortageLocked = false;
    }

    private static void clearInputLocks() {
        adsManaShortageLocked = false;
        nonAdsAttackLocked = false;
    }

    private static boolean isEpicFightBattleFullAuto(LocalPlayer player) {
        var minecraft = Minecraft.getInstance();
        return player != null
                && minecraft.options.keyUse.isDown()
                && player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle
                && isEpicFightBattleMode();
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
