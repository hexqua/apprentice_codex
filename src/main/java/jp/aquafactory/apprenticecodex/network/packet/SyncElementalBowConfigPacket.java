package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientConfigState;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncElementalBowConfigPacket(List<ResourceLocation> magicArrowCatalystItemIds, List<ElementalBowModeDefinition> definitions) {
    public SyncElementalBowConfigPacket {
        magicArrowCatalystItemIds = List.copyOf(magicArrowCatalystItemIds);
        definitions = List.copyOf(definitions);
    }

    public SyncElementalBowConfigPacket(List<ResourceLocation> ids) {
        this(ids, ElementalBowModeManager.createSnapshot());
    }

    public static void encode(SyncElementalBowConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.magicArrowCatalystItemIds.size());
        for (var itemId : packet.magicArrowCatalystItemIds) {
            buffer.writeResourceLocation(itemId);
        }
        buffer.writeCollection(packet.definitions, (buf, definition) -> {
            buf.writeResourceLocation(definition.spell());
            buf.writeVarInt(definition.requiredDrawTicks());
        });
    }

    public static SyncElementalBowConfigPacket decode(FriendlyByteBuf buffer) {
        var itemCount = buffer.readVarInt();
        var itemIds = new ArrayList<ResourceLocation>(itemCount);
        for (var index = 0; index < itemCount; ++index) {
            itemIds.add(buffer.readResourceLocation());
        }
        return new SyncElementalBowConfigPacket(itemIds, buffer.readList(buf ->
                new ElementalBowModeDefinition(buf.readResourceLocation(), buf.readVarInt())));
    }

    public static void handle(
            SyncElementalBowConfigPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncElementalBowConfigPacket packet) {
            ElementalBowClientConfigState.setMagicArrowCatalystItemIds(packet.magicArrowCatalystItemIds);
            ElementalBowModeManager.applySnapshot(packet.definitions);
        }
    }
}
