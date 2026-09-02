package jp.aquafactory.apprenticecodex.item.curios.undyingemblem;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncUndyingEmblemStatePacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

public final class UndyingEmblemRuntime {
    public static final int COOLDOWN_TICKS = 20 * 60 * 20;
    public static final int CAST_INTERVAL_TICKS = 10;
    public static final String SPELL_SELECTION_SLOT = "apprenticecodex_undying_emblem";

    private UndyingEmblemRuntime() {
    }

    public static boolean isEquipped(LivingEntity entity) {
        return entity != null && CuriosApi.getCuriosInventory(entity)
                .map(inventory -> !inventory.findCurios(stack -> stack.getItem() instanceof UndyingEmblem).isEmpty())
                .orElse(false);
    }

    public static int getRemainingCooldownTicks(Player player) {
        return Capabilities.getSpellData(player)
                .map(data -> data.get(CodexSpellStateTypeRegister.UNDYING_EMBLEM_STATE).getRemainingCooldownTicks())
                .orElse(0);
    }

    public static boolean isOnCooldown(Player player) {
        return getRemainingCooldownTicks(player) > 0;
    }

    public static void startCooldown(ServerPlayer player) {
        setRemainingCooldownTicks(player, COOLDOWN_TICKS, true);
    }

    public static void advanceCooldown(ServerPlayer player, int ticks) {
        if (ticks <= 0) {
            return;
        }
        var previous = getRemainingCooldownTicks(player);
        if (previous <= 0) {
            return;
        }
        setRemainingCooldownTicks(player, previous - ticks, true);
    }

    public static void tickCooldown(ServerPlayer player) {
        var previous = getRemainingCooldownTicks(player);
        if (previous <= 0) {
            return;
        }

        var remaining = previous - 1;
        setRemainingCooldownTicks(player, remaining, remaining == 0 || player.tickCount % 20 == 0);
    }

    public static void sync(ServerPlayer player) {
        Networks.sendToPlayer(player, new SyncUndyingEmblemStatePacket(
                getRemainingCooldownTicks(player),
                player.level().getGameTime()
        ));
    }

    private static void setRemainingCooldownTicks(ServerPlayer player, int value, boolean sync) {
        var previous = getRemainingCooldownTicks(player);
        var remaining = Math.max(0, value);
        Capabilities.withSpellData(player, data ->
                data.edit(CodexSpellStateTypeRegister.UNDYING_EMBLEM_STATE,
                        state -> state.setRemainingCooldownTicks(remaining)));

        if (sync) {
            sync(player);
        }
        if ((previous == 0) != (remaining == 0)) {
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
        if (remaining == 0) {
            cancelReconstructionCast(player);
        }
    }

    public static void cancelReconstructionCast(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.isCasting()
                && SpellRegistry.IDOL_RECONSTRUCTION.get().getSpellId().equals(magicData.getCastingSpellId())) {
            Utils.serverSideCancelCast(player, false);
        }
    }
}
