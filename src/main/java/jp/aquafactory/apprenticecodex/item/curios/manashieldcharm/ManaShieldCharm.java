package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Set;

public class ManaShieldCharm extends Item implements ICurioItem, NonDamageableAnvilMergeItem {
    private static final int ENCHANTMENT_VALUE = 15;
    private static final Set<ResourceKey<Enchantment>> SUPPORTED_ENCHANTMENTS = Set.of(
            Enchantments.SHELL,
            Enchantments.SYNCHRONIZATION,
            Enchantments.NEUTRALIZATION
    );
    private final String slotIdentifier;

    public ManaShieldCharm() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.CHARM;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        ManaShieldCharmLogic.onCurioTick(slotContext);
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_3"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return SUPPORTED_ENCHANTMENTS.stream().anyMatch(enchantment::is);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }
}
