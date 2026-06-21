package jp.aquafactory.apprenticecodex.spell.linearbuild;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class LinearBuild extends AbstractSpell implements IClientBlockTargetingSpell {
    private static final String NOT_BLOCK_MESSAGE = "ui.apprenticecodex.linear_build.not_block";
    private static final String TOO_FAR_MESSAGE = "ui.apprenticecodex.too_far";
    private static final String RETRIEVED_MESSAGE = "ui.apprenticecodex.linear_build.retrieved_from_container";
    private static final int PLAYER_INVENTORY_ITEM_SLOTS = 36;
    private static final int SHULKER_SLOT_COUNT = 27;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "linear_build");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .build();

    public LinearBuild() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0))
        );
    }

    private int getRange(){
        return 32;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.REVOLVE.get());
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new LinearBuildCastData();
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            sendError(entity, NOT_BLOCK_MESSAGE);
            return false;
        }

        var blockTemplate = resolveHeldBlockTemplate(player);
        if (blockTemplate.isEmpty()) {
            sendError(entity, NOT_BLOCK_MESSAGE);
            return false;
        }

        var target = resolveTarget(level, entity);
        if (target.isEmpty()) {
            sendError(entity, TOO_FAR_MESSAGE);
            return false;
        }

        var castData = new LinearBuildCastData();
        castData.hitBlockPos = target.get().hitPos();
        castData.hitFace = target.get().hitFace();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            var blockTemplate = resolveHeldBlockTemplate(player);
            if (blockTemplate.isEmpty()) {
                sendError(player, NOT_BLOCK_MESSAGE);
            } else {
                var target = restoreTarget(playerMagicData).or(() -> resolveTarget(level, entity));
                if (target.isEmpty()) {
                    sendError(player, TOO_FAR_MESSAGE);
                } else {
                    placeLine(level, player, blockTemplate.get(), target.get());
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            if (playerMagicData.getAdditionalCastData() instanceof LinearBuildCastData castData) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private Optional<ItemStack> resolveHeldBlockTemplate(Player player) {
        var offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof BlockItem) {
            return Optional.of(offhand.copy());
        }

        var mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BlockItem) {
            return Optional.of(mainHand.copy());
        }

        return Optional.empty();
    }

    private Optional<PlacementTarget> resolveTarget(Level level, LivingEntity entity) {
        var clientTarget = BlockTargetingHelper.getPendingHitTargetIgnoringRange(level, entity, getSpellResource());
        if (clientTarget.isPresent()) {
            var data = clientTarget.get();
            if (data.getHitBlockPos() != null && data.getHitFace() != null
                    && isWithinAxisRange(entity.blockPosition(), data.getHitBlockPos())) {
                return Optional.of(new PlacementTarget(data.getHitBlockPos(), data.getHitFace()));
            }
            return Optional.empty();
        }

        return raycastTargetBlock(level, entity)
                .filter(hit -> isWithinAxisRange(entity.blockPosition(), hit.getBlockPos()))
                .map(hit -> new PlacementTarget(hit.getBlockPos(), hit.getDirection()));
    }

    private Optional<BlockHitResult> raycastTargetBlock(Level level, LivingEntity entity) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(getRange()));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.of(hit);
    }

    private boolean isWithinAxisRange(BlockPos origin, BlockPos target) {
        var range = getRange();
        return Math.abs(origin.getX() - target.getX()) <= range
                && Math.abs(origin.getY() - target.getY()) <= range
                && Math.abs(origin.getZ() - target.getZ()) <= range;
    }

    private Optional<PlacementTarget> restoreTarget(MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof LinearBuildCastData castData)) {
            return Optional.empty();
        }
        if (castData.hitBlockPos == null || castData.hitFace == null) {
            return Optional.empty();
        }
        return Optional.of(new PlacementTarget(castData.hitBlockPos, castData.hitFace));
    }

    private void placeLine(Level level, ServerPlayer player, ItemStack blockTemplate, PlacementTarget target) {
        if (!(blockTemplate.getItem() instanceof BlockItem blockItem)) {
            sendError(player, NOT_BLOCK_MESSAGE);
            return;
        }

        List<LinearBuildItemSource> sources = player.getAbilities().instabuild
                ? List.of(CreativeItemSource.INSTANCE)
                : collectItemSources(player, blockTemplate);
        var retrievedLabels = new LinkedHashSet<Component>();
        var copiedState = resolveCopiedState(level, blockItem, target);
        var abortOnFailedPlacement = ApprenticeCodexServerConfig.linearBuildConfig().abortOnFailedPlacement();
        var placed = false;

        for (var pos : collectLinePositions(player.blockPosition(), target)) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                if (abortOnFailedPlacement) {
                    break;
                }
                continue;
            }

            var source = findNextSource(sources, blockTemplate);
            if (source == null) {
                break;
            }

            boolean placedAtPosition = copiedState
                    .map(state -> tryPlaceCopiedState(level, blockItem, source, blockTemplate, pos, state))
                    .orElse(false);
            if (!placedAtPosition) {
                placedAtPosition = tryPlaceAt(level, player, blockTemplate, source, pos, target.hitFace());
            }
            if (!placedAtPosition) {
                if (abortOnFailedPlacement) {
                    break;
                }
                continue;
            }

            placed = true;
            if (source.shouldNotifyRetrieved()) {
                retrievedLabels.add(source.label());
            }
        }

        if (!placed) {
            sendError(player, "ui.apprenticecodex.cant_place");
            return;
        }

        if (!retrievedLabels.isEmpty()) {
            sendRetrievedMessage(player, retrievedLabels);
        }
        player.getInventory().setChanged();
    }

    private Optional<BlockState> resolveCopiedState(Level level, BlockItem blockItem, PlacementTarget target) {
        var hitState = level.getBlockState(target.hitPos());
        if (hitState.getBlock() != blockItem.getBlock()) {
            return Optional.empty();
        }
        return Optional.of(hitState);
    }

    private List<BlockPos> collectLinePositions(BlockPos playerPos, PlacementTarget target) {
        var positions = new ArrayList<BlockPos>();
        var axis = target.hitFace().getAxis();
        var playerAxisCoordinate = axisCoordinate(playerPos, axis);
        var targetIsAlreadyOnPlayerAxis = axisCoordinate(target.hitPos(), axis) == playerAxisCoordinate;
        var cursor = target.hitPos().relative(target.hitFace());
        for (var step = 0; step <= getRange(); ++step) {
            var cursorAxisCoordinate = axisCoordinate(cursor, axis);
            if (step > 0 && cursorAxisCoordinate == playerAxisCoordinate) {
                break;
            }
            positions.add(cursor.immutable());
            if (targetIsAlreadyOnPlayerAxis || cursorAxisCoordinate == playerAxisCoordinate) {
                break;
            }
            cursor = cursor.relative(target.hitFace());
        }
        return positions;
    }

    private static int axisCoordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private LinearBuildItemSource findNextSource(List<LinearBuildItemSource> sources, ItemStack template) {
        for (var source : sources) {
            if (source.hasMatchingItem(template)) {
                return source;
            }
        }
        return null;
    }

    private boolean tryPlaceAt(Level level, ServerPlayer player, ItemStack blockTemplate,
                               LinearBuildItemSource source, BlockPos pos, Direction hitFace) {
        var before = level.getBlockState(pos);
        var result = BlockTools.useItemOnBlockByPlayerMainHand(level, player, pos, singleUseStack(blockTemplate), hitFace);
        if (!result.consumesAction() && result != InteractionResult.SUCCESS) {
            return false;
        }
        if (level.getBlockState(pos) == before || level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        return source.consumeOne(blockTemplate);
    }

    private boolean tryPlaceCopiedState(Level level, BlockItem blockItem, LinearBuildItemSource source,
                                        ItemStack blockTemplate, BlockPos pos, BlockState copiedState) {
        if (level.getBlockState(pos).getBlock() == copiedState.getBlock()) {
            return false;
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        if (!copiedState.canSurvive(level, pos)) {
            return false;
        }
        if (!source.hasMatchingItem(blockTemplate)) {
            return false;
        }

        var before = level.getBlockState(pos);
        if (!level.setBlock(pos, copiedState, Block.UPDATE_ALL)) {
            return false;
        }
        if (source.consumeOne(blockTemplate)) {
            return true;
        }
        level.setBlock(pos, before, Block.UPDATE_ALL);
        return false;
    }

    private ItemStack singleUseStack(ItemStack template) {
        var stack = template.copy();
        stack.setCount(1);
        return stack;
    }

    private List<LinearBuildItemSource> collectItemSources(ServerPlayer player, ItemStack template) {
        var sources = new ArrayList<LinearBuildItemSource>();
        addPersonalShelfSources(player, sources);
        addEnderChestSource(player, sources);
        addCompanionTrunkSource(player, sources);
        addOwnedChestedHorseSources(player, sources);
        addCuriosItemHandlerSources(player, sources);
        addInventoryItemHandlerSources(player, sources);
        addShulkerSources(player, sources);
        addBundleSources(player, sources);
        addInventorySlotSources(player, sources, 9, PLAYER_INVENTORY_ITEM_SLOTS);
        addInventorySlotSources(player, sources, 0, 9);
        addMainHandSource(player, sources);
        addOffhandSource(player, sources);
        return sources;
    }

    private void addPersonalShelfSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        if (!hasOwnedPersonalShelfNearby(player)) {
            return;
        }
        player.getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(inventory -> sources.add(new ItemHandlerSource(
                inventory.getHandler(),
                Component.translatable("container.apprenticecodex.personal_shelf"),
                true
        )));
    }

    private boolean hasOwnedPersonalShelfNearby(ServerPlayer player) {
        var level = player.serverLevel();
        var origin = player.blockPosition();
        var range = getRange();
        var min = origin.offset(-range, -range, -range);
        var max = origin.offset(range, range, range);
        for (var pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof PersonalShelfChestBlockEntity shelf
                    && player.getUUID().equals(shelf.getOwner())) {
                return true;
            }
        }
        return false;
    }

    private void addEnderChestSource(ServerPlayer player, List<LinearBuildItemSource> sources) {
        if (!hasEnderChestNearby(player)) {
            return;
        }
        sources.add(new ContainerSource(
                player.getEnderChestInventory(),
                Component.translatable("container.enderchest"),
                true
        ));
    }

    private boolean hasEnderChestNearby(ServerPlayer player) {
        var level = player.serverLevel();
        var origin = player.blockPosition();
        var range = getRange();
        var min = origin.offset(-range, -range, -range);
        var max = origin.offset(range, range, range);
        for (var pos : BlockPos.betweenClosed(min, max)) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                return true;
            }
        }
        return false;
    }

    private void addCompanionTrunkSource(ServerPlayer player, List<LinearBuildItemSource> sources) {
        var storage = Capabilities.getCompanionTrunkInventoryOrNull(player);
        if (storage == null) {
            return;
        }
        sources.add(new ItemHandlerSource(
                storage.getHandler(),
                Component.translatable("container.apprenticecodex.companion_trunk.default"),
                true
        ));
    }

    private void addOwnedChestedHorseSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        var box = player.getBoundingBox().inflate(getRange());
        for (var horse : player.level().getEntitiesOfClass(AbstractChestedHorse.class, box, horse ->
                horse.isAlive()
                        && horse.hasChest()
                        && horse.isTamed()
                        && player.getUUID().equals(horse.getOwnerUUID())
                        && (horse.getType() == EntityType.DONKEY || horse.getType() == EntityType.MULE))) {
            horse.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> sources.add(new ItemHandlerSource(
                    handler,
                    horse.getDisplayName(),
                    true
            )));
        }
    }

    private void addCuriosItemHandlerSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()))
                .orElse(List.of())
                .forEach(slotResult -> slotResult.stack().getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler ->
                        sources.add(new ItemHandlerSource(handler, slotResult.stack().getHoverName(), true))));
    }

    private void addInventoryItemHandlerSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        for (var slot = 0; slot < player.getInventory().items.size(); ++slot) {
            var stack = player.getInventory().items.get(slot);
            if (stack.isEmpty() || isDedicatedNestedContainerStack(stack)) {
                continue;
            }
            var label = stack.getHoverName();
            stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler ->
                    sources.add(new InventoryStackItemHandlerSource(handler, label, player.getInventory())));
        }
    }

    private boolean isDedicatedNestedContainerStack(ItemStack stack) {
        if (stack.is(Items.BUNDLE)) {
            return true;
        }
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private void addShulkerSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        forEachInventoryStack(player.getInventory(), (slot, stack) -> {
            if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
                return;
            }
            sources.add(new ShulkerBoxSource(stack, stack.getHoverName(), player.getInventory()));
        });
    }

    private void addBundleSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        forEachInventoryStack(player.getInventory(), (slot, stack) -> {
            if (!stack.is(Items.BUNDLE)) {
                return;
            }
            sources.add(new BundleSource(stack, stack.getHoverName(), player.getInventory()));
        });
    }

    private void forEachInventoryStack(Inventory inventory, InventoryStackConsumer consumer) {
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            consumer.accept(slot, inventory.items.get(slot));
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            consumer.accept(PLAYER_INVENTORY_ITEM_SLOTS + slot, inventory.offhand.get(slot));
        }
    }

    private void addInventorySlotSources(ServerPlayer player, List<LinearBuildItemSource> sources, int start, int end) {
        var inventory = player.getInventory();
        for (var slot = start; slot < end && slot < inventory.items.size(); ++slot) {
            if (slot == inventory.selected) {
                continue;
            }
            sources.add(new InventorySlotSource(inventory, slot));
        }
    }

    private void addMainHandSource(ServerPlayer player, List<LinearBuildItemSource> sources) {
        sources.add(new InventorySlotSource(player.getInventory(), player.getInventory().selected));
    }

    private void addOffhandSource(ServerPlayer player, List<LinearBuildItemSource> sources) {
        sources.add(new OffhandSlotSource(player.getInventory()));
    }

    private static boolean isSameItemIgnoringEmptyTag(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty() || !left.is(right.getItem())) {
            return false;
        }
        if (left.isDamageableItem() && left.getDamageValue() != right.getDamageValue()) {
            return false;
        }
        var leftTag = normalizedTag(left);
        var rightTag = normalizedTag(right);
        if (leftTag == null) {
            return rightTag == null;
        }
        return leftTag.equals(rightTag);
    }

    private static CompoundTag normalizedTag(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null || tag.isEmpty() ? null : tag;
    }

    private void sendError(LivingEntity entity, String key) {
        if (entity instanceof ServerPlayer player && player.connection != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key).withStyle(ChatFormatting.RED)));
        }
    }

    private void sendRetrievedMessage(ServerPlayer player, LinkedHashSet<Component> retrievedLabels) {
        if (player.connection == null) {
            return;
        }

        MutableComponent labels = Component.empty();
        var first = true;
        for (var label : retrievedLabels) {
            if (!first) {
                labels.append(Component.literal(", "));
            }
            labels.append(label.copy());
            first = false;
        }

        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable(RETRIEVED_MESSAGE, labels).withStyle(ChatFormatting.GREEN)
        ));
    }

    private record PlacementTarget(BlockPos hitPos, Direction hitFace) {
    }

    private interface InventoryStackConsumer {
        void accept(int slot, ItemStack stack);
    }

    private interface LinearBuildItemSource {
        Component label();

        boolean shouldNotifyRetrieved();

        boolean hasMatchingItem(ItemStack template);

        boolean consumeOne(ItemStack template);
    }

    private static class ItemHandlerSource implements LinearBuildItemSource {
        private final IItemHandler handler;
        private final Component label;
        private final boolean notifyRetrieved;

        private ItemHandlerSource(IItemHandler handler, Component label, boolean notifyRetrieved) {
            this.handler = handler;
            this.label = label;
            this.notifyRetrieved = notifyRetrieved;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return notifyRetrieved;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return findSlot(template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var slot = findSlot(template);
            return slot >= 0 && !handler.extractItem(slot, 1, false).isEmpty();
        }

        private int findSlot(ItemStack template) {
            for (var slot = 0; slot < handler.getSlots(); ++slot) {
                var stack = handler.getStackInSlot(slot);
                if (isSameItemIgnoringEmptyTag(stack, template) && !handler.extractItem(slot, 1, true).isEmpty()) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private static final class InventoryStackItemHandlerSource extends ItemHandlerSource {
        private final Inventory inventory;

        private InventoryStackItemHandlerSource(IItemHandler handler, Component label, Inventory inventory) {
            super(handler, label, true);
            this.inventory = inventory;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var consumed = super.consumeOne(template);
            if (consumed) {
                inventory.setChanged();
            }
            return consumed;
        }
    }

    private static final class ContainerSource implements LinearBuildItemSource {
        private final Container container;
        private final Component label;
        private final boolean notifyRetrieved;

        private ContainerSource(Container container, Component label, boolean notifyRetrieved) {
            this.container = container;
            this.label = label;
            this.notifyRetrieved = notifyRetrieved;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return notifyRetrieved;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return findSlot(template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var slot = findSlot(template);
            if (slot < 0) {
                return false;
            }
            container.removeItem(slot, 1);
            container.setChanged();
            return true;
        }

        private int findSlot(ItemStack template) {
            for (var slot = 0; slot < container.getContainerSize(); ++slot) {
                var stack = container.getItem(slot);
                if (isSameItemIgnoringEmptyTag(stack, template)) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private static class InventorySlotSource implements LinearBuildItemSource {
        private final Inventory inventory;
        private final int slot;

        private InventorySlotSource(Inventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        public Component label() {
            return Component.translatable("container.inventory");
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return false;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return isSameItemIgnoringEmptyTag(getStack(), template);
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var stack = getStack();
            if (!isSameItemIgnoringEmptyTag(stack, template)) {
                return false;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                setStack(ItemStack.EMPTY);
            }
            inventory.setChanged();
            return true;
        }

        protected ItemStack getStack() {
            return inventory.getItem(slot);
        }

        protected void setStack(ItemStack stack) {
            inventory.setItem(slot, stack);
        }
    }

    private static final class OffhandSlotSource implements LinearBuildItemSource {
        private final Inventory inventory;

        private OffhandSlotSource(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Component label() {
            return Component.translatable("container.inventory");
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return false;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return !inventory.offhand.isEmpty() && isSameItemIgnoringEmptyTag(inventory.offhand.get(0), template);
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            if (inventory.offhand.isEmpty()) {
                return false;
            }
            var stack = inventory.offhand.get(0);
            if (!isSameItemIgnoringEmptyTag(stack, template)) {
                return false;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.offhand.set(0, ItemStack.EMPTY);
            }
            inventory.setChanged();
            return true;
        }
    }

    private static final class CreativeItemSource implements LinearBuildItemSource {
        private static final CreativeItemSource INSTANCE = new CreativeItemSource();

        private CreativeItemSource() {
        }

        @Override
        public Component label() {
            return Component.empty();
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return false;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return true;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            return true;
        }
    }

    private abstract static class NestedItemListSource implements LinearBuildItemSource {
        protected final ItemStack containerStack;
        private final Component label;
        private final Inventory inventory;

        private NestedItemListSource(ItemStack containerStack, Component label, Inventory inventory) {
            this.containerStack = containerStack;
            this.label = label;
            this.inventory = inventory;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return true;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return findEntry(template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var entries = getItems();
            var entry = findEntry(template);
            if (entry < 0 || entry >= entries.size()) {
                return false;
            }

            var stack = ItemStack.of(entries.getCompound(entry));
            if (!isSameItemIgnoringEmptyTag(stack, template)) {
                return false;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                entries.remove(entry);
            } else {
                entries.set(entry, stack.save(new CompoundTag()));
            }
            saveItems(entries);
            inventory.setChanged();
            return true;
        }

        private int findEntry(ItemStack template) {
            var entries = getItems();
            for (var i = 0; i < entries.size(); ++i) {
                if (isSameItemIgnoringEmptyTag(ItemStack.of(entries.getCompound(i)), template)) {
                    return i;
                }
            }
            return -1;
        }

        protected abstract ListTag getItems();

        protected abstract void saveItems(ListTag items);
    }

    private static final class ShulkerBoxSource extends NestedItemListSource {
        private ShulkerBoxSource(ItemStack containerStack, Component label, Inventory inventory) {
            super(containerStack, label, inventory);
        }

        @Override
        protected ListTag getItems() {
            var blockEntityTag = containerStack.getTagElement("BlockEntityTag");
            if (blockEntityTag == null) {
                return new ListTag();
            }
            return blockEntityTag.getList("Items", Tag.TAG_COMPOUND).copy();
        }

        @Override
        protected void saveItems(ListTag items) {
            for (var i = 0; i < items.size(); ++i) {
                var entry = items.getCompound(i);
                if (!entry.contains("Slot", Tag.TAG_BYTE)) {
                    entry.putByte("Slot", (byte) Math.min(i, SHULKER_SLOT_COUNT - 1));
                }
            }

            var blockEntityTag = containerStack.getOrCreateTagElement("BlockEntityTag");
            if (items.isEmpty()) {
                blockEntityTag.remove("Items");
            } else {
                blockEntityTag.put("Items", items);
            }
            if (blockEntityTag.isEmpty()) {
                containerStack.getOrCreateTag().remove("BlockEntityTag");
            }
        }
    }

    private static final class BundleSource extends NestedItemListSource {
        private BundleSource(ItemStack containerStack, Component label, Inventory inventory) {
            super(containerStack, label, inventory);
        }

        @Override
        protected ListTag getItems() {
            var tag = containerStack.getTag();
            if (tag == null) {
                return new ListTag();
            }
            return tag.getList("Items", Tag.TAG_COMPOUND).copy();
        }

        @Override
        protected void saveItems(ListTag items) {
            if (items.isEmpty()) {
                containerStack.removeTagKey("Items");
            } else {
                containerStack.getOrCreateTag().put("Items", items);
            }
        }
    }

    public static class LinearBuildCastData implements ICastDataSerializable {
        private BlockPos hitBlockPos;
        private Direction hitFace;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(hitBlockPos != null && hitFace != null);
            if (hitBlockPos == null || hitFace == null) {
                return;
            }
            friendlyByteBuf.writeBlockPos(hitBlockPos);
            friendlyByteBuf.writeEnum(hitFace);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            if (!friendlyByteBuf.readBoolean()) {
                reset();
                return;
            }
            hitBlockPos = friendlyByteBuf.readBlockPos();
            hitFace = friendlyByteBuf.readEnum(Direction.class);
        }

        @Override
        public void reset() {
            hitBlockPos = null;
            hitFace = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            if (hitBlockPos == null || hitFace == null) {
                return tag;
            }
            tag.putInt("HitX", hitBlockPos.getX());
            tag.putInt("HitY", hitBlockPos.getY());
            tag.putInt("HitZ", hitBlockPos.getZ());
            tag.putInt("HitFace", hitFace.get3DDataValue());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (!nbt.contains("HitX")) {
                reset();
                return;
            }
            hitBlockPos = new BlockPos(nbt.getInt("HitX"), nbt.getInt("HitY"), nbt.getInt("HitZ"));
            hitFace = Direction.from3DDataValue(nbt.getInt("HitFace"));
        }
    }
}
