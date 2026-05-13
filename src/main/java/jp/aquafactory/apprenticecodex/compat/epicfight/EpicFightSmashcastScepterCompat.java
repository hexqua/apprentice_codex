package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.datafixers.util.Pair;
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
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets;
import yesman.epicfight.world.entity.eventlistener.BasicAttackEvent;
import yesman.epicfight.world.entity.eventlistener.DealDamageEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings({"unused", "removal"})
public final class EpicFightSmashcastScepterCompat {
    public static final String MOD_ID = "epicfight";
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter");

    private static final UUID SMASHCAST_SCEPTER_EVENT_UUID =
            UUID.fromString("0eadb38d-7580-41ee-bd6b-a94398ffcd64");
    private static final UUID EPIC_FIGHT_ATTACK_SPEED_MODIFIER_ID =
            UUID.fromString("9a8f0f18-76b6-46e4-a237-48cb2368ac8b");
    private static final String EPIC_FIGHT_ATTACK_SPEED_MODIFIER_NAME =
            "apprenticecodex.smashcast_scepter.epicfight_attack_speed";
    private static final double DESCENDING_ATTACK_Y_VELOCITY = -0.05D;
    private static final long DESCENDING_ATTACK_CONTEXT_TICKS = 20L;
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, DescendingAttackContext> DESCENDING_ATTACK_CONTEXTS = new ConcurrentHashMap<>();

    private EpicFightSmashcastScepterCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightSmashcastScepterCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightSmashcastScepterCompat::buildCapability);
    }

    private static CapabilityItem.Builder buildCapability(Item item) {
        var builder = (WeaponCapability.Builder) WeaponCapabilityPresets.SWORD.apply(item);

        builder.constructor(EpicFightSmashcastScepterCapability::new);
        builder.styleProvider(entityPatch -> CapabilityItem.Styles.ONE_HAND);
        builder.canBePlacedOffhand(false);
        builder.weaponCombinationPredicator(entityPatch -> false);
        builder.addStyleAttibutes(
                CapabilityItem.Styles.ONE_HAND,
                Pair.of(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                EPIC_FIGHT_ATTACK_SPEED_MODIFIER_ID,
                                EPIC_FIGHT_ATTACK_SPEED_MODIFIER_NAME,
                                1.0D,
                                AttributeModifier.Operation.ADDITION
                        )
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
            playerpatch.getEventListener().addEventListener(
                    EventType.SKILL_CAST_EVENT,
                    SMASHCAST_SCEPTER_EVENT_UUID,
                    EpicFightSmashcastScepterCompat::onSkillCast
            );
            playerpatch.getEventListener().addEventListener(
                    EventType.BASIC_ATTACK_EVENT,
                    SMASHCAST_SCEPTER_EVENT_UUID,
                    EpicFightSmashcastScepterCompat::onBasicAttack
            );
            playerpatch.getEventListener().addEventListener(
                    EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                    SMASHCAST_SCEPTER_EVENT_UUID,
                    EpicFightSmashcastScepterCompat::onDealDamage
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
        var allow = shouldAllowDescendingBasicAttack(event.getSkillContainer(), player);
        if (allow) {
            event.setStateExecutable(true);
        }
    }

    private static void onBasicAttack(BasicAttackEvent event) {
        var playerpatch = event.getPlayerPatch();
        if (!isDescendingSmashcastAttack(playerpatch.getOriginal())) {
            return;
        }

        // BasicAttack 本体は下降中を air attack として扱わないため、ここで片手剣標準の空中攻撃へ差し替える。
        var player = playerpatch.getOriginal();
        var fallDistance = Math.max(0.0F, player.fallDistance);
        DESCENDING_ATTACK_CONTEXTS.put(
                player.getUUID(),
                new DescendingAttackContext(player.serverLevel().getGameTime(), fallDistance)
        );
        event.setCanceled(true);
        playerpatch.playAnimationSynchronized(Animations.SWORD_AIR_SLASH, 0.0F);
    }

    private static void onDealDamage(DealDamageEvent.Damage event) {
        var playerpatch = event.getPlayerPatch();
        var player = playerpatch.getOriginal();
        if (!(player.getMainHandItem().getItem() instanceof SmashcastScepter)) {
            return;
        }

        var storedFallDistance = consumeStoredFallDistance(player);
        var effectiveFallDistance = Math.max(player.fallDistance, storedFallDistance);
        var smashAttack = isSmashAttack(player, effectiveFallDistance);
        if (!smashAttack) {
            return;
        }

        SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                player,
                event.getTarget(),
                event.getForgeEvent(),
                effectiveFallDistance
        );
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
