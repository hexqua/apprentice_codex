package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SearchBeaconJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String OFFERED_ITEM_TAG = "OfferedItem";
    private static final String TARGET_LABEL_TAG = "TargetLabel";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof SearchBeaconEntity beacon)) {
            return;
        }

        var ownerName = beacon.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            data.putString(OWNER_NAME_TAG, ownerName);
        }

        var offeredItem = beacon.getOfferedItem();
        if (!offeredItem.isEmpty()) {
            data.put(OFFERED_ITEM_TAG, offeredItem.save(new CompoundTag()));
        }

        var targetLabel = beacon.getTargetLabel();
        if (targetLabel != null && !targetLabel.isBlank()) {
            data.putString(TARGET_LABEL_TAG, targetLabel);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (data.contains(OFFERED_ITEM_TAG, CompoundTag.TAG_COMPOUND)) {
            var stack = ItemStack.of(data.getCompound(OFFERED_ITEM_TAG));
            if (!stack.isEmpty()) {
                JadeTooltipHelper.appendItemCountLine(tooltip, stack, stack.getCount());
            }
        }

        var targetLabel = data.getString(TARGET_LABEL_TAG);
        if (!targetLabel.isBlank()) {
            tooltip.add(Component.translatable("jade.apprenticecodex.search_beacon.targets", Component.literal(targetLabel)));
        }
    }

    @Override
    public int getDefaultPriority() {
        return -1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.SEARCH_BEACON_UID;
    }
}
