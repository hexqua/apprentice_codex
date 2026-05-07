package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ArchivistsGrimoireSpellSelectionEvents {
    private ArchivistsGrimoireSpellSelectionEvents() {
    }

    @SubscribeEvent
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        if (spellbookStack == null || !(spellbookStack.getItem() instanceof ArchivistsGrimoire)) {
            return;
        }

        for (var visibleSlot = 0; visibleSlot < ArchivistsGrimoire.COLUMN_COUNT; ++visibleSlot) {
            var spellData = ArchivistsGrimoire.getVisibleSpell(spellbookStack, visibleSlot, player.registryAccess());
            if (spellData != SpellData.EMPTY) {
                event.addSelectionOption(spellData, Curios.SPELLBOOK_SLOT, visibleSlot);
            }
        }
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || !Curios.SPELLBOOK_SLOT.equals(event.getIdentifier())) {
            return;
        }

        // Iron's本体はISpellContainer持ちの装備変更だけを同期するため、独自保管型の魔導書は明示的に更新させる。
        if (event.getFrom().getItem() instanceof ArchivistsGrimoire || event.getTo().getItem() instanceof ArchivistsGrimoire) {
            if (event.getTo().getItem() instanceof ArchivistsGrimoire) {
                ArchivistsGrimoire.ensureSelectedRowHasScroll(event.getTo(), serverPlayer.registryAccess());
            }
            PacketDistributor.sendToPlayer(serverPlayer, new EquipmentChangedPacket());
        }
    }
}
