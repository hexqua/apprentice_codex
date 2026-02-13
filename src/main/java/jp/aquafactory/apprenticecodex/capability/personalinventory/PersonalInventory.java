package jp.aquafactory.apprenticecodex.capability.personalinventory;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public class PersonalInventory {
    // ラージサイズ固定.
    public static final int MAX_SIZE = 54;
    private final ItemStackHandler handler = new ItemStackHandler(MAX_SIZE);

    public ItemStackHandler getHandler() {
        return handler;
    }

    public CompoundTag serializeNBT() {
        return handler.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        // ワールドに入れなくなる不具合だけは回避するため、ログを出して握りつぶし.
        try {
            handler.deserializeNBT(nbt);
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.error("Failed to load PersonalInventory data, resetting.", e);
            handler.deserializeNBT(new CompoundTag());
        }
    }
}
