package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum AlchemyBrewerJadeFluidStorageProvider implements IServerExtensionProvider<AlchemyBrewerBlockEntity, CompoundTag> {
    INSTANCE;

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel level,
                                                  AlchemyBrewerBlockEntity blockEntity, boolean showDetails) {
        var potionId = blockEntity.getTankPotionId();
        var amountMb = blockEntity.getTankAmountMb();
        if (potionId == null || amountMb <= 0) {
            return List.of();
        }

        var potion = AlchemyBrewerBlockEntity.resolveRegisteredPotion(potionId);
        if (potion == null) {
            return List.of();
        }

        var representativeItem = PotionContentsHelper.createPotionStack(Items.POTION, potion);
        var fluidStack = AbstractPotionFlaskItem.createFluidForStoredItem(
                level,
                representativeItem,
                amountMb
        );
        if (fluidStack == null || fluidStack.isEmpty()) {
            return List.of();
        }

        var view = FluidView.writeDefault(
                JadeFluidObject.of(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag()),
                AlchemyBrewerBlockEntity.TANK_CAPACITY_MB
        );
        return List.of(new ViewGroup<>(List.of(view)));
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ALCHEMY_BREWER_FLUID_UID;
    }
}
