package jp.aquafactory.apprenticecodex.gametest.create;

import com.google.common.collect.ImmutableMap;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.compat.create.SpellDispenserMovementBehaviour;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.ArrayList;

public final class CreateGameTestHooks {
    private CreateGameTestHooks() {
    }

    // optional 依存の absent 環境でも GameTest 本体を読めるよう、Create API 依存はこの隔離クラスへ閉じ込める。
    public static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile
    ) {
        return createSpellDispenserMovementHarness(
                level,
                worldPos,
                mountedInventory,
                ownerProfile,
                SpellDispenserManaHelper.MAX_MANA,
                null,
                false
        );
    }

    public static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile,
            ItemStackHandler externalInventory,
            boolean externalAvailableForFuel
    ) {
        return createSpellDispenserMovementHarness(
                level,
                worldPos,
                mountedInventory,
                ownerProfile,
                SpellDispenserManaHelper.MAX_MANA,
                externalInventory,
                externalAvailableForFuel
        );
    }

    public static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile,
            int currentMana,
            ItemStackHandler externalInventory,
            boolean externalAvailableForFuel
    ) {
        var localPos = BlockPos.ZERO;
        var blockEntityTag = new CompoundTag();
        SpellDispenserBlockEntity.saveOwnerProfile(blockEntityTag, ownerProfile);
        SpellDispenserBlockEntity.saveCurrentMana(blockEntityTag, currentMana);
        var blockInfo = new StructureTemplate.StructureBlockInfo(
                localPos,
                BlockRegistry.SPELL_DISPENSER.get().defaultBlockState(),
                blockEntityTag
        );
        var contraption = new TestSpellDispenserContraption(localPos, mountedInventory, externalInventory, externalAvailableForFuel);
        var context = new MovementContext(level, blockInfo, contraption);
        context.position = Vec3.atCenterOf(worldPos);
        context.motion = Vec3.ZERO;
        context.relativeMotion = Vec3.ZERO;
        context.rotation = vec -> vec;
        context.disabled = false;
        return new SpellDispenserMovementHarness(context, new SpellDispenserMovementBehaviour());
    }

    public static void startMoving(Object harness) {
        var typedHarness = asHarness(harness);
        typedHarness.behaviour.startMoving(typedHarness.context);
    }

    public static void tick(Object harness) {
        var typedHarness = asHarness(harness);
        typedHarness.behaviour.tick(typedHarness.context);
    }

    public static void visitNewPosition(Object harness, BlockPos pos) {
        var typedHarness = asHarness(harness);
        typedHarness.behaviour.visitNewPosition(typedHarness.context, pos);
    }

    public static void stopMoving(Object harness) {
        var typedHarness = asHarness(harness);
        typedHarness.behaviour.stopMoving(typedHarness.context);
    }

    public static void setDisabled(Object harness, boolean disabled) {
        asHarness(harness).context.disabled = disabled;
    }

    public static boolean hasRunningContinuousCast(Object harness) {
        return SpellDispenserMovementBehaviour.hasRunningContinuousCast(asHarness(harness).context);
    }

    public static boolean requiresContinuousReset(Object harness) {
        return SpellDispenserMovementBehaviour.requiresContinuousReset(asHarness(harness).context);
    }

    public static boolean isCoolingDown(Object harness) {
        return SpellDispenserMovementBehaviour.isCoolingDown(asHarness(harness).context);
    }

    public static void placeDepotWithItem(ServerLevel level, BlockPos worldPos, ItemStack stack) {
        level.setBlock(worldPos, AllBlocks.DEPOT.getDefaultState(), 3);
        invokeBlockEntityMethod(level, worldPos, "setHeldItem", new Class<?>[]{ItemStack.class}, stack.copy());
    }

    public static ItemStack getDepotItem(ServerLevel level, BlockPos worldPos) {
        return invokeBlockEntityItemStackGetter(level, worldPos, "getHeldItem");
    }

    public static void placeChuteWithItem(ServerLevel level, BlockPos worldPos, ItemStack stack) {
        level.setBlock(worldPos, AllBlocks.CHUTE.getDefaultState(), 3);
        invokeBlockEntityMethod(level, worldPos, "setItem", new Class<?>[]{ItemStack.class}, stack.copy());
    }

    public static ItemStack getChuteItem(ServerLevel level, BlockPos worldPos) {
        return invokeBlockEntityItemStackGetter(level, worldPos, "getItem");
    }

    public static void placeBasinWithItems(ServerLevel level, BlockPos worldPos, ItemStack[] stacks) {
        level.setBlock(worldPos, AllBlocks.BASIN.getDefaultState(), 3);
        var blockEntity = level.getBlockEntity(worldPos);
        if (blockEntity == null) {
            return;
        }

        try {
            var inputInventory = blockEntity.getClass().getMethod("getInputInventory").invoke(blockEntity);
            var setStackInSlot = inputInventory.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            for (var slot = 0; slot < stacks.length; slot++) {
                setStackInSlot.invoke(inputInventory, slot, stacks[slot].copy());
            }
            blockEntity.getClass().getMethod("notifyChangeOfContents").invoke(blockEntity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest basin setup failed", exception);
        }
    }

    public static int getBasinItemCount(ServerLevel level, BlockPos worldPos, ItemStack prototype) {
        var blockEntity = level.getBlockEntity(worldPos);
        if (blockEntity == null || prototype.isEmpty()) {
            return 0;
        }

        try {
            var inputInventory = blockEntity.getClass().getMethod("getInputInventory").invoke(blockEntity);
            var outputInventory = blockEntity.getClass().getMethod("getOutputInventory").invoke(blockEntity);
            return countMatchingInventoryItems(inputInventory, prototype) + countMatchingInventoryItems(outputInventory, prototype);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest basin count failed", exception);
        }
    }

    private static int countMatchingInventoryItems(Object inventory, ItemStack prototype) throws ReflectiveOperationException {
        var getSlots = inventory.getClass().getMethod("getSlots");
        var getStackInSlot = inventory.getClass().getMethod("getStackInSlot", int.class);
        var count = 0;
        var slots = (int) getSlots.invoke(inventory);
        for (var slot = 0; slot < slots; slot++) {
            var rawStack = getStackInSlot.invoke(inventory, slot);
            if (rawStack instanceof ItemStack stack && ItemStack.isSameItemSameComponents(stack, prototype)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void invokeBlockEntityMethod(
            ServerLevel level,
            BlockPos worldPos,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        var blockEntity = level.getBlockEntity(worldPos);
        if (blockEntity == null) {
            return;
        }

        try {
            blockEntity.getClass().getMethod(methodName, parameterTypes).invoke(blockEntity, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest block entity method failed: " + methodName, exception);
        }
    }

    private static ItemStack invokeBlockEntityItemStackGetter(ServerLevel level, BlockPos worldPos, String methodName) {
        var blockEntity = level.getBlockEntity(worldPos);
        if (blockEntity == null) {
            return ItemStack.EMPTY;
        }

        try {
            var result = blockEntity.getClass().getMethod(methodName).invoke(blockEntity);
            return result instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest block entity getter failed: " + methodName, exception);
        }
    }

    private static SpellDispenserMovementHarness asHarness(Object harness) {
        if (harness instanceof SpellDispenserMovementHarness typedHarness) {
            return typedHarness;
        }

        throw new IllegalArgumentException("Create GameTest harness type mismatch: " + harness);
    }

    private record SpellDispenserMovementHarness(
            MovementContext context,
            SpellDispenserMovementBehaviour behaviour
    ) {
    }

    private static final class TestSpellDispenserContraption extends Contraption {
        private TestSpellDispenserContraption(
                BlockPos localPos,
                ItemStackHandler mountedInventory,
                ItemStackHandler externalInventory,
                boolean externalAvailableForFuel
        ) {
            this.storage = new TestMountedStorageManager(localPos, mountedInventory, externalInventory, externalAvailableForFuel);
            this.disabledActors = new ArrayList<>();
        }

        @Override
        public boolean assemble(net.minecraft.world.level.Level level, BlockPos pos) {
            return false;
        }

        @Override
        public boolean canBeStabilized(Direction direction, BlockPos pos) {
            return false;
        }

        @Override
        public com.simibubi.create.api.contraption.ContraptionType getType() {
            throw new UnsupportedOperationException("GameTest helper contraption does not provide a type");
        }
    }

    private static final class TestMountedStorageManager extends MountedStorageManager {
        private final ImmutableMap<BlockPos, MountedItemStorage> storages;
        private final CombinedInvWrapper allItems;
        private final MountedItemStorageWrapper fuelItems;

        private TestMountedStorageManager(
                BlockPos localPos,
                ItemStackHandler mountedInventory,
                ItemStackHandler externalInventory,
                boolean externalAvailableForFuel
        ) {
            var localStorage = new TestMountedItemStorage(mountedInventory);
            var storagesBuilder = ImmutableMap.<BlockPos, MountedItemStorage>builder()
                    .put(localPos, localStorage);
            var allWrappers = new ArrayList<MountedItemStorage>();
            allWrappers.add(localStorage);

            var fuelBuilder = ImmutableMap.<BlockPos, MountedItemStorage>builder()
                    .put(localPos, localStorage);
            if (externalInventory != null) {
                var externalPos = new BlockPos(1, 0, 0);
                var externalStorage = new TestMountedItemStorage(externalInventory);
                storagesBuilder.put(externalPos, externalStorage);
                allWrappers.add(externalStorage);
                if (externalAvailableForFuel) {
                    fuelBuilder.put(externalPos, externalStorage);
                }
            }

            this.storages = storagesBuilder.build();
            this.allItems = new CombinedInvWrapper(allWrappers.toArray(IItemHandlerModifiable[]::new));
            this.fuelItems = new MountedItemStorageWrapper(fuelBuilder.build());
        }

        @Override
        public ImmutableMap<BlockPos, MountedItemStorage> getAllItemStorages() {
            return storages;
        }

        @Override
        public CombinedInvWrapper getAllItems() {
            return allItems;
        }

        @Override
        public MountedItemStorageWrapper getFuelItems() {
            return fuelItems;
        }
    }

    private static final class TestMountedItemStorage extends WrapperMountedItemStorage<ItemStackHandler> {
        private TestMountedItemStorage(ItemStackHandler wrapped) {
            super((MountedItemStorageType<?>) AllMountedStorageTypes.SIMPLE.get(), wrapped);
        }

        @Override
        public void unmount(net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        }
    }
}
