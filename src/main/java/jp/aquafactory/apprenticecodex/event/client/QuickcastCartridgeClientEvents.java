package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientQuickcastCartridgePacket;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceClientController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class QuickcastCartridgeClientEvents {
    private static final java.util.Set<KeyMapping> HELD = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static int lastSentTick = -1;
    private static boolean awaitingRelease;
    public static final KeyMapping CAST = new KeyMapping("key.apprenticecodex.quickcast_cartridge",
            InputConstants.UNKNOWN.getValue(), "key.categories.apprenticecodex");

    private QuickcastCartridgeClientEvents() {}

    public static java.util.Optional<net.minecraft.network.chat.Component> getCastKeyDescription() {
        return CAST.isUnbound()
                ? java.util.Optional.empty()
                : java.util.Optional.of(net.minecraft.network.chat.Component.translatable(
                        "item.apprenticecodex.quickcast_scroll_cartridge.cast_key", CAST.getTranslatedKeyMessage()));
    }

    @SubscribeEvent
    public static void beforeTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        HELD.removeIf(key -> !key.isDown());
        if (!CAST.isDown() && !io.redspace.ironsspellbooks.player.KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP.isDown()
                && io.redspace.ironsspellbooks.player.KeyMappings.QUICK_CAST_MAPPINGS.stream().noneMatch(KeyMapping::isDown)) {
            awaitingRelease = false;
        }
        if (minecraft.player == null) {
            QuickcastCartridgeClientState.reset();
            HELD.clear();
            lastSentTick = -1;
        } else if (!minecraft.isPaused()) QuickcastCartridgeClientState.tick();
        if (minecraft.screen != null) {
            HELD.add(CAST);
            HELD.add(io.redspace.ironsspellbooks.player.KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP);
            HELD.addAll(io.redspace.ironsspellbooks.player.KeyMappings.QUICK_CAST_MAPPINGS);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        while (CAST.consumeClick()) sendCast(CAST);
    }

    public static void sendCast(KeyMapping key) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!HELD.add(key)) return;
        if (player == null || minecraft.screen != null || player.isSpectator() || awaitingRelease
                || lastSentTick == player.tickCount || FocusStaffbowClientCastState.hasPendingCast(player)) return;
        if (MirageAvoidanceClientController.isActive()) {
            MirageAvoidanceClientController.showDuringEffectMessage();
            return;
        }
        var stack = QuickcastCartridgeCasting.findEquipped(player);
        if (stack.isEmpty()) return;
        var spell = QuickcastScrollCartridge.getSelectedSpellData(stack);
        if (spell == SpellData.EMPTY) return;
        lastSentTick = player.tickCount;
        awaitingRelease = true;
        QuickcastCartridgeClientState.interrupt();
        var input = MirageAvoidanceClientController.captureCurrentInput();
        Networks.sendToServer(new ClientQuickcastCartridgePacket(spell.getSpell().getSpellResource(),
                ClientBlockTargetSyncService.captureForEmbeddedCast(spell), input.forward(), input.strafe()));
    }

    public static boolean sendSelectedCast(int quickSlot) {
        var manager = io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager();
        if (manager == null) return false;
        var option = quickSlot < 0 ? manager.getSelection() : manager.getSpellSlot(quickSlot);
        if (option == null || !QuickcastCartridgeCasting.SLOT.equals(option.slot)) return false;
        var keys = io.redspace.ironsspellbooks.player.KeyMappings.QUICK_CAST_MAPPINGS;
        sendCast(quickSlot < 0 ? io.redspace.ironsspellbooks.player.KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP
                : keys.get(quickSlot));
        return true;
    }

    @SubscribeEvent
    public static void onMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!QuickcastCartridgeClientState.reloading()) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onInteraction(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() || event.isUseItem()) QuickcastCartridgeClientState.interrupt();
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onMouse(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && (minecraft.options.keyAttack.matchesMouse(event.getButton()) || minecraft.options.keyUse.matchesMouse(event.getButton()))) {
            // 戦闘MODが通常の攻撃イベントを差し替えていても、空振りを含む操作で中断する。
            QuickcastCartridgeClientState.interrupt();
        }
    }

    @SubscribeEvent
    public static void onKey(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && (minecraft.options.keyAttack.matches(event.getKey(), event.getScanCode())
                || minecraft.options.keyUse.matches(event.getKey(), event.getScanCode()))) {
            QuickcastCartridgeClientState.interrupt();
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        QuickcastCartridgeClientState.reset();
        HELD.clear();
        lastSentTick = -1;
        awaitingRelease = false;
    }

    @EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) { event.register(CAST); }
    }
}
