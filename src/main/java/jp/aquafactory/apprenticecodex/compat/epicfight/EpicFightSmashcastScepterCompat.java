package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.datafixers.util.Pair;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.BasicAttackEvent;
import yesman.epicfight.world.entity.eventlistener.DealDamageEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightSmashcastScepterCompat {
    public static final String MOD_ID = "epicfight";
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter");
    private static final ResourceLocation SWORD_PRESET_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "sword");

    static Skill WIND_LEAP;

    private static final UUID SMASHCAST_SCEPTER_EVENT_UUID =
            UUID.fromString("0eadb38d-7580-41ee-bd6b-a94398ffcd64");
    private static final UUID EPIC_FIGHT_ATTACK_SPEED_MODIFIER_ID =
            UUID.fromString("9a8f0f18-76b6-46e4-a237-48cb2368ac8b");
    private static final String EPIC_FIGHT_ATTACK_SPEED_MODIFIER_NAME =
            "apprenticecodex.smashcast_scepter.epicfight_attack_speed";
    private static final double DESCENDING_ATTACK_Y_VELOCITY = -0.05D;
    private static final long DESCENDING_ATTACK_CONTEXT_TICKS = 20L;
    private static final long WIND_LEAP_CONTEXT_TICKS = 120L;
    private static final double WIND_LEAP_AUTO_HIT_INFLATE = 0.75D;
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, DescendingAttackContext> DESCENDING_ATTACK_CONTEXTS = new ConcurrentHashMap<>();
    private static final Map<UUID, WindLeapContext> WIND_LEAP_CONTEXTS = new ConcurrentHashMap<>();

    private EpicFightSmashcastScepterCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightSmashcastScepterCompat::onSkillBuild);
        modEventBus.addListener(EpicFightSmashcastScepterCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onSkillBuild(SkillBuildEvent event) {
        var worker = event.createRegistryWorker(ApprenticeCodex.MODID);
        WIND_LEAP = worker.build(
                "wind_leap",
                EpicFightWindLeapSkill::new,
                EpicFightWindLeapSkill.createWindLeapBuilder()
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(
                WEAPON_TYPE_ID,
                item -> EpicFightSmashcastScepterCompat.buildCapability(item, SWORD_PRESET_ID)
        );
    }

    private static CapabilityItem.Builder buildCapability(Item item, ResourceLocation basePresetId) {
        var builder = (WeaponCapability.Builder) WeaponTypeReloadListener.getOrThrow(basePresetId.toString()).apply(item);

        builder.constructor(EpicFightSmashcastScepterCapability::new);
        builder.styleProvider(entityPatch -> CapabilityItem.Styles.ONE_HAND);
        builder.canBePlacedOffhand(false);
        builder.weaponCombinationPredicator(entityPatch -> false);
        // 1.20.1 の Epic Fight では MoveSet.addInnateSkill が無いため builder 経由で登録する。
        // 1.21.1 へ移植する際は ONE_HAND の MoveSet 側へ移す。
        //noinspection deprecation
        builder.innateSkill(CapabilityItem.Styles.ONE_HAND, stack -> WIND_LEAP);
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
        WIND_LEAP_CONTEXTS.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        var context = WIND_LEAP_CONTEXTS.get(player.getUUID());
        if (context == null) {
            return;
        }

        var currentGameTime = player.serverLevel().getGameTime();
        if (!player.isAlive()
                || player.onGround()
                || currentGameTime - context.gameTime() > WIND_LEAP_CONTEXT_TICKS
                || !(player.getMainHandItem().getItem() instanceof SmashcastScepter)) {
            WIND_LEAP_CONTEXTS.remove(player.getUUID());
            return;
        }

        if (!isSmashAttack(player, player.fallDistance)) {
            return;
        }

        findNearestWindLeapTarget(player).ifPresent(target -> {
            WIND_LEAP_CONTEXTS.remove(player.getUUID());
            attackWithWindLeap(player, target);
        });
    }

    static void launchWindLeap(ServerPlayerPatch playerpatch, int chargeTicks) {
        var player = playerpatch.getOriginal();
        if (!(player.getMainHandItem().getItem() instanceof SmashcastScepter)) {
            return;
        }

        var direction = resolveWindLeapDirection(playerpatch);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = Vec3.ZERO;
        }

        var chargeRatio = Math.min(1.0D, Math.max(0.0D, chargeTicks / (double) EpicFightWindLeapSkill.MAX_CHARGING_TICKS));
        var upward = EpicFightWindLeapSkill.MIN_UPWARD_IMPULSE
                + (EpicFightWindLeapSkill.MAX_UPWARD_IMPULSE - EpicFightWindLeapSkill.MIN_UPWARD_IMPULSE) * chargeRatio;
        var horizontalSpeed = direction.lengthSqr() < 1.0E-6D ? 0.0D : estimateWindLeapHorizontalSpeed(upward);
        var movement = direction.scale(horizontalSpeed).add(0.0D, upward, 0.0D);

        player.setDeltaMovement(movement);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        playerpatch.playAnimationSynchronized(Animations.BIPED_DEMOLITION_LEAP, 0.0F);
        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                io.redspace.ironsspellbooks.registries.SoundRegistry.GUST_CAST.get(),
                SoundSource.PLAYERS,
                0.8F,
                1.05F
        );
        WIND_LEAP_CONTEXTS.put(player.getUUID(), new WindLeapContext(player.serverLevel().getGameTime()));
    }

    private static Vec3 resolveWindLeapDirection(ServerPlayerPatch playerpatch) {
        var player = playerpatch.getOriginal();
        var target = playerpatch.getTarget();
        if (isValidWindLeapTarget(player, target)) {
            return horizontalDirection(player.position(), target.position());
        }

        var raycast = RaycastTools.raycastFromEye(
                player,
                EpicFightWindLeapSkill.TARGET_RAYCAST_DISTANCE,
                1.25D,
                entity -> entity instanceof LivingEntity livingEntity && isValidWindLeapTarget(player, livingEntity)
        );
        if (raycast.hitEntity() instanceof LivingEntity livingEntity) {
            return horizontalDirection(player.position(), livingEntity.position());
        }

        var look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-6D) {
            look = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
        }
        return look.lengthSqr() < 1.0E-6D ? Vec3.ZERO : look.normalize();
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        var horizontal = to.subtract(from).multiply(1.0D, 0.0D, 1.0D);
        return horizontal.lengthSqr() < 1.0E-6D ? Vec3.ZERO : horizontal.normalize();
    }

    private static double estimateWindLeapHorizontalSpeed(double upward) {
        var chargeRatio = (upward - EpicFightWindLeapSkill.MIN_UPWARD_IMPULSE)
                / (EpicFightWindLeapSkill.MAX_UPWARD_IMPULSE - EpicFightWindLeapSkill.MIN_UPWARD_IMPULSE);
        return EpicFightWindLeapSkill.TARGET_DISTANCE * (0.075D + 0.012D * Math.max(0.0D, Math.min(1.0D, chargeRatio)));
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

    private static Optional<LivingEntity> findNearestWindLeapTarget(ServerPlayer player) {
        var playerHitBox = player.getBoundingBox().inflate(WIND_LEAP_AUTO_HIT_INFLATE);
        var searchArea = playerHitBox.inflate(0.5D);
        return player.serverLevel()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        searchArea,
                        target -> isValidWindLeapTarget(player, target)
                                && target.getBoundingBox().intersects(playerHitBox)
                )
                .stream()
                .min(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(player.position())));
    }

    private static boolean isValidWindLeapTarget(Player player, LivingEntity target) {
        return target != null
                && target != player
                && target.isAlive()
                && !target.isSpectator()
                && !player.isAlliedTo(target);
    }

    private static void attackWithWindLeap(ServerPlayer player, LivingEntity target) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerpatch -> {
            DESCENDING_ATTACK_CONTEXTS.put(
                    player.getUUID(),
                    new DescendingAttackContext(player.serverLevel().getGameTime(), Math.max(0.0F, player.fallDistance))
            );

            // Wind Leap はスマッシュ成立のお膳立てに留め、スマッシュ効果は onDealDamage の既存条件でだけ発火させる。
            // SWORD_AIR_SLASH は縦移動を持つ ActionAnimation のため、再生すると Wind Leap / Wind Burst の打ち上げを上書きする。
            // 1.21.1 へ移植する際も、DamageSource の属性付けと攻撃モーション再生を分けて扱う。
            var damageSource = playerpatch.getDamageSource(Animations.SWORD_AIR_SLASH, InteractionHand.MAIN_HAND)
                    .attachArmorNegationModifier(ValueModifier.adder(50.0F))
                    .attachImpactModifier(ValueModifier.multiplier(1.6F))
                    .setStunType(StunType.NONE)
                    .addRuntimeTag(EpicFightDamageTypeTags.WEAPON_INNATE);
            playerpatch.attack(damageSource, target, InteractionHand.MAIN_HAND);
        });
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

    private record WindLeapContext(long gameTime) {
    }
}
