package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import jp.aquafactory.apprenticecodex.renderer.item.SpellReaperScytheRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
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

import java.util.List;
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
    private static final ResourceLocation MALUM_ASCENSION_ID = ResourceLocation.fromNamespaceAndPath(
            MalumCompatibility.MOD_ID,
            "ascension"
    );
    private static final Set<ResourceLocation> EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "transcendence"),
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "rebound"),
            MALUM_ASCENSION_ID
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
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);
        var ascensionResult = MalumSpellReaperScytheBridge.tryTriggerAscension(level, player, hand, stack);
        if (ascensionResult != InteractionResult.PASS) {
            return new InteractionResultHolder<>(ascensionResult, stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            var enchantmentId = entry.getKey().unwrapKey().map(ResourceKey::location).orElse(null);
            if (!MALUM_ASCENSION_ID.equals(enchantmentId)) {
                continue;
            }

            var enchantmentLevel = entry.getIntValue();
            var manaCost = ApprenticeCodexServerConfig.spellReaperScytheConfig()
                    .ascensionManaCost(enchantmentLevel);
            lines.add(Component.translatable(
                    "item.apprenticecodex.spell_reaper_scythe.malum.ascension_cost",
                    Enchantment.getFullname(entry.getKey(), enchantmentLevel),
                    Component.literal(Integer.toString(manaCost)).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
            break;
        }
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            // Malum導入時は本家大鎌のレスポンダーが範囲攻撃を担うため、バニラスイープを重ねない。
            return !MalumSpellReaperScytheBridge.isAvailable();
        }
        return super.canPerformAction(stack, itemAbility);
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
