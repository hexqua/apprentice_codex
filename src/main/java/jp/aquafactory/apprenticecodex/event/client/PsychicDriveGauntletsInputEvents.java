package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientPsychicDriveGauntletsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PsychicDriveGauntletsInputEvents {
    private static boolean pressed;
    private static boolean blockedUntilRelease;
    private static long nextRepeat;
    private static ItemStack heldStack = ItemStack.EMPTY;
    private static int heldSlot;
    private static ClientLevel inputLevel;
    private static final Set<InputConstants.Key> PHYSICAL_KEYS = new HashSet<>();
    private PsychicDriveGauntletsInputEvents() {}

    private static boolean eligible(Minecraft minecraft) {
        var player = minecraft.player;
        return minecraft.screen == null && minecraft.level != null && minecraft.isWindowActive() && player != null
                && player.isAlive() && !player.isSpectator()
                && PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem());
    }
    private static boolean epicMode() {
        return ModList.get().isLoaded(EpicFightClientCompat.MOD_ID) && EpicFightClientCompat.isBattleMode();
    }
    private static boolean matches(InputConstants.Type type, int key) {
        var minecraft = Minecraft.getInstance();
        var attack = minecraft.options.keyAttack.getKey();
        return (attack.getType() == type && attack.getValue() == key)
                || (epicMode() && EpicFightClientCompat.matchesAttackInput(type, key));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (!matches(InputConstants.Type.MOUSE, event.getButton())) return;
        physical(InputConstants.Type.MOUSE, event.getButton(), event.getAction());
        if (event.getAction() == GLFW.GLFW_PRESS && eligible(Minecraft.getInstance()) && epicMode()) {
            // Epic Fightの独自入力が近接コンボを始める前に、共通の発射入力へ合流する。
            event.setCanceled(true);
            press();
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void key(InputEvent.Key event) {
        if (!matches(InputConstants.Type.KEYSYM, event.getKey())) return;
        physical(InputConstants.Type.KEYSYM, event.getKey(), event.getAction());
        if (event.getAction() == GLFW.GLFW_PRESS && eligible(Minecraft.getInstance())) press();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && eligible(Minecraft.getInstance())) {
            event.setCanceled(true);
            event.setSwingHand(false);
            press();
        }
    }
    public static void press() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !eligible(minecraft) || pressed || blockedUntilRelease) return;
        pressed = true;
        heldStack = player.getMainHandItem();
        heldSlot = player.getInventory().selected;
        nextRepeat = level.getGameTime() + PsychicDriveGauntletsAttackState.REPEAT_TICKS;
        send(Input.PRESS);
    }
    private static void physical(InputConstants.Type type, int value, int action) {
        var key = type.getOrCreate(value);
        if (action == GLFW.GLFW_PRESS) PHYSICAL_KEYS.add(key);
        if (action == GLFW.GLFW_RELEASE) {
            PHYSICAL_KEYS.remove(key);
            if (PHYSICAL_KEYS.isEmpty()) release();
        }
    }
    private static void send(Input input) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (input != Input.RELEASE) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround()));
        }
        Networks.sendToServer(new ClientPsychicDriveGauntletsPacket(input));
    }
    private static void release() {
        if (pressed) send(Input.RELEASE);
        pressed = false;
        blockedUntilRelease = false;
        heldStack = ItemStack.EMPTY;
    }
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        var window = minecraft.getWindow().getWindow();
        // フォーカス喪失中にrelease eventを取り逃しても、物理キー状態から保持を解除する。
        PHYSICAL_KEYS.removeIf(key -> key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) != GLFW.GLFW_PRESS
                : !InputConstants.isKeyDown(window, key.getValue()));
        if (inputLevel != minecraft.level) {
            pressed = false;
            blockedUntilRelease = false;
            heldStack = ItemStack.EMPTY;
            inputLevel = minecraft.level;
        }
        boolean down = !PHYSICAL_KEYS.isEmpty() || minecraft.options.keyAttack.isDown()
                || (epicMode() && EpicFightClientCompat.isAttackActive());
        if (!down) {
            release();
            return;
        }
        if (player == null || level == null || !eligible(minecraft)
                || (pressed && (player.getMainHandItem() != heldStack || player.getInventory().selected != heldSlot))) {
            release();
            blockedUntilRelease = true;
            return;
        }
        if (blockedUntilRelease) return;
        if (!pressed) press();
        else if (level.getGameTime() >= nextRepeat) {
            nextRepeat = level.getGameTime() + PsychicDriveGauntletsAttackState.REPEAT_TICKS;
            send(Input.REPEAT);
        }
    }
}
