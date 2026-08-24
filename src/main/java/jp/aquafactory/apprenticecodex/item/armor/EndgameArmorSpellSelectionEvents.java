package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EndgameArmorSpellSelectionEvents {
    public static final String HEAD_SLOT = "apprenticecodex_scrollwoven_head";
    public static final String LEGS_SLOT = "apprenticecodex_scrollwoven_legs";
    public static final String FEET_SLOT = "apprenticecodex_scrollwoven_feet";

    private static final EquipmentSlot[] ORDERED_ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private EndgameArmorSpellSelectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        for (var equipmentSlot : ORDERED_ARMOR_SLOTS) {
            var armorStack = player.getItemBySlot(equipmentSlot);
            if (EndgameArmorCalibration.getEnabledStoredScrollSlotCount(armorStack) <= 0) {
                continue;
            }
            var spellData = EndgameArmorCalibration.getStoredSpellData(armorStack);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            // Iron'sのイベント追加経路は標準コンテナの重複統合を通らないため、部位ごとの魔法を独立して残す。
            event.addSelectionOption(spellData, selectionSlotId(equipmentSlot), 0);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // Iron'sはISpellContainer装備だけを再同期するため、独自保存型防具の着脱を明示通知する。
        if (EndgameArmorCalibration.usesStoredCalibrationScrolls(event.getFrom())
                || EndgameArmorCalibration.usesStoredCalibrationScrolls(event.getTo())) {
            PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
        }
    }

    public static String selectionSlotId(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> HEAD_SLOT;
            case LEGS -> LEGS_SLOT;
            case FEET -> FEET_SLOT;
            default -> "";
        };
    }
}
