package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class ProtectionSpellSupporter extends Item implements ICurioItem, IJeiInfoItem {
    private static final float MANA_COST_DISCOUNT_MULTIPLIER = 0.5f;
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.protection_spell_supporter.desc_";
    private static final List<DeferredHolder<AbstractSpell, AbstractSpell>> TARGET_SPELLS = List.of(
            SpellRegistry.FORCE_FIELD,
            SpellRegistry.PHALANX_CHARGE
    );

    private final String slotIdentifier;

    public ProtectionSpellSupporter() {
        super(new Item.Properties().stacksTo(1));
        slotIdentifier = CuriosSlotConstants.BELT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            appendTargetSpellTooltips(result);
        }

        return result;
    }

    private static void appendTargetSpellTooltips(List<Component> tooltips) {
        for (var spellEntry : TARGET_SPELLS) {
            var spell = spellEntry.get();
            tooltips.add(Component.literal(" - ")
                    .append(spell.getDisplayName(null))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()))
                .orElse(false);
    }

    public static boolean isManaCostDiscountTargetSpell(String spellId) {
        for (var spellEntry : TARGET_SPELLS) {
            if (spellEntry.get().getSpellId().equals(spellId)) {
                return true;
            }
        }
        return false;
    }

    public static int applyManaCostDiscount(int manaCost, @Nullable LivingEntity entity) {
        if (manaCost <= 0 || !isEquippedBy(entity)) {
            return manaCost;
        }

        return Math.max(1, Math.round(manaCost * MANA_COST_DISCOUNT_MULTIPLIER));
    }
}
