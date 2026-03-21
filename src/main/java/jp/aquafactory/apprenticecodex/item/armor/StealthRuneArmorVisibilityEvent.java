package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class StealthRuneArmorVisibilityEvent {
    private StealthRuneArmorVisibilityEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        var entity = event.getEntity();
        if (!entity.isInvisible()) {
            return;
        }

        var armorCover = calculateArmorCover(entity);
        if (armorCover.stealthRuneArmorPieces() <= 0 || armorCover.vanillaCover() <= 0.0D) {
            return;
        }

        // LivingVisibilityEvent は visibility 最終倍率だけを触れるため、
        // stealth rune 分のカバー率だけ差し替えると vanilla/Forge/他 Mod の残り補正を壊しにくい.
        event.modifyVisibility(armorCover.effectiveCover() / armorCover.vanillaCover());
    }

    private static ArmorCover calculateArmorCover(LivingEntity entity) {
        int totalSlots = 0;
        int equippedPieces = 0;
        int effectivePieces = 0;
        int stealthRuneArmorPieces = 0;

        for (var stack : entity.getArmorSlots()) {
            ++totalSlots;
            if (stack.isEmpty()) {
                continue;
            }

            ++equippedPieces;
            if (StealthRuneArmorItem.isStealthRuneArmor(stack)) {
                ++stealthRuneArmorPieces;
                continue;
            }

            ++effectivePieces;
        }

        return new ArmorCover(
                stealthRuneArmorPieces,
                clampArmorCover(equippedPieces, totalSlots),
                clampArmorCover(effectivePieces, totalSlots)
        );
    }

    private static double clampArmorCover(int equippedPieces, int totalSlots) {
        if (totalSlots <= 0) {
            return 0.0D;
        }

        return Math.max((double) equippedPieces / (double) totalSlots, 0.1D);
    }

    private record ArmorCover(int stealthRuneArmorPieces, double vanillaCover, double effectiveCover) {
    }
}
