package jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffVisibilityClientBridge;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SpellCastParryingRingClientEvents {
    private static final String KEY = "item.apprenticecodex.spell_cast_parrying_ring.malum.staff";

    private SpellCastParryingRingClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ItemRegistry.SPELL_CAST_PARRYING_RING.get())) return;
        var minecraft = Minecraft.getInstance();
        // 起動時の検索情報生成ではタグ・開示判定を行わない。
        if (minecraft.level == null || minecraft.player == null) return;
        var lines = event.getToolTip();
        if (indexOfTranslation(lines, KEY) >= 0 || !MalumStaffVisibilityClientBridge.isAnyStaffVisible()) return;

        // 2行目決め打ちで拾ってその後ろに差し込む.
        var index = indexOfTranslation(lines, "item.apprenticecodex.spell_cast_parrying_ring.desc_2");
        lines.add(index < 0 ? lines.size() : index + 1,
                Component.literal(" ").append(Component.translatable(KEY)).withStyle(ChatFormatting.YELLOW));
    }

    private static int indexOfTranslation(List<Component> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            if (containsTranslation(lines.get(i), key)) return i;
        }
        return -1;
    }

    private static boolean containsTranslation(Component component, String key) {
        // Curios の説明は字下げ用 literal の sibling に翻訳キーを保持する.
        return component.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey())
                || component.getSiblings().stream().anyMatch(sibling -> containsTranslation(sibling, key));
    }
}
