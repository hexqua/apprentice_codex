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
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.animation.AnimationBeginEvent;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class EpicFightClientCompat {
    public static final String MOD_ID = "epicfight";
    private static final IdentifierProvider SMASHCAST_SCEPTER_CLIENT_EVENT_ID = IdentifierProvider.constant(
            "apprenticecodex:smashcast_scepter_client"
    );
    private static final IdentifierProvider SPELLGUN_BASIC_ATTACK_CLIENT_EVENT_ID = IdentifierProvider.constant(
            "apprenticecodex:spellgun_basic_attack_client"
    );
    private static final IdentifierProvider ATTACK_ANIMATION_CLIENT_EVENT_ID = IdentifierProvider.constant(
            "apprenticecodex:attack_animation_client"
    );

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
        var playerpatch = EpicFightCapabilities.getCachedLocalPlayerPatch();
        if (playerpatch == null || playerpatch.getOriginal() == null || !playerpatch.getOriginal().isAlive()) {
            installedSmashcastScepterPlayerPatch = null;
            return;
        }

        if (playerpatch == installedSmashcastScepterPlayerPatch) {
            return;
        }

        playerpatch.getEventListener().registerEvent(
                EpicFightEventHooks.Player.CAST_SKILL,
                EpicFightClientCompat::onSkillCast,
                SMASHCAST_SCEPTER_CLIENT_EVENT_ID
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

        playerpatch.getEventListener().registerContextAwareEvent(
                EpicFightEventHooks.Player.CAST_SKILL,
                (event, context) -> EpicFightClientCompat.onSpellgunSkillCast(event),
                SPELLGUN_BASIC_ATTACK_CLIENT_EVENT_ID,
                -1
        );
        playerpatch.getEventListener().registerEvent(
                EpicFightEventHooks.Animation.BEGIN,
                EpicFightClientCompat::onAttackAnimationBegin,
                ATTACK_ANIMATION_CLIENT_EVENT_ID
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

    private static void onSpellgunSkillCast(SkillCastEvent event) {
        if (!EpicFightSpellgunCompat.isMainhandSpellgunBasicAttack(
                event.getPlayerPatch(),
                event.getSkillContainer()
        ) || event.isCanceled() || !event.isExecutable()) {
            return;
        }

        // ComboAttacks を成功扱いのまま通し、サーバー側の COMBO_ATTACK で発射を合流させる。
        ClientSpellgunInputEvent.trySendEpicFightMainhandCast();
    }

    private static void onAttackAnimationBegin(AnimationBeginEvent event) {
        if (event.getEntityPatch().isEpicFightMode()
                && event.getAnimation().get() instanceof AttackAnimation) {
            // Epic Fight は vanilla の攻撃パケットを使わないため、実際の攻撃アニメーション開始時に対象だけを同期する。
            ClientSwingMagicAttackTrigger.trySyncTargetsForEpicFight(Minecraft.getInstance());
        }
    }

    private static boolean matchesKey(KeyMapping keyMapping, InputConstants.Type type, int value) {
        var key = keyMapping.getKey();
        return key.getType() == type && key.getValue() == value;
    }
}
