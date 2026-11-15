package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonConfigState;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonInputState;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.MagiEngineersLegExoskeleton;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientExoskeletonJumpPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.stln.magitech.content.event.KeyMappingEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ExoskeletonClientEvents {
    private static final ExoskeletonInputState STATE = new ExoskeletonInputState();
    private static LocalPlayer previousPlayer;
    private static Level previousLevel;
    private static boolean groundedBeforeTick;
    private static boolean previousLifterDown;
    private static boolean previousStriderDown;

    private ExoskeletonClientEvents() {}

    @SubscribeEvent
    public static void beforeTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        if (previousPlayer != minecraft.player || previousLevel != minecraft.level) {
            reset();
            previousPlayer = minecraft.player;
            previousLevel = minecraft.level;
        }
        groundedBeforeTick = minecraft.player != null && minecraft.player.onGround();
    }

    @SubscribeEvent
    public static void afterTick(ClientTickEvent.Post event) {
        if (!MagiEngineersLegExoskeleton.available()) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        // consumeClickは上流の靴も使うため、独立した押下端で検出し併発を許す。
        boolean lifterDown = KeyMappingEvent.DOUBLE_JUMP.get().isDown();
        boolean striderDown = KeyMappingEvent.LONG_JUMP.get().isDown();
        boolean lifterPressed = lifterDown && !previousLifterDown;
        boolean striderPressed = striderDown && !previousStriderDown;
        previousLifterDown = lifterDown;
        previousStriderDown = striderDown;
        if (player == null || minecraft.screen != null) return;
        boolean grounded = groundedBeforeTick || player.onGround();
        if (ExoskeletonMovement.disabled(player) || !ExoskeletonMovement.equipped(player)) return;
        var action = ExoskeletonAction.select(grounded, player.isSprinting(), lifterPressed, striderPressed);
        if (action == null) return;
        if (action.isStrider() && player.getCooldowns().isOnCooldown(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get())) return;
        int cost = action.manaCost(ExoskeletonConfigState.lifterManaCost(), ExoskeletonConfigState.striderManaCost());
        if (STATE.availableMana(ClientMagicData.getPlayerMana()) < cost) return;
        long sequence = STATE.reserve(cost);
        ExoskeletonMovement.apply(player, action);
        Networks.sendToServer(new ClientExoskeletonJumpPacket(sequence, action));
    }

    public static void acknowledge(long sequence, int mana) {
        if (STATE.acknowledge(sequence)) ClientMagicData.setMana(mana);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        ExoskeletonConfigState.reset();
    }

    private static void reset() {
        STATE.reset();
        previousLifterDown = false;
        previousStriderDown = false;
        groundedBeforeTick = false;
        previousPlayer = null;
        previousLevel = null;
    }
}
