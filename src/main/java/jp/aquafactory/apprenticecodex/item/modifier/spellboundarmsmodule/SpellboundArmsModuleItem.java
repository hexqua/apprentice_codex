package jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule;

import io.redspace.irons_artifice.modifier.ModifierItem;

public final class SpellboundArmsModuleItem extends ModifierItem {
    public SpellboundArmsModuleItem() {
        super(new Properties().stacksTo(1), new SpellboundArmsModuleModifier());
    }
}
