package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

public final class BroomCurioSupport {
    public static final String SPELL_SELECTION_SLOT = "apprenticecodex_broom";
    public static final String CURIO_SLOT = CuriosSlotConstants.BACK;
    public static final ResourceLocation CALIBRATED_BROOM_PREDICATE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "calibrated_broom"
    );

    private BroomCurioSupport() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BroomCurioSupport::onCommonSetup);
    }

    public static boolean canEquip(SlotContext targetSlot, ItemStack stack) {
        if (!CURIO_SLOT.equals(targetSlot.identifier())
                || !AbstractBroomItem.isBackCurioEnabled(stack)) {
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
            var equippedBrooms = inventory.findCurios(BroomCurioSupport::isActiveBroom);
            if (equippedBrooms.size() != 1) {
                return Optional.empty();
            }

            var equippedBroom = equippedBrooms.getFirst();
            return CURIO_SLOT.equals(equippedBroom.slotContext().identifier())
                    ? Optional.of(equippedBroom)
                    : Optional.empty();
        });
    }

    public static boolean isBroom(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AbstractBroomItem;
    }

    public static boolean isActiveBroom(ItemStack stack) {
        return isBroom(stack) && AbstractBroomItem.isBackCurioEnabled(stack);
    }

    public static AbstractBroomEntity createBroom(ItemStack stack, Level level) {
        return stack.getItem() instanceof AbstractBroomItem broomItem ? broomItem.createBroom(level) : null;
    }

    private static boolean hasNoOtherEquippedBroom(ICuriosItemHandler inventory, SlotContext targetSlot) {
        // 外部要因でslot数や種類が増えても、Curiosが保持する実装備領域全体で箒を一つに制限する.
        for (var entry : inventory.getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (var index = 0; index < stacks.getSlots(); index++) {
                if (entry.getKey().equals(targetSlot.identifier()) && index == targetSlot.index()) {
                    continue;
                }
                if (isActiveBroom(stacks.getStackInSlot(index))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CuriosApi.registerCurioPredicate(
                CALIBRATED_BROOM_PREDICATE,
                slotResult -> AbstractBroomItem.isBackCurioEnabled(slotResult.stack())
        ));
    }
}
