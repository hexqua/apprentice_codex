package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SchoolAffinityPotionTooltipEvent {
    private SchoolAffinityPotionTooltipEvent() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }

        for (var effectInstance : PotionUtils.getMobEffects(event.getItemStack())) {
            if (!(effectInstance.getEffect() instanceof SchoolAffinityEffect schoolAffinityEffect)) {
                continue;
            }

            var oldLine = buildTooltipLine(Component.translatable(effectInstance.getDescriptionId()), effectInstance);
            var newLine = buildTooltipLine(schoolAffinityEffect.getDisplayName().copy(), effectInstance);
            replaceTooltipLine(tooltip, oldLine, newLine);
        }
    }

    private static MutableComponent buildTooltipLine(MutableComponent baseName, net.minecraft.world.effect.MobEffectInstance effectInstance) {
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
                    MobEffectUtil.formatDuration(effectInstance, 1.0F)
            );
        }

        return line.withStyle(effectInstance.getEffect().getCategory().getTooltipFormatting());
    }

    private static void replaceTooltipLine(java.util.List<Component> tooltip, Component oldLine, Component newLine) {
        var oldText = oldLine.getString();
        for (var i = 0; i < tooltip.size(); i++) {
            if (!tooltip.get(i).getString().equals(oldText)) {
                continue;
            }

            tooltip.set(i, newLine);
            return;
        }
    }
}
