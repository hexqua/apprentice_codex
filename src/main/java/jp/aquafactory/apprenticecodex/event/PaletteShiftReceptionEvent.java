package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PaletteShiftReceptionEvent {
    private PaletteShiftReceptionEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        var player = event.getEntity();
        if (!player.hasEffect(EffectRegistry.PALETTE_RECEPTION.get())) {
            return;
        }

        if (SpellRegistry.PALETTE_SHIFT.get().getSpellId().equals(event.getSpellId())) {
            return;
        }

        var schoolType = resolveCastingSchoolType(event);
        var applied = applyPastelAffinity(player, schoolType);
        if (!applied) {
            return;
        }

        player.removeEffect(EffectRegistry.PALETTE_RECEPTION.get());
        // PaletteReception を最優先で消費するため、対象魔法の詠唱は常に中断する.
        event.setCanceled(true);
    }

    private static SchoolType resolveCastingSchoolType(SpellPreCastEvent event) {
        // 後続実装で SchoolType を起点に分岐を増やすため、取得処理をここに集約する.
        return event.getSchoolType();
    }

    private static boolean applyPastelAffinity(Player player, SchoolType schoolType) {
        var mainHand = player.getMainHandItem();
        var offHand = player.getOffhandItem();

        var applied = applyPastelAffinityToStack(mainHand, schoolType);
        if (offHand != mainHand) {
            applied |= applyPastelAffinityToStack(offHand, schoolType);
        }
        return applied;
    }

    private static boolean applyPastelAffinityToStack(ItemStack stack, SchoolType schoolType) {
        if (!PastelStaff.isPastelStaff(stack)) {
            return false;
        }

        PastelStaff.writeStoneTintColor(stack, resolveSchoolTintColor(schoolType));
        PastelStaff.writeStoneAffinitySchool(stack, schoolType);
        return true;
    }

    private static int resolveSchoolTintColor(SchoolType schoolType) {
        var color = schoolType.getDisplayName().getStyle().getColor();
        if (color == null) {
            return PastelStaff.DEFAULT_STONE_TINT_COLOR;
        }
        return color.getValue();
    }
}
