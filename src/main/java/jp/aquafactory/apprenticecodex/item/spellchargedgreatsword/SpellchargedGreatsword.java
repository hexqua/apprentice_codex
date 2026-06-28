package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellchargedGreatswordServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.SpellchargedGreatswordRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class SpellchargedGreatsword extends SwordItem implements GeoItem, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellcharged_greatsword.desc_";
    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final double DISPLAY_ATTACK_DAMAGE = 8.0D;
    public static final double DISPLAY_ATTACK_SPEED = 1.1D;
    public static final double MAX_CHARGE_TICKS = 800.0D;
    public static final int MAX_GAIN_TICKS = 200;
    public static final int SHORT_CAST_THRESHOLD_TICKS = 40;
    public static final int DECAY_DELAY_TICKS = 100;
    public static final double DECAY_TICKS_PER_TICK = MAX_CHARGE_TICKS * 0.1D / 20.0D;
    public static final int LEVEL_1_THRESHOLD_TICKS = 200;
    public static final int LEVEL_2_THRESHOLD_TICKS = 400;
    public static final int LEVEL_3_THRESHOLD_TICKS = 800;
    public static final int OVERCHARGE_ACTIVATION_HOLD_TICKS = 20;
    public static final int OVERCHARGE_LEVEL_2_DURATION_TICKS = 200;
    public static final int OVERCHARGE_LEVEL_3_DURATION_TICKS = 600;
    public static final int OVERCHARGE_AURA_FADE_TICKS = 20;

    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    private static final double ATTACK_SPEED_MODIFIER_AMOUNT = DISPLAY_ATTACK_SPEED - 4.0D;
    private static final String TAG_CHARGE_TICKS = "SpellchargedGreatswordChargeTicks";
    private static final String TAG_LAST_CHARGE_GAME_TIME = "SpellchargedGreatswordLastChargeGameTime";
    private static final String TAG_CHARGE_LEVEL = "SpellchargedGreatswordChargeLevel";
    private static final String TAG_OVERCHARGE_REMAINING_TICKS = "SpellchargedGreatswordOverchargeRemainingTicks";
    private static final String TAG_OVERCHARGE_MAX_TICKS = "SpellchargedGreatswordOverchargeMaxTicks";
    private static final String TAG_OVERCHARGE_ACTIVATED_GAME_TIME = "SpellchargedGreatswordOverchargeActivatedGameTime";
    private static final String TAG_OVERCHARGE_END_GAME_TIME = "SpellchargedGreatswordOverchargeEndGameTime";
    private static final String TAG_OVERCHARGE_FADE_START_GAME_TIME = "SpellchargedGreatswordOverchargeFadeStartGameTime";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK =
            new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final Set<ResourceLocation> EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom")
    );
    private static final UUID ENTITY_REACH_MODIFIER_ID = UUID.nameUUIDFromBytes(
            "apprenticecodex:spellcharged_greatsword/entity_reach".getBytes(StandardCharsets.UTF_8)
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpellchargedGreatsword() {
        super(SpellchargedGreatswordTier.INSTANCE, 4, (float) ATTACK_SPEED_MODIFIER_AMOUNT,
                new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }

        return buildMainhandModifiers(stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (isOverchargeActive(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        if (getChargeLevel(stack, level.getGameTime()) < 2) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity livingEntity, @NotNull ItemStack stack,
                          int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!(livingEntity instanceof Player)) {
            return;
        }

        if (!isOverchargeActive(stack) && getChargeLevel(stack, level.getGameTime()) >= 2) {
            freezeChargeDecay(stack, level.getGameTime());
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity,
                             int timeLeft) {
        super.releaseUsing(stack, level, livingEntity, timeLeft);
        if (!(livingEntity instanceof Player player) || isOverchargeActive(stack)) {
            return;
        }

        var elapsedTicks = getUseDuration(stack) - timeLeft;
        var chargeLevel = getChargeLevel(stack, level.getGameTime());
        if (elapsedTicks >= OVERCHARGE_ACTIVATION_HOLD_TICKS && chargeLevel >= 2) {
            startOvercharge(stack, level.getGameTime(), chargeLevel);
            playOverchargeActivationSound(level, player);
            syncMainhandIfServer(player, stack);
        }
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 72000;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) {
            return;
        }

        cleanupExpiredAuraFade(stack, level.getGameTime());
        if (!hasOverchargeState(stack)) {
            return;
        }

        if (!isOverchargeActive(stack, level.getGameTime())) {
            clearOvercharge(stack, level.getGameTime(), true);
            syncMainhandIfServer(player, stack);
        }

    }

    public static double computeChargeGainTicks(AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
            return 0.0D;
        }

        return computeChargeGainTicks(spell.getCastTime(spellLevel), spell.getSpellCooldown());
    }

    public static double computeChargeGainTicks(int castTimeTicks, int cooldownTicks) {
        var baseTicks = Math.min(MAX_GAIN_TICKS, Math.max(0, castTimeTicks) + Math.max(0, cooldownTicks));
        if (baseTicks <= SHORT_CAST_THRESHOLD_TICKS) {
            return baseTicks * 0.5D;
        }
        return baseTicks;
    }

    public static boolean addCharge(ItemStack stack, long gameTime, double chargeTicks) {
        if (!isSpellchargedGreatsword(stack) || isOverchargeActive(stack) || chargeTicks <= 0.0D) {
            return false;
        }

        var currentCharge = getEffectiveChargeTicks(stack, gameTime);
        var previousLevel = currentCharge > 0.0D ? getChargeLevel(stack) : 0;
        var nextCharge = Mth.clamp(currentCharge + chargeTicks, 0.0D, MAX_CHARGE_TICKS);
        var nextLevel = Math.max(previousLevel, computeChargeLevel(nextCharge));

        var tag = stack.getOrCreateTag();
        tag.putDouble(TAG_CHARGE_TICKS, nextCharge);
        tag.putLong(TAG_LAST_CHARGE_GAME_TIME, gameTime);
        tag.putInt(TAG_CHARGE_LEVEL, nextLevel);
        return nextLevel > previousLevel;
    }

    public static boolean refreshChargeDecayDelay(ItemStack stack, long gameTime) {
        if (!isSpellchargedGreatsword(stack) || isOverchargeActive(stack) || !hasChargeState(stack)) {
            return false;
        }

        stack.getOrCreateTag().putLong(TAG_LAST_CHARGE_GAME_TIME, gameTime);
        return true;
    }

    public static boolean refreshDecay(ItemStack stack, long gameTime) {
        if (!isSpellchargedGreatsword(stack) || !hasChargeState(stack)) {
            return false;
        }

        if (getEffectiveChargeTicks(stack, gameTime) > 0.0D) {
            return false;
        }

        resetCharge(stack);
        return true;
    }

    public static void resetCharge(ItemStack stack) {
        if (!isSpellchargedGreatsword(stack) || !hasChargeState(stack)) {
            return;
        }

        var tag = stack.getOrCreateTag();
        tag.remove(TAG_CHARGE_TICKS);
        tag.remove(TAG_LAST_CHARGE_GAME_TIME);
        tag.remove(TAG_CHARGE_LEVEL);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static double getEffectiveChargeTicks(ItemStack stack, long gameTime) {
        return getEffectiveChargeTicks(stack, (double) gameTime);
    }

    public static double getEffectiveChargeTicks(ItemStack stack, double gameTime) {
        if (!isSpellchargedGreatsword(stack) || isOverchargeActive(stack) || !stack.hasTag()) {
            return 0.0D;
        }

        var tag = stack.getOrCreateTag();
        var storedCharge = Mth.clamp(tag.getDouble(TAG_CHARGE_TICKS), 0.0D, MAX_CHARGE_TICKS);
        var lastChargeGameTime = tag.contains(TAG_LAST_CHARGE_GAME_TIME)
                ? tag.getLong(TAG_LAST_CHARGE_GAME_TIME)
                : (long) Math.floor(gameTime);
        var decayElapsed = gameTime - lastChargeGameTime - DECAY_DELAY_TICKS;
        if (decayElapsed <= 0.0D) {
            return storedCharge;
        }

        return Mth.clamp(storedCharge - decayElapsed * DECAY_TICKS_PER_TICK, 0.0D, MAX_CHARGE_TICKS);
    }

    public static int getChargeLevel(ItemStack stack) {
        if (!isSpellchargedGreatsword(stack) || isOverchargeActive(stack) || !stack.hasTag()) {
            return 0;
        }

        return Mth.clamp(stack.getOrCreateTag().getInt(TAG_CHARGE_LEVEL), 0, 3);
    }

    public static int getChargeLevel(ItemStack stack, long gameTime) {
        return getEffectiveChargeTicks(stack, gameTime) > 0.0D ? getChargeLevel(stack) : 0;
    }

    public static int computeChargeLevel(double chargeTicks) {
        if (chargeTicks >= LEVEL_3_THRESHOLD_TICKS) {
            return 3;
        }
        if (chargeTicks >= LEVEL_2_THRESHOLD_TICKS) {
            return 2;
        }
        if (chargeTicks >= LEVEL_1_THRESHOLD_TICKS) {
            return 1;
        }
        return 0;
    }

    public static boolean isOverchargeActive(ItemStack stack) {
        return hasOverchargeState(stack);
    }

    public static boolean isOverchargeActive(ItemStack stack, double gameTime) {
        return isSpellchargedGreatsword(stack)
                && stack.hasTag()
                && resolveOverchargeRemainingTicks(stack, gameTime) > 0;
    }

    public static int getSweepingEdgeLevelBonus(ItemStack stack) {
        if (!isSpellchargedGreatsword(stack)) {
            return 0;
        }

        var config = ApprenticeCodexServerConfig.spellchargedGreatswordConfig();
        return isOverchargeActive(stack)
                ? config.overchargeSweepingEdgeLevelBonus()
                : config.normalSweepingEdgeLevelBonus();
    }

    private static int resolveOverchargeRemainingTicks(ItemStack stack, double gameTime) {
        if (!hasOverchargeState(stack)) {
            return 0;
        }

        var tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_OVERCHARGE_END_GAME_TIME)) {
            return tag.getInt(TAG_OVERCHARGE_REMAINING_TICKS);
        }

        return Math.max(0, (int) Math.ceil(tag.getLong(TAG_OVERCHARGE_END_GAME_TIME) - gameTime));
    }

    public static float getOverchargeRemainingRatio(ItemStack stack) {
        if (!hasOverchargeState(stack)) {
            return 0.0F;
        }

        var tag = stack.getOrCreateTag();
        var maxTicks = Math.max(1, tag.getInt(TAG_OVERCHARGE_MAX_TICKS));
        return Mth.clamp(tag.getInt(TAG_OVERCHARGE_REMAINING_TICKS) / (float) maxTicks, 0.0F, 1.0F);
    }

    public static float getOverchargeRemainingRatio(ItemStack stack, double gameTime) {
        if (!hasOverchargeState(stack)) {
            return 0.0F;
        }

        var tag = stack.getOrCreateTag();
        var maxTicks = Math.max(1, tag.getInt(TAG_OVERCHARGE_MAX_TICKS));
        return Mth.clamp(resolveOverchargeRemainingTicks(stack, gameTime) / (float) maxTicks, 0.0F, 1.0F);
    }

    public static float getOverchargeAuraIntensity(ItemStack stack, double gameTime) {
        if (isOverchargeActive(stack, gameTime)) {
            return 1.0F;
        }
        if (!isSpellchargedGreatsword(stack) || !stack.hasTag()) {
            return 0.0F;
        }

        var tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_OVERCHARGE_FADE_START_GAME_TIME)) {
            return 0.0F;
        }

        var elapsed = gameTime - tag.getLong(TAG_OVERCHARGE_FADE_START_GAME_TIME);
        if (elapsed < 0.0D || elapsed >= OVERCHARGE_AURA_FADE_TICKS) {
            return 0.0F;
        }

        var progress = Mth.clamp(elapsed / OVERCHARGE_AURA_FADE_TICKS, 0.0D, 1.0D);
        return 1.0F - Mth.sin((float) (progress * Mth.HALF_PI));
    }

    public static void resetAllChargeState(ItemStack stack) {
        resetCharge(stack);
        clearOvercharge(stack, 0L, false);
    }

    private static boolean hasChargeState(ItemStack stack) {
        if (!stack.hasTag()) {
            return false;
        }

        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(TAG_CHARGE_TICKS)
                || tag.contains(TAG_LAST_CHARGE_GAME_TIME)
                || tag.contains(TAG_CHARGE_LEVEL);
    }

    private static boolean isSpellchargedGreatsword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpellchargedGreatsword;
    }

    private static boolean hasOverchargeState(ItemStack stack) {
        return isSpellchargedGreatsword(stack)
                && stack.hasTag()
                && stack.getOrCreateTag().contains(TAG_OVERCHARGE_REMAINING_TICKS)
                && stack.getOrCreateTag().getInt(TAG_OVERCHARGE_REMAINING_TICKS) > 0;
    }

    private static void startOvercharge(ItemStack stack, long gameTime, int chargeLevel) {
        var durationTicks = chargeLevel >= 3
                ? OVERCHARGE_LEVEL_3_DURATION_TICKS
                : OVERCHARGE_LEVEL_2_DURATION_TICKS;
        resetCharge(stack);

        var tag = stack.getOrCreateTag();
        tag.putInt(TAG_OVERCHARGE_REMAINING_TICKS, durationTicks);
        tag.putInt(TAG_OVERCHARGE_MAX_TICKS, durationTicks);
        tag.putLong(TAG_OVERCHARGE_ACTIVATED_GAME_TIME, gameTime);
        tag.putLong(TAG_OVERCHARGE_END_GAME_TIME, gameTime + durationTicks);
        tag.remove(TAG_OVERCHARGE_FADE_START_GAME_TIME);
    }

    private static void freezeChargeDecay(ItemStack stack, long gameTime) {
        if (!isSpellchargedGreatsword(stack) || !hasChargeState(stack)) {
            return;
        }

        var currentCharge = getEffectiveChargeTicks(stack, gameTime);
        var tag = stack.getOrCreateTag();
        tag.putDouble(TAG_CHARGE_TICKS, currentCharge);
        tag.putLong(TAG_LAST_CHARGE_GAME_TIME, gameTime);
        tag.putInt(TAG_CHARGE_LEVEL, Math.max(getChargeLevel(stack), computeChargeLevel(currentCharge)));
    }

    private static void playOverchargeActivationSound(Level level, Player player) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundRegistry.SPELLCHARGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static void clearOvercharge(ItemStack stack, long gameTime, boolean keepAuraFade) {
        if (!isSpellchargedGreatsword(stack) || !stack.hasTag()) {
            return;
        }

        var tag = stack.getOrCreateTag();
        var wasActive = tag.contains(TAG_OVERCHARGE_REMAINING_TICKS);
        tag.remove(TAG_OVERCHARGE_REMAINING_TICKS);
        tag.remove(TAG_OVERCHARGE_MAX_TICKS);
        tag.remove(TAG_OVERCHARGE_ACTIVATED_GAME_TIME);
        tag.remove(TAG_OVERCHARGE_END_GAME_TIME);
        if (keepAuraFade && wasActive) {
            tag.putLong(TAG_OVERCHARGE_FADE_START_GAME_TIME, gameTime);
        } else {
            tag.remove(TAG_OVERCHARGE_FADE_START_GAME_TIME);
        }
        cleanupEmptyTag(stack);
    }

    private static void cleanupExpiredAuraFade(ItemStack stack, long gameTime) {
        if (!isSpellchargedGreatsword(stack) || !stack.hasTag()) {
            return;
        }

        var tag = stack.getOrCreateTag();
        if (tag.contains(TAG_OVERCHARGE_FADE_START_GAME_TIME)
                && gameTime - tag.getLong(TAG_OVERCHARGE_FADE_START_GAME_TIME) >= OVERCHARGE_AURA_FADE_TICKS) {
            tag.remove(TAG_OVERCHARGE_FADE_START_GAME_TIME);
            cleanupEmptyTag(stack);
        }
    }

    private static void cleanupEmptyTag(ItemStack stack) {
        if (stack.hasTag() && stack.getOrCreateTag().isEmpty()) {
            stack.setTag(null);
        }
    }

    private static void syncMainhandIfServer(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getMainHandItem() == stack) {
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            return true;
        }

        return EXTRA_ENCHANTMENTS.contains(enchantmentId)
                || enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ToolAction toolAction) {
        return ToolActions.SWORD_SWEEP == toolAction || super.canPerformAction(stack, toolAction);
    }

    @Override
    public @NotNull AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target) {
        if (!isOverchargeActive(stack)) {
            return super.getSweepHitBox(stack, player, target);
        }

        return target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SpellchargedGreatswordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SpellchargedGreatswordRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        var overcharged = isOverchargeActive(stack);
        var normalizedChargeLevel = overcharged ? 0 : Mth.clamp(getChargeLevel(stack), 0, 3);
        var config = ApprenticeCodexServerConfig.spellchargedGreatswordConfig();
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        ATTACK_DAMAGE_MODIFIER_AMOUNT
                                + (overcharged
                                ? config.overchargeAttackDamageBonus()
                                : chargeAttackDamageBonus(config, normalizedChargeLevel)),
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_MODIFIER_AMOUNT
                                + (overcharged
                                ? config.overchargeAttackSpeedBonus()
                                : chargeAttackSpeedBonus(config, normalizedChargeLevel)),
                        AttributeModifier.Operation.ADDITION
                )
        );
        var entityReachBonus = overcharged ? config.overchargeEntityReachBonus() : config.normalEntityReachBonus();
        if (entityReachBonus != 0.0D) {
            builder.put(
                    ForgeMod.ENTITY_REACH.get(),
                    new AttributeModifier(
                            ENTITY_REACH_MODIFIER_ID,
                            "Spellcharged greatsword entity reach",
                            entityReachBonus,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }
        return builder.build();
    }

    private static double chargeAttackDamageBonus(
            SpellchargedGreatswordServerConfig.Values config,
            int chargeLevel
    ) {
        return switch (chargeLevel) {
            case 1 -> config.chargeLevel1AttackDamageBonus();
            case 2 -> config.chargeLevel2AttackDamageBonus();
            case 3 -> config.chargeLevel3AttackDamageBonus();
            default -> 0.0D;
        };
    }

    private static double chargeAttackSpeedBonus(
            SpellchargedGreatswordServerConfig.Values config,
            int chargeLevel
    ) {
        return switch (chargeLevel) {
            case 1 -> config.chargeLevel1AttackSpeedBonus();
            case 2 -> config.chargeLevel2AttackSpeedBonus();
            case 3 -> config.chargeLevel3AttackSpeedBonus();
            default -> 0.0D;
        };
    }

    private enum SpellchargedGreatswordTier implements Tier {
        INSTANCE;

        @Override
        public int getUses() {
            return DURABILITY;
        }

        @Override
        public float getSpeed() {
            return 8.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 3.0F;
        }

        @Override
        public int getLevel() {
            return 3;
        }

        @Override
        public int getEnchantmentValue() {
            return ENCHANTMENT_VALUE;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.of(ItemRegistry.ARCANE_INGOT.get());
        }
    }
}
