package jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class MonarchBondCharm extends Item implements ICurioItem{
    private final String slotIdentifier;

    public MonarchBondCharm() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.CHARM;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // todo:常時tickがあればなにかする.
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new java.util.ArrayList<>(tooltips);
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_3"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.MONARCH_BOND_CHARM.get()))
                .orElse(false);
    }
}
