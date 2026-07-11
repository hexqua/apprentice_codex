package jp.aquafactory.apprenticecodex.spell.servantgaze;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

public final class ServantGazeManager {
    private ServantGazeManager() {}

    public static boolean toggle(ServerPlayer player, int spellLevel, float damage, double radius, int manaCost) {
        var data = Capabilities.getSpellDataOrNull(player);
        if (data == null) return false;
        var state = data.get(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE);
        if (state.active) {
            deactivate(player);
            return false;
        }
        activate(player, spellLevel, damage, radius, manaCost);
        return true;
    }

    public static void activate(ServerPlayer player, int spellLevel, float damage, double radius, int manaCost) {
        var data = Capabilities.getSpellDataOrNull(player);
        if (data == null) return;
        var staff = normalize(player, null, spellLevel, damage, radius, manaCost, true);
        data.edit(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE, state -> {
            state.active = true;
            state.spellLevel = spellLevel;
            state.damage = damage;
            state.radius = radius;
            state.attackManaCost = manaCost;
            state.setStaffUuid(staff == null ? null : staff.getUUID());
        });
    }

    public static void deactivate(ServerPlayer player) {
        var data = Capabilities.getSpellDataOrNull(player);
        if (data == null) return;
        discardOwned(player, null);
        data.edit(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE, state -> {
            state.active = false;
            state.setStaffUuid(null);
        });
    }

    public static void removeOnlyEntity(ServerPlayer player) {
        var data = Capabilities.getSpellDataOrNull(player);
        if (data == null) return;
        discardOwned(player, null);
        data.edit(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE, state -> state.setStaffUuid(null));
    }

    public static void ensureActive(ServerPlayer player) {
        var data = Capabilities.getSpellDataOrNull(player);
        if (data == null) return;
        var state = data.get(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE);
        if (!state.active || !player.isAlive()) return;
        var staff = normalize(player, state.getStaffUuid(), state.spellLevel, state.damage,
                state.radius, state.attackManaCost, true);
        var uuid = staff == null ? null : staff.getUUID();
        if (!java.util.Objects.equals(uuid, state.getStaffUuid())) {
            data.edit(CodexSpellStateTypeRegister.SERVANT_GAZE_STATE, value -> value.setStaffUuid(uuid));
        }
    }

    private static @Nullable ServantGazeStaffEntity normalize(ServerPlayer player, @Nullable UUID managedUuid,
                                                               int spellLevel, float damage, double radius,
                                                               int manaCost, boolean spawn) {
        var owned = findOwned(player);
        ServantGazeStaffEntity primary = null;
        for (var staff : owned) {
            if (managedUuid != null && managedUuid.equals(staff.getUUID()) && staff.level() == player.level()) {
                primary = staff;
                break;
            }
        }
        if (primary == null) {
            for (var staff : owned) {
                if (staff.level() == player.level()) {
                    primary = staff;
                    break;
                }
            }
        }
        if (primary == null && spawn) {
            primary = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), player.serverLevel(), player);
            player.serverLevel().addFreshEntity(primary);
            owned.add(primary);
        }
        discardOwned(player, primary);
        if (primary != null) primary.configure(spellLevel, damage, radius, manaCost);
        return primary;
    }

    private static ArrayList<ServantGazeStaffEntity> findOwned(ServerPlayer player) {
        var result = new ArrayList<ServantGazeStaffEntity>();
        for (var level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ServantGazeStaffEntity staff && !staff.isRemoved()
                        && staff.getOwner() != null && staff.getOwner().getUUID().equals(player.getUUID())) result.add(staff);
            }
        }
        return result;
    }

    private static void discardOwned(ServerPlayer player, @Nullable ServantGazeStaffEntity keep) {
        for (var staff : findOwned(player)) if (staff != keep) staff.discard();
    }
}
