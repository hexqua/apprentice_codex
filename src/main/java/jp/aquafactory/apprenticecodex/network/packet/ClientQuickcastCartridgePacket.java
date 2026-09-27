package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientQuickcastCartridgePacket(ResourceLocation expectedSpell, BlockTargetData target) {
    public static void encode(ClientQuickcastCartridgePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.expectedSpell);
        packet.target.writeToBuffer(buffer);
    }

    public static ClientQuickcastCartridgePacket decode(FriendlyByteBuf buffer) {
        var spell = buffer.readResourceLocation();
        var target = new BlockTargetData();
        target.readFromBuffer(buffer);
        return new ClientQuickcastCartridgePacket(spell, target);
    }

    public static void handle(ClientQuickcastCartridgePacket packet,
                              Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) handleOnServer(packet, player);
        });
        context.setPacketHandled(true);
    }

    public static boolean handleOnServer(ClientQuickcastCartridgePacket packet, ServerPlayer player) {
        var stack = QuickcastCartridgeCasting.findEquipped(player);
        if (stack.isEmpty() || player.isSpectator() || !player.isAlive()) return false;
        var spell = QuickcastScrollCartridge.getSelectedSpellData(stack);
        if (spell == SpellData.EMPTY || !spell.getSpell().getSpellResource().equals(packet.expectedSpell)) return false;
        // 送信された対象情報は照準の補助だけに使い、魔法・レベル・コストはサーバー上の実装備から決定する。
        BlockTargetingHelper.setPendingServerTarget(player, packet.expectedSpell, packet.target);
        try {
            return QuickcastCartridgeCasting.initiate(player);
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
        }
    }
}
