package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsCharge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsUseSession;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientPsychicDriveGauntletsUsePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PsychicDriveGauntletsUseInput {
    private static boolean held;
    private static boolean opportunity;
    private static boolean claimed;
    private static final PsychicDriveGauntletsUseSession SESSION = new PsychicDriveGauntletsUseSession();
    private static ClientLevel level;
    private static ItemStack source = ItemStack.EMPTY;
    private static InteractionHand sourceHand;

    private PsychicDriveGauntletsUseInput() {}

    public static boolean isCharging() { return SESSION.active(); }

    private static void finish(UseAction action) {
        var player = Minecraft.getInstance().player;
        if (player != null && claimed) {
            if (action == UseAction.RELEASE) {
                player.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround()));
            }
            Networks.sendToServer(new ClientPsychicDriveGauntletsUsePacket(action, sourceHand, SESSION.id()));
        }
        SESSION.end();
        source = ItemStack.EMPTY;
        opportunity = false;
    }

    public static void accept(long session, boolean charging) {
        SESSION.accept(session, charging);
        if (!SESSION.active()) source = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && SESSION.active()) finish(UseAction.CANCEL);
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        if (claimed) {
            // 溜め開始後は、視線先が変わっても保持中の通常interactionを再開しない。
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!held) {
            held = true;
            opportunity = true;
        }
    }

    public static InteractionResultHolder<ItemStack> use(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!PsychicDriveGauntletsSelection.duplicate(player) && !PsychicDriveGauntletsSelection.canUse(player, hand)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!opportunity) return InteractionResultHolder.consume(stack);
        opportunity = false;
        claimed = true;
        source = stack;
        sourceHand = hand;
        long session = SESSION.begin(player.level().getGameTime());
        Networks.sendToServer(new ClientPsychicDriveGauntletsUsePacket(UseAction.START, hand, session));
        return InteractionResultHolder.consume(stack);
    }

    private static void update() {
        var minecraft = Minecraft.getInstance();
        if (level != minecraft.level) {
            // セッション番号はworldを跨いでも戻さず、遅着した応答を次の押下へ適用しない。
            SESSION.end();
            held = minecraft.options.keyUse.isDown();
            opportunity = false;
            claimed = false;
            source = ItemStack.EMPTY;
            level = minecraft.level;
        }
        var player = minecraft.player;
        // GUIによるキー解除を発射と誤認しないよう、無効化をreleaseより先に判定する。
        boolean invalid = player == null || minecraft.screen != null || !minecraft.isWindowActive()
                || minecraft.isPaused() || !player.isAlive() || player.isSpectator();
        if (SESSION.active() && (invalid || player.isUsingItem()
                || !PsychicDriveGauntletsSelection.canUse(player, sourceHand)
                || player.getItemInHand(sourceHand) != source)) finish(UseAction.CANCEL);
        if (!minecraft.options.keyUse.isDown()) {
            if (claimed) finish(SESSION.active() && !invalid ? UseAction.RELEASE : UseAction.CANCEL);
            held = false;
            claimed = false;
            opportunity = false;
        } else if (!held) {
            held = true;
            opportunity = !invalid;
        }
    }

    @SubscribeEvent
    public static void validate(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return; update(); }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        update();
        // 通常操作が消費した押下を、後続tickのItem.useで溜め開始へ転用しない。
        opportunity = false;
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (player != Minecraft.getInstance().player || !SESSION.active()) return;
        var amount = PsychicDriveGauntletsCharge.amount(SESSION.elapsed(player.level().getGameTime()));
        var factor = Mth.lerp(amount, 1, PsychicDriveGauntletsCharge.FOV_FACTOR);
        var scale = Minecraft.getInstance().options.fovEffectScale().get();
        event.setNewFovModifier(event.getNewFovModifier() * (float) Mth.lerp(scale, 1, factor));
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!PsychicDriveGauntletsSelection.isGauntlets(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.rule")
                .withStyle(ChatFormatting.GRAY));
        var options = Minecraft.getInstance().options;
        event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.attack",
                options.keyAttack.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW),
                Component.literal(Integer.toString(ApprenticeCodexServerConfig.psychicDriveGauntletsAttackManaCost()))
                        .withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.charge",
                options.keyUse.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW),
                Component.literal(Integer.toString(ApprenticeCodexServerConfig.psychicDriveGauntletsChargeManaCostPerTick()
                        * PsychicDriveGauntletsCharge.MAX_TICKS)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.charge.notice")
                .withStyle(ChatFormatting.GRAY));
        if (event.getEntity() != null && PsychicDriveGauntletsSelection.duplicate(event.getEntity())) {
            event.getToolTip().add(Component.translatable("item.apprenticecodex.psychic_drive_gauntlets.desc.error")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
