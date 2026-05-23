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
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.renderer.item.MithrilFreecastStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MithrilFreecastStaff extends AbstractRightClickMagicWeaponItem
        implements GeoItem, CastAnimationOverrideItem, IJeiInfoItem, SwingTriggeredMagicItem, ArcaneAnvilImbueBlockItem {
    private static final String ITEM_KEY = "mithril_freecast_staff";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.mithril_freecast_staff.desc_";
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double DISPLAYED_ATTACK_DAMAGE = 8.0D;
    private static final double DISPLAYED_ATTACK_SPEED = 1.6D;
    private static final double SPELL_POWER_BONUS = 0.1D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/geo/" + ITEM_KEY + ".png"
    );

    public MithrilFreecastStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                true,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                DISPLAYED_ATTACK_DAMAGE,
                DISPLAYED_ATTACK_SPEED - 4.0D,
                bonus(AttributeRegistry.SPELL_POWER, SPELL_POWER_BONUS, AttributeModifier.Operation.MULTIPLY_BASE)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
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
        initializeSpellContainer(stack);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.TRANSCENDENCE)) {
            return false;
        }

        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
        if (player.level().isClientSide) {
            return false;
        }

        var stack = player.getItemInHand(hand);
        if (!isSameItem(stack) || (!bypassChargeCheck && !isFullyChargedAttack(player))) {
            return false;
        }

        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return false;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        if (!canSwingCastSpell(spell)) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        try (var ignored = MithrilFreecastStaffCastContext.open(player.getUUID(), stack, spell)) {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    resolveSpellSelectionSlot(hand)
            );
            if (!casted) {
                return false;
            }

            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    resolveSpellSelectionSlot(hand),
                    spell.getCastType() == CastType.LONG ? 0 : null
            );
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Mithril Freecast Staff swing cast context failed to close.", exception);
        }
    }

    public int resolveSwingTriggeredCooldownTicks(Player player, ItemStack stack, AbstractSpell spell, int currentEffectiveCooldown) {
        var spellLevel = resolveEffectiveSpellLevel(player, spell);
        return currentEffectiveCooldown
                + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(spellLevel, player) : 0);
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.freecast.common.desc").withStyle(ChatFormatting.GRAY));
        appendFreecastTooltip(lines);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MithrilFreecastStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MithrilFreecastStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
        );
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public static boolean canSwingCastSpell(@Nullable AbstractSpell spell) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        return EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
                .contains(SpellGunCastType.from(spell.getCastType()));
    }

    private int resolveEffectiveSpellLevel(Player player, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null
                && Objects.equals(spell.getSpellId(), magicData.getCastingSpellId())
                && magicData.getCastingSpellLevel() > 0) {
            return magicData.getCastingSpellLevel();
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption != null && selectionOption.spellData != SpellData.EMPTY
                && spell.equals(selectionOption.spellData.getSpell())) {
            return spell.getLevelFor(selectionOption.spellData.getLevel(), player);
        }

        return spell.getLevelFor(1, player);
    }

    private void appendFreecastTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
                        ),
                        ImbueTooltipHelper.translatableGray(
                                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
                        )
                ),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_freecast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                ImbueTooltipHelper.collectCastTypeRestrictionLines(
                        EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
                ),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_freecast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static String resolveSpellSelectionSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
    }
}
