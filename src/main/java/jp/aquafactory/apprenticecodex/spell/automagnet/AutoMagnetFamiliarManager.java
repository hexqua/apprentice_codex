package jp.aquafactory.apprenticecodex.spell.automagnet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AutoMagnetFamiliarManager {
    private static final double MIN_RANGE = 0.5;
    private static final double DEFAULT_RANGE = 8.0;

    private AutoMagnetFamiliarManager() {
    }

    public static boolean toggle(ServerPlayer player, double summonRange, double collectMana, AutoMagnetCollectionMode collectionMode) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        if (state.active && state.getCollectionMode() == collectionMode) {
            deactivate(player);
            return false;
        }

        activate(player, summonRange, collectMana, collectionMode);
        return true;
    }

    public static void activate(ServerPlayer player, double summonRange, double collectMana) {
        activate(player, summonRange, collectMana, AutoMagnetCollectionMode.NORMAL);
    }

    public static void activate(ServerPlayer player, double summonRange, double collectMana, AutoMagnetCollectionMode collectionMode) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var fixedRange = Math.max(MIN_RANGE, summonRange);
        var fixedCollectMana = Math.max(0.0, collectMana);
        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        var managedUuid = state.getFamiliarUuid();
        var spawned = normalizeOwnedFamiliars(player, managedUuid, fixedRange, fixedCollectMana, collectionMode, true);
        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.active = true;
            s.range = fixedRange;
            s.collectMana = fixedCollectMana;
            s.setCollectionMode(collectionMode);
            s.setFamiliarUuid(spawned != null ? spawned.getUUID() : null);
        });
    }

    public static void deactivate(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        discardOwnedFamiliars(player.server, player.getUUID(), null);

        if (!state.active && state.getFamiliarUuid() == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.active = false;
            s.range = 0.0;
            s.collectMana = 0.0;
            s.setCollectionMode(AutoMagnetCollectionMode.NORMAL);
            s.setFamiliarUuid(null);
        });
    }

    public static void removeOnlyEntity(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        discardOwnedFamiliars(player.server, player.getUUID(), null);

        if (state.getFamiliarUuid() != null) {
            spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> s.setFamiliarUuid(null));
        }
    }

    public static void ensureActive(ServerPlayer player) {
        maintainActive(player, false);
    }

    public static void reconcileActive(ServerPlayer player) {
        maintainActive(player, true);
    }

    private static void maintainActive(ServerPlayer player, boolean reconcile) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        if (!state.active || !player.isAlive()) {
            return;
        }

        var fixedRange = state.range > 0.0 ? state.range : DEFAULT_RANGE;
        var fixedCollectMana = Math.max(0.0, state.collectMana);
        var collectionMode = state.getCollectionMode();
        var spawned = reconcile ? null : resolveManagedFamiliar(player, state.getFamiliarUuid());
        if (spawned == null) {
            spawned = normalizeOwnedFamiliars(
                    player, state.getFamiliarUuid(), fixedRange, fixedCollectMana, collectionMode, true);
        } else {
            spawned.configureCollection(fixedRange, fixedCollectMana, collectionMode);
        }
        var spawnedUuid = spawned != null ? spawned.getUUID() : null;
        if (state.range == fixedRange
                && state.collectMana == fixedCollectMana
                && state.getCollectionMode() == collectionMode
                && managedUuidEquals(state.getFamiliarUuid(), spawnedUuid)) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.range = fixedRange;
            s.collectMana = fixedCollectMana;
            s.setCollectionMode(collectionMode);
            s.setFamiliarUuid(spawnedUuid);
        });
    }

    private static @Nullable AutoMagnetFamiliarEntity resolveManagedFamiliar(ServerPlayer player,
                                                                              @Nullable UUID managedUuid) {
        if (managedUuid == null) {
            return null;
        }
        var entity = player.serverLevel().getEntity(managedUuid);
        return entity instanceof AutoMagnetFamiliarEntity familiar && isValidForOwner(familiar, player)
                ? familiar
                : null;
    }

    private static AutoMagnetFamiliarEntity spawn(ServerPlayer player, double range, double collectMana,
                                                 AutoMagnetCollectionMode collectionMode) {
        var level = player.serverLevel();
        var familiar = new AutoMagnetFamiliarEntity(
                EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, player, range, collectMana, collectionMode);
        level.addFreshEntity(familiar);
        return familiar;
    }

    private static boolean isValidForOwner(@Nullable AutoMagnetFamiliarEntity familiar, ServerPlayer player) {
        return familiar != null
                && !familiar.isRemoved()
                && familiar.level() == player.level()
                && familiar.getOwner() == player;
    }

    private static @Nullable AutoMagnetFamiliarEntity normalizeOwnedFamiliars(ServerPlayer player,
                                                                              @Nullable UUID managedUuid,
                                                                              double range,
                                                                              double collectMana,
                                                                              AutoMagnetCollectionMode collectionMode,
                                                                              boolean shouldSpawn) {
        var ownedFamiliars = findOwnedFamiliars(player.server, player.getUUID());
        AutoMagnetFamiliarEntity primary = null;

        if (managedUuid != null) {
            for (var familiar : ownedFamiliars) {
                if (managedUuid.equals(familiar.getUUID())) {
                    primary = familiar;
                    break;
                }
            }
        }

        if (!isValidForOwner(primary, player)) {
            primary = null;
        }

        if (primary == null) {
            for (var familiar : ownedFamiliars) {
                if (isValidForOwner(familiar, player)) {
                    primary = familiar;
                    break;
                }
            }
        }

        if (primary == null && shouldSpawn) {
            primary = spawn(player, range, collectMana, collectionMode);
            ownedFamiliars.add(primary);
        }

        discardOwnedFamiliars(player.getUUID(), ownedFamiliars, primary);
        if (primary != null) {
            primary.configureCollection(range, collectMana, collectionMode);
        }
        return primary;
    }

    private static List<AutoMagnetFamiliarEntity> findOwnedFamiliars(MinecraftServer server, UUID ownerUuid) {
        var familiars = new ArrayList<AutoMagnetFamiliarEntity>();
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof AutoMagnetFamiliarEntity familiar
                        && !familiar.isRemoved()
                        && hasOwner(familiar, ownerUuid)) {
                    familiars.add(familiar);
                }
            }
        }
        return familiars;
    }

    private static void discardOwnedFamiliars(MinecraftServer server, UUID ownerUuid, @Nullable AutoMagnetFamiliarEntity keep) {
        discardOwnedFamiliars(ownerUuid, findOwnedFamiliars(server, ownerUuid), keep);
    }

    private static void discardOwnedFamiliars(UUID ownerUuid,
                                              List<AutoMagnetFamiliarEntity> ownedFamiliars,
                                              @Nullable AutoMagnetFamiliarEntity keep) {
        if (ownedFamiliars.isEmpty()) {
            return;
        }
        var discardedIds = new ArrayList<UUID>();
        for (var familiar : ownedFamiliars) {
            if (keep != null && familiar == keep) {
                continue;
            }
            discardedIds.add(familiar.getUUID());
            familiar.discard();
        }

        if (keep != null && !discardedIds.isEmpty()) {
            ApprenticeCodex.LOGGER.debug(
                    "AutoMagnet duplicated familiars detected for owner {}. kept={}, discarded={}",
                    ownerUuid,
                    keep.getUUID(),
                    discardedIds
            );
        }
    }

    private static boolean hasOwner(AutoMagnetFamiliarEntity familiar, UUID ownerUuid) {
        var owner = familiar.getOwner();
        return owner != null && owner.getUUID().equals(ownerUuid);
    }

    private static boolean managedUuidEquals(@Nullable UUID left, @Nullable UUID right) {
        return left == null ? right == null : left.equals(right);
    }
}
