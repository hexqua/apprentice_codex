package jp.aquafactory.apprenticecodex.spell.callbroom;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CallBroomSpellSelectionEvents {
    private CallBroomSpellSelectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        if (BroomCurioSupport.findUniqueEquippedBroom(event.getEntity()).isEmpty()) {
            return;
        }

        event.addSelectionOption(
                new SpellData(SpellRegistry.CALL_BROOM.get(), 1),
                BroomCurioSupport.SPELL_SELECTION_SLOT,
                0
        );
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || (!BroomCurioSupport.isBroom(event.getFrom()) && !BroomCurioSupport.isBroom(event.getTo()))) {
            return;
        }

        // CallBroomはISpellContainer由来ではないため、装備変更時にIron's側の選択一覧を明示更新する.
        PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
    }
}
