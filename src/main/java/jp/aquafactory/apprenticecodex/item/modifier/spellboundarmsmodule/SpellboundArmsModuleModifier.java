package jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeManaCost;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.SpellboundArmsModuleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SpellboundArmsModuleModifier implements GunModifier {
    public static final ComponentType<Integer> MODULE_COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellbound_arms_module_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(MODULE_COUNT, components.getOrDefault(MODULE_COUNT) + 1);
        components.getOrCreate(ShotComponents.ON_SHOT).getOrCreate(IronsArtificeManaCost.class, IronsArtificeManaCost::new);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        SpellboundArmsModuleEvents.describe(builder, SpellboundArmsModuleEvents.Power.NEUTRAL, SpellboundArmsModuleConfigState.values());
    }
}
