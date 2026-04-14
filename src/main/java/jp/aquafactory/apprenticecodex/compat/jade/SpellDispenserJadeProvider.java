package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SpellDispenserJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof SpellDispenserBlockEntity blockEntity)) {
            return;
        }

        tooltip.add(Component.translatable(
                "container.apprenticecodex.spell_dispenser.mana.tooltip",
                blockEntity.getCurrentMana(),
                blockEntity.getMaxMana()
        ));

        var ownerName = blockEntity.getOwnerName();
        var ownerDisplay = ownerName == null || ownerName.isBlank()
                ? Component.translatable("jade.apprenticecodex.none")
                : Component.literal(ownerName);
        tooltip.add(Component.translatable(
                "container.apprenticecodex.spell_dispenser.owner.tooltip",
                ownerDisplay
        ));
    }

    @Override
    public int getDefaultPriority() {
        return -1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.SPELL_DISPENSER_UID;
    }
}
