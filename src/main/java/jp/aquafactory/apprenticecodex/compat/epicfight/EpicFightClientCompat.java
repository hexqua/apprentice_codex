package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.client.KeyMapping;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class EpicFightClientCompat {
    public static final String MOD_ID = "epicfight";
    private static final IdentifierProvider SMASHCAST_SCEPTER_CLIENT_EVENT_ID = IdentifierProvider.constant(
            "apprenticecodex:smashcast_scepter_client"
    );

    private static java.util.UUID installedSmashcastScepterPlayerId;

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
        var playerpatch = EpicFightCapabilities.getCachedLocalPlayerPatch();
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedSmashcastScepterPlayerId = null;
            return;
        }

        var playerId = playerpatch.getOriginal().getUUID();
        if (playerId.equals(installedSmashcastScepterPlayerId)) {
            return;
        }

        playerpatch.getEventListener().registerEvent(
                EpicFightEventHooks.Player.CAST_SKILL,
                EpicFightClientCompat::onSkillCast,
                SMASHCAST_SCEPTER_CLIENT_EVENT_ID
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
