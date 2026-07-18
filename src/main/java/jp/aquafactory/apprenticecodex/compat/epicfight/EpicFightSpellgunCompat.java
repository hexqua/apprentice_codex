package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightSpellgunCompat {
    public static final String MOD_ID = "epicfight";
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellgun");
    private static final IdentifierProvider COMBO_ATTACK_EVENT_ID = IdentifierProvider.constant(
            "apprenticecodex:spellgun_combo_attack"
    );
    private static final int PENDING_CAST_LIFETIME_TICKS = 4;
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingCast> PENDING_CASTS = new ConcurrentHashMap<>();

    private EpicFightSpellgunCompat() {
    }

    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightSpellgunCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:spellgun"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightSpellgunCompat::buildCapability);
    }

    private static RangedWeaponCapability.Builder buildCapability(Item item) {
        var builder = RangedWeaponCapability.builder()
                .zoomInType(CapabilityItem.ZoomInType.NONE);
        builder.constructor(EpicFightSpellgunCapability::new);
        return builder;
    }

    public static void install(ServerPlayer player) {
        if (!player.isAlive() || INSTALLED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerPatch -> {
            // ComboAttacks の検証後、他の通常 priority リスナーが攻撃を取り消していない場合だけ発射する。
            playerPatch.getEventListener().registerContextAwareEvent(
                    EpicFightEventHooks.Player.COMBO_ATTACK,
                    (event, context) -> EpicFightSpellgunCompat.onComboAttack(event),
                    COMBO_ATTACK_EVENT_ID,
                    -1
            );
            INSTALLED_PLAYERS.add(player.getUUID());
        });
    }

    public static void tick(ServerPlayer player) {
        var pendingCast = PENDING_CASTS.get(player.getUUID());
        if (pendingCast != null && pendingCast.isExpired(player.level().getGameTime())) {
            PENDING_CASTS.remove(player.getUUID(), pendingCast);
        }
    }

    public static void clear(ServerPlayer player) {
        INSTALLED_PLAYERS.remove(player.getUUID());
        PENDING_CASTS.remove(player.getUUID());
    }

    public static boolean queueMainhandCast(ServerPlayer player, BlockTargetData targetData) {
        if (!isMainhandSpellgunBasicAttack(player)) {
            return false;
        }

        PENDING_CASTS.put(
                player.getUUID(),
                new PendingCast(
                        targetData == null ? new BlockTargetData() : targetData.copy(),
                        player.level().getGameTime() + PENDING_CAST_LIFETIME_TICKS
                )
        );
        return true;
    }

    private static void onComboAttack(ComboAttackEvent event) {
        var player = event.getPlayerPatch().getOriginal();
        var pendingCast = PENDING_CASTS.remove(player.getUUID());
        if (pendingCast == null
                || event.isCanceled()
                || pendingCast.isExpired(player.level().getGameTime())
                || !isMainhandSpellgunBasicAttack(player)) {
            return;
        }

        if (player.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun) {
            // 発射失敗を ComboAttacks の成否へ戻さない。Capability 側はモーションを返さないが、
            // このイベントまで到達させることでクライアントの成功扱いと入力解放を維持する。
            spellgun.tryTriggerImbuedSpell(player, InteractionHand.MAIN_HAND, pendingCast.targetData());
        }
    }

    public static boolean canUseOffhandSpellgun(ServerPlayer player) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        return playerPatch == null || playerPatch.isVanillaMode() || playerPatch.isOffhandItemValid();
    }

    public static boolean shouldPrioritizeOffhandSpellgun(PlayerPatch<?> playerPatch) {
        return playerPatch != null
                && playerPatch.isEpicFightMode()
                && playerPatch.getOriginal().getOffhandItem().getItem() instanceof AbstractSpellGunItem
                && playerPatch.isOffhandItemValid();
    }

    public static boolean isMainhandSpellgunBasicAttack(
            PlayerPatch<?> playerPatch,
            SkillContainer skillContainer
    ) {
        return playerPatch != null
                && skillContainer != null
                && skillContainer.getSlot() == SkillSlots.COMBO_ATTACKS
                && playerPatch.isEpicFightMode()
                && playerPatch.getOriginal().getMainHandItem().getItem() instanceof AbstractSpellGunItem;
    }

    public static boolean isMainhandSpellgunBasicAttack(ServerPlayer player) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        return playerPatch != null
                && isMainhandSpellgunBasicAttack(playerPatch, playerPatch.getSkill(SkillSlots.COMBO_ATTACKS));
    }

    public static boolean enterBattleMode(ServerPlayer player) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        if (playerPatch == null) {
            return false;
        }

        playerPatch.toEpicFightMode(false);
        return playerPatch.isEpicFightMode();
    }

    public static boolean hasExpectedSpellgunCapability(ServerPlayer player, ItemStack stack) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        var capability = EpicFightCapabilities.getItemStackCapability(stack);
        return playerPatch != null
                && capability instanceof EpicFightSpellgunCapability
                && capability.getStyle(playerPatch) == CapabilityItem.Styles.ONE_HAND
                && capability.canBePlacedOffhand()
                && capability.canHoldInOffhandAlone()
                && capability.getUseAnimation(playerPatch) == UseAnim.NONE
                && capability.getInnateSkill(playerPatch, stack) == null;
    }

    public static boolean hasNoSpellgunAttackMotion(ServerPlayer player, ItemStack stack) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        var capability = EpicFightCapabilities.getItemStackCapability(stack);
        if (playerPatch == null || !(capability instanceof EpicFightSpellgunCapability)) {
            return false;
        }

        return capability.getAutoAttackMotion(playerPatch) == null;
    }

    public static boolean canExecuteGuard(ServerPlayer player) {
        var playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        if (playerPatch == null) {
            return false;
        }

        var guardContainer = playerPatch.getSkill(SkillSlots.GUARD);
        guardContainer.setSkill(EpicFightSkills.GUARD.get());
        return EpicFightSkills.GUARD.get().canExecute(guardContainer);
    }

    private record PendingCast(BlockTargetData targetData, long expiresAt) {
        private boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }
    }
}
