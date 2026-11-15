package jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.compat.magitech.ExoskeletonTooltip;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public final class MagiEngineersLegExoskeleton extends Item implements ICurioItem {
    public static final String KEY = "item.apprenticecodex.magi_engineers_leg_exoskeleton";

    public MagiEngineersLegExoskeleton() { super(new Properties().stacksTo(1)); }

    public static boolean available() { return ModList.get().isLoaded("magitech"); }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder()
                .putAll(ICurioItem.super.getAttributeModifiers(slotContext, id, stack))
                .put(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(id.withSuffix("/exoskeleton_safe_fall"),
                        5, AttributeModifier.Operation.ADD_VALUE)).build();
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        result.add(Component.translatable(KEY + ".lore").withColor(0x808080));
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers." + CuriosSlotConstants.FEET).withStyle(ChatFormatting.GOLD));
        // キー表示と外部MODの色定義はclient側の連携クラスへ隔離する。
        if (available() && FMLEnvironment.dist == Dist.CLIENT) ExoskeletonTooltip.append(result);
        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) { return true; }
}
