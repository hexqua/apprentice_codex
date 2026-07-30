package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Objects;

public class ExplorersCane extends AbstractOffhandMagicItem implements GeoItem, UniqueItem {
    private static final int ENCHANTMENT_VALUE = 15;
    private static final String NETHER_PORTAL_POS_TAG = "NetherPortalPos";
    private static final String WAS_IN_NETHER_TAG = "WasInNether";
    private static final String RESPAWN_TARGET_POS_TAG = "RespawnTargetPos";
    private static final String RESPAWN_TARGET_DIMENSION_TAG = "RespawnTargetDimension";
    private static final String LODESTONE_POS_TAG = "LodestonePos";
    private static final String LODESTONE_DIMENSION_TAG = "LodestoneDimension";
    private static final String LODESTONE_TRACKED_TAG = "LodestoneTracked";
    private static final float UNKNOWN_TARGET_SPIN_SPEED = 0.3f;
    private static final int LODESTONE_VALIDATION_INTERVAL = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExplorersCane() {
        super(
                SpellRegistry.LONG_STRIDE,
                1,
                Rarity.RARE,
                "explorers_cane",
                bonus(Attributes.MOVEMENT_SPEED, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                bonus(Attributes.STEP_HEIGHT, 0.5D, AttributeModifier.Operation.ADD_VALUE)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = updateNetherPortalTarget(stack, serverPlayer);
        if ((serverLevel.getGameTime() + slotId) % LODESTONE_VALIDATION_INTERVAL == 0L) {
            changed |= refreshStoredRespawnTarget(stack, serverPlayer);
            changed |= invalidateLodestoneIfMissing(stack, serverLevel);
        }
        if (changed) {
            syncInventory(serverPlayer);
        }
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull Item.TooltipContext context,
            @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        var lodestonePos = getLodestoneTarget(stack);
        var level = context.level();
        if (lodestonePos == null || level == null) {
            return;
        }

        var sameDimension = lodestonePos.dimension() == level.dimension();
        lines.add(Component.translatable(
                sameDimension
                        ? "item.apprenticecodex.explorers_cane.tooltip.lodestone_same_dimension"
                        : "item.apprenticecodex.explorers_cane.tooltip.lodestone_other_dimension"
        ).withStyle(sameDimension ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static float resolveCompassAngle(ItemStack stack, Level level,
                                            double sourceX, double sourceZ, float sourceYaw, float tick) {
        var target = resolveCompassTarget(stack, level);
        if (target.spinning()) {
            return tick * UNKNOWN_TARGET_SPIN_SPEED;
        }
        if (target.pos() == null) {
            return 0.0f;
        }

        double dx = target.pos().getX() + 0.5D - sourceX;
        double dz = target.pos().getZ() + 0.5D - sourceZ;
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq < 1.0e-6D) {
            return 0.0f;
        }

        double yawRad = sourceYaw * Mth.DEG_TO_RAD;
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);
        double localX = dx * cosYaw + dz * sinYaw;
        double localZ = -dx * sinYaw + dz * cosYaw;
        return (float) Math.atan2(-localZ, localX);
    }

    public static boolean copyLodestoneData(ItemStack caneStack, ItemStack compassStack) {
        var lodestoneTracker = compassStack.get(DataComponents.LODESTONE_TRACKER);
        if (lodestoneTracker == null) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, caneStack, tag -> {
            if (lodestoneTracker.target().isPresent()) {
                var target = lodestoneTracker.target().orElseThrow();
                tag.put(LODESTONE_POS_TAG, NbtUtils.writeBlockPos(target.pos()));
                tag.putString(LODESTONE_DIMENSION_TAG, target.dimension().location().toString());
                tag.putBoolean(LODESTONE_TRACKED_TAG, lodestoneTracker.tracked());
            } else {
                tag.remove(LODESTONE_POS_TAG);
                tag.remove(LODESTONE_DIMENSION_TAG);
                tag.remove(LODESTONE_TRACKED_TAG);
            }
        });
        caneStack.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker);
        return true;
    }

    public static boolean hasTransferableLodestoneData(ItemStack stack) {
        return stack.get(DataComponents.LODESTONE_TRACKER) != null;
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean updateNetherPortalTarget(ItemStack stack, ServerPlayer player) {
        boolean inNether = player.serverLevel().dimension() == Level.NETHER;
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(WAS_IN_NETHER_TAG, Tag.TAG_BYTE)) {
            // ネザー内で新規入手した杖が現在地を入口扱いしないよう、最初は次元状態だけ合わせる。
            return setWasInNether(stack, inNether);
        }

        boolean wasInNether = tag.getBoolean(WAS_IN_NETHER_TAG);
        if (inNether) {
            if (!wasInNether && player.serverLevel().getBlockState(player.blockPosition()).is(Blocks.NETHER_PORTAL)) {
                boolean changed = setWasInNether(stack, true);
                changed |= setStoredNetherPortalPos(stack, player.blockPosition());
                return changed;
            }

            return false;
        }

        if (!wasInNether) {
            return false;
        }

        return setWasInNether(stack, false);
    }

    private static boolean setWasInNether(ItemStack stack, boolean inNether) {
        var tag = getCustomDataTag(stack);
        if (tag != null && tag.contains(WAS_IN_NETHER_TAG, Tag.TAG_BYTE) && tag.getBoolean(WAS_IN_NETHER_TAG) == inNether) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, dataTag -> dataTag.putBoolean(WAS_IN_NETHER_TAG, inNether));
        return true;
    }

