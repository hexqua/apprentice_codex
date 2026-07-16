package jp.aquafactory.apprenticecodex.item.shield;

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
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;

public abstract class AbstractImbueShieldItem extends ShieldItem implements IPresetSpellContainer, RestrictedSpellImbuableItem,
        ManaBypassSpellItem, CastAnimationOverrideItem, TranscendencePolicy {
    private static final int BLOCK_READY_TICKS = 5;
    private static final int SPELL_TRIGGER_WINDOW_TICKS = 15;
    private static final String TRIGGER_WINDOW_START_TAG_PREFIX = "ApprenticeCodexImbueShieldWindowStart.";
    private static final String TRIGGER_WINDOW_CASTED_TAG_PREFIX = "ApprenticeCodexImbueShieldWindowCasted.";

    protected AbstractImbueShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        repairPresetSpellContainerStateIfNeeded(itemStack);

        if (ISpellContainer.isSpellContainer(itemStack)) {
            normalizeHiddenSpellContainer(itemStack);
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, false, false));
    }

    public final boolean repairPresetSpellContainerStateIfNeeded(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        return PresetSpellContainerStateHelper.restoreIfNeeded(itemStack, 1, false, false, this::canImbueSpell);
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
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        appendImbueTargetSpellTooltip(stack, lines);
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
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsManaBypass(spell);
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
        if (shouldPrimeImmediateShieldBlock()) {
            primeImmediateShieldBlock(player, usingStack);
        }

        if (!level.isClientSide && supportsBlockTriggeredImbuedSpell()) {
            markTriggerWindowStart(player, usedHand);
        }

        return result;
    }

    /**
     * 既存の Imbue 盾は即時防御を仕様としているが、通常の盾準備時間を使う派生盾は false を返す。
     */
    protected boolean shouldPrimeImmediateShieldBlock() {
        return true;
    }

    /**
     * ブロック成功直後に魔法を発動しない派生盾が、共通イベントから除外されるための拡張点。
     */
    public boolean supportsBlockTriggeredImbuedSpell() {
        return true;
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
            // Workbench 抽出可否は locked を見るため、差し替え後の呪文は preset 扱いにしない。
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        } else {
            PresetSpellContainerStateHelper.clearRememberedState(stack);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    private void normalizeHiddenSpellContainer(ItemStack stack) {
        var current = ISpellContainer.get(stack);
        if (current == null || !current.isSpellWheel() && !current.mustEquip()) {
            return;
        }

        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        var spellData = current.getActiveSpellCount() > 0 ? current.getSpellAtIndex(0) : SpellData.EMPTY;
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            // 既存スタックのホイール公開フラグだけを直し、Workbench で設定した魔法と抽出可否は維持する。
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, !spellData.canRemove());
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

    private void appendImbueTargetSpellTooltip(ItemStack stack, List<Component> lines) {
        appendAlwaysVisibleImbueTooltip(stack, lines);
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                getImbueShieldAbilityTooltipSection(stack),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                null
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                getImbueShieldRestrictionTooltipSection(stack),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                null
        );
    }

    protected void appendAlwaysVisibleImbueTooltip(ItemStack stack, List<Component> lines) {
    }

    private static List<Component> collectImbueShieldAbilityTooltipSection() {
        return List.of(
                ImbueTooltipHelper.translatableGray("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_no_mana"),
                ImbueTooltipHelper.translatableGray("item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant")
        );
    }

    private static List<Component> collectRestrictTooltipSection() {
        var translatedLines = new ArrayList<>(ImbueTooltipHelper.collectCastTypeRestrictionLines(
                EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
        ));
        ImbueTooltipHelper.appendNoRecastRestrictionLine(translatedLines, true);
        return translatedLines;
    }

    protected List<Component> getImbueShieldAbilityTooltipSection() {
        return collectImbueShieldAbilityTooltipSection();
    }

    protected List<Component> getImbueShieldAbilityTooltipSection(ItemStack stack) {
        return getImbueShieldAbilityTooltipSection();
    }

    protected List<Component> getImbueShieldRestrictionTooltipSection() {
        return collectRestrictTooltipSection();
    }

    protected List<Component> getImbueShieldRestrictionTooltipSection(ItemStack stack) {
        return getImbueShieldRestrictionTooltipSection();
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return getImbueShieldRestrictionTooltipSection();
    }
}
