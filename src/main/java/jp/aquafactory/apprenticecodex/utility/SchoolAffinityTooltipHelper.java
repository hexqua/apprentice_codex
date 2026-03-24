package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SchoolAffinityTooltipHelper {
    private SchoolAffinityTooltipHelper() {
    }

    public static Map<String, Component> createTooltipReplacements(
            Iterable<MobEffectInstance> effects,
            float durationScale,
            float tickRate
    ) {
        var replacements = new LinkedHashMap<String, Component>();

        for (var effectInstance : effects) {
            if (!(effectInstance.getEffect().value() instanceof SchoolAffinityEffect schoolAffinityEffect)) {
                continue;
            }

            var oldLine = buildTooltipLine(
                    Component.translatable(effectInstance.getDescriptionId()),
                    effectInstance,
                    durationScale,
                    tickRate
            );
            var newLine = buildTooltipLine(
                    schoolAffinityEffect.getDisplayName().copy(),
                    effectInstance,
                    durationScale,
                    tickRate
            );
            replacements.put(oldLine.getString(), newLine);
        }

        return replacements;
    }

    public static MutableComponent buildTooltipLine(
            MutableComponent baseName,
            MobEffectInstance effectInstance,
            float durationScale,
            float tickRate
    ) {
        var line = baseName;
        if (effectInstance.getAmplifier() > 0) {
            line = Component.translatable(
                    "potion.withAmplifier",
                    line,
                    Component.translatable("potion.potency." + effectInstance.getAmplifier())
            );
        }

        if (!effectInstance.endsWithin(20)) {
            line = Component.translatable(
                    "potion.withDuration",
                    line,
                    MobEffectUtil.formatDuration(effectInstance, durationScale, tickRate)
            );
        }

        return line.withStyle(effectInstance.getEffect().value().getCategory().getTooltipFormatting());
    }

    public static Component buildFluidDescription(ItemStack potionStack, SchoolAffinityPotion schoolAffinityPotion) {
        var effects = PotionContentsHelper.getMobEffects(potionStack);
        if (effects.isEmpty()) {
            return potionStack.getHoverName();
        }

        var primary = effects.get(0);
        var description = schoolAffinityPotion.getItemDisplayName(potionStack.getItem()).copy();

        if (primary.getAmplifier() > 0) {
            description = Component.translatable(
                    "potion.withAmplifier",
                    description,
                    Component.translatable("potion.potency." + primary.getAmplifier())
            );
        }

        if (!primary.endsWithin(20)) {
            description = Component.translatable(
                    "potion.withDuration",
                    description,
                    // Fluid tooltip もゲーム内表記に合わせ、20 ticks = 1 second で整形する。
                    MobEffectUtil.formatDuration(primary, 1.0F, 20.0F)
            );
        }

        return description.withStyle(primary.getEffect().value().getCategory().getTooltipFormatting());
    }

    @Nullable
    public static Component tryBuildFluidDescription(FluidStack fluidStack) {
        var potionStack = PotionFluid.from(fluidStack);
        if (potionStack.isEmpty()) {
            return null;
        }

        var potion = PotionContentsHelper.getPotion(potionStack);
        if (!(potion instanceof SchoolAffinityPotion schoolAffinityPotion)) {
            return null;
        }

        return buildFluidDescription(potionStack, schoolAffinityPotion);
    }
}
