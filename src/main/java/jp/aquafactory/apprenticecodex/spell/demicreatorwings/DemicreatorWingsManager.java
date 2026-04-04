package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.DemicreatorWingsState;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.jetbrains.annotations.Nullable;

public final class DemicreatorWingsManager {
    public static final int ALERT_THRESHOLD_TICKS = 20 * 10;
    public static final int ALERT_FAST_THRESHOLD_TICKS = 20 * 3;
    public static final int ALERT_INTERVAL_TICKS = 20;
    public static final int ALERT_FAST_INTERVAL_TICKS = 10;

    private DemicreatorWingsManager() {
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData playerMagicData, DemicreatorWings spell) {
        deactivate(player, false);

        var level = player.serverLevel();
        var core = new DemicreatorWingsCoreEntity(
                EntityRegistry.DEMICREATOR_WINGS_CORE.get(),
                level,
                player,
                spell.getActivateArea(spellLevel, player),
                spell.getDuration(spellLevel, player)
        );
        core.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(core);

        var wing = new DemicreatorWingsWingEntity(EntityRegistry.DEMICREATOR_WINGS_WING.get(), level, player);
        level.addFreshEntity(wing);

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE, state -> {
            state.active = true;
            state.coreEntityId = core.getId();
            state.wingEntityId = wing.getId();
            state.grantedFlight = false;
        }));

        var recastInstance = new RecastInstance(
                spell.getSpellId(),
                spellLevel,
                spell.getRecastCount(spellLevel, player),
                spell.getDuration(spellLevel, player),
                castSource,
                DemicreatorWings.DemicreatorWingsCastData.openCast()
        );
        playerMagicData.getPlayerRecasts().addRecast(recastInstance, playerMagicData);
    }

    public static void deactivate(ServerPlayer player, boolean removeRecast) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        var magicData = MagicData.getPlayerMagicData(player);
        if (spellData == null) {
            if (removeRecast && magicData != null) {
                removeActiveRecast(magicData);
            }
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        discardManagedEntities(player, state);
        stripGrantedFlight(player, state.grantedFlight);

        if (removeRecast && magicData != null) {
            removeActiveRecast(magicData);
        }

        if (!state.active && state.coreEntityId < 0 && state.wingEntityId < 0 && !state.grantedFlight) {
            return;
        }

        if (state.active && player.isAlive()) {
            player.playNotifySound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        spellData.edit(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE, DemicreatorWingsState::reset);
    }

    public static boolean hasActiveCore(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        return state.active && getManagedCore(player, state) != null;
    }

    public static @Nullable DemicreatorWingsCoreEntity getManagedCore(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }
        return getManagedCore(player, spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE));
    }

    public static @Nullable DemicreatorWingsWingEntity ensureWing(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        if (!state.active) {
            return null;
        }

        var managedWing = getManagedWing(player, state);
        if (managedWing != null) {
            return managedWing;
        }

        var level = player.serverLevel();
        managedWing = new DemicreatorWingsWingEntity(EntityRegistry.DEMICREATOR_WINGS_WING.get(), level, player);
        level.addFreshEntity(managedWing);
        var spawnedWingId = managedWing.getId();
        spellData.edit(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE, current -> current.wingEntityId = spawnedWingId);
        return managedWing;
    }

    public static void ensureFlightGranted(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        if (!state.active) {
            return;
        }

        var abilities = player.getAbilities();
        if (abilities.instabuild || player.isSpectator()) {
            return;
        }

        if (!abilities.mayfly) {
            abilities.mayfly = true;
            player.onUpdateAbilities();
        }

        if (!state.grantedFlight) {
            spellData.edit(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE, current -> current.grantedFlight = true);
        }
    }

    public static boolean isInsideCoreArea(ServerPlayer player, DemicreatorWingsCoreEntity core) {
        return Math.abs(player.getX() - core.getX()) <= core.getAllowedRadius()
                && Math.abs(player.getZ() - core.getZ()) <= core.getAllowedRadius();
    }

    private static @Nullable DemicreatorWingsCoreEntity getManagedCore(ServerPlayer player, DemicreatorWingsState state) {
        if (!state.active || state.coreEntityId < 0) {
            return null;
        }

        var entity = player.level().getEntity(state.coreEntityId);
        return entity instanceof DemicreatorWingsCoreEntity core
                && !core.isRemoved()
                && core.getOwner() == player ? core : null;
    }

    private static @Nullable DemicreatorWingsWingEntity getManagedWing(ServerPlayer player, DemicreatorWingsState state) {
        if (!state.active || state.wingEntityId < 0) {
            return null;
        }

        var entity = player.level().getEntity(state.wingEntityId);
        return entity instanceof DemicreatorWingsWingEntity wing
                && !wing.isRemoved()
                && wing.getOwner() == player ? wing : null;
    }

    private static void discardManagedEntities(ServerPlayer player, DemicreatorWingsState state) {
        var core = getManagedCore(player, state);
        if (core != null) {
            core.discard();
        }

        var wing = getManagedWing(player, state);
        if (wing != null) {
            wing.discard();
        }
    }

    private static void stripGrantedFlight(ServerPlayer player, boolean grantedFlight) {
        if (!grantedFlight) {
            return;
        }

        Abilities abilities = player.getAbilities();
        if (abilities.instabuild || player.isSpectator()) {
            return;
        }

        var changed = false;
        if (abilities.flying) {
            abilities.flying = false;
            changed = true;
        }
        if (abilities.mayfly) {
            abilities.mayfly = false;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    private static void removeActiveRecast(MagicData playerMagicData) {
        var recast = playerMagicData.getPlayerRecasts().getRecastInstance(SpellRegistry.DEMICREATOR_WINGS.get().getSpellId());
        if (recast != null) {
            playerMagicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }
}
