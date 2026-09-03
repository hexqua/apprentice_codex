package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import jp.aquafactory.apprenticecodex.renderer.item.SpellReaperScytheRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;
import java.util.function.Consumer;

public final class SpellReaperScythe extends SwordItem
        implements GeoItem, IPresetSpellContainer, SpellSlotUpgradeableItem, TranscendencePolicy, WisdomPolicy {
    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 15;
    public static final double DISPLAY_ATTACK_DAMAGE = 10.0D;
    public static final double DISPLAY_ATTACK_SPEED = 1.0D;

    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    public static final double ATTACK_SPEED_MODIFIER_AMOUNT = DISPLAY_ATTACK_SPEED - 4.0D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final Set<ResourceLocation> EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "transcendence"),
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "rebound"),
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "ascension")
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpellReaperScythe() {
        super(Tiers.NETHERITE, new Properties()
                .stacksTo(1)
                .durability(DURABILITY)
                .rarity(Rarity.RARE)
                .fireResistant()
                .attributes(SwordItem.createAttributes(
                        Tiers.NETHERITE,
                        (int) (ATTACK_DAMAGE_MODIFIER_AMOUNT - Tiers.NETHERITE.getAttackDamageBonus()),
                        (float) ATTACK_SPEED_MODIFIER_AMOUNT
                )));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
                              int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty() || ISpellContainer.isSpellContainer(stack)) {
            return;
        }
        ISpellContainer.set(stack, ISpellContainer.create(1, true, false));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        return enchantmentId != null
                && (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)
                || MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)
                || EXTRA_ENCHANTMENTS.contains(enchantmentId)
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment));
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
    public ItemStack createSpellSlotUpgradeResult(ItemStack baseStack, SpellSlotUpgradeItem upgradeItem) {
        return ItemStack.EMPTY;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SpellReaperScytheRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new SpellReaperScytheRenderer();
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
}
