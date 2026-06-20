package jp.aquafactory.apprenticecodex.item.spellsideedge;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.SpellSideEdgeMirrorRenderer;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerClientTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SpellSideEdgeMirror extends AbstractSpellSideEdgeItem {
    private static final String INSTANCE_ID_TAG = "apprenticecodex:edge_dancer_instance_id";
    private static final String BETTER_COMBAT_DESCRIPTION_TRANSLATION_KEY =
            "item.apprenticecodex.spell_side_edge_mirror.desc.better_combat";

    public static ItemStack create(UUID instanceId, ItemStack sourceStack) {
        var stack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get());
        if (sourceStack != null) {
            stack.applyComponents(sourceStack.getComponentsPatch());
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(INSTANCE_ID_TAG, instanceId));
        setInitialSpellContainer(stack, SpellRegistry.ANCHOR_BLINK);
        return stack;
    }

    public static boolean isSpellSideEdgeMirror(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpellSideEdgeMirror;
    }

    public static boolean isGeneratedMirror(ItemStack stack) {
        return isSpellSideEdgeMirror(stack) && getInstanceId(stack).isPresent();
    }

    public static Optional<UUID> getInstanceId(ItemStack stack) {
        if (!isSpellSideEdgeMirror(stack)) {
            return Optional.empty();
        }

        CompoundTag tag = getCustomDataTag(stack);
        return tag != null && tag.hasUUID(INSTANCE_ID_TAG)
                ? Optional.of(tag.getUUID(INSTANCE_ID_TAG))
                : Optional.empty();
    }

    public static boolean hasInstanceId(ItemStack stack, @Nullable UUID instanceId) {
        if (instanceId == null) {
            return false;
        }
        return getInstanceId(stack).map(instanceId::equals).orElse(false);
    }

    @Override
    protected Supplier<? extends AbstractSpell> initialSpellSupplier() {
        return SpellRegistry.ANCHOR_BLINK;
    }

    @Override
    protected String betterCombatDescriptionTranslationKey() {
        return BETTER_COMBAT_DESCRIPTION_TRANSLATION_KEY;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return false;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SpellSideEdgeMirrorRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new SpellSideEdgeMirrorRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        if (FMLEnvironment.dist == Dist.CLIENT) {
                EdgeDancerClientTooltip.getStoredItemName(stack).ifPresent(storedItemName -> {
                    lines.add(Component.translatable(
                            "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.item",
                            storedItemName
                    ).withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable(
                            "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.hint"
                    ).withStyle(ChatFormatting.DARK_GRAY));
                });
        }
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
