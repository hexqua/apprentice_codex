package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractImbueShieldItem extends ShieldItem implements IPresetSpellContainer, RestrictedSpellImbuableItem,
        ManaBypassSpellItem, CastAnimationOverrideItem {
    private static final int BLOCK_READY_TICKS = 5;
    private static final int SPELL_TRIGGER_WINDOW_TICKS = 15;
    private static final String TRIGGER_WINDOW_START_TAG_PREFIX = "ApprenticeCodexImbueShieldWindowStart.";
    private static final String TRIGGER_WINDOW_CASTED_TAG_PREFIX = "ApprenticeCodexImbueShieldWindowCasted.";

    protected AbstractImbueShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
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
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        appendImbueTargetSpellTooltip(lines);
    }

    @Override
    public final boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        return SpellGunCastType.from(spell.getCastType()) != null
                && spell.getRecastCount(spellLevel, null) <= 0;
    }

    @Override
    public boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        return spell != null
                && (spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG)
                && spell.getRecastCount(1, null) <= 0;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsManaBypass(spell);
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsManaBypass(spell);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        var result = super.use(level, player, usedHand);
        var usingStack = player.getItemInHand(usedHand);
        primeImmediateShieldBlock(player, usingStack);

        if (!level.isClientSide) {
            markTriggerWindowStart(player, usedHand);
        }

        return result;
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

        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    public final boolean tryTriggerImbuedSpellOnBlock(Player player, ItemStack shieldStack, InteractionHand usedHand) {
        if (player.level().isClientSide || !isWithinTriggerWindow(player, usedHand) || hasTriggeredSpellThisWindow(player, usedHand)) {
            return false;
        }

        var spellData = getPrimarySpellData(shieldStack);
        if (spellData == null || !canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;

        if (!tryCastSpellWithoutMana(player, shieldStack, spellLevel, slotId, spell)) {
            return false;
        }

        markTriggeredSpellThisWindow(player, usedHand);
        resumeShieldUse(player, usedHand);
        return true;
    }

    @Nullable
    protected final SpellData getPrimarySpellData(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData == SpellData.EMPTY ? null : spellData;
    }

    private boolean matchesImbuedSpell(ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null) {
            return false;
        }

        var spellData = getPrimarySpellData(stack);
        return spellData != null && spell.equals(spellData.getSpell());
    }

    private boolean tryCastSpellWithoutMana(Player player, ItemStack stack, int spellLevel, String slotId, AbstractSpell spell) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || player.isCreative()) {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    slotId
            );
            if (casted) {
                TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell, magicData, slotId, 0);
            }
            return casted;
        }

        var borrowedMana = Math.max(0f, spell.getManaCost(spellLevel) - magicData.getMana());
        if (borrowedMana > 0f) {
            // 通常の詠唱条件を通しつつ、SpellOnCastEvent 側で実消費だけ 0 に差し替える.
            magicData.addMana(borrowedMana);
        }

        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                CastSource.SWORD,
                true,
                slotId
        );
        if (!casted) {
            if (borrowedMana > 0f) {
                magicData.setMana(Math.max(0f, magicData.getMana() - borrowedMana));
            }
            return false;
        }

        if (borrowedMana > 0f) {
            ItemManaBypassCastEvent.reserveBorrowedMana(player, borrowedMana);
        }

        TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell, magicData, slotId, 0);
        return true;
    }

    private static void primeImmediateShieldBlock(Player player, ItemStack expectedStack) {
        if (!player.isUsingItem()) {
            return;
        }

        var usingStack = player.getUseItem();
        if (usingStack.isEmpty()
                || (usingStack != expectedStack && !ItemStack.isSameItemSameComponents(usingStack, expectedStack))) {
            return;
        }

        var useDuration = usingStack.getUseDuration(player);
        if (useDuration <= BLOCK_READY_TICKS) {
            return;
        }

        ((LivingEntityAccessor) player).apprenticecodex$setUseItemRemaining(useDuration - BLOCK_READY_TICKS);
    }

    private void resumeShieldUse(Player player, InteractionHand usedHand) {
        var currentStack = player.getItemInHand(usedHand);
        if (currentStack.isEmpty() || currentStack.getItem() != this) {
            return;
        }

        player.startUsingItem(usedHand);
        primeImmediateShieldBlock(player, currentStack);
    }

    private static void markTriggerWindowStart(Player player, InteractionHand usedHand) {
        var tag = player.getPersistentData();
        tag.putLong(triggerWindowStartTag(usedHand), player.level().getGameTime());
        tag.putBoolean(triggerWindowCastedTag(usedHand), false);
    }

    private static boolean isWithinTriggerWindow(Player player, InteractionHand usedHand) {
        var tag = player.getPersistentData();
        if (!tag.contains(triggerWindowStartTag(usedHand))) {
            return false;
        }

        var startGameTime = tag.getLong(triggerWindowStartTag(usedHand));
        return player.level().getGameTime() - startGameTime <= SPELL_TRIGGER_WINDOW_TICKS;
    }

    private static boolean hasTriggeredSpellThisWindow(Player player, InteractionHand usedHand) {
        return player.getPersistentData().getBoolean(triggerWindowCastedTag(usedHand));
    }

    private static void markTriggeredSpellThisWindow(Player player, InteractionHand usedHand) {
        player.getPersistentData().putBoolean(triggerWindowCastedTag(usedHand), true);
    }

    private static String triggerWindowStartTag(InteractionHand usedHand) {
        return TRIGGER_WINDOW_START_TAG_PREFIX + usedHand.name().toLowerCase(Locale.ROOT);
    }

    private static String triggerWindowCastedTag(InteractionHand usedHand) {
        return TRIGGER_WINDOW_CASTED_TAG_PREFIX + usedHand.name().toLowerCase(Locale.ROOT);
    }

    private static void appendImbueTargetSpellTooltip(List<Component> lines) {
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }

        if (!Screen.hasShiftDown()) {
            lines.add(Component.translatable("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        appendTooltipSection(
                lines,
                List.of(Component.translatable("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant")
                        .withStyle(ChatFormatting.GRAY)),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title"
        );
        appendTooltipSection(
                lines,
                collectRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title"
        );
    }

    private static void appendTooltipSection(List<Component> lines, List<Component> sectionLines, String titleTranslationKey) {
        lines.add(Component.translatable(titleTranslationKey).withStyle(ChatFormatting.GOLD));
        lines.addAll(sectionLines);
    }

    private static List<Component> collectRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_not_continuous"
        ).withStyle(ChatFormatting.GRAY));
        translatedLines.add(Component.translatable(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_no_recast"
        ).withStyle(ChatFormatting.GRAY));
        return translatedLines;
    }
}
