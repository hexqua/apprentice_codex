package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.SchoolAffinityTooltipHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {
    @ModifyVariable(
            method = "addPotionTooltip(Ljava/util/function/Consumer;FF)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Consumer<Component> apprenticecodex$rewriteSchoolAffinityTooltipLines(
            Consumer<Component> originalTooltipAdder,
            Consumer<Component> tooltipAdder,
            float durationScale,
            float tickRate
    ) {
        var replacements = SchoolAffinityTooltipHelper.createTooltipReplacements(
                ((PotionContents) (Object) this).getAllEffects(),
                durationScale,
                tickRate
        );
        if (replacements.isEmpty()) {
            return originalTooltipAdder;
        }

        return component -> originalTooltipAdder.accept(replacements.getOrDefault(component.getString(), component));
    }
}
