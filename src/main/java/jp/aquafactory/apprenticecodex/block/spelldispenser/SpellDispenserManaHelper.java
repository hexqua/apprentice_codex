package jp.aquafactory.apprenticecodex.block.spelldispenser;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserManaHelper {
    public static final int MAX_MANA = 1000;
    public static final int REFILL_INTERVAL_TICKS = 40;
    private static final int INSTANT_MANA_BASE_RECOVERY = 25;
    private static final float INSTANT_MANA_MAX_MANA_RATIO = 0.05F;

    private SpellDispenserManaHelper() {
    }

    public static int clampMana(int mana) {
        return Math.max(0, Math.min(MAX_MANA, mana));
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
        if (spellData == SpellData.EMPTY) {
            return 0;
        }

        return Math.max(0, spellData.getSpell().getManaCost(spellData.getLevel()));
    }

    public static boolean canAffordSpell(int currentMana, SpellData spellData) {
        return currentMana >= getSpellManaCost(spellData);
    }

    public static boolean tryConsumeSpellMana(@NotNull ManaAccess manaAccess, SpellData spellData) {
        var manaCost = getSpellManaCost(spellData);
        if (manaCost <= 0) {
            return true;
        }

        var currentMana = clampMana(manaAccess.getCurrentMana());
        if (currentMana < manaCost) {
            return false;
        }

        manaAccess.setCurrentMana(currentMana - manaCost);
        return true;
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

    private static boolean isSupportedManaPotion(@NotNull ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }

        var effects = PotionContentsHelper.getMobEffects(stack);
        return !effects.isEmpty()
                && effects.stream().allMatch(effect -> effect.getEffect().value() == MobEffectRegistry.INSTANT_MANA.get());
    }

    private static boolean isEmptyFlask(@NotNull ItemStack stack) {
        return stack.is(ItemRegistry.SPELLCASTERS_FLASK.get()) && !SpellcastersFlask.canExtractOneDose(stack);
    }

    private static int resolveManaRecoveryFromPotionStack(@NotNull ItemStack potionStack, int amplifierBonus) {
        if (potionStack.isEmpty()) {
            return 0;
        }

        for (var effect : PotionContentsHelper.getMobEffects(potionStack)) {
            if (effect.getEffect().value() == MobEffectRegistry.INSTANT_MANA.get()) {
                return resolveManaRecoveryFromAmplifier(effect.getAmplifier() + amplifierBonus);
            }
        }

        return 0;
    }

    private static int resolveManaRecoveryFromAmplifier(int amplifier) {
        if (amplifier < 0) {
            return 0;
        }

        var level = amplifier + 1;
        // Iron's Spells の即時マナ回復式を Spell Dispenser の固定最大マナ 1000 前提で再現する。
        return Math.round(level * INSTANT_MANA_BASE_RECOVERY + MAX_MANA * (level * INSTANT_MANA_MAX_MANA_RATIO));
    }

    private static int getGlowEnergyLevel(@NotNull ItemStack flaskStack) {
        return Enchantments.getLevel(flaskStack, Enchantments.GLOW_ENERGY);
    }

    public interface ManaAccess {
        int getCurrentMana();

        void setCurrentMana(int mana);

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
