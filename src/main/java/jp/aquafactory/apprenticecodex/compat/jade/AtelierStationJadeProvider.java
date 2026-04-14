package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum AtelierStationJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof AtelierStationBlockEntity blockEntity)) {
            return;
        }

        var flasks = blockEntity.getLoadedFlasksForDisplay();
        if (flasks.isEmpty()) {
            if (blockEntity.getStoredFluidAmount() <= 0) {
                tooltip.add(Component.translatable("jade.apprenticecodex.atelier_station.fluid.empty_entry"));
            }
            return;
        }

        for (var flask : flasks) {
            JadeTooltipHelper.appendItemLine(tooltip, flask);
        }
    }

    @Override
    public int getDefaultPriority() {
        return 2000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ATELIER_STATION_UID;
    }
}
