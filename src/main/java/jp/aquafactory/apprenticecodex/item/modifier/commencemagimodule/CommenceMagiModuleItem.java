package jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule;

import io.redspace.irons_artifice.modifier.ModifierItem;

public final class CommenceMagiModuleItem extends ModifierItem {
    public CommenceMagiModuleItem() {
        super(new Properties().stacksTo(1), new CommenceMagiModuleModifier());
    }
}
