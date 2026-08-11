package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SearchBeaconRefundManager {
    private SearchBeaconRefundManager() {
    }

    public static boolean hasPending(ServerPlayer owner) {
        var data = Capabilities.getSpellDataOrNull(owner);
        return data != null
                && data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE).hasPendingInstantBrazier();
    }

    public static boolean reserve(ServerPlayer owner, UUID beaconUuid, ItemStack refundStack) {
        if (refundStack.isEmpty()) {
            return true;
        }
        var data = Capabilities.getSpellDataOrNull(owner);
        if (data == null) {
            return false;
        }
        var state = data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE);
        var serializedStack = refundStack.copyWithCount(1).save(new CompoundTag());
        if (!state.reserveInstantBrazier(beaconUuid, serializedStack)) {
            return false;
        }
        data.markDirty(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE.id());
        return true;
    }

    public static boolean matches(ServerPlayer owner, UUID beaconUuid) {
        var data = Capabilities.getSpellDataOrNull(owner);
        return data != null
                && data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE)
                .matchesPendingInstantBrazier(beaconUuid);
    }

    public static boolean consume(ServerPlayer owner, UUID beaconUuid) {
        return !claim(owner, beaconUuid).isEmpty();
    }

    public static ItemStack refund(ServerPlayer owner, UUID beaconUuid) {
        return decode(claim(owner, beaconUuid));
    }

    public static void recoverPending(ServerPlayer owner) {
        var refundStack = decode(claim(owner, null));
        placeBackInInventory(owner, refundStack);
    }

    public static void recover(ServerPlayer owner, UUID beaconUuid) {
        var refundStack = refund(owner, beaconUuid);
        placeBackInInventory(owner, refundStack);
    }

    private static void placeBackInInventory(ServerPlayer owner, ItemStack refundStack) {
        if (refundStack.isEmpty()) {
            return;
        }
        owner.getInventory().placeItemBackInInventory(refundStack);
        owner.getInventory().setChanged();
    }

    private static CompoundTag claim(ServerPlayer owner, @Nullable UUID expectedBeaconUuid) {
        var data = Capabilities.getSpellDataOrNull(owner);
        if (data == null) {
            return new CompoundTag();
        }
        var state = data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE);
        var claimed = state.claimPendingInstantBrazier(expectedBeaconUuid);
        if (!claimed.isEmpty()) {
            data.markDirty(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE.id());
        }
        return claimed;
    }

    private static ItemStack decode(CompoundTag serializedStack) {
        return serializedStack.isEmpty()
                ? ItemStack.EMPTY
                : ItemStack.of(serializedStack);
    }
}
