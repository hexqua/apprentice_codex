package jp.aquafactory.apprenticecodex.spell.automagnet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class AutoMagnetFamiliarManager {
    private static final double MIN_RANGE = 0.5;
    private static final double DEFAULT_RANGE = 8.0;

    private AutoMagnetFamiliarManager() {
    }

    public static void toggle(ServerPlayer player, double summonRange) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        if (state.active) {
            deactivate(player);
            return;
        }

        activate(player, summonRange);
    }

    public static void activate(ServerPlayer player, double summonRange) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var fixedRange = Math.max(MIN_RANGE, summonRange);
        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        var current = findByUuid(player.server, state.getFamiliarUuid());
        if (isValidForOwner(current, player)) {
            if (!state.active || state.range != fixedRange) {
                spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
                    s.active = true;
                    s.range = fixedRange;
                });
            }
            return;
        }

        if (current != null) {
            current.discard();
        }

        var spawned = spawn(player, fixedRange);
        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.active = true;
            s.range = fixedRange;
            s.setFamiliarUuid(spawned.getUUID());
        });
    }

    public static void deactivate(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        var familiar = findByUuid(player.server, state.getFamiliarUuid());
        if (familiar != null) {
            familiar.discard();
        }

        if (!state.active && state.getFamiliarUuid() == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.active = false;
            s.range = 0.0;
            s.setFamiliarUuid(null);
        });
    }

    public static void removeOnlyEntity(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        var familiar = findByUuid(player.server, state.getFamiliarUuid());
        if (familiar != null) {
            familiar.discard();
        }

        if (state.getFamiliarUuid() != null) {
            spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> s.setFamiliarUuid(null));
        }
    }

    public static void ensureActive(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE);
        if (!state.active || !player.isAlive()) {
            return;
        }

        var fixedRange = state.range > 0.0 ? state.range : DEFAULT_RANGE;
        var familiar = findByUuid(player.server, state.getFamiliarUuid());
        if (isValidForOwner(familiar, player)) {
            return;
        }

        if (familiar != null) {
            familiar.discard();
        }

        var spawned = spawn(player, fixedRange);
        spellData.edit(CodexSpellStateTypeRegister.AUTO_MAGNET_STATE, s -> {
            s.range = fixedRange;
            s.setFamiliarUuid(spawned.getUUID());
        });
    }

    private static AutoMagnetFamiliarEntity spawn(ServerPlayer player, double range) {
        var level = player.serverLevel();
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, player, range);
        level.addFreshEntity(familiar);
        return familiar;
    }

    private static boolean isValidForOwner(@Nullable AutoMagnetFamiliarEntity familiar, ServerPlayer player) {
        return familiar != null
                && !familiar.isRemoved()
                && familiar.level() == player.level()
                && familiar.getOwner() == player;
    }

    private static @Nullable AutoMagnetFamiliarEntity findByUuid(MinecraftServer server, @Nullable UUID entityUuid) {
        if (entityUuid == null) {
            return null;
        }

        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(entityUuid);
            if (entity instanceof AutoMagnetFamiliarEntity familiar && !familiar.isRemoved()) {
                return familiar;
            }
        }
        return null;
    }
}
