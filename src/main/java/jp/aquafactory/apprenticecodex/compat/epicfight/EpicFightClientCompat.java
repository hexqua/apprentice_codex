package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.event.client.ClientSpellgunInputEvent;
import jp.aquafactory.apprenticecodex.event.client.ClientSwingMagicAttackTrigger;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.eventlistener.AnimationBeginEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

import java.util.UUID;

public final class EpicFightClientCompat {
    public static final String MOD_ID = "epicfight";
    private static final UUID SMASHCAST_SCEPTER_CLIENT_EVENT_UUID =
            UUID.fromString("e514912a-b67e-4f6a-9382-3302b0f70d94");
    private static final UUID BASIC_ATTACK_CLIENT_EVENT_UUID =
            UUID.fromString("6a955f7d-d51c-4538-a7fd-d1e11c90f842");
    private static final UUID ATTACK_ANIMATION_CLIENT_EVENT_UUID =
            UUID.fromString("a3e92bf0-6f8b-4ed8-b3b4-774d51947b93");

    // ディメンション移動では UUID が同じまま Patch が再生成されるため、登録済み Patch の同一性を追跡する。
    private static LocalPlayerPatch installedSmashcastScepterPlayerPatch;
    private static LocalPlayerPatch installedAttackPlayerPatch;

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
        installAttackEvents();
    }

    public static void clear() {
        installedSmashcastScepterPlayerPatch = null;
        installedAttackPlayerPatch = null;
    }

    public static boolean matchesAttackInput(InputConstants.Type type, int value) {
        return matchesKey(EpicFightInputAction.ATTACK.keyMapping(), type, value);
    }

    public static boolean isAttackActive() {
        return InputManager.isActionActive(EpicFightInputAction.ATTACK);
    }

    public static boolean canHandleAttackInput() {
        var player = Minecraft.getInstance().player;
        var playerpatch = player != null ? EpicFightCapabilities.getLocalPlayerPatch(player) : null;
        return playerpatch != null && playerpatch.canPlayAttackAnimation();
    }

    private static void installSmashcastScepterEvents() {
        var player = Minecraft.getInstance().player;
        var playerpatch = player != null ? EpicFightCapabilities.getLocalPlayerPatch(player) : null;
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedSmashcastScepterPlayerPatch = null;
            return;
        }

        if (playerpatch == installedSmashcastScepterPlayerPatch) {
            return;
        }

        playerpatch.getEventListener().addEventListener(
                EventType.SKILL_CAST_EVENT,
                SMASHCAST_SCEPTER_CLIENT_EVENT_UUID,
                EpicFightClientCompat::onSkillCast
        );
        installedSmashcastScepterPlayerPatch = playerpatch;
    }

    private static void installAttackEvents() {
        var player = Minecraft.getInstance().player;
        var playerpatch = player != null ? EpicFightCapabilities.getLocalPlayerPatch(player) : null;
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedAttackPlayerPatch = null;
            return;
        }

        if (playerpatch == installedAttackPlayerPatch) {
            return;
        }

        playerpatch.getEventListener().addEventListener(
                EventType.SKILL_CAST_EVENT,
                BASIC_ATTACK_CLIENT_EVENT_UUID,
                EpicFightClientCompat::onBasicAttackSkillCast,
                -1
        );
        playerpatch.getEventListener().addEventListener(
                EventType.ANIMATION_BEGIN_EVENT,
                ATTACK_ANIMATION_CLIENT_EVENT_UUID,
                EpicFightClientCompat::onAttackAnimationBegin
        );
        installedAttackPlayerPatch = playerpatch;
    }

    private static void onSkillCast(SkillCastEvent event) {
        if (EpicFightSmashcastScepterCompat.shouldAllowDescendingBasicAttack(
                event.getSkillContainer(),
                event.getPlayerPatch().getOriginal()
        )) {
            event.setStateExecutable(true);
        }
    }

    private static void onBasicAttackSkillCast(SkillCastEvent event) {
        if (event.isCanceled() || !event.isExecutable()) {
            return;
        }

        if (EpicFightSpellgunCompat.isMainhandSpellgunBasicAttack(
                event.getPlayerPatch(),
                event.getSkillContainer()
        )) {
            // BasicAttack を成功扱いのまま通し、サーバー側の BASIC_ATTACK_EVENT で発射を合流させる。
            // 1.21.1 側では SkillCastEvent の priority/cancel 処理順を再確認する。
            ClientSpellgunInputEvent.trySendEpicFightMainhandCast();
            return;
        }

    }

    private static void onAttackAnimationBegin(AnimationBeginEvent event) {
        if (event.getPlayerPatch() != null
                && event.getPlayerPatch().isEpicFightMode()
                && event.getAnimation() instanceof AttackAnimation) {
            // Epic Fight は vanilla の攻撃パケットを使わないため、実際の攻撃アニメーション開始時に対象だけを同期する。
            ClientSwingMagicAttackTrigger.trySyncTargetsForEpicFight(Minecraft.getInstance());
        }
    }

    private static boolean matchesKey(KeyMapping keyMapping, InputConstants.Type type, int value) {
        var key = keyMapping.getKey();
        return key.getType() == type && key.getValue() == value;
    }
}
