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
    public static final KeyMapping CAST = new KeyMapping("key.apprenticecodex.quickcast_cartridge",
            InputConstants.UNKNOWN.getValue(), "key.categories.apprenticecodex");

    private QuickcastCartridgeClientEvents() {}

    public static net.minecraft.network.chat.Component getCastKeyDescription() {
        return CAST.isUnbound()
                ? net.minecraft.network.chat.Component.translatable("item.apprenticecodex.quickcast_scroll_cartridge.no_assign")
                : net.minecraft.network.chat.Component.translatable("item.apprenticecodex.quickcast_scroll_cartridge.desc_1",
                        CAST.getTranslatedKeyMessage());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        while (CAST.consumeClick()) {
            var player = minecraft.player;
            if (player == null || minecraft.screen != null || player.isSpectator()
                    || FocusStaffbowClientCastState.hasPendingCast(player)) continue;
            if (MirageAvoidanceClientController.isActive()) {
                MirageAvoidanceClientController.showDuringEffectMessage();
                continue;
            }
            var stack = QuickcastCartridgeCasting.findEquipped(player);
            if (stack.isEmpty()) continue;
            var spell = QuickcastScrollCartridge.getSelectedSpellData(stack);
            if (spell == SpellData.EMPTY) continue;
            var input = MirageAvoidanceClientController.captureCurrentInput();
            Networks.sendToServer(new ClientQuickcastCartridgePacket(spell.getSpell().getSpellResource(),
                    ClientBlockTargetSyncService.captureForEmbeddedCast(spell), input.forward(), input.strafe()));
        }
    }

    @EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) { event.register(CAST); }
    }
}