    private static boolean invalidateLodestoneIfMissing(ItemStack stack, ServerLevel level) {
        var lodestoneTracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (lodestoneTracker == null || lodestoneTracker.target().isEmpty()) {
            return false;
        }
        var lodestonePos = lodestoneTracker.target().orElseThrow();
        if (lodestonePos.dimension() != level.dimension()) {
            return false;
        }
        if (!level.isInWorldBounds(lodestonePos.pos()) || !level.hasChunkAt(lodestonePos.pos())) {
            return false;
        }
        var updatedTracker = lodestoneTracker.tick(level);
        if (updatedTracker == lodestoneTracker) {
            return false;
        }

        if (updatedTracker.target().isEmpty()) {
            return clearLodestoneData(stack);
        }

        stack.set(DataComponents.LODESTONE_TRACKER, updatedTracker);
        return true;
    }

    private static @Nullable GlobalPos getLodestoneTarget(ItemStack stack) {
        var customDataTarget = getStoredLodestoneTarget(stack);
        if (customDataTarget != null) {
            return customDataTarget;
        }

        var lodestoneTracker = stack.get(DataComponents.LODESTONE_TRACKER);
        return lodestoneTracker == null ? null : lodestoneTracker.target().orElse(null);
    }

    private static CompassTarget resolveCompassTarget(ItemStack stack, Level level) {
        var lodestonePos = getLodestoneTarget(stack);
        if (lodestonePos != null && lodestonePos.dimension() == level.dimension()) {
            return CompassTarget.position(lodestonePos.pos());
        }
        var respawnTarget = getStoredRespawnTarget(stack);

        if (level.dimension() == Level.END) {
            return CompassTarget.position(BlockPos.ZERO);
        }
        if (level.dimension() == Level.OVERWORLD) {
            return respawnTarget != null && respawnTarget.dimension() == Level.OVERWORLD
                    ? CompassTarget.position(respawnTarget.pos())
                    : CompassTarget.position(level.getSharedSpawnPos());
        }
        if (level.dimension() == Level.NETHER) {
            var netherPortalPos = getStoredNetherPortalPos(stack);
            if (netherPortalPos != null) {
                return CompassTarget.position(netherPortalPos);
            }

            var anchorPos = respawnTarget != null && respawnTarget.dimension() == Level.NETHER ? respawnTarget.pos() : null;
            return anchorPos != null ? CompassTarget.position(anchorPos) : CompassTarget.spin();
        }

        var anchorPos = respawnTarget != null && respawnTarget.dimension() == level.dimension() ? respawnTarget.pos() : null;
        return anchorPos != null ? CompassTarget.position(anchorPos) : CompassTarget.spin();
    }

    private static boolean clearLodestoneData(ItemStack stack) {
        boolean changed = stack.remove(DataComponents.LODESTONE_TRACKER) != null;
        var tag = getCustomDataTag(stack);
        if (tag == null) {
            return changed;
        }

        if (!tag.contains(LODESTONE_POS_TAG) && !tag.contains(LODESTONE_DIMENSION_TAG) && !tag.contains(LODESTONE_TRACKED_TAG)) {
            return changed;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, dataTag -> {
            dataTag.remove(LODESTONE_POS_TAG);
            dataTag.remove(LODESTONE_DIMENSION_TAG);
            dataTag.remove(LODESTONE_TRACKED_TAG);
        });
        return true;
    }

