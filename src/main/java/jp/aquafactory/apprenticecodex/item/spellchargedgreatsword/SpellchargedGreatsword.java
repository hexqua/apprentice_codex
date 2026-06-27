package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;
import java.util.function.Consumer;

public final class SpellchargedGreatsword extends SwordItem implements GeoItem, UniqueItem {
    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 22;
    public static final double DISPLAY_ATTACK_DAMAGE = 8.0D;
    public static final double DISPLAY_ATTACK_SPEED = 1.1D;
    public static final double ENTITY_REACH_BONUS = 0.5D;

    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    private static final double ATTACK_SPEED_MODIFIER_AMOUNT = DISPLAY_ATTACK_SPEED - 4.0D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK =
            new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_shatter_capable_weapon")
    );
    private static final Set<ResourceLocation> EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom")
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpellchargedGreatsword() {
        super(SpellchargedGreatswordTier.INSTANCE,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(DURABILITY)
                        .rarity(Rarity.RARE)
                        .fireResistant()
                        .attributes(buildMainhandModifiers()));
        GeoItem.registerSyncedAnimatable(this);
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
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null) {
            return false;
        }

        if (MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON)) {
            return true;
        }

        return EXTRA_ENCHANTMENTS.contains(enchantmentId)
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SpellchargedGreatswordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
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

    private static ItemAttributeModifiers buildMainhandModifiers() {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcharged_greatsword_entity_reach"),
                        ENTITY_REACH_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
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
