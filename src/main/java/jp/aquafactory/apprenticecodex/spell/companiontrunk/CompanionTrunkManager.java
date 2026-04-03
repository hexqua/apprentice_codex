package jp.aquafactory.apprenticecodex.spell.companiontrunk;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CompanionTrunkManager {
    private static final double DEFAULT_MAX_HEALTH = 20.0;

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

        var storage = Capabilities.getCompanionTrunkInventoryOrNull(player);
        if (storage != null && !storage.isEmpty()) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(
                            "ui.apprenticecodex.companion_trunk.cannot_despawn",
                            getDisplayName(player)
                    ).withStyle(ChatFormatting.RED)
            ));
            return false;
        }

        discardOwnedTrunks(player.server, player.getUUID(), null);
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
        trunk.setCompanionMaxHealth((float) maxHealth);
        trunk.applyStoredCustomName();
        level.addFreshEntity(trunk);
        return trunk;
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
        return left == null ? right == null : left.equals(right);
    }
}
