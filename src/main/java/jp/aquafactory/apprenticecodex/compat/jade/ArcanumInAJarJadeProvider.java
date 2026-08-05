package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ArcanumInAJarJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String VALID_SETTINGS_TAG = "ValidSettings";
    private static final String MATERIAL_ITEM_TAG = "MaterialItem";
    private static final String PRODUCT_ITEM_TAG = "ProductItem";
    private static final String MATERIAL_COUNT_TAG = "MaterialCount";
    private static final String PRODUCT_COUNT_TAG = "ProductCount";
    private static final String REMAINING_TICKS_TAG = "RemainingTicks";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return;
        }

        var settings = ApprenticeCodexServerConfig.arcanumInAJarItemSettings();
        var validSettings = settings.isValid();
        data.putBoolean(VALID_SETTINGS_TAG, validSettings);
        data.putLong(REMAINING_TICKS_TAG, blockEntity.getRemainingTicksUntilNextConversion());
        if (!validSettings) {
            return;
        }

        // isValid()がtrueの時点でmaterialItemもproductItemも非nullが決まる.
        //noinspection DataFlowIssue
        data.put(MATERIAL_ITEM_TAG, new ItemStack(settings.materialItem())
                .saveOptional(accessor.getLevel().registryAccess()));
        //noinspection DataFlowIssue
        data.put(PRODUCT_ITEM_TAG, new ItemStack(settings.productItem())
                .saveOptional(accessor.getLevel().registryAccess()));
        data.putInt(MATERIAL_COUNT_TAG, blockEntity.getRemainingMaterialCount());
        data.putInt(PRODUCT_COUNT_TAG, blockEntity.getStoredProductCount());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains(VALID_SETTINGS_TAG)) {
            return;
        }

        if (data.getBoolean(VALID_SETTINGS_TAG)) {
            appendConfiguredItemLine(tooltip, accessor, data, MATERIAL_ITEM_TAG, MATERIAL_COUNT_TAG);
            appendConfiguredItemLine(tooltip, accessor, data, PRODUCT_ITEM_TAG, PRODUCT_COUNT_TAG);
        } else {
            tooltip.add(Component.translatable(
                    "jade.apprenticecodex.arcanum_in_a_jar.error_setting"
            ).withStyle(ChatFormatting.RED));
        }

        var remainingTicks = data.getLong(REMAINING_TICKS_TAG);
        if (remainingTicks > 0L) {
            tooltip.add(Component.translatable(
                    "jade.apprenticecodex.arcanum_in_a_jar.next_conversion",
                    JadeTooltipHelper.toDisplaySeconds(remainingTicks)
            ));
        }
    }

    private static void appendConfiguredItemLine(
            ITooltip tooltip,
            BlockAccessor accessor,
            CompoundTag data,
            String itemTag,
            String countTag
    ) {
        if (!data.contains(itemTag, CompoundTag.TAG_COMPOUND)) {
            return;
        }

        var stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound(itemTag));
        if (stack.isEmpty()) {
            return;
        }

        JadeTooltipHelper.appendItemLine(
                tooltip,
                stack,
                Component.translatable(
                        "jade.apprenticecodex.arcanum_in_a_jar.item_line",
                        stack.getHoverName().copy(),
                        data.getInt(countTag)
                )
        );
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ARCANUM_IN_A_JAR_UID;
    }
}
