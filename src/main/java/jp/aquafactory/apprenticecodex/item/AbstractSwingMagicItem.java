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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractSwingMagicItem extends AbstractRightClickMagicWeaponItem
        implements RestrictedSpellImbuableItem, CastAnimationOverrideItem, IPresetSpellContainer {

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
        if (player.level().isClientSide) {
            return false;
        }

        var stack = player.getMainHandItem();
        if (!isSameItem(stack) || (!bypassChargeCheck && !isFullyChargedAttack(player))) {
            return false;
        }

        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellData = getPrimarySpellData(stack);
        if (spellData == null || !canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        return tryCastSpell(player, stack, spell, spellLevel, magicData);
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

    private boolean tryCastSpell(Player player, ItemStack stack, AbstractSpell spell, int spellLevel, @Nullable MagicData magicData) {
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                io.redspace.ironsspellbooks.api.spells.CastSource.SWORD,
                true,
                SpellSelectionManager.MAINHAND
        );
        if (!casted) {
            return false;
        }

        TriggeredSpellCastHelper.applyLongCastDurationOverride(
                player,
                spellLevel,
                spell,
                magicData,
                SpellSelectionManager.MAINHAND,
                0
        );
        return true;
    }
}
