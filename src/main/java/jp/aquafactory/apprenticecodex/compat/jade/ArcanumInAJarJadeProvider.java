package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ArcanumInAJarJadeProvider implements IBlockComponentProvider {
    INSTANCE;
    private static final ResourceLocation ARCANE_ESSENCE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_essence");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return;
        }

        JadeTooltipHelper.appendItemLine(
                tooltip,
                new ItemStack(Items.REDSTONE),
                Component.translatable(
                        "jade.apprenticecodex.item_count",
                        blockEntity.getRemainingOperationCount(),
                        Items.REDSTONE.getDescription()
                )
        );

        var arcaneEssence = BuiltInRegistries.ITEM.getOptional(ARCANE_ESSENCE_ITEM_ID).orElse(null);
        if (arcaneEssence != null) {
            JadeTooltipHelper.appendItemLine(
                    tooltip,
                    new ItemStack(arcaneEssence),
                    Component.translatable(
                            "jade.apprenticecodex.item_count",
                            blockEntity.getStoredParameterCount(),
                            arcaneEssence.getDescription()
                    )
            );
        } else {
            tooltip.add(Component.translatable(
                    "jade.apprenticecodex.arcanum_in_a_jar.arcane_essence",
                    blockEntity.getStoredParameterCount()
            ));
        }

        var remainingTicks = blockEntity.getRemainingTicksUntilNextConversion();
        if (remainingTicks > 0L) {
            tooltip.add(Component.translatable(
                    "jade.apprenticecodex.arcanum_in_a_jar.next_conversion",
                    JadeTooltipHelper.toDisplaySeconds(remainingTicks)
            ));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.ARCANUM_IN_A_JAR_UID;
    }
}
