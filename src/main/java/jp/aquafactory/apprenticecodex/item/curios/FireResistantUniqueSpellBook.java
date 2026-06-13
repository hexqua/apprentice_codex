package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.Arrays;
import java.util.List;

public class FireResistantUniqueSpellBook extends SpellBook implements UniqueItem {
    private List<SpellData> spellData = null;
    private SpellDataRegistryHolder[] spellDataRegistryHolders;

    public FireResistantUniqueSpellBook(SpellDataRegistryHolder[] spellDataRegistryHolders) {
        this(spellDataRegistryHolders, 0);
    }

    public FireResistantUniqueSpellBook(SpellDataRegistryHolder[] spellDataRegistryHolders, int additionalSlots) {
        super(spellDataRegistryHolders.length + additionalSlots, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .fireResistant());
        this.spellDataRegistryHolders = spellDataRegistryHolders;
    }

    public List<SpellData> getSpells() {
        if (spellData == null) {
            spellData = Arrays.stream(spellDataRegistryHolders).map(SpellDataRegistryHolder::getSpellData).toList();
            spellDataRegistryHolders = null;
        }
        return spellData;
    }

    @Override
    public Component getName(ItemStack stack) {
        return stack.has(ComponentRegistry.SPELL_CONTAINER) && stack.get(ComponentRegistry.SPELL_CONTAINER).isImproved()
                ? Component.translatable("tooltip.irons_spellbooks.improved_format", super.getName(stack))
                : super.getName(stack);
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null) {
            return;
        }

        if (!ISpellContainer.isSpellContainer(itemStack)) {
            var spellContainer = ISpellContainer.create(getMaxSpellSlots(), true, true).mutableCopy();
            getSpells().forEach(spellSlot -> spellContainer.addSpell(spellSlot.getSpell(), spellSlot.getLevel(), true));
            ISpellContainer.set(itemStack, spellContainer.toImmutable());
        }
    }
}
