package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ScarletThirst extends Item implements ICurioItem {
    final String slotIdentifier;

    public ScarletThirst() {
        super(new Item.Properties().stacksTo(1));
        slotIdentifier = Curios.RING_SLOT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();

        if (level.isClientSide) {
            return;
        }

        ApprenticeCodex.LOGGER.debug("Equip: ScarletThirst");
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();
        if (level.isClientSide){
            return;
        }

        ApprenticeCodex.LOGGER.debug("Unequip: ScarletThirst");
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();
        //noinspection resource
        var level = entity.level();
        if (level.isClientSide) {
            return;
        }

        if (entity.tickCount % 20 != 0) {
            return;
        }

        // todo:実装本体(今はCuriosが動いているかのテスト)
        entity.sendSystemMessage(Component.literal("[tick]" + entity.tickCount));
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            tooltips.add(Component.literal(" ").append(Component.translatable(getDescriptionId() + ".desc")).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
