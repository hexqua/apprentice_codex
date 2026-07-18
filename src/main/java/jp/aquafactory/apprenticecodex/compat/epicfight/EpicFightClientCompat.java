package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.event.client.ClientSpellgunInputEvent;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import yesman.epicfight.api.client.input.InputManager;
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
    private static final UUID SPELLGUN_BASIC_ATTACK_CLIENT_EVENT_UUID =
            UUID.fromString("6a955f7d-d51c-4538-a7fd-d1e11c90f842");

    private static UUID installedSmashcastScepterPlayerId;
    private static UUID installedSpellgunPlayerId;

    private EpicFightClientCompat() {
    }

    public static boolean isBattleMode() {
        var clientEngine = ClientEngine.getInstance();
        return clientEngine != null && clientEngine.isEpicFightMode();
    }

    public static void register() {
        // RangedWeaponCapability は Find Weapon の自動判定対象外なので、アイテムクラスを明示登録する。
        ItemsPreferenceScreen.registerWeaponCategorizedItemClasses(
                MultipurposeStaffrifle.class,
                AbstractSpellGunItem.class
        );
    }

    public static void tick() {
        installSmashcastScepterEvents();
        installSpellgunEvents();
    }

    public static void clear() {
        installedSmashcastScepterPlayerId = null;
        installedSpellgunPlayerId = null;
    }

    public static boolean matchesAttackInput(InputConstants.Type type, int value) {
        return matchesKey(EpicFightInputAction.ATTACK.keyMapping(), type, value);
    }

    public static boolean isAttackActive() {
        return InputManager.isActionActive(EpicFightInputAction.ATTACK);
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

    private static void installSpellgunEvents() {
        var player = Minecraft.getInstance().player;
        var playerpatch = player != null ? EpicFightCapabilities.getLocalPlayerPatch(player) : null;
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedSpellgunPlayerId = null;
            return;
        }

        var playerId = playerpatch.getOriginal().getUUID();
        if (playerId.equals(installedSpellgunPlayerId)) {
            return;
        }

        playerpatch.getEventListener().addEventListener(
                EventType.SKILL_CAST_EVENT,
                SPELLGUN_BASIC_ATTACK_CLIENT_EVENT_UUID,
                EpicFightClientCompat::onSpellgunSkillCast,
                -1
        );
        installedSpellgunPlayerId = playerId;
    }

    private static void onSkillCast(SkillCastEvent event) {
        if (EpicFightSmashcastScepterCompat.shouldAllowDescendingBasicAttack(
                event.getSkillContainer(),
                event.getPlayerPatch().getOriginal()
        )) {
            event.setStateExecutable(true);
        }
    }

    private static void onSpellgunSkillCast(SkillCastEvent event) {
        if (!EpicFightSpellgunCompat.isMainhandSpellgunBasicAttack(
                event.getPlayerPatch(),
                event.getSkillContainer()
        ) || event.isCanceled() || !event.isExecutable()) {
            return;
        }

        // BasicAttack を成功扱いのまま通し、サーバー側の BASIC_ATTACK_EVENT で発射を合流させる。
        // 1.21.1 側では SkillCastEvent の priority/cancel 処理順を再確認する。
        ClientSpellgunInputEvent.trySendEpicFightMainhandCast();
    }

    private static boolean matchesKey(KeyMapping keyMapping, InputConstants.Type type, int value) {
        var key = keyMapping.getKey();
        return key.getType() == type && key.getValue() == value;
    }
}
