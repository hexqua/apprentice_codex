package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorSpellSelectionEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Objects;

public final class SpellSelectionStackResolver {
    private SpellSelectionStackResolver() {
    }

    public static ItemStack resolveSelectionStack(Player player, String slot) {
        Objects.requireNonNull(player, "player");
        if (slot == null || slot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (SpellSelectionManager.MAINHAND.equals(slot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(slot)) {
            return player.getOffhandItem();
        }
        if (EndgameArmorSpellSelectionEvents.HEAD_SLOT.equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.HEAD);
        }
        if (EndgameArmorSpellSelectionEvents.LEGS_SLOT.equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.LEGS);
        }
        if (EndgameArmorSpellSelectionEvents.FEET_SLOT.equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.FEET);
        }

        for (var equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getName().equals(slot)) {
                return player.getItemBySlot(equipmentSlot);
            }
        }

        return resolveCurioSelectionStack(player, slot);
    }

    private static ItemStack resolveCurioSelectionStack(Player player, String slot) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> true).stream()
                        .filter(slotResult -> matchesCurioSlot(
                                slot,
                                slotResult.slotContext().identifier(),
                                slotResult.slotContext().index()
                        ))
                        .map(SlotResult::stack)
                        .findFirst()
                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    private static boolean matchesCurioSlot(String slot, String identifier, int index) {
        return slot.equals(identifier) || slot.equals(identifier + "_" + index);
    }
}
