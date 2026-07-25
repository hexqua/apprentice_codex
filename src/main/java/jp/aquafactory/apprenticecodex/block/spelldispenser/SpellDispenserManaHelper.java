package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.ManaPotionRecoveryHelper;
import jp.aquafactory.apprenticecodex.utility.SpellManaAccessHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserManaHelper {
    public static final int MAX_MANA = SpellManaAccessHelper.MAX_MANA;
    public static final int REFILL_INTERVAL_TICKS = 40;
    private SpellDispenserManaHelper() {
    }

    public static int clampMana(int mana) {
        return SpellManaAccessHelper.clampMana(mana);
    }

    public static boolean isSupportedFlaskSlotItem(@NotNull ItemStack stack) {
        return isAutomationInputItem(stack);
    }

    public static boolean isAutomationInputItem(@NotNull ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }

        return isSupportedManaFlask(stack) || isSupportedManaPotion(stack);
    }

    public static boolean canAutomationExtract(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(Items.GLASS_BOTTLE) || isEmptyFlask(stack);
    }

    public static int getAutomationInputManaRecovery(@NotNull ItemStack stack) {
        if (isSupportedManaFlask(stack)) {
            return resolveManaRecoveryFromPotionStack(
                    SpellcastersFlask.getStoredItem(stack),
                    getGlowEnergyLevel(stack)
            );
        }

        if (isSupportedManaPotion(stack)) {
            return resolveManaRecoveryFromPotionStack(stack, 0);
        }

        return 0;
    }

    public static @NotNull ItemStack consumeAutomationInput(@NotNull ItemStack stack) {
        if (isSupportedManaFlask(stack)) {
            return SpellcastersFlask.copyAfterExtractingOneDose(stack);
        }

        if (isSupportedManaPotion(stack)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }

        return ItemStack.EMPTY;
    }

    public static int getSpellManaCost(SpellData spellData) {
        return SpellManaAccessHelper.getSpellManaCost(spellData);
    }

    public static boolean canAffordSpell(int currentMana, SpellData spellData) {
        return SpellManaAccessHelper.canAffordSpell(currentMana, spellData);
    }

    public static boolean canAffordSpell(@NotNull ManaAccess manaAccess, SpellData spellData) {
        return SpellManaAccessHelper.canAffordSpell(manaAccess, spellData);
    }

    public static boolean tryConsumeSpellMana(@NotNull ManaAccess manaAccess, SpellData spellData) {
        return SpellManaAccessHelper.tryConsumeSpellMana(manaAccess, spellData);
    }

    public static boolean tryRefillMana(@NotNull ManaAccess manaAccess) {
        var currentMana = clampMana(manaAccess.getCurrentMana());
        var remainingCapacity = MAX_MANA - currentMana;
        if (remainingCapacity <= 0) {
            return false;
        }

        RefillCandidate bestCandidate = null;
        var lastInventorySlot = Math.min(
                manaAccess.getInventorySlotCount(),
                SpellDispenserBlockEntity.FLASK_SLOT_START + SpellDispenserBlockEntity.FLASK_SLOT_COUNT
        );
        for (var slot = SpellDispenserBlockEntity.FLASK_SLOT_START; slot < lastInventorySlot; ++slot) {
            var candidate = resolveRefillCandidate(manaAccess.getInventoryStack(slot), remainingCapacity);
            if (candidate == null) {
                continue;
            }

            if (bestCandidate == null || candidate.recoveredMana() > bestCandidate.recoveredMana()) {
                bestCandidate = candidate.withSlot(slot);
            }
        }

        if (bestCandidate == null) {
            return false;
        }

        manaAccess.setInventoryStack(bestCandidate.slot(), bestCandidate.remainingStack());
        manaAccess.setCurrentMana(currentMana + bestCandidate.recoveredMana());
        return true;
    }

    private static RefillCandidate resolveRefillCandidate(@NotNull ItemStack stack, int remainingCapacity) {
        if (stack.isEmpty() || !isAutomationInputItem(stack)) {
            return null;
        }

        var recoveredMana = getAutomationInputManaRecovery(stack);
        if (recoveredMana <= 0 || recoveredMana > remainingCapacity) {
            return null;
        }

        var remainingStack = consumeAutomationInput(stack);
        if (remainingStack.isEmpty() && stack.is(ItemRegistry.SPELLCASTERS_FLASK.get())) {
            return null;
        }

        return new RefillCandidate(-1, recoveredMana, remainingStack);
    }

    private static boolean isSupportedManaFlask(@NotNull ItemStack stack) {
        return stack.is(ItemRegistry.SPELLCASTERS_FLASK.get())
                && SpellcastersFlask.canExtractOneDose(stack)
                && isSupportedManaPotion(SpellcastersFlask.getStoredItem(stack));
    }

    public static boolean isSupportedManaPotion(@NotNull ItemStack stack) {
        return ManaPotionRecoveryHelper.isSupportedManaPotion(stack);
    }

    private static boolean isEmptyFlask(@NotNull ItemStack stack) {
        return stack.is(ItemRegistry.SPELLCASTERS_FLASK.get()) && !SpellcastersFlask.canExtractOneDose(stack);
    }

    public static int getManaPotionRecovery(@NotNull ItemStack potionStack) {
        return isSupportedManaPotion(potionStack) ? resolveManaRecoveryFromPotionStack(potionStack, 0) : 0;
    }

    private static int resolveManaRecoveryFromPotionStack(@NotNull ItemStack potionStack, int amplifierBonus) {
        return ManaPotionRecoveryHelper.getRecovery(potionStack, MAX_MANA, amplifierBonus);
    }

    private static int getGlowEnergyLevel(@NotNull ItemStack flaskStack) {
        return Enchantments.getLevel(flaskStack, Enchantments.GLOW_ENERGY);
    }

    public interface ManaAccess extends SpellManaAccessHelper.ManaAccess {
        int getInventorySlotCount();

        @NotNull ItemStack getInventoryStack(int slot);

        void setInventoryStack(int slot, @NotNull ItemStack stack);
    }

    private record RefillCandidate(int slot, int recoveredMana, ItemStack remainingStack) {
        private RefillCandidate withSlot(int slot) {
            return new RefillCandidate(slot, recoveredMana, remainingStack);
        }
    }
}
