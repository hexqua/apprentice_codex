package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastScrollCartridge;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceInput;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientQuickcastCartridgePacket(ResourceLocation expectedSpell, BlockTargetData target,
                                             float forward, float strafe) implements CustomPacketPayload {
    public static final Type<ClientQuickcastCartridgePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quickcast_cartridge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientQuickcastCartridgePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeResourceLocation(packet.expectedSpell);
                packet.target.writeToBuffer(buffer);
                buffer.writeFloat(packet.forward);
                buffer.writeFloat(packet.strafe);
            }, buffer -> {
                var spell = buffer.readResourceLocation();
                var target = new BlockTargetData();
                target.readFromBuffer(buffer);
                return new ClientQuickcastCartridgePacket(spell, target, buffer.readFloat(), buffer.readFloat());
            });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ClientQuickcastCartridgePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            handleOnServer(packet, player);
        });
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
        }
    }
}
