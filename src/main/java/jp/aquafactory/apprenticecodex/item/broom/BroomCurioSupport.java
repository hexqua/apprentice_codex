package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

public final class BroomCurioSupport {
    public static final String SPELL_SELECTION_SLOT = "apprenticecodex_broom";

    private BroomCurioSupport() {
    }

    public static boolean canEquip(SlotContext targetSlot) {
        if (!CuriosSlotConstants.BACK.equals(targetSlot.identifier())) {
            return false;
        }
        if (targetSlot.entity() == null) {
            return true;
        }

        return CuriosApi.getCuriosInventory(targetSlot.entity())
                .map(inventory -> hasNoOtherEquippedBroom(inventory, targetSlot))
                .orElse(true);
    }

    public static Optional<SlotResult> findUniqueEquippedBroom(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> {
            var equippedBrooms = inventory.findCurios(BroomCurioSupport::isBroom);
            if (equippedBrooms.size() != 1) {
                return Optional.empty();
            }

            var equippedBroom = equippedBrooms.getFirst();
            return CuriosSlotConstants.BACK.equals(equippedBroom.slotContext().identifier())
                    ? Optional.of(equippedBroom)
                    : Optional.empty();
        });
    }

    public static boolean isBroom(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AbstractBroomItem;
    }

    private static boolean hasNoOtherEquippedBroom(ICuriosItemHandler inventory, SlotContext targetSlot) {
        // 外部要因でslot数や種類が増えても、Curiosが保持する実装備領域全体で箒を一つに制限する.
        for (var entry : inventory.getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (var index = 0; index < stacks.getSlots(); index++) {
                if (entry.getKey().equals(targetSlot.identifier()) && index == targetSlot.index()) {
                    continue;
                }
                if (isBroom(stacks.getStackInSlot(index))) {
                    return false;
                }
            }
        }
        return true;
    }
}
