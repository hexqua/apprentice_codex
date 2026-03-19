package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ExplorersCaneRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class ExplorersCane extends AbstractOffhandMagicItem implements GeoItem, UniqueItem {
    private static final int ENCHANTMENT_VALUE = 15;
    private static final String NETHER_PORTAL_POS_TAG = "NetherPortalPos";
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
                bonus(Attributes.MOVEMENT_SPEED, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL),
                bonus(ForgeMod.STEP_HEIGHT_ADDITION, 0.5D, AttributeModifier.Operation.ADDITION)
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
        if ((serverLevel.getGameTime() + slotId) % LODESTONE_VALIDATION_INTERVAL != 0L) {
            return;
        }

        boolean changed = refreshStoredRespawnTarget(stack, serverPlayer);
        changed |= invalidateLodestoneIfMissing(stack, serverLevel);
        if (changed) {
            syncInventory(serverPlayer);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        var lodestonePos = getLodestoneTarget(stack);
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ExplorersCaneRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ExplorersCaneRenderer();
                }

                return renderer;
            }
        });
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
        var compassTag = compassStack.getTag();
        var globalPos = compassTag == null ? null : CompassItem.getLodestonePosition(compassTag);
        if (globalPos == null) {
            return false;
        }

        var caneTag = caneStack.getOrCreateTag();
        copyTagElement(compassTag, caneTag, LODESTONE_POS_TAG);
        copyTagElement(compassTag, caneTag, LODESTONE_DIMENSION_TAG);
        if (compassTag.contains(LODESTONE_TRACKED_TAG, Tag.TAG_BYTE)) {
            caneTag.putBoolean(LODESTONE_TRACKED_TAG, compassTag.getBoolean(LODESTONE_TRACKED_TAG));
        } else {
            caneTag.putBoolean(LODESTONE_TRACKED_TAG, true);
        }
        return true;
    }

    public static void captureNetherPortalDestination(ServerPlayer player, @Nullable BlockPos portalPos) {
        var resolvedPortalPos = portalPos != null ? portalPos.immutable() : findNearbyNetherPortal(player.serverLevel(), player.blockPosition());
        boolean changed = false;

        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            var stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof ExplorersCane)) {
                continue;
            }

            changed |= setStoredNetherPortalPos(stack, resolvedPortalPos);
        }

        if (changed) {
            syncInventory(player);
        }
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static @Nullable BlockPos findNearbyNetherPortal(ServerLevel level, BlockPos origin) {
        return level.getPortalForcer()
                .findPortalAround(origin, false, level.getWorldBorder())
                .map(rectangle -> rectangle.minCorner)
                .map(BlockPos::immutable)
                .orElse(null);
    }

    private static boolean invalidateLodestoneIfMissing(ItemStack stack, ServerLevel level) {
        var lodestonePos = getLodestoneTarget(stack);
        if (lodestonePos == null || lodestonePos.dimension() != level.dimension()) {
            return false;
        }
        if (!level.isInWorldBounds(lodestonePos.pos()) || !level.hasChunkAt(lodestonePos.pos())) {
            return false;
        }
        if (level.getPoiManager().existsAtPosition(PoiTypes.LODESTONE, lodestonePos.pos())) {
            return false;
        }

        return clearLodestoneData(stack);
    }

    private static @Nullable GlobalPos getLodestoneTarget(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? null : CompassItem.getLodestonePosition(tag);
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
        var tag = stack.getTag();
        if (tag == null) {
            return false;
        }

        boolean changed = false;
        changed |= removeTag(tag, LODESTONE_POS_TAG);
        changed |= removeTag(tag, LODESTONE_DIMENSION_TAG);
        changed |= removeTag(tag, LODESTONE_TRACKED_TAG);
        return changed;
    }

    private static boolean setStoredNetherPortalPos(ItemStack stack, @Nullable BlockPos pos) {
        var oldPos = getStoredNetherPortalPos(stack);
        if (oldPos == null ? pos == null : oldPos.equals(pos)) {
            return false;
        }

        var tag = stack.getOrCreateTag();
        if (pos == null) {
            tag.remove(NETHER_PORTAL_POS_TAG);
            return true;
        }

        tag.put(NETHER_PORTAL_POS_TAG, NbtUtils.writeBlockPos(pos));
        return true;
    }

    private static @Nullable BlockPos getStoredNetherPortalPos(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NETHER_PORTAL_POS_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        return NbtUtils.readBlockPos(tag.getCompound(NETHER_PORTAL_POS_TAG));
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
        if (current == null ? target == null : current.equals(target)) {
            return false;
        }

        var tag = stack.getOrCreateTag();
        if (target == null) {
            tag.remove(RESPAWN_TARGET_POS_TAG);
            tag.remove(RESPAWN_TARGET_DIMENSION_TAG);
            return true;
        }

        tag.put(RESPAWN_TARGET_POS_TAG, NbtUtils.writeBlockPos(target.pos()));
        tag.putString(RESPAWN_TARGET_DIMENSION_TAG, target.dimension().location().toString());
        return true;
    }

    private static @Nullable GlobalPos getStoredRespawnTarget(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null
                || !tag.contains(RESPAWN_TARGET_POS_TAG, Tag.TAG_COMPOUND)
                || !tag.contains(RESPAWN_TARGET_DIMENSION_TAG, Tag.TAG_STRING)) {
            return null;
        }

        var dimensionId = ResourceLocation.tryParse(tag.getString(RESPAWN_TARGET_DIMENSION_TAG));
        if (dimensionId == null) {
            return null;
        }

        return GlobalPos.of(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                NbtUtils.readBlockPos(tag.getCompound(RESPAWN_TARGET_POS_TAG))
        );
    }

    private static void copyTagElement(CompoundTag from, CompoundTag to, String key) {
        var value = from.get(key);
        if (value != null) {
            to.put(key, value.copy());
        }
    }

    private static boolean removeTag(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return false;
        }

        tag.remove(key);
        return true;
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
