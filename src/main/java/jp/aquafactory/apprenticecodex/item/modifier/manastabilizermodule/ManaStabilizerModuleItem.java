package jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule;

import io.redspace.irons_artifice.modifier.ModifierItem;

public final class ManaStabilizerModuleItem extends ModifierItem {
    public ManaStabilizerModuleItem() {
        super(new Properties().stacksTo(1), new ManaStabilizerModuleModifier());
    }
}
