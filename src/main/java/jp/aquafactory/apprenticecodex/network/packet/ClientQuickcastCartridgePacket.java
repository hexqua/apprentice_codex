package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceInput;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record ClientQuickcastCartridgePacket(ResourceLocation expectedSpell, BlockTargetData target,
                                             float forward, float strafe) {
    public static void encode(ClientQuickcastCartridgePacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.expectedSpell);
        packet.target.writeToBuffer(buffer);
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
    }

    public static ClientQuickcastCartridgePacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        var spell = buffer.readResourceLocation();
        var target = new BlockTargetData();
        target.readFromBuffer(buffer);
        return new ClientQuickcastCartridgePacket(spell, target, buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientQuickcastCartridgePacket packet,
                              java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
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
        // 送信値は照準の補助だけに使い、魔法・レベル・コストはサーバー上の実装備から決定する。
        MirageAvoidanceInput.setPending(player, packet.forward, packet.strafe);
        BlockTargetingHelper.setPendingServerTarget(player, packet.expectedSpell, packet.target);
        try {
            return QuickcastCartridgeCasting.initiate(player);
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
            MirageAvoidanceInput.clearPending(player);
        }
    }
}
