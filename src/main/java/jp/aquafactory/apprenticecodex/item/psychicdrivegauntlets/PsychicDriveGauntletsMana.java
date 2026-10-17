package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

final class PsychicDriveGauntletsMana {
    private PsychicDriveGauntletsMana() {}

    static boolean insufficient(Player player, int amount) {
        if (player.getAbilities().instabuild || amount <= 0
                || MagicData.getPlayerMagicData(player).getMana() >= amount) return false;
        player.displayClientMessage(Component.translatable("ui.apprenticecodex.psychic_drive_gauntlets.insufficient_mana")
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    static void spend(Player player, int amount) {
        if (player.level().isClientSide || player.getAbilities().instabuild || amount <= 0) return;
        var data = MagicData.getPlayerMagicData(player);
        // 開始時に必要量を確認済みなので、溜め中に他用途でマナが減っても成立させる。
        data.setMana(Math.max(0, data.getMana() - amount));
        if (player instanceof ServerPlayer server && !(server instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(server, new SyncManaPacket(data));
        }
    }
}
