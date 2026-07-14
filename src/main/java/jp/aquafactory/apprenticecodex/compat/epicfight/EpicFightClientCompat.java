package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

import java.util.UUID;

public final class EpicFightClientCompat {
    public static final String MOD_ID = "epicfight";
    private static final UUID SMASHCAST_SCEPTER_CLIENT_EVENT_UUID =
            UUID.fromString("e514912a-b67e-4f6a-9382-3302b0f70d94");

    private static UUID installedSmashcastScepterPlayerId;

    private EpicFightClientCompat() {
    }

    public static boolean isBattleMode() {
        var clientEngine = ClientEngine.getInstance();
        return clientEngine != null && clientEngine.isEpicFightMode();
    }

    public static void register() {
        ItemsPreferenceScreen.registerWeaponCategorizedItemClasses(MultipurposeStaffrifle.class);
    }

    public static void tick() {
        installSmashcastScepterEvents();
    }

    public static void clear() {
        installedSmashcastScepterPlayerId = null;
    }

    public static boolean matchesAttackInput(InputConstants.Type type, int value) {
        return matchesKey(EpicFightInputAction.ATTACK.keyMapping(), type, value);
    }

    private static void installSmashcastScepterEvents() {
        var player = Minecraft.getInstance().player;
        var playerpatch = player != null ? EpicFightCapabilities.getLocalPlayerPatch(player) : null;
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedSmashcastScepterPlayerId = null;
            return;
        }

        var playerId = playerpatch.getOriginal().getUUID();
        if (playerId.equals(installedSmashcastScepterPlayerId)) {
            return;
        }

        playerpatch.getEventListener().addEventListener(
                EventType.SKILL_CAST_EVENT,
                SMASHCAST_SCEPTER_CLIENT_EVENT_UUID,
                EpicFightClientCompat::onSkillCast
        );
        installedSmashcastScepterPlayerId = playerId;
    }

    private static void onSkillCast(SkillCastEvent event) {
        if (EpicFightSmashcastScepterCompat.shouldAllowDescendingBasicAttack(
                event.getSkillContainer(),
                event.getPlayerPatch().getOriginal()
        )) {
            event.setStateExecutable(true);
        }
    }

    private static boolean matchesKey(KeyMapping keyMapping, InputConstants.Type type, int value) {
        var key = keyMapping.getKey();
        return key.getType() == type && key.getValue() == value;
    }
}
