package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum AlchemyBrewerJadeItemStorageProvider implements IServerExtensionProvider<AlchemyBrewerBlockEntity, ItemStack> {
    INSTANCE;

    @Override
    public List<ViewGroup<ItemStack>> getGroups(ServerPlayer player, ServerLevel level,
                                                AlchemyBrewerBlockEntity blockEntity, boolean showDetails) {
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
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ALCHEMY_BREWER_UID;
    }
}
