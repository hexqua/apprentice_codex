package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientMultipurposeStaffrifleInputEvent {
    private static final float CLIENT_MANA_SAFE_MARGIN = 0.0001F;
    private static boolean adsManaShortageLocked;
    private static boolean nonAdsAttackLocked;

    private ClientMultipurposeStaffrifleInputEvent() {
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

        if (!MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)) {
            clearAdsManaShortageLock();
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
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
        Networks.sendToServer(new ClientMultipurposeStaffrifleCastPacket(adsFullAuto));
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
}
