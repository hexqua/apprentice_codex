package jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class CommenceMagiModuleModifier implements GunModifier {
    public static final ComponentType<Integer> MODULE_COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "commence_magi_module_count"), () -> 0);
    public static final ComponentType<Double> HEADSHOT_MULTIPLIER = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "commence_magi_module_multiplier"), () -> 1.0);
    public static final ComponentType<Integer> MANA_COST = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "commence_magi_module_mana_cost"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(MODULE_COUNT, components.getOrDefault(MODULE_COUNT) + 1);
        components.getOrCreate(ShotComponents.ON_SHOT).getOrCreate(CommenceMagiModuleManaCost.class, CommenceMagiModuleManaCost::new);
        components.getOrCreate(ShotComponents.ON_HIT).getOrCreate(CommenceMagiModuleHeadshotSound.class, CommenceMagiModuleHeadshotSound::new);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = CommenceMagiModuleConfigState.values();
        String key = "item.apprenticecodex.commence_magi_module";
        builder.accept(Component.translatable(key + ".desc_1", values.initialHeadshotBonusPercent()).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_2", values.additionalHeadshotBonusPercent()).withStyle(ChatFormatting.GREEN));
        if (values.manaCost() > 0) {
            builder.accept(Component.translatable(key + ".desc_3", values.manaCost()).withStyle(ChatFormatting.RED));
        }
    }
}
