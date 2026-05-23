package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserVariant;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserInteractionBehaviour extends MovingInteractionBehaviour {
    private static final String CREATIVE_DENY_OPEN_KEY = "ui.apprenticecodex.spell_dispenser.creative_version.deny_open";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (activeHand == InteractionHand.OFF_HAND) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        var serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
        var contraption = contraptionEntity.getContraption();
        var mountedInventory = contraption.getStorage().getAllItemStorages().get(localPos);
        var blockInfo = contraption.getBlocks().get(localPos);
        if (mountedInventory == null || blockInfo == null) {
            return false;
        }

        var variant = SpellDispenserVariant.fromState(blockInfo.state());
        if (!variant.canOpenMenu(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.translatable(CREATIVE_DENY_OPEN_KEY).withStyle(ChatFormatting.RED));
            return true;
        }

        var ownerProfile = variant.storesOwnerProfile() ? SpellDispenserBlockEntity.readOwnerProfile(blockInfo.nbt()) : null;
        var ownerName = ownerProfile != null ? ownerProfile.getName() : null;
        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("container.apprenticecodex.spell_dispenser");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.@NotNull Inventory inventory, @NotNull Player menuPlayer) {
                return SpellDispenserMenu.createMounted(
                        containerId,
                        inventory,
                        localPos,
                        mountedInventory,
                        ownerProfile != null,
                        ownerName,
                        () -> SpellDispenserBlockEntity.readCurrentMana(blockInfo.nbt()),
                        variant
                );
            }
        }, buffer -> {
            buffer.writeBoolean(true);
            buffer.writeBlockPos(localPos);
            // mounted menu は client 側に block entity が無いので owner 名を明示同期する。
            buffer.writeBoolean(ownerName != null && !ownerName.isBlank());
            if (ownerName != null && !ownerName.isBlank()) {
                buffer.writeUtf(ownerName);
            }
            buffer.writeBoolean(ownerProfile != null);
            buffer.writeVarInt(SpellDispenserBlockEntity.readCurrentMana(blockInfo.nbt()));
            buffer.writeBoolean(variant.isCreative());
            for (var slot = 0; slot < SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT; ++slot) {
                var stack = slot < mountedInventory.getSlots() ? mountedInventory.getStackInSlot(slot).copy() : net.minecraft.world.item.ItemStack.EMPTY;
                buffer.writeItem(stack);
            }
        });
        return true;
    }
}
