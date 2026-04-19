package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.renderer.item.FocusStaffbowRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;
import java.util.function.Consumer;

public final class FocusStaffbow extends CastingItem implements GeoItem, NonDamageableAnvilMergeItem, UniqueItem {
    private static final int MAX_USE_DURATION = 72000;
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final Set<ResourceLocation> ALLOWED_MAGIC_ITEM_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "plunder")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("core_idle");
    private static final int ENCHANTMENT_VALUE = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FocusStaffbow() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY || selection.spellData.getSpell() == SpellRegistry.none()) {
            return InteractionResultHolder.pass(stack);
        }

        if (selection.spellData.getSpell().getCastType() != io.redspace.ironsspellbooks.api.spells.CastType.CONTINUOUS) {
            if (level.isClientSide) {
                player.startUsingItem(usedHand);
                return InteractionResultHolder.consume(stack);
            }

            var handled = FocusStaffbowCastManager.handleSelectedSpellInput(player, stack);
            if (!handled) {
                return InteractionResultHolder.fail(stack);
            }

            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        var handled = FocusStaffbowCastManager.handleSelectedSpellInput(player, stack);
        return handled
                ? InteractionResultHolder.sidedSuccess(stack, false)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }

        FocusStaffbowCastManager.releasePendingCast(player, stack, getUseDuration(stack) - timeLeft);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return MAX_USE_DURATION;
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

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumHauntedCompat.isAnimatedEnchantment(enchantmentId)) {
            return false;
        }

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            return true;
        }

        if (isMalumSpiritPlunder(stack, enchantmentId)) {
            return true;
        }

        return ALLOWED_MAGIC_ITEM_ENCHANTMENTS.contains(enchantmentId);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FocusStaffbowRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new FocusStaffbowRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static boolean isMalumSpiritPlunder(ItemStack stack, ResourceLocation enchantmentId) {
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON);
    }
}