    private static boolean setStoredNetherPortalPos(ItemStack stack, @Nullable BlockPos pos) {
        var oldPos = getStoredNetherPortalPos(stack);
        if (Objects.equals(oldPos, pos)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (pos == null) {
                tag.remove(NETHER_PORTAL_POS_TAG);
            } else {
                tag.put(NETHER_PORTAL_POS_TAG, NbtUtils.writeBlockPos(pos));
            }
        });
        return true;
    }

    private static @Nullable BlockPos getStoredNetherPortalPos(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(NETHER_PORTAL_POS_TAG, Tag.TAG_INT_ARRAY)) {
            return null;
        }

        return NbtUtils.readBlockPos(tag, NETHER_PORTAL_POS_TAG).orElse(null);
    }

    private static boolean refreshStoredRespawnTarget(ItemStack stack, ServerPlayer player) {
        var respawnPos = player.getRespawnPosition();
        if (respawnPos == null) {
            return setStoredRespawnTarget(stack, null);
        }

        var respawnDimension = player.getRespawnDimension();
        var respawnLevel = player.server.getLevel(respawnDimension);
        if (respawnLevel == null || !respawnLevel.isInWorldBounds(respawnPos)) {
            return setStoredRespawnTarget(stack, null);
        }
        if (!respawnLevel.hasChunkAt(respawnPos)) {
            // 強制ロードは避け、未ロード時はプレイヤー記録をそのまま採用する.
            return setStoredRespawnTarget(stack, GlobalPos.of(respawnDimension, respawnPos));
        }

        var state = respawnLevel.getBlockState(respawnPos);
        if (respawnDimension == Level.OVERWORLD) {
            return setStoredRespawnTarget(
                    stack,
                    state.getBlock() instanceof BedBlock ? GlobalPos.of(respawnDimension, respawnPos) : null
            );
        }

        var validAnchor = state.getBlock() instanceof RespawnAnchorBlock
                && state.getValue(RespawnAnchorBlock.CHARGE) > 0;
        return setStoredRespawnTarget(stack, validAnchor ? GlobalPos.of(respawnDimension, respawnPos) : null);
    }

    private static boolean setStoredRespawnTarget(ItemStack stack, @Nullable GlobalPos target) {
        var current = getStoredRespawnTarget(stack);
        if (Objects.equals(current, target)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (target == null) {
                tag.remove(RESPAWN_TARGET_POS_TAG);
                tag.remove(RESPAWN_TARGET_DIMENSION_TAG);
            } else {
                tag.put(RESPAWN_TARGET_POS_TAG, NbtUtils.writeBlockPos(target.pos()));
                tag.putString(RESPAWN_TARGET_DIMENSION_TAG, target.dimension().location().toString());
            }
        });
        return true;
    }

    private static @Nullable GlobalPos getStoredRespawnTarget(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null
                || !tag.contains(RESPAWN_TARGET_POS_TAG, Tag.TAG_INT_ARRAY)
                || !tag.contains(RESPAWN_TARGET_DIMENSION_TAG, Tag.TAG_STRING)) {
            return null;
        }

        var dimensionId = ResourceLocation.tryParse(tag.getString(RESPAWN_TARGET_DIMENSION_TAG));
        if (dimensionId == null) {
            return null;
        }

        return GlobalPos.of(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                NbtUtils.readBlockPos(tag, RESPAWN_TARGET_POS_TAG).orElseThrow()
        );
    }

    private static @Nullable GlobalPos getStoredLodestoneTarget(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null
                || !tag.contains(LODESTONE_POS_TAG, Tag.TAG_INT_ARRAY)
                || !tag.contains(LODESTONE_DIMENSION_TAG, Tag.TAG_STRING)) {
            return null;
        }

        var dimensionId = ResourceLocation.tryParse(tag.getString(LODESTONE_DIMENSION_TAG));
        if (dimensionId == null) {
            return null;
        }

        var pos = NbtUtils.readBlockPos(tag, LODESTONE_POS_TAG).orElse(null);
        if (pos == null) {
            return null;
        }

        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimensionId), pos);
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private record CompassTarget(@Nullable BlockPos pos, boolean spinning) {
        private static CompassTarget position(BlockPos pos) {
            return new CompassTarget(pos.immutable(), false);
        }

        private static CompassTarget spin() {
            return new CompassTarget(null, true);
        }
    }
}
