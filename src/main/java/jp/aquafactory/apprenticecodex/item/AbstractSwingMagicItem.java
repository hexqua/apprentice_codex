package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractSwingMagicItem extends AbstractRightClickMagicWeaponItem
        implements RestrictedSpellImbuableItem, CastAnimationOverrideItem, IPresetSpellContainer, SwingTriggeredMagicItem {

    protected AbstractSwingMagicItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(
                properties,
                configuredSpell,
                configuredSpellLevel,
                false,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                handBonuses
        );
    }

    protected AbstractSwingMagicItem(
            Properties properties,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(
                properties,
                false,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                handBonuses
        );
    }

    protected AbstractSwingMagicItem(
            Properties properties,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        super(
                properties,
                false,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                handBonuses
        );
    }

    protected AbstractSwingMagicItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        super(
                properties,
                configuredSpell,
                configuredSpellLevel,
                false,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                handBonuses
        );
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

        return spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG;
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return ImbueTooltipHelper.collectCastTypeRestrictionLines(
                EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG)
        );
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
            // 初期 preset と違い、後から注入した呪文は Spellcaster Workbench で取り外せる状態を維持する。
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        } else {
            PresetSpellContainerStateHelper.clearRememberedState(stack);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    @Override
    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        var spellData = getPrimarySpellData(stack);
        if (spellData == null
                || spellData.canRemove()
                || !canImbueSpell(spellData)
                || matchesConfiguredPresetSpell(spellData)) {
            return false;
        }

        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        ISpellContainer.set(stack, normalized.toImmutable());
        PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        return true;
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsCastAnimationOverride(spell);
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsCastAnimationOverride(spell);
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell) && supportsCastAnimationOverride(spell);
    }

    public final boolean tryTriggerImbuedSpellOnSwing(Player player) {
        return tryTriggerImbuedSpellOnSwing(player, false);
    }

    public final boolean tryTriggerImbuedSpellOnSwing(Player player, boolean bypassChargeCheck) {
        return tryTriggerImbuedSpellOnSwing(player, InteractionHand.MAIN_HAND, bypassChargeCheck);
    }

    public final boolean tryTriggerImbuedSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
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

        var spellData = getPrimarySpellData(stack);
        if (spellData == null) {
            return false;
        }

        if (!canImbueSpell(spellData)) {
            onInvalidSwingTriggeredSpell(player, stack, spellData);
            return false;
        }

        var spell = spellData.getSpell();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        return tryCastSpell(player, stack, spell, spellLevel, magicData, resolveSpellSelectionSlot(hand));
    }

    @Override
    public final boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
        return tryTriggerImbuedSpellOnSwing(player, hand, bypassChargeCheck);
    }

    private boolean matchesImbuedSpell(ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null) {
            return false;
        }

        var spellData = getPrimarySpellData(stack);
        return spellData != null && spell.equals(spellData.getSpell());
    }

    private boolean supportsCastAnimationOverride(@Nullable AbstractSpell spell) {
        return spell != null && (spell.getCastType() == CastType.INSTANT || spell.getCastType() == CastType.LONG);
    }

    protected @Nullable Integer getSwingTriggeredLongCastDurationOverrideTicks(
            Player player,
            ItemStack stack,
            AbstractSpell spell,
            int spellLevel,
            @Nullable MagicData magicData
    ) {
        return 0;
    }

    protected void onInvalidSwingTriggeredSpell(Player player, ItemStack stack, SpellData spellData) {
    }

    protected AutoCloseable openSwingTriggeredSpellCastContext(
            Player player,
            ItemStack stack,
            AbstractSpell spell,
            int spellLevel,
            @Nullable MagicData magicData
    ) {
        return SwingcastStaffCastContext.open(player.getUUID(), stack, spell);
    }

    protected boolean tryCastSpell(Player player, ItemStack stack, AbstractSpell spell, int spellLevel, @Nullable MagicData magicData) {
        return tryCastSpell(player, stack, spell, spellLevel, magicData, SpellSelectionManager.MAINHAND);
    }

    protected boolean tryCastSpell(
            Player player,
            ItemStack stack,
            AbstractSpell spell,
            int spellLevel,
            @Nullable MagicData magicData,
            String slotId
    ) {
        AutoCloseable contextHandle = openSwingTriggeredSpellCastContext(player, stack, spell, spellLevel, magicData);
        try {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD,
                    true,
                    slotId
            );
            if (!casted) {
                return false;
            }

            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    slotId,
                    getSwingTriggeredLongCastDurationOverrideTicks(player, stack, spell, spellLevel, magicData)
            );
            return true;
        } finally {
            try {
                Objects.requireNonNull(contextHandle).close();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to close swing-triggered cast context.", e);
            }
        }
    }

    private static String resolveSpellSelectionSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(getSwingCastTooltipTranslationKey()).withStyle(ChatFormatting.GRAY));
    }

    protected String getSwingCastTooltipTranslationKey() {
        return "item.apprenticecodex.swingcast.common.desc";
    }
}
