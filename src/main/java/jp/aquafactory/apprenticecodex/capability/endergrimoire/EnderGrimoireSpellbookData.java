package jp.aquafactory.apprenticecodex.capability.endergrimoire;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.capabilities.magic.SpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.function.Consumer;

public class EnderGrimoireSpellbookData {
    private static final String SPELL_CONTAINER_TAG = "spell_container";
    private ISpellContainer spellContainer = createDefaultContainer();

    // TOMagicの拡張アイテムを考え一旦15スロット.
    private static ISpellContainer createDefaultContainer() {
        return ISpellContainer.create(15, true, true);
    }

    public ISpellContainer getSpellContainer() {
        return spellContainer;
    }

    public void setSpellContainer(ISpellContainer spellContainer) {
        this.spellContainer = spellContainer == null ? createDefaultContainer() : spellContainer;
    }

    public void edit(Consumer<ISpellContainerMutable> editor) {
        var mutable = spellContainer.mutableCopy();
        editor.accept(mutable);
        spellContainer = mutable.toImmutable();
    }

    public CompoundTag save() {
        var root = new CompoundTag();
        SpellContainer.CODEC.encodeStart(NbtOps.INSTANCE, spellContainer)
                .resultOrPartial(error -> ApprenticeCodex.LOGGER.warn("Failed to encode EnderGrimoireSpellbookData: {}", error))
                .ifPresent(tag -> root.put(SPELL_CONTAINER_TAG, tag));
        return root;
    }

    public void load(CompoundTag root) {
        if (!root.contains(SPELL_CONTAINER_TAG)) {
            spellContainer = createDefaultContainer();
            return;
        }

        spellContainer = SpellContainer.CODEC.parse(NbtOps.INSTANCE, root.get(SPELL_CONTAINER_TAG))
                .resultOrPartial(error -> ApprenticeCodex.LOGGER.warn("Failed to decode EnderGrimoireSpellbookData: {}", error))
                .orElseGet(EnderGrimoireSpellbookData::createDefaultContainer);
    }
}
