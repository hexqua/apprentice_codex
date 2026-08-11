package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum AlchemyBrewerJadeItemStorageProvider implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    @Override
    public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)
                || !(blockAccessor.getBlockEntity() instanceof AlchemyBrewerBlockEntity blockEntity)) {
            return List.of();
        }

        var inventory = blockEntity.getInventory();
        var stacks = new ArrayList<ItemStack>(AlchemyBrewerBlockEntity.SLOT_COUNT);
        for (var slot = AlchemyBrewerBlockEntity.FIRST_MATERIAL_SLOT;
             slot < AlchemyBrewerBlockEntity.FIRST_MATERIAL_SLOT + AlchemyBrewerBlockEntity.MATERIAL_SLOT_COUNT;
             ++slot) {
            addIfPresent(stacks, inventory.getStackInSlot(slot));
        }
        addIfPresent(stacks, inventory.getStackInSlot(AlchemyBrewerBlockEntity.INPUT_SLOT));
        addIfPresent(stacks, inventory.getStackInSlot(AlchemyBrewerBlockEntity.OUTPUT_SLOT));

        return stacks.isEmpty() ? List.of() : List.of(new ViewGroup<>(stacks));
    }

    private static void addIfPresent(List<ItemStack> stacks, ItemStack stack) {
        if (!stack.isEmpty()) {
            stacks.add(stack.copy());
        }
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(
            Accessor<?> accessor,
            List<ViewGroup<ItemStack>> groups
    ) {
        return ClientViewGroup.map(groups, ItemView::new, null);
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ALCHEMY_BREWER_UID;
    }
}
