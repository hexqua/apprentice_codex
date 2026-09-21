package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 注入魔法の保存・初期化を必要とする右クリック魔法武器の基盤。 */
public abstract class AbstractImbuedMagicWeaponItem extends AbstractRightClickMagicWeaponItem
        implements IPresetSpellContainer {
    private final @Nullable Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final boolean spellWheelEnabled;

    protected AbstractImbuedMagicWeaponItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(properties, enchantmentValue, itemKey, attackDamage, attackSpeed, handBonuses);
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.spellWheelEnabled = spellWheelEnabled;
    }

    protected AbstractImbuedMagicWeaponItem(
            Properties properties,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(properties, enchantmentValue, itemKey, attackDamage, attackSpeed, handBonuses);
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.spellWheelEnabled = spellWheelEnabled;
    }

    protected AbstractImbuedMagicWeaponItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        this(
                properties,
                configuredSpell,
                configuredSpellLevel,
                spellWheelEnabled,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                List.of(handBonuses)
        );
    }

    protected AbstractImbuedMagicWeaponItem(
            Properties properties,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        this(
                properties,
                spellWheelEnabled,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                List.of(handBonuses)
        );
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        if (repairPresetSpellContainerStateIfNeeded(itemStack)) {
            return;
        }

        if (ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, spellWheelEnabled, false).mutableCopy();
        if (startsWithPresetSpell) {
            InitialSpellContainerHelper.addInitialSpellIfEnabled(
                    spellContainer,
                    configuredSpell,
                    configuredSpellLevel,
                    0,
                    true
            );
        }
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    public final boolean repairPresetSpellContainerStateIfNeeded(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Predicate<SpellData> trackedStateValidator = this instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem
                ? restrictedSpellImbuableItem::canImbueSpell
                : spellData -> spellData != SpellData.EMPTY && spellData.getSpell() != SpellRegistry.none();
        if (PresetSpellContainerStateHelper.restoreIfNeeded(
                itemStack,
                1,
                false,
                false,
                trackedStateValidator
        )) {
            return true;
        }

        return normalizeLegacyOverriddenSpellContainerIfNeeded(itemStack);
    }

    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && !shouldPrioritizeOffhandUse(player)
                && !ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }
        return super.use(level, player, hand);
    }

    protected final @Nullable SpellData getPrimarySpellData(ItemStack stack) {
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

    protected final boolean matchesConfiguredPresetSpell(@Nullable SpellData spellData) {
        return spellData != null
                && startsWithPresetSpell
                && configuredSpell != null
                && configuredSpell.get().equals(spellData.getSpell())
                && configuredSpellLevel == spellData.getLevel();
    }
}
