package jp.aquafactory.apprenticecodex.capability.companiontrunkinventory;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CompanionTrunkInventory {
    public static final int SIZE = 27;

    private final ItemStackHandler handler = new ItemStackHandler(SIZE);
    private @Nullable String customName;

    public ItemStackHandler getHandler() {
        return handler;
    }

    public boolean isEmpty() {
        for (var i = 0; i < handler.getSlots(); ++i) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void clearItems() {
        for (var i = 0; i < handler.getSlots(); ++i) {
            handler.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    public @Nullable Component getCustomName() {
        return customName == null || customName.isBlank() ? null : Component.literal(customName);
    }

    public void setCustomName(@Nullable Component name) {
        if (name == null) {
            customName = null;
            return;
        }

        var text = name.getString();
        customName = text.isBlank() ? null : text;
    }

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.put("Items", handler.serializeNBT());
        if (customName != null && !customName.isBlank()) {
            tag.putString("CustomName", customName);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        try {
            handler.deserializeNBT(nbt.getCompound("Items"));
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.error("Failed to load CompanionTrunk inventory data, resetting.", e);
            handler.deserializeNBT(new CompoundTag());
        }
        customName = nbt.contains("CustomName") ? nbt.getString("CustomName") : null;
    }

    public void copyAllFrom(CompanionTrunkInventory other) {
        deserializeNBT(other.serializeNBT());
    }

    public void copyNameOnlyFrom(CompanionTrunkInventory other) {
        clearItems();
        setCustomName(other.getCustomName());
    }
}
