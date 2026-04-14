package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HealingBloomJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String NEXT_FRUIT_TICKS_TAG = "NextFruitTicks";
    private static final String FRUIT_COUNT_TAG = "FruitCount";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof HealingBloomEntity bloom)) {
            return;
        }

        var ownerName = bloom.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            data.putString(JadeTooltipHelper.OWNER_NAME_TAG, ownerName);
        }

        data.putInt(NEXT_FRUIT_TICKS_TAG, bloom.getRemainingTicksUntilNextFruit());
        data.putInt(FRUIT_COUNT_TAG, bloom.getFruitCount());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        JadeTooltipHelper.appendOwnerLine(tooltip, data.getString(JadeTooltipHelper.OWNER_NAME_TAG));
        var nextFruitTicks = data.getInt(NEXT_FRUIT_TICKS_TAG);
        if (nextFruitTicks > 0) {
            tooltip.add(Component.translatable(
                    "jade.apprenticecodex.healing_bloom.next_fruit",
                    JadeTooltipHelper.toDisplaySeconds(nextFruitTicks)
            ));
        }

        JadeTooltipHelper.appendItemCountLine(
                tooltip,
                new ItemStack(ItemRegistry.COMFORT_BERRIES.get()),
                Math.max(0, data.getInt(FRUIT_COUNT_TAG))
        );
    }

    @Override
    public int getDefaultPriority() {
        return -1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.HEALING_BLOOM_UID;
    }
}
