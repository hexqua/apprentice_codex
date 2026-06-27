package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

public final class SpellchargedGreatsword extends SwordItem implements GeoItem, UniqueItem {
    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final double DISPLAY_ATTACK_DAMAGE = 8.0D;
    public static final double DISPLAY_ATTACK_SPEED = 1.1D;
    public static final double ENTITY_REACH_BONUS = 0.5D;
    public static final double MAX_CHARGE_TICKS = 800.0D;
    public static final int MAX_GAIN_TICKS = 200;
    public static final int SHORT_CAST_THRESHOLD_TICKS = 40;
    public static final int DECAY_DELAY_TICKS = 100;
    public static final double DECAY_TICKS_PER_TICK = MAX_CHARGE_TICKS * 0.1D / 20.0D;
    public static final int LEVEL_1_THRESHOLD_TICKS = 200;
    public static final int LEVEL_2_THRESHOLD_TICKS = 400;
    public static final int LEVEL_3_THRESHOLD_TICKS = 800;

    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    private static final double ATTACK_SPEED_MODIFIER_AMOUNT = DISPLAY_ATTACK_SPEED - 4.0D;
    private static final double[] CHARGE_ATTACK_DAMAGE_BONUSES = {0.0D, 2.0D, 5.0D, 10.0D};
    private static final double[] CHARGE_ATTACK_SPEED_BONUSES = {0.0D, -0.1D, -0.2D, -0.4D};
    private static final String TAG_CHARGE_TICKS = "SpellchargedGreatswordChargeTicks";
    private static final String TAG_LAST_CHARGE_GAME_TIME = "SpellchargedGreatswordLastChargeGameTime";
    private static final String TAG_CHARGE_LEVEL = "SpellchargedGreatswordChargeLevel";
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }

        return buildMainhandModifiers(getChargeLevel(stack));
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
        if (!isSpellchargedGreatsword(stack) || chargeTicks <= 0.0D) {
            return false;
        }

        var currentCharge = getEffectiveChargeTicks(stack, gameTime);
        var previousLevel = getChargeLevel(stack);
        var nextCharge = Mth.clamp(currentCharge + chargeTicks, 0.0D, MAX_CHARGE_TICKS);
        var nextLevel = Math.max(previousLevel, computeChargeLevel(nextCharge));

        var tag = stack.getOrCreateTag();
        tag.putDouble(TAG_CHARGE_TICKS, nextCharge);
        tag.putLong(TAG_LAST_CHARGE_GAME_TIME, gameTime);
        tag.putInt(TAG_CHARGE_LEVEL, nextLevel);
        return nextLevel > previousLevel;
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
        if (!isSpellchargedGreatsword(stack) || !stack.hasTag()) {
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
        if (!isSpellchargedGreatsword(stack) || !stack.hasTag()) {
            return 0;
        }

        return Mth.clamp(stack.getOrCreateTag().getInt(TAG_CHARGE_LEVEL), 0, 3);
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

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers(int chargeLevel) {
        var normalizedChargeLevel = Mth.clamp(chargeLevel, 0, 3);
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        ATTACK_DAMAGE_MODIFIER_AMOUNT + CHARGE_ATTACK_DAMAGE_BONUSES[normalizedChargeLevel],
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_MODIFIER_AMOUNT + CHARGE_ATTACK_SPEED_BONUSES[normalizedChargeLevel],
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        ENTITY_REACH_MODIFIER_ID,
                        "Spellcharged greatsword entity reach",
                        ENTITY_REACH_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
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
            return Ingredient.EMPTY;
        }
    }
}
