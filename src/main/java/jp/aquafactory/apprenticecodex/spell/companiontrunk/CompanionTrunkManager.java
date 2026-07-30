package jp.aquafactory.apprenticecodex.spell.companiontrunk;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CompanionTrunkManager {
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double MANUAL_RECALL_MIN_DISTANCE_SQR = 4.0;
    private static final int SUMMON_SEARCH_RADIUS = 3;
    private static final int[] SUMMON_Y_OFFSETS = {0, 1, -1, 2, -2};

    private CompanionTrunkManager() {
    }

    public static void toggle(ServerPlayer player, double maxHealth) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE);
        if (state.active) {
            tryDeactivate(player);
            return;
        }

        activate(player, maxHealth);
    }

    public static void activate(ServerPlayer player, double maxHealth) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var fixedMaxHealth = Math.max(1.0, maxHealth);
        var state = spellData.get(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE);
        var managedUuid = state.getTrunkUuid();
        var spawned = normalizeOwnedTrunks(player, managedUuid, fixedMaxHealth, true);
        spellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, s -> {
            s.active = true;
            s.maxHealth = fixedMaxHealth;
            s.setTrunkUuid(spawned != null ? spawned.getUUID() : null);
        });
    }

    public static boolean tryDeactivate(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE);
        var storage = Capabilities.getCompanionTrunkInventoryOrNull(player);
        if (storage != null && !storage.isEmpty()) {
            var fixedMaxHealth = state.maxHealth > 0.0 ? state.maxHealth : DEFAULT_MAX_HEALTH;
            var trunk = normalizeOwnedTrunks(player, state.getTrunkUuid(), fixedMaxHealth, false);
            if (isValidForOwner(trunk, player) && trunk.distanceToSqr(player) >= MANUAL_RECALL_MIN_DISTANCE_SQR) {
                if (trunk.recallNearOwner(player)) {
                    return false;
                }

                sendActionBar(
                        player,
                        Component.translatable(
                                "ui.apprenticecodex.companion_trunk.cannot_recall",
                                getDisplayName(player)
                        ).withStyle(ChatFormatting.RED)
                );
                return false;
            }

            sendActionBar(
                    player,
                    Component.translatable(
                            "ui.apprenticecodex.companion_trunk.cannot_despawn",
                            getDisplayName(player)
                    ).withStyle(ChatFormatting.RED)
            );
            return false;
        }

        var ownedTrunks = findOwnedTrunks(player.server, player.getUUID());
        if (!ownedTrunks.isEmpty()) {
            // 手動送還だけ送還音を鳴らして、他の自動消滅経路とは演出を分ける。
            ownedTrunks.get(0).playManualDismissSound();
        }
        discardOwnedTrunks(player.getUUID(), ownedTrunks, null);
        clearState(player, false);
        return true;
    }

    public static void removeOnlyEntity(ServerPlayer player) {
        discardOwnedTrunks(player.server, player.getUUID(), null);

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE);
        if (state.getTrunkUuid() != null) {
            spellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, s -> s.setTrunkUuid(null));
        }
    }

    public static void ensureActive(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE);
        if (!state.active || !player.isAlive()) {
            return;
        }

        var fixedMaxHealth = state.maxHealth > 0.0 ? state.maxHealth : DEFAULT_MAX_HEALTH;
        var spawned = normalizeOwnedTrunks(player, state.getTrunkUuid(), fixedMaxHealth, true);
        var spawnedUuid = spawned != null ? spawned.getUUID() : null;
        if (state.maxHealth == fixedMaxHealth && managedUuidEquals(state.getTrunkUuid(), spawnedUuid)) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, s -> {
            s.maxHealth = fixedMaxHealth;
            s.setTrunkUuid(spawnedUuid);
        });
    }

    public static void deactivateBecauseOwnerDied(ServerPlayer player) {
        var ownedTrunks = findOwnedTrunks(player.server, player.getUUID());
        if (ownedTrunks.isEmpty()) {
            dropStoredInventory(player);
        } else {
            for (var trunk : ownedTrunks) {
                trunk.dropAllContentsAndDiscard();
            }
        }

        clearState(player, true);
    }

    public static void onTrunkDestroyed(ServerPlayer owner, @Nullable CompanionTrunkEntity trunk) {
        var spellData = Capabilities.getSpellDataOrNull(owner);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, state -> {
            if (trunk == null || state.getTrunkUuid() == null || state.getTrunkUuid().equals(trunk.getUUID())) {
                state.active = false;
                state.maxHealth = 0.0;
                state.setTrunkUuid(null);
            }
        });
    }

    private static CompanionTrunkEntity spawn(ServerPlayer player, double maxHealth) {
        var level = player.serverLevel();
        var trunk = new CompanionTrunkEntity(EntityRegistry.COMPANION_TRUNK.get(), level, player);
        var summonPosition = findSummonPosition(player, trunk);
        trunk.moveTo(summonPosition.x, summonPosition.y, summonPosition.z, player.getYRot(), 0.0f);
        trunk.setCompanionMaxHealth((float) maxHealth);
        trunk.applyStoredCustomName();
        level.addFreshEntity(trunk);
        return trunk;
    }

    private static Vec3 findSummonPosition(ServerPlayer player, CompanionTrunkEntity trunk) {
        var basePos = player.blockPosition();
        var forward = getHorizontalLook(player);
        var candidateOffsets = new ArrayList<SummonOffset>();

        for (var x = -SUMMON_SEARCH_RADIUS; x <= SUMMON_SEARCH_RADIUS; ++x) {
            for (var z = -SUMMON_SEARCH_RADIUS; z <= SUMMON_SEARCH_RADIUS; ++z) {
                if (x == 0 && z == 0) {
                    continue;
                }

                var distanceSqr = x * x + z * z;
                if (distanceSqr > SUMMON_SEARCH_RADIUS * SUMMON_SEARCH_RADIUS) {
                    continue;
                }

                var horizontalOffset = new Vec3(x, 0.0, z).normalize();
                var priority = horizontalOffset.dot(forward) * 100.0 - distanceSqr;
                candidateOffsets.add(new SummonOffset(x, z, priority));
            }
        }

        candidateOffsets.sort((left, right) -> Double.compare(right.priority(), left.priority()));
        for (var offset : candidateOffsets) {
            var placement = tryBuildSummonPlacement(player, trunk, basePos.offset(offset.x(), 0, offset.z()));
            if (placement != null) {
                return placement;
            }
        }

        var fallback = tryBuildSummonPlacement(player, trunk, basePos);
        return fallback != null ? fallback : player.position();
    }

    private static @Nullable Vec3 tryBuildSummonPlacement(ServerPlayer player,
                                                          CompanionTrunkEntity trunk,
                                                          BlockPos basePos) {
        var level = player.serverLevel();
        for (var yOffset : SUMMON_Y_OFFSETS) {
            var placementPos = basePos.offset(0, yOffset, 0);
            if (!level.getBlockState(placementPos).canBeReplaced() || !level.getFluidState(placementPos).isEmpty()) {
                continue;
            }

            var headPos = placementPos.above();
            if (!level.getBlockState(headPos).canBeReplaced() || !level.getFluidState(headPos).isEmpty()) {
                continue;
            }

            var supportPos = placementPos.below();
            if (!level.getFluidState(supportPos).isEmpty()) {
                continue;
            }

            var supportShape = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
            if (supportShape.isEmpty()) {
                continue;
            }

            var spawnY = supportPos.getY() + supportShape.max(Direction.Axis.Y);
            var candidate = new Vec3(placementPos.getX() + 0.5, spawnY, placementPos.getZ() + 0.5);
            var bounds = trunk.getDimensions(trunk.getPose()).makeBoundingBox(candidate.x, candidate.y, candidate.z);
            if (level.noCollision(trunk, bounds)) {
                // 召喚直後の違和感を減らすため、初期位置は視線方向を優先しつつ足場と衝突を満たす地点に固定する。
                return candidate;
            }
        }

        return null;
    }

    private static Vec3 getHorizontalLook(ServerPlayer player) {
        var look = player.getLookAngle();
        var horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            return horizontal.normalize();
        }

        var facing = player.getDirection();
        var fallback = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
        return fallback.lengthSqr() > 1.0E-6 ? fallback.normalize() : new Vec3(0.0, 0.0, 1.0);
    }

    private static boolean isValidForOwner(@Nullable CompanionTrunkEntity trunk, ServerPlayer player) {
        return trunk != null
                && !trunk.isRemoved()
                && trunk.level() == player.level()
                && trunk.getOwner() == player;
    }

    private static @Nullable CompanionTrunkEntity normalizeOwnedTrunks(ServerPlayer player,
                                                                       @Nullable UUID managedUuid,
                                                                       double maxHealth,
                                                                       boolean shouldSpawn) {
        var ownedTrunks = findOwnedTrunks(player.server, player.getUUID());
        CompanionTrunkEntity primary = null;

        if (managedUuid != null) {
            for (var trunk : ownedTrunks) {
                if (managedUuid.equals(trunk.getUUID())) {
                    primary = trunk;
                    break;
                }
            }
        }

        if (!isValidForOwner(primary, player)) {
            primary = null;
        }

        if (primary == null) {
            for (var trunk : ownedTrunks) {
                if (isValidForOwner(trunk, player)) {
                    primary = trunk;
                    break;
                }
            }
        }

        if (primary == null && shouldSpawn) {
            primary = spawn(player, maxHealth);
            ownedTrunks.add(primary);
        } else if (primary != null) {
            primary.setCompanionMaxHealth((float) maxHealth);
            primary.applyStoredCustomName();
        }

        discardOwnedTrunks(player.getUUID(), ownedTrunks, primary);
        return primary;
    }

    private static List<CompanionTrunkEntity> findOwnedTrunks(MinecraftServer server, UUID ownerUuid) {
        var trunks = new ArrayList<CompanionTrunkEntity>();
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof CompanionTrunkEntity trunk
                        && !trunk.isRemoved()
                        && ownerUuid.equals(trunk.getOwnerUuid())) {
                    trunks.add(trunk);
                }
            }
        }
        return trunks;
    }

    private static void discardOwnedTrunks(MinecraftServer server, UUID ownerUuid, @Nullable CompanionTrunkEntity keep) {
        discardOwnedTrunks(ownerUuid, findOwnedTrunks(server, ownerUuid), keep);
    }

    private static void discardOwnedTrunks(UUID ownerUuid,
                                           List<CompanionTrunkEntity> ownedTrunks,
                                           @Nullable CompanionTrunkEntity keep) {
        if (ownedTrunks.isEmpty()) {
            return;
        }

        var discardedIds = new ArrayList<UUID>();
        for (var trunk : ownedTrunks) {
            if (keep != null && trunk == keep) {
                continue;
            }
            discardedIds.add(trunk.getUUID());
            trunk.discardWithoutInventory();
        }

        if (keep != null && !discardedIds.isEmpty()) {
            ApprenticeCodex.LOGGER.debug(
                    "CompanionTrunk duplicated entities detected for owner {}. kept={}, discarded={}",
                    ownerUuid,
                    keep.getUUID(),
                    discardedIds
            );
        }
    }

    private static void clearState(ServerPlayer player, boolean resetMaxHealth) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, state -> {
            state.active = false;
            if (resetMaxHealth) {
                state.maxHealth = 0.0;
            }
            state.setTrunkUuid(null);
        });
    }

    private static void dropStoredInventory(ServerPlayer player) {
        var storage = Capabilities.getCompanionTrunkInventoryOrNull(player);
        if (storage == null) {
            return;
        }

        var handler = storage.getHandler();
        for (var i = 0; i < handler.getSlots(); ++i) {
            var stack = handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
            }
        }
    }

    private static Component getDisplayName(ServerPlayer player) {
        var storage = Capabilities.getCompanionTrunkInventoryOrNull(player);
        if (storage != null) {
            var customName = storage.getCustomName();
            if (customName != null) {
                return customName;
            }
        }
        return Component.translatable("container.apprenticecodex.companion_trunk.default");
    }

    private static boolean managedUuidEquals(@Nullable UUID left, @Nullable UUID right) {
        return Objects.equals(left, right);
    }

    private static void sendActionBar(ServerPlayer player, Component message) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(message));
            return;
        }
        player.displayClientMessage(message, true);
    }

    private record SummonOffset(int x, int z, double priority) {
    }
}
