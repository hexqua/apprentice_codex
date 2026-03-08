package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EssenceSmokerBlockEntity extends BlockEntity {
    public static final int MAX_MATERIAL_COUNT = 8;
    public static final int PROCESS_DURATION_TICKS = 20 * 30;
    private static final String CATALYST_TAG = "Catalyst";
    private static final String MATERIALS_TAG = "Materials";
    private static final String PROCESSING_TAG = "Processing";
    private static final String COMPLETED_TAG = "Completed";
    private static final String PROCESS_FINISH_GAME_TIME_TAG = "ProcessFinishGameTime";
    private static final ResourceLocation ARCANE_ESSENCE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_essence");

    private ItemStack catalyst = ItemStack.EMPTY;
    private final List<ItemStack> materials = new ArrayList<>();
    private boolean processing;
    private boolean completed;
    private long processFinishGameTime = -1L;

    public EssenceSmokerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ESSENCE_SMOKER.get(), pos, state);
    }

    public boolean hasCatalyst() {
        return !catalyst.isEmpty();
    }

    public @NotNull ItemStack getCatalyst() {
        return catalyst.copy();
    }

    public @NotNull List<ItemStack> getMaterials() {
        return copyMaterials();
    }

    public boolean hasMaterials() {
        return !materials.isEmpty();
    }

    public boolean isMaterialSlotsFull() {
        return materials.size() >= MAX_MATERIAL_COUNT;
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean canAcceptCatalyst(ItemStack stack) {
        var arcaneEssenceItem = ForgeRegistries.ITEMS.getValue(ARCANE_ESSENCE_ITEM_ID);
        return arcaneEssenceItem != null && stack.is(arcaneEssenceItem);
    }

    public boolean canAcceptMaterial(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    public boolean canIgnite() {
        return hasCatalyst() && hasMaterials() && !processing && !completed;
    }

    public boolean setCatalyst(ItemStack stack) {
        if (hasCatalyst() || !canAcceptCatalyst(stack)) {
            return false;
        }

        catalyst = stack.copyWithCount(1);
        markUpdated();
        return true;
    }

    public boolean addMaterial(ItemStack stack) {
        if (!hasCatalyst() || isMaterialSlotsFull() || !canAcceptMaterial(stack) || processing || completed) {
            return false;
        }

        materials.add(stack.copyWithCount(1));
        markUpdated();
        return true;
    }

    public @NotNull ItemStack popLastMaterial() {
        if (materials.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var removed = materials.remove(materials.size() - 1);
        markUpdated();
        return removed;
    }

    public @NotNull ItemStack popCatalyst() {
        if (catalyst.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var removed = catalyst;
        catalyst = ItemStack.EMPTY;
        markUpdated();
        return removed;
    }

    public boolean startProcessing(long gameTime) {
        if (!canIgnite()) {
            return false;
        }

        processing = true;
        completed = false;
        processFinishGameTime = gameTime + PROCESS_DURATION_TICKS;
        markUpdated();
        return true;
    }

    public boolean ignite(long gameTime) {
        if (!startProcessing(gameTime)) {
            return false;
        }

        // 着火手段に関わらず同じ開始フィードバックを返す。
        playIgniteSound();
        return true;
    }

    public List<ItemStack> collectCompletedItems() {
        if (!completed) {
            return List.of();
        }

        var drops = copyMaterials();
        resetContents();
        markUpdated();
        return drops;
    }

    public List<ItemStack> getDropsForBlockBreak() {
        var drops = new ArrayList<ItemStack>();
        if (!catalyst.isEmpty()) {
            drops.add(catalyst.copy());
        }
        drops.addAll(copyMaterials());
        return drops;
    }

    public void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!catalyst.isEmpty()) {
            tag.put(CATALYST_TAG, catalyst.save(new CompoundTag()));
        }

        if (!materials.isEmpty()) {
            var materialListTag = new ListTag();
            for (var material : materials) {
                materialListTag.add(material.save(new CompoundTag()));
            }
            tag.put(MATERIALS_TAG, materialListTag);
        }

        tag.putBoolean(PROCESSING_TAG, processing);
        tag.putBoolean(COMPLETED_TAG, completed);
        if (processFinishGameTime >= 0L) {
            tag.putLong(PROCESS_FINISH_GAME_TIME_TAG, processFinishGameTime);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        catalyst = tag.contains(CATALYST_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(CATALYST_TAG))
                : ItemStack.EMPTY;

        materials.clear();
        if (tag.contains(MATERIALS_TAG, Tag.TAG_LIST)) {
            var materialListTag = tag.getList(MATERIALS_TAG, Tag.TAG_COMPOUND);
            for (var i = 0; i < materialListTag.size(); i++) {
                var material = ItemStack.of(materialListTag.getCompound(i));
                if (!material.isEmpty()) {
                    materials.add(material);
                }
            }
        }

        processing = tag.getBoolean(PROCESSING_TAG);
        completed = tag.getBoolean(COMPLETED_TAG);
        processFinishGameTime = tag.contains(PROCESS_FINISH_GAME_TIME_TAG, Tag.TAG_LONG)
                ? tag.getLong(PROCESS_FINISH_GAME_TIME_TAG)
                : -1L;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceSmokerBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !blockEntity.processing) {
            return;
        }

        if (serverLevel.getGameTime() < blockEntity.processFinishGameTime) {
            return;
        }

        blockEntity.finishProcessing();
    }

    private void finishProcessing() {
        if (!processing) {
            return;
        }

        processing = false;
        completed = true;
        processFinishGameTime = -1L;
        catalyst = ItemStack.EMPTY;
        for (var i = 0; i < materials.size(); i++) {
            materials.set(i, new ItemStack(Items.LEATHER, materials.get(i).getCount()));
        }
        playCompletionSound();
        markUpdated();
    }

    private List<ItemStack> copyMaterials() {
        var copies = new ArrayList<ItemStack>(materials.size());
        for (var material : materials) {
            copies.add(material.copy());
        }
        return copies;
    }

    private void resetContents() {
        catalyst = ItemStack.EMPTY;
        materials.clear();
        processing = false;
        completed = false;
        processFinishGameTime = -1L;
    }

    private void markUpdated() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void playCompletionSound() {
        if (level == null) {
            return;
        }

        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.9F, 0.08F);
    }

    private void playIgniteSound() {
        if (level == null) {
            return;
        }

        AudioTools.playSoundFromPosition(level, worldPosition.getCenter(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
