package jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet;

import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class AbsorptionAmplifyAmulet extends Item implements ICurioItem {
    private final String slotIdentifier;

    public AbsorptionAmplifyAmulet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = Curios.NECKLACE_SLOT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        AbsorptionAmplifyAmuletLogic.onEquip(slotContext.entity());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        AbsorptionAmplifyAmuletLogic.onUnequip(slotContext.entity());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        AbsorptionAmplifyAmuletLogic.onCurioTick(slotContext);
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
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_3"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        }

        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()))
                .map(SlotResult::slotContext)
                .map(firstSlot -> firstSlot.index() == slotContext.index()
                        && firstSlot.identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()))
                .orElse(false);
    }
}
