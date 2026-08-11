package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum AlchemyBrewerJadeFluidStorageProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)
                || !(blockAccessor.getBlockEntity() instanceof AlchemyBrewerBlockEntity blockEntity)) {
            return List.of();
        }

        var potionId = blockEntity.getTankPotionId();
        var amountMb = blockEntity.getTankAmountMb();
        if (potionId == null || amountMb <= 0) {
            return List.of();
        }

        var potion = BuiltInRegistries.POTION.get(potionId);
        if (potion == null) {
            return List.of();
        }

        var representativeItem = PotionContentsHelper.createPotionStack(Items.POTION, potion);
        var fluidStack = AbstractPotionFlaskItem.createFluidForStoredItem(
                accessor.getLevel(),
                representativeItem,
                amountMb
        );
        if (fluidStack == null || fluidStack.isEmpty()) {
            return List.of();
        }

        var view = FluidView.writeDefault(
                JadeFluidObject.of(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getComponentsPatch()),
                AlchemyBrewerBlockEntity.TANK_CAPACITY_MB
        );
        return List.of(new ViewGroup<>(List.of(view)));
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(
            Accessor<?> accessor,
            List<ViewGroup<CompoundTag>> groups
    ) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ALCHEMY_BREWER_FLUID_UID;
    }
}
