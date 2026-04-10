package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider {
    private static final String INVENTORY_TAG = "Inventory";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String OWNER_NAME_TAG = "OwnerName";
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return SpellDispenserSpellValidator.isSupported(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    @Nullable
    private GameProfile ownerProfile;

    public SpellDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SPELL_DISPENSER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.apprenticecodex.spell_dispenser");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new SpellDispenserMenu(containerId, inventory, this);
    }

    public @NotNull ItemStackHandler getInventory() {
        return inventory;
    }

    public @NotNull ItemStack getSpellSource() {
        return inventory.getStackInSlot(0);
    }

    public void setOwnerProfile(@Nullable GameProfile ownerProfile) {
        this.ownerProfile = normalizeOwnerProfile(ownerProfile);
        markUpdated();
    }

    public @Nullable GameProfile getOwnerProfile() {
        return ownerProfile;
    }

    public boolean hasOwnerProfile() {
        return normalizeOwnerProfile(ownerProfile) != null;
    }

    public SpellDispenserCastHelper.CastResult tryActivate() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new SpellDispenserCastHelper.CastResult(
                    false,
                    SpellDispenserSpellValidator.validate(ItemStack.EMPTY),
                    null,
                    null,
                    null,
                    false
            );
        }

        var state = getBlockState();
        if (!(state.getBlock() instanceof SpellDispenser spellDispenser)) {
            return new SpellDispenserCastHelper.CastResult(
                    false,
                    SpellDispenserSpellValidator.validate(ItemStack.EMPTY),
                    null,
                    null,
                    null,
                    false
            );
        }

        var source = getSpellSource();
        if (source.isEmpty()) {
            return new SpellDispenserCastHelper.CastResult(
                    false,
                    SpellDispenserSpellValidator.validate(source),
                    null,
                    null,
                    null,
                    false
            );
        }

        if (!hasOwnerProfile()) {
            return SpellDispenserCastHelper.CastResult.missingOwnerProfile(SpellDispenserSpellValidator.validate(source));
        }

        return SpellDispenserCastHelper.tryCast(serverLevel, worldPosition, spellDispenser.getFacing(state), source.copy(), ownerProfile);
    }

    public void dropStoredItem() {
        if (level == null || level.isClientSide) {
            return;
        }

        var stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        if (ownerProfile != null && ownerProfile.getId() != null && ownerProfile.getName() != null && !ownerProfile.getName().isBlank()) {
            tag.putUUID(OWNER_UUID_TAG, ownerProfile.getId());
            tag.putString(OWNER_NAME_TAG, ownerProfile.getName());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        if (tag.hasUUID(OWNER_UUID_TAG) && tag.contains(OWNER_NAME_TAG, net.minecraft.nbt.Tag.TAG_STRING)) {
            ownerProfile = normalizeOwnerProfile(new GameProfile(tag.getUUID(OWNER_UUID_TAG), tag.getString(OWNER_NAME_TAG)));
        } else {
            ownerProfile = null;
        }
    }

    private void markUpdated() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static @Nullable GameProfile normalizeOwnerProfile(@Nullable GameProfile ownerProfile) {
        if (ownerProfile == null || ownerProfile.getId() == null || ownerProfile.getName() == null || ownerProfile.getName().isBlank()) {
            return null;
        }
        return new GameProfile(ownerProfile.getId(), ownerProfile.getName());
    }
}
