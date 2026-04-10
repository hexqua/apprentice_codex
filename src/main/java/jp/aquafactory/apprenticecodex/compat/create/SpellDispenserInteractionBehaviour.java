package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserInteractionBehaviour extends MovingInteractionBehaviour {
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

        var ownerProfile = SpellDispenserBlockEntity.readOwnerProfile(blockInfo.nbt());
        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("container.apprenticecodex.spell_dispenser");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.@NotNull Inventory inventory, @NotNull Player menuPlayer) {
                return SpellDispenserMenu.createMounted(containerId, inventory, localPos, mountedInventory, ownerProfile != null);
            }
        }, buffer -> {
            buffer.writeBoolean(true);
            buffer.writeBlockPos(localPos);
            buffer.writeBoolean(ownerProfile != null);
            buffer.writeItem(mountedInventory.getStackInSlot(0).copy());
        });
        return true;
    }
}
