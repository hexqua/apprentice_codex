package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsCharge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientPsychicDriveGauntletsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PsychicDriveGauntletsUseInput {
    private static boolean held;
    private static boolean opportunity;
    private static boolean attempted;
    private static ClientLevel level;
    private static ItemStack source = ItemStack.EMPTY;
    private static InteractionHand sourceHand;

    private PsychicDriveGauntletsUseInput() {}

    public static void cancel() {
        var player = Minecraft.getInstance().player;
        if (player == null || source.isEmpty()) return;
        Networks.sendToServer(new ClientPsychicDriveGauntletsPacket(Input.CANCEL_USE));
        if (PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())) player.stopUsingItem();
        source = ItemStack.EMPTY;
        opportunity = false;
    }

    public static void rejectUse() {
        var player = Minecraft.getInstance().player;
        if (player != null && PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())) player.stopUsingItem();
        source = ItemStack.EMPTY;
        opportunity = false;
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && !held) {
            held = true;
            opportunity = true;
        }
    }

    public static InteractionResultHolder<ItemStack> use(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!PsychicDriveGauntletsSelection.duplicate(player) && !PsychicDriveGauntletsSelection.canUse(player, hand)) {
            return InteractionResultHolder.pass(stack);
        }
        attempted = true;
        if (!opportunity) return InteractionResultHolder.consume(stack);
        opportunity = false;
        if (PsychicDriveGauntletsSelection.duplicate(player)) return InteractionResultHolder.consume(stack);
        source = stack;
        sourceHand = hand;
        var right = PsychicDriveGauntletsClientState.get(player.getUUID(), true, 0);
        var left = PsychicDriveGauntletsClientState.get(player.getUUID(), false, 0);
        if ((right == null || right.phase() == Phase.IDLE) || (left == null || left.phase() == Phase.IDLE)) {
            player.startUsingItem(hand);
        }
        return InteractionResultHolder.consume(stack);
    }

    public static boolean blockRepeatedUse(Player player, InteractionHand hand) {
        // Item.use内の拒否ではC2S使用packetが既に送られている。通常interactionの後、送信前に止める。
        return !opportunity && PsychicDriveGauntletsSelection.isGauntlets(player.getItemInHand(hand))
                && (PsychicDriveGauntletsSelection.duplicate(player) || PsychicDriveGauntletsSelection.canUse(player, hand));
    }

    @SubscribeEvent
    public static void validate(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        if (level != minecraft.level) {
            held = false;
            opportunity = false;
            attempted = false;
            source = ItemStack.EMPTY;
            level = minecraft.level;
        }
        var player = minecraft.player;
        if (player != null && !source.isEmpty() && (minecraft.screen != null || !minecraft.isWindowActive()
                || !PsychicDriveGauntletsSelection.canUse(player, sourceHand)
                || player.getItemInHand(sourceHand) != source)) {
            cancel();
        }
        if (minecraft.options.keyUse.isDown() && !held) {
            held = true;
            opportunity = player != null && minecraft.screen == null && minecraft.isWindowActive();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (!minecraft.options.keyUse.isDown()) {
            if (attempted && player != null) Networks.sendToServer(new ClientPsychicDriveGauntletsPacket(Input.RESET_USE));
            held = false;
            attempted = false;
            source = ItemStack.EMPTY;
        }
        // 通常操作が終わってからの自動再試行を、新しい押下開始として扱わない。
        opportunity = false;
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (!player.isUsingItem() || !PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())
                || PsychicDriveGauntletsSelection.duplicate(player)) return;
        var amount = PsychicDriveGauntletsCharge.amount(player.getTicksUsingItem());
        var factor = Mth.lerp(amount, 1, PsychicDriveGauntletsCharge.FOV_FACTOR);
        var scale = Minecraft.getInstance().options.fovEffectScale().get();
        event.setNewFovModifier(event.getNewFovModifier() * (float) Mth.lerp(scale, 1, factor));
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!PsychicDriveGauntletsSelection.isGauntlets(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.rule")
                .withStyle(ChatFormatting.GRAY));
        if (event.getEntity() != null && PsychicDriveGauntletsSelection.duplicate(event.getEntity())) {
            event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.error")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
