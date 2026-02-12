package jp.aquafactory.apprenticecodex.common.spells.personalshelf;

import jp.aquafactory.apprenticecodex.common.capability.Capabilities;
import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventoryMenu;
import jp.aquafactory.apprenticecodex.common.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersonalShelfChestBlockEntity extends BlockEntity implements MenuProvider {
    public PersonalShelfChestBlockEntity(BlockPos pos, BlockState state){
        super(BlockEntityRegistry.PERSONAL_SHELF_CHEST.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.personal_shelf");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inventory, @NotNull Player player) {
        var capability = player.getCapability(Capabilities.PERSONAL_INVENTORY);
        if (capability.isPresent()) {
            var shelf = capability.orElseThrow(IllegalStateException::new);
            return new PersonalInventoryMenu(windowId, inventory, shelf.getHandler(), getBlockPos());
        }

        return null;
    }
}
