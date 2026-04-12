package jp.aquafactory.apprenticecodex.spell.healingbloom;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HealingBloomManager {
    private HealingBloomManager() {
    }

    public static boolean hasManagedBloom(ServerPlayer player) {
        return getManagedBloomUuid(player) != null;
    }

    public static @Nullable UUID getManagedBloomUuid(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE);
        var managedUuid = state.getBloomUuid();
        if (managedUuid != null) {
            var managedBloom = findOwnedBloom(player.server, player.getUUID(), managedUuid);
            if (managedBloom != null) {
                if (managedBloom.isAlive()) {
                    return managedUuid;
                }

                spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, current -> current.setBloomUuid(null));
                managedUuid = null;
            }
        }

        var loadedBlooms = findOwnedBlooms(player.server, player.getUUID());
        if (!loadedBlooms.isEmpty()) {
            var adoptedUuid = loadedBlooms.get(0).getUUID();
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, current -> current.setBloomUuid(adoptedUuid));
            return adoptedUuid;
        }

        if (managedUuid != null) {
            // チャンク外の旧 Bloom と消滅済み Bloom はここだけでは区別できない。
            // 一旦 stale state を外し、再設置後に旧 Bloom が読み込まれたら unmanaged として静かに除去する。
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, current -> current.setBloomUuid(null));
        }
        return null;
    }

    public static void registerBloom(ServerPlayer player, HealingBloomEntity bloom, boolean replaceExisting) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE);
        var previousUuid = state.getBloomUuid();
        if (replaceExisting && previousUuid != null && !previousUuid.equals(bloom.getUUID())) {
            var previousBloom = findOwnedBloom(player.server, player.getUUID(), previousUuid);
            if (previousBloom != null && previousBloom != bloom) {
                previousBloom.dieFromReplacement();
            }
        }

        discardAliveDuplicates(player.server, player.getUUID(), bloom, previousUuid);
        spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, current -> current.setBloomUuid(bloom.getUUID()));
    }

    public static boolean shouldKeepLoadedBloom(ServerPlayer owner, HealingBloomEntity bloom) {
        var spellData = Capabilities.getSpellDataOrNull(owner);
        if (spellData == null) {
            return true;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE);
        var managedUuid = state.getBloomUuid();
        if (managedUuid == null) {
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, current -> current.setBloomUuid(bloom.getUUID()));
            return true;
        }

        return managedUuid.equals(bloom.getUUID());
    }

    public static void onBloomRemoved(ServerPlayer owner, @Nullable HealingBloomEntity bloom) {
        var spellData = Capabilities.getSpellDataOrNull(owner);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, state -> {
            if (bloom == null || state.getBloomUuid() == null || state.getBloomUuid().equals(bloom.getUUID())) {
                state.setBloomUuid(null);
            }
        });
    }

    private static void discardAliveDuplicates(MinecraftServer server,
                                               UUID ownerUuid,
                                               HealingBloomEntity keep,
                                               @Nullable UUID replacementTargetUuid) {
        for (var bloom : findOwnedBlooms(server, ownerUuid)) {
            if (bloom == keep) {
                continue;
            }
            if (replacementTargetUuid != null && replacementTargetUuid.equals(bloom.getUUID())) {
                continue;
            }
            if (bloom.isAlive()) {
                bloom.discardWithoutDeath();
            }
        }
    }

    private static @Nullable HealingBloomEntity findOwnedBloom(MinecraftServer server, UUID ownerUuid, UUID bloomUuid) {
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(bloomUuid);
            if (entity instanceof HealingBloomEntity bloom
                    && !bloom.isRemoved()
                    && ownerUuid.equals(bloom.getOwnerUuid())) {
                return bloom;
            }
        }
        return null;
    }

    private static List<HealingBloomEntity> findOwnedBlooms(MinecraftServer server, UUID ownerUuid) {
        var blooms = new ArrayList<HealingBloomEntity>();
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof HealingBloomEntity bloom
                        && bloom.isAlive()
                        && !bloom.isRemoved()
                        && ownerUuid.equals(bloom.getOwnerUuid())) {
                    blooms.add(bloom);
                }
            }
        }
        return blooms;
    }
}
