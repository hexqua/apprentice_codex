package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.entity.DealDamageEvent;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightSmashcastScepterCompat {
    public static final String MOD_ID = "epicfight";
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter");

    private static final ResourceLocation SWORD_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(EpicFightCompat.MOD_ID, "sword");
    private static final IdentifierProvider SMASHCAST_SCEPTER_EVENT_ID = IdentifierProvider.constant(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter")
    );
    private static final ResourceLocation EPIC_FIGHT_ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter_epicfight_attack_speed");
    private static final double DESCENDING_ATTACK_Y_VELOCITY = -0.05D;
    private static final long DESCENDING_ATTACK_CONTEXT_TICKS = 20L;
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, DescendingAttackContext> DESCENDING_ATTACK_CONTEXTS = new ConcurrentHashMap<>();

    private EpicFightSmashcastScepterCompat() {
    }

    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightSmashcastScepterCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:smashcast_scepter"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, item -> buildCapability(event, item));
    }

    private static CapabilityItem.Builder<?> buildCapability(WeaponCapabilityPresetRegistryEvent event, Item item) {
        var swordFactory = event.getTypeEntry().get(SWORD_TYPE_ID);
        var builder = swordFactory != null
                ? (WeaponCapability.Builder) swordFactory.apply(item)
                : WeaponCapability.builder();

        builder.constructor(EpicFightSmashcastScepterCapability::new);
        builder.styleProvider(entityPatch -> CapabilityItem.Styles.ONE_HAND);
        builder.canBePlacedOffhand(false);
        builder.weaponCombinationPredicator(entityPatch -> false);
        builder.addStyleAttibutes(
                CapabilityItem.Styles.ONE_HAND,
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        EPIC_FIGHT_ATTACK_SPEED_MODIFIER_ID,
                        1.0D,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );

        return builder;
    }

    public static void install(ServerPlayer player) {
        if (!player.isAlive() || INSTALLED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerpatch -> {
            // 下降中の基本攻撃は Epic Fight 標準では弾かれるため、SmashcastScepter 装備時だけ入力を通す。
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Player.CAST_SKILL,
                    EpicFightSmashcastScepterCompat::onSkillCast,
                    SMASHCAST_SCEPTER_EVENT_ID
            );
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Player.COMBO_ATTACK,
                    EpicFightSmashcastScepterCompat::onComboAttack,
                    SMASHCAST_SCEPTER_EVENT_ID
            );
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE,
                    EpicFightSmashcastScepterCompat::onDealDamagePre,
                    SMASHCAST_SCEPTER_EVENT_ID
            );
            INSTALLED_PLAYERS.add(player.getUUID());
        });
    }

    public static void clear(ServerPlayer player) {
        INSTALLED_PLAYERS.remove(player.getUUID());
        DESCENDING_ATTACK_CONTEXTS.remove(player.getUUID());
    }

    private static void onSkillCast(SkillCastEvent event) {
        var player = event.getPlayerPatch().getOriginal();
        if (shouldAllowDescendingBasicAttack(event.getSkillContainer(), player)) {
            event.setStateExecutable(true);
        }
    }

    private static void onComboAttack(ComboAttackEvent event) {
        var playerpatch = event.getPlayerPatch();
        if (!isDescendingSmashcastAttack(playerpatch.getOriginal())) {
            return;
        }

        // ComboAttack 本体は下降中を air attack として扱わないため、ここで片手剣標準の空中攻撃へ差し替える。
        var player = playerpatch.getOriginal();
        var fallDistance = Math.max(0.0F, player.fallDistance);
        DESCENDING_ATTACK_CONTEXTS.put(
                player.getUUID(),
                new DescendingAttackContext(player.serverLevel().getGameTime(), fallDistance)
        );
        event.cancel();
        playerpatch.playAnimationSynchronized(Animations.SWORD_AIR_SLASH, 0.0F);
    }

    private static void onDealDamagePre(DealDamageEvent.Pre event) {
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch playerpatch)) {
            return;
        }

        var player = playerpatch.getOriginal();
        if (!(player.getMainHandItem().getItem() instanceof SmashcastScepter)) {
            return;
        }

        var storedFallDistance = consumeStoredFallDistance(player);
        var effectiveFallDistance = Math.max(player.fallDistance, storedFallDistance);
        if (!isSmashAttack(player, effectiveFallDistance)) {
            return;
        }

        var bonusDamage = SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                player,
                event.getTarget(),
                event.getDamageSource(),
                effectiveFallDistance
        );
        if (bonusDamage > 0.0F) {
            event.setModifiedDamage(event.getModifiedDamage() + bonusDamage);
        }
    }

    public static boolean shouldAllowDescendingBasicAttack(SkillContainer skillContainer, Player player) {
        return skillContainer != null
                && skillContainer.getSlot() == SkillSlots.BASIC_ATTACK
                && isDescendingSmashcastAttack(player);
    }

    private static boolean isDescendingSmashcastAttack(Player player) {
        return player != null
                && player.getMainHandItem().getItem() instanceof SmashcastScepter
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.SLOW_FALLING)
                && player.getDeltaMovement().y < DESCENDING_ATTACK_Y_VELOCITY;
    }

    private static float consumeStoredFallDistance(ServerPlayer player) {
        var context = DESCENDING_ATTACK_CONTEXTS.remove(player.getUUID());
        if (context == null) {
            return 0.0F;
        }
        if (player.serverLevel().getGameTime() - context.gameTime() > DESCENDING_ATTACK_CONTEXT_TICKS) {
            return 0.0F;
        }
        return context.fallDistance();
    }

    private static boolean isSmashAttack(Player player, float fallDistance) {
        return player != null
                && fallDistance > SmashcastScepter.SMASH_ATTACK_FALL_DISTANCE_THRESHOLD
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.SLOW_FALLING);
    }

    private record DescendingAttackContext(long gameTime, float fallDistance) {
    }
}
