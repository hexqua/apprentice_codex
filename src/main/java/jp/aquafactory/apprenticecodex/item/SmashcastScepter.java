package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.SmashcastScepterRenderer;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class SmashcastScepter extends AbstractRightClickMagicWeaponItem
        implements GeoItem, RestrictedSpellImbuableItem, ManaBypassSpellItem, IJeiInfoItem {
    public static final double ATTACK_DAMAGE_MODIFIER = 5.0D;
    public static final double ATTACK_SPEED_MODIFIER = -3.4D;
    public static final float SMASH_ATTACK_FALL_DISTANCE_THRESHOLD = 1.5F;
    public static final float HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD = 5.0F;
    public static final double SMASH_KNOCKBACK_RADIUS = 3.5D;
    public static final double SMASH_KNOCKBACK_POWER = 0.7D;
    public static final double WIND_BURST_MOTION_EPSILON = 0.01D;

    private static final String ITEM_KEY = "smashcast_scepter";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.smashcast_scepter.desc_";
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double MAX_SMASH_SPELL_POWER_MULTIPLIER = 10.0D;
    private static final double LOW_FALL_SPELL_POWER_PER_BLOCK = 0.10D;
    private static final double HIGH_FALL_SPELL_POWER_PER_BLOCK = 0.05D;
    private static final double COMPRESS_SPELL_POWER_PER_LEVEL_PER_BLOCK = 0.02D;
    private static final float COMPRESS_DAMAGE_PER_LEVEL_PER_BLOCK = 0.5F;
    private static final double WIND_BURST_FALLBACK_BASE = 1.5D;
    private static final double WIND_BURST_FALLBACK_PER_LEVEL_ABOVE_FIRST = 0.35D;
    private static final UUID SMASH_SPELL_POWER_MODIFIER_ID =
            UUID.fromString("f8cb06ee-20d8-46f9-bc53-69f5a7452abf");
    private static final String SMASH_SPELL_POWER_MODIFIER_NAME = "apprenticecodex.smashcast_scepter.smash_spell_power";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath("malum", "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("malum", "soul_hunter_weapon")
    );
    private static final Set<ResourceLocation> ALLOWED_VANILLA_ENCHANTMENTS = Set.of(
            ResourceLocation.withDefaultNamespace("smite"),
            ResourceLocation.withDefaultNamespace("bane_of_arthropods"),
            ResourceLocation.withDefaultNamespace("fire_aspect")
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SmashcastScepter() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                true,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                ATTACK_DAMAGE_MODIFIER,
                ATTACK_SPEED_MODIFIER
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none() && spell.getCastType() != CastType.CONTINUOUS;
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        SpellData spellData = SpellData.EMPTY;
        if (ISpellContainer.isSpellContainer(stack)) {
            var spellContainer = ISpellContainer.get(stack);
            if (spellContainer != null && spellContainer.getActiveSpellCount() > 0) {
                spellData = spellContainer.getSpellAtIndex(0);
            }
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        } else {
            PresetSpellContainerStateHelper.clearRememberedState(stack);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    public @Nullable SpellData getImbuedSpellData(ItemStack stack) {
        return getPrimarySpellData(stack);
    }

    public boolean canStartSmashcast(Player player, ItemStack stack) {
        if (!isSmashAttack(player) || !isSameItem(stack)) {
            return false;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }
        return true;
    }

    public boolean tryCastSmashSpell(Player player, ItemStack stack, float fallDistance) {
        if (!isSameItem(stack)) {
            return false;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellData = getImbuedSpellData(stack);
        if (spellData == null || !canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && (magicData.isCasting() || magicData.getPlayerCooldowns().isOnCooldown(spell))) {
            return false;
        }

        var borrowedMana = borrowRequiredMana(player, magicData, spell, spellLevel);
        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute != null) {
            spellPowerAttribute.removeModifier(SMASH_SPELL_POWER_MODIFIER_ID);
            var spellPowerMultiplier = calculateSmashSpellPowerMultiplier(stack, fallDistance);
            if (spellPowerMultiplier > 1.0D) {
                spellPowerAttribute.addTransientModifier(new AttributeModifier(
                        SMASH_SPELL_POWER_MODIFIER_ID,
                        SMASH_SPELL_POWER_MODIFIER_NAME,
                        spellPowerMultiplier - 1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }

        try {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    SpellSelectionManager.MAINHAND
            );
            if (!casted) {
                releaseBorrowedMana(magicData, borrowedMana);
                return false;
            }

            if (borrowedMana > 0.0F) {
                ItemManaBypassCastEvent.reserveBorrowedMana(player, borrowedMana);
            }
            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    SpellSelectionManager.MAINHAND,
                    spell.getCastType() == CastType.LONG ? 0 : null
            );
            return true;
        } finally {
            if (spellPowerAttribute != null) {
                spellPowerAttribute.removeModifier(SMASH_SPELL_POWER_MODIFIER_ID);
            }
        }
    }

    public static boolean isSmashAttack(Player player) {
        return player != null
                && player.fallDistance > SMASH_ATTACK_FALL_DISTANCE_THRESHOLD
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
    }

    public static float calculateSmashBonusDamage(ItemStack stack, float fallDistance) {
        var clampedFallDistance = Math.max(0.0F, fallDistance);
        var maceBonus = clampedFallDistance <= 3.0F
                ? 4.0F * clampedFallDistance
                : clampedFallDistance <= 8.0F
                ? 12.0F + 2.0F * (clampedFallDistance - 3.0F)
                : 22.0F + clampedFallDistance - 8.0F;
        return maceBonus + getCompressLevel(stack) * clampedFallDistance * COMPRESS_DAMAGE_PER_LEVEL_PER_BLOCK;
    }

    public static double calculateSmashSpellPowerMultiplier(ItemStack stack, float fallDistance) {
        var clampedFallDistance = Math.max(0.0D, fallDistance);
        var baseBonus = clampedFallDistance <= 10.0D
                ? clampedFallDistance * LOW_FALL_SPELL_POWER_PER_BLOCK
                : 10.0D * LOW_FALL_SPELL_POWER_PER_BLOCK
                + (clampedFallDistance - 10.0D) * HIGH_FALL_SPELL_POWER_PER_BLOCK;
        var compressBonus = clampedFallDistance * getCompressLevel(stack) * COMPRESS_SPELL_POWER_PER_LEVEL_PER_BLOCK;
        return 1.0D + Math.min(MAX_SMASH_SPELL_POWER_MULTIPLIER, baseBonus + compressBonus);
    }

    public static int getReleaseLevel(ItemStack stack) {
        return EnchantmentRegistry.RELEASE.isPresent() ? stack.getEnchantmentLevel(EnchantmentRegistry.RELEASE.get()) : 0;
    }

    public static double calculateReleaseBounceImpulse(int releaseLevel) {
        return switch (releaseLevel) {
            case 1 -> 1.2D;
            case 2 -> 1.75D;
            case 3 -> 2.2D;
            default -> releaseLevel <= 0
                    ? 0.0D
                    : WIND_BURST_FALLBACK_BASE + WIND_BURST_FALLBACK_PER_LEVEL_ABOVE_FIRST * (releaseLevel - 1);
        };
    }

    @Override
    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        var spellData = getPrimarySpellData(stack);
        if (spellData == null || spellData.canRemove() || !canImbueSpell(spellData)) {
            return false;
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        ISpellContainer.set(stack, normalized.toImmutable());
        PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        return true;
    }

    @Override
    public boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        return spell != null && spell != SpellRegistry.none();
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null || isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumHauntedCompat.isAnimatedEnchantment(enchantmentId)) {
            return false;
        }

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            return true;
        }

        if (MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            return true;
        }

        if (EnchantmentRegistry.COMPRESS.isPresent() && enchantment == EnchantmentRegistry.COMPRESS.get()) {
            return true;
        }

        if (EnchantmentRegistry.RELEASE.isPresent() && enchantment == EnchantmentRegistry.RELEASE.get()) {
            return false;
        }

        return (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get())
                || (EnchantmentRegistry.PLUNDER.isPresent() && enchantment == EnchantmentRegistry.PLUNDER.get())
                || (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get())
                || ALLOWED_VANILLA_ENCHANTMENTS.contains(enchantmentId);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return false;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment)
                        || (EnchantmentRegistry.RELEASE.isPresent() && enchantment == EnchantmentRegistry.RELEASE.get()));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment)
                || (EnchantmentRegistry.RELEASE.isPresent() && enchantment == EnchantmentRegistry.RELEASE.get());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SmashcastScepterRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SmashcastScepterRenderer();
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

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static int getCompressLevel(ItemStack stack) {
        return EnchantmentRegistry.COMPRESS.isPresent() ? stack.getEnchantmentLevel(EnchantmentRegistry.COMPRESS.get()) : 0;
    }

    private static float borrowRequiredMana(Player player, @Nullable MagicData magicData, AbstractSpell spell, int spellLevel) {
        if (magicData == null || player.getAbilities().instabuild) {
            return 0.0F;
        }

        var borrowedMana = Math.max(0.0F, spell.getManaCost(spellLevel) - magicData.getMana());
        if (borrowedMana > 0.0F) {
            magicData.addMana(borrowedMana);
        }
        return borrowedMana;
    }

    private static void releaseBorrowedMana(@Nullable MagicData magicData, float borrowedMana) {
        if (magicData != null && borrowedMana > 0.0F) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - borrowedMana));
        }
    }
}
