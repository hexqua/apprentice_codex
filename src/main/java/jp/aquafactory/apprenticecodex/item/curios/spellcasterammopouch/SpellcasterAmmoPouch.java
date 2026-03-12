package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class SpellcasterAmmoPouch extends Item implements ICurioItem {
    private static final float EQUIPPED_EMPTY_CASING_RETURN_CHANCE = 0.9F;

    private final String slotIdentifier;

    public SpellcasterAmmoPouch() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.BELT;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    public static float applyEmptyCasingReturnChanceBonus(float baseChance, @Nullable LivingEntity entity) {
        if (!isEquippedBy(entity)) {
            return baseChance;
        }

        return Math.max(baseChance, EQUIPPED_EMPTY_CASING_RETURN_CHANCE);
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()))
                .orElse(false);
    }
}
