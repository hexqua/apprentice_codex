package jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.SpellboundArmsModuleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class ManaStabilizerModuleModifier implements GunModifier {
    public static final ComponentType<Integer> MODULE_COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_stabilizer_module_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(MODULE_COUNT, components.getOrDefault(MODULE_COUNT) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ManaStabilizerModuleConfigState.values();
        String key = "item.apprenticecodex.mana_stabilizer_module";
        builder.accept(Component.translatable(key + ".desc_1").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(key + ".desc_2", Component.keybind("key.use")).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(key + ".desc_3", SpellboundArmsModuleEvents.number(values.adsRecoilMultiplier() * 100)).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_4", SpellboundArmsModuleEvents.number(values.adsSpreadBonus())).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_5", SpellboundArmsModuleEvents.number(values.adsMovementSpeedMultiplier() * 100)).withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable(key + ".desc_6").withStyle(ChatFormatting.AQUA));
    }
}
