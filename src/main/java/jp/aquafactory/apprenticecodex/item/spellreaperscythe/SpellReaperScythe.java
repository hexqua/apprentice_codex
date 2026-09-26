package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;

import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import jp.aquafactory.apprenticecodex.renderer.item.SpellReaperScytheRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class SpellReaperScythe extends SwordItem
        implements GeoItem, IPresetSpellContainer, SpellSlotUpgradeableItem, WisdomPolicy {
    static final UUID BASE_DAMAGE_ID = BASE_ATTACK_DAMAGE_UUID;
    static final UUID BASE_SPEED_ID = BASE_ATTACK_SPEED_UUID;
    public static final int DURABILITY = 2031;
    public static final int ENCHANTMENT_VALUE = 15;
    public static final double DISPLAY_ATTACK_DAMAGE = 10.0D;
    public static final double DISPLAY_ATTACK_SPEED = 1.0D;

    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    public static final double ATTACK_SPEED_MODIFIER_AMOUNT = DISPLAY_ATTACK_SPEED - 4.0D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final ResourceLocation MALUM_ASCENSION_ID = ResourceLocation.fromNamespaceAndPath(
            "malum",
            "ascension"
    );
    private static final ResourceLocation MALUM_REBOUND_ID = ResourceLocation.fromNamespaceAndPath("malum", "rebound");
    private static final Set<ResourceLocation> EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom"),
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "transcendence"),
            ResourceLocation.fromNamespaceAndPath("malum", "animated"),
            ResourceLocation.fromNamespaceAndPath("malum", "rebound"),
            MALUM_ASCENSION_ID
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SpellReaperScythe() {
        super(Tiers.NETHERITE, (int) (ATTACK_DAMAGE_MODIFIER_AMOUNT - Tiers.NETHERITE.getAttackDamageBonus()),
                (float) ATTACK_SPEED_MODIFIER_AMOUNT,
                new Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE).fireResistant());
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
            if (entity instanceof Player player) ScytheThrowManager.sanitize(stack, player);
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
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack, @NotNull Enchantment enchantment) {
        var id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return id != null && (EXTRA_ENCHANTMENTS.contains(id)
                || id.equals(ResourceLocation.fromNamespaceAndPath("malum", "spirit_plunder"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("malum", "haunted"))
                || enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK));
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);
        if (ModList.get().isLoaded("epicfight")) {
            // ガード入力はEpic Fightに任せ、大鎌固有の使用経路だけを無効化する.
            // 大鎌固有機能は戦闘モードのインネイトスキル側で処理する.
            return InteractionResultHolder.pass(stack);
        }
        if (ScytheThrowManager.isThrown(stack) || (!level.isClientSide && ScytheThrowManager.active(player) != null)) {
            return ScytheThrowManager.use(level, player, hand);
        }
        var ascensionResult = MalumSpellReaperScytheBridge.tryTriggerAscension(level, player, hand, stack);
        if (ascensionResult != InteractionResult.PASS) {
            return new InteractionResultHolder<>(ascensionResult, stack);
        }
        return ScytheThrowManager.use(level, player, hand);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) { return 72000; }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, LivingEntity entity, int remaining) {
        if (!ModList.get().isLoaded("epicfight") && entity instanceof Player player) {
            ScytheThrowManager.release(level, player, stack);
        }
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Level context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        if (ModList.get().isLoaded("epicfight")) {
            appendEpicFightHoverText(stack, lines);
            return;
        }
        if (getEnchantmentLevel(stack, MALUM_ASCENSION_ID) == 0) {
            var config = SpellReaperScytheClientConfigState.values();
            int reboundLevel = getEnchantmentLevel(stack, MALUM_REBOUND_ID);
            if (reboundLevel > 0) {
                lines.add(Component.translatable("item.apprenticecodex.spell_reaper_scythe.malum.rebound.desc_1",
                        Component.literal(Integer.toString(config.reboundManaCost(reboundLevel))).withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.apprenticecodex.spell_reaper_scythe.malum.rebound.desc_2").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("item.apprenticecodex.spell_reaper_scythe.throw.desc_1",
                        Component.literal(Integer.toString(config.throwManaCost())).withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.apprenticecodex.spell_reaper_scythe.throw.desc_2",
                        Component.literal(Long.toString(config.throwManaPerTick() * 20L)).withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        for (var entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (!MALUM_ASCENSION_ID.equals(enchantmentId)) {
                continue;
            }

            var enchantmentLevel = entry.getValue();
            var manaCost = SpellReaperScytheClientConfigState.values()
                    .ascensionManaCost(enchantmentLevel);
            lines.add(Component.translatable(
                    "item.apprenticecodex.spell_reaper_scythe.malum.ascension_cost",
                    entry.getKey().getFullname(enchantmentLevel),
                    Component.literal(Integer.toString(manaCost)).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
            break;
        }
    }

    private static void appendEpicFightHoverText(ItemStack stack, List<Component> lines) {
        var config = SpellReaperScytheClientConfigState.values();
        int ascension = getEnchantmentLevel(stack, MALUM_ASCENSION_ID);
        int rebound = getEnchantmentLevel(stack, MALUM_REBOUND_ID);
        String prefix = "item.apprenticecodex.spell_reaper_scythe.epicfight.";
        if (ascension > 0) {
            var enchantment = EnchantmentHelper.getEnchantments(stack).keySet().stream()
                    .filter(holder -> MALUM_ASCENSION_ID.equals(ForgeRegistries.ENCHANTMENTS.getKey(holder)))
                    .findFirst().orElseThrow();
            lines.add(Component.translatable(prefix + "ascension.desc_1", enchantment.getFullname(ascension)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(prefix + "ascension.desc_2", manaText(config.ascensionManaCost(ascension))).withStyle(ChatFormatting.GRAY));
        } else if (rebound > 0) {
            lines.add(Component.translatable(prefix + "rebound.desc_1").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(prefix + "rebound.desc_2",
                    manaText(config.reboundManaCost(rebound))).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(prefix + "throw.desc_1").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(prefix + "throw.desc_2", manaText(config.throwManaCost()),
                    manaText(config.throwManaPerTick() * 20L)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static int getEnchantmentLevel(ItemStack stack, ResourceLocation id) {
        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        return enchantment == null ? 0 : stack.getEnchantmentLevel(enchantment);
    }

    private static Component manaText(long amount) {
        return Component.literal(Long.toString(amount)).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ToolAction itemAbility) {
        if (itemAbility == ToolActions.SWORD_SWEEP) {
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SpellReaperScytheRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
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
