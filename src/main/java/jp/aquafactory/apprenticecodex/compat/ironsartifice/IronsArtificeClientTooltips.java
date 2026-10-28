package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class IronsArtificeClientTooltips {
    private IronsArtificeClientTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var lines = event.getToolTip();
        if (stack.getItem() instanceof SpellboundArmsModuleItem) {
            lines.removeIf(line -> containsKey(line, "item.apprenticecodex.spellbound_arms_module.desc_1")
                    || containsKey(line, "item.apprenticecodex.spellbound_arms_module.desc_2")
                    || containsKey(line, "item.apprenticecodex.spellbound_arms_module.desc_3")
                    || containsKey(line, "item.apprenticecodex.spellbound_arms_module.desc_3.no_school")
                    || containsKey(line, "item.apprenticecodex.spellbound_arms_module.desc_4"));
            SpellboundArmsModuleEvents.describe(line -> lines.add(Component.literal(" ").append(line)),
                    SpellboundArmsModuleEvents.power(event.getEntity()), SpellboundArmsModuleConfigState.values());
        } else if (stack.getItem() instanceof GunItem gun) {
            var profile = GunplayManager.compose(event.getEntity(), gun.getGun(), stack);
            int cost = IronsArtificeManaEvents.cost(profile, true);
            if (cost <= 0) return;
            int insertion = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                if (containsKey(lines.get(i), "irons_artifice.tooltip.ammo_capacity")) { insertion = i + 1; break; }
            }
            lines.add(insertion, Component.literal(" ").append(Component.translatable(
                    "item.apprenticecodex.irons_artifice.tooltip.mana_cost",
                    Component.literal(Integer.toString(cost)).withStyle(ChatFormatting.GREEN))).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    private static boolean containsKey(Component line, String key) {
        return line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                || line.getSiblings().stream().anyMatch(child -> containsKey(child, key));
    }
}
