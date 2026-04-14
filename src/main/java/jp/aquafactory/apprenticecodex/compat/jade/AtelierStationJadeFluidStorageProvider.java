package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import net.minecraft.nbt.CompoundTag;
import snownee.jade.api.Accessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum AtelierStationJadeFluidStorageProvider implements IServerExtensionProvider<CompoundTag> {
    INSTANCE;

    private static final int MAX_DISPLAY_FLUID_COUNT = 4;

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof AtelierStationBlockEntity blockEntity)) {
            return List.of();
        }

        var storedFluids = blockEntity.getTopStoredFluidsForJade(MAX_DISPLAY_FLUID_COUNT);
        if (storedFluids.isEmpty()) {
            return List.of();
        }

        var level = accessor.getLevel();
        var views = new ArrayList<CompoundTag>(storedFluids.size());
        for (var entry : storedFluids) {
            var fluidStack = AbstractPotionFlaskItem.createFluidForStoredItem(
                    level,
                    entry.representativeItem(),
                    entry.amountMb()
            );
            if (fluidStack == null || fluidStack.isEmpty()) {
                continue;
            }

            views.add(FluidView.writeDefault(
                    JadeFluidObject.of(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getComponentsPatch()),
                    AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT
            ));
        }

        return views.isEmpty() ? List.of() : List.of(new ViewGroup<>(views));
    }

    @Override
    public net.minecraft.resources.ResourceLocation getUid() {
        return JadeIds.UNIVERSAL_FLUID_STORAGE;
    }
}
