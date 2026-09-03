package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaManeuverGearJumpPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaManeuverGearManager {
    private static final Map<UUID, Long> LAST_WALL_JUMP_GAME_TIME = new HashMap<>();

    private ManaManeuverGearManager() {
    }

    static void onCurioTick(SlotContext slotContext) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!isPrimaryEquippedCurio(slotContext)) {
            return;
        }

        tickWallSlide(player);
    }

    public static boolean tryWallJump(ServerPlayer player) {
        if (player.isSpectator() || player.onGround() || isWallMovementBlocked(player) || !isEquipped(player)) {
            return false;
        }
        if (!ManaManeuverGearMovement.isTouchingWall(player)) {
            return false;
        }

        var gameTime = player.level().getGameTime();
        if (LAST_WALL_JUMP_GAME_TIME.getOrDefault(player.getUUID(), Long.MIN_VALUE) == gameTime) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return false;
        }
        var manaCost = Math.max(0, ApprenticeCodexServerConfig.manaManeuverGearManaCostPerJump());
        if (magicData.getMana() + 1.0e-4F < manaCost) {
            sendActionBar(player, Component.translatable(
                    "ui.apprenticecodex.mana_maneuver_gear.insufficient_mana"
            ).withStyle(ChatFormatting.RED));
            return false;
        }

        var impulse = ManaManeuverGearMovement.wallJumpImpulse(player.getLookAngle());
        ManaManeuverGearMovement.applyWallJump(player, impulse);
        LAST_WALL_JUMP_GAME_TIME.put(player.getUUID(), gameTime);

        if (manaCost > 0) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            syncMana(player, magicData);
        }
        if (!(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, new SyncManaManeuverGearJumpPacket(impulse));
        }
        return true;
    }

    public static boolean tickWallSlide(ServerPlayer player) {
        if (player.onGround()
                || isWallMovementBlocked(player)
                || !isEquipped(player)
                || !ManaManeuverGearMovement.isTouchingWall(player)
                || player.getDeltaMovement().y >= 0.0D) {
            return false;
        }

        ManaManeuverGearMovement.applyWallSlide(player);
        return true;
    }

    public static boolean isWallMovementBlocked(ServerPlayer player) {
        return player.onClimbable() || player.level().getBlockState(player.blockPosition()).is(Blocks.SCAFFOLDING);
    }

    public static boolean isEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(stack -> stack.getItem() instanceof ManaManeuverGear))
                .orElse(false);
    }

    public static void clear(ServerPlayer player) {
        LAST_WALL_JUMP_GAME_TIME.remove(player.getUUID());
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof ManaManeuverGear))
                .map(slotResult -> slotResult.slotContext().index() == slotContext.index()
                        && slotResult.slotContext().identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    private static void syncMana(ServerPlayer player, MagicData magicData) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }

    private static void sendActionBar(ServerPlayer player, Component message) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }
}
