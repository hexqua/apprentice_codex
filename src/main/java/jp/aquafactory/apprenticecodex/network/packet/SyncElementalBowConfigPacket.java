package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientConfigState;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record SyncElementalBowConfigPacket(List<ResourceLocation> magicArrowCatalystItemIds, List<ElementalBowModeDefinition> definitions,
                                          double schoolRuneManaCostMultiplier)
        implements CustomPacketPayload {
    public static final Type<SyncElementalBowConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_elemental_bow_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncElementalBowConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncElementalBowConfigPacket::decode);

    public SyncElementalBowConfigPacket {
        magicArrowCatalystItemIds = List.copyOf(magicArrowCatalystItemIds);
        definitions = List.copyOf(definitions);
    }

    public SyncElementalBowConfigPacket(List<ResourceLocation> ids) {
        this(ids, ElementalBowModeManager.createSnapshot());
    }

    public SyncElementalBowConfigPacket(List<ResourceLocation> ids, List<ElementalBowModeDefinition> definitions) {
        this(ids, definitions, jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig.elementalBowSchoolRuneManaCostMultiplier());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
        buffer.writeDouble(packet.schoolRuneManaCostMultiplier);
    }

    public static SyncElementalBowConfigPacket decode(FriendlyByteBuf buffer) {
        var itemCount = buffer.readVarInt();
        var itemIds = new ArrayList<ResourceLocation>(itemCount);
        for (var index = 0; index < itemCount; ++index) {
            itemIds.add(buffer.readResourceLocation());
        }
        return new SyncElementalBowConfigPacket(itemIds, buffer.readList(buf ->
                new ElementalBowModeDefinition(buf.readResourceLocation(), buf.readVarInt())), buffer.readDouble());
    }

    public static void handle(SyncElementalBowConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncElementalBowConfigPacket packet) {
            ElementalBowClientConfigState.setMagicArrowCatalystItemIds(packet.magicArrowCatalystItemIds);
            ElementalBowModeManager.applySnapshot(packet.definitions);
            ElementalBowClientConfigState.setSchoolRuneManaCostMultiplier(packet.schoolRuneManaCostMultiplier);
        }
    }
}
