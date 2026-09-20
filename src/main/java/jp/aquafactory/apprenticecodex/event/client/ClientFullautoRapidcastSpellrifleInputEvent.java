package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientFullautoRapidcastSpellrifleCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientFullautoRapidcastSpellrifleInputEvent {
    private static final float CLIENT_MANA_SAFE_MARGIN = 0.0001F;
    private static boolean manaShortageLocked;
    private static boolean initialAttackLocked;

    private ClientFullautoRapidcastSpellrifleInputEvent() {
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
        trySendInitialSpecialCast(minecraft);
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

        if (!(player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle)) {
            clearInputLocks();
            return;
        }

        if (isEpicFightBattleMode()) {
            // Staffrifle は近接武器ではないため、Epic Fight の基本攻撃モーションへ渡さず射撃詠唱へ差し替える。
            // 攻撃キー長押しは client tick 側で、構えの有無によらず連射として処理する。
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.isAttack()) {
                trySendInitialSpecialCast(minecraft);
            }
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isUseItem()) {
            return;
        }

        trySendInitialSpecialCast(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FullautoRapidcastSpellrifleClientAdsState.syncToServer();
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || player.isSpectator()) {
            clearInputLocks();
            return;
        }

        if (!(player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle)) {
            clearInputLocks();
            return;
        }

        var attackHeld = minecraft.options.keyAttack.isDown()
                || (isEpicFightBattleMode() && EpicFightClientCompat.isAttackActive());
        if (!attackHeld) {
            initialAttackLocked = false;
            clearManaShortageLock();
            return;
        }

        if (manaShortageLocked) {
            return;
        }

        if (shouldLockManaShortage(player)) {
            sendSpecialCast(minecraft, FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(player));
            manaShortageLocked = true;
            return;
        }

        sendSpecialCast(minecraft, FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(player));
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (!FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(event.getPlayer())) {
            return;
        }

        event.setNewFovModifier(event.getFovModifier() * FullautoRapidcastSpellrifle.getAdsFovModifier());
    }

    public static void sendSpecialCast(Minecraft minecraft, boolean adsFullAuto) {
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        FullautoRapidcastSpellrifleClientFireEffectState.applyCameraRecoil();
        // 描画フレームで動いた視線を、照準取得と射撃要求より先に server へ送る。
        player.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround()));
        ClientFullautoRapidcastSpellrifleCastContext.beginPending(player.getUUID(), player.getMainHandItem());
        var spellData = resolveSelectedSpellData(player);
        // 内部適用の Transcendence は汎用レベルイベントで加算されないため、照準にも射撃と同じ基礎レベルを渡す。
        if (spellData != SpellData.EMPTY) {
            spellData = new SpellData(spellData.getSpell(),
                    FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(player.getMainHandItem(), spellData));
        }
        var targetData = ClientBlockTargetSyncService.captureForEmbeddedCast(spellData);
        Networks.sendToServer(new ClientFullautoRapidcastSpellrifleCastPacket(adsFullAuto, targetData));
    }

    public static void trySendInitialSpecialCast(Minecraft minecraft) {
        if (initialAttackLocked || manaShortageLocked) {
            return;
        }

        initialAttackLocked = true;
        if (minecraft.player != null && shouldLockManaShortage(minecraft.player)) {
            manaShortageLocked = true;
        }
        sendSpecialCast(minecraft, FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(minecraft.player));
    }

    private static boolean shouldLockManaShortage(LocalPlayer player) {
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

        var spellLevel = spell.getLevelFor(FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(player.getMainHandItem(), spellData), player);
        return ClientMagicData.getPlayerMana() + CLIENT_MANA_SAFE_MARGIN < spell.getManaCost(spellLevel);
    }

    private static SpellData resolveSelectedSpellData(LocalPlayer player) {
        return FullautoRapidcastSpellrifle.getSelectedSpellData(player.getMainHandItem(), player.level().registryAccess());
    }

    private static void clearManaShortageLock() {
        manaShortageLocked = false;
    }

    private static void clearInputLocks() {
        manaShortageLocked = false;
        initialAttackLocked = false;
    }

    private static boolean shouldReplaceEpicFightAttackInput(Minecraft minecraft, InputConstants.Type type, int value) {
        var player = minecraft.player;
        return minecraft.screen == null
                && player != null
                && !player.isSpectator()
                && player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle
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
