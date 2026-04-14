package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum EssenceSmokerJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof EssenceSmokerBlockEntity blockEntity) || !blockEntity.isProcessing()) {
            return;
        }

        tooltip.add(Component.translatable(
                "jade.apprenticecodex.essence_smoker.remaining",
                JadeTooltipHelper.toDisplaySeconds(blockEntity.getRemainingProcessTicks())
        ));
    }

    @Override
    public int getDefaultPriority() {
        return 1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ESSENCE_SMOKER_UID;
    }
}
