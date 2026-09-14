package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaSoulTransducerEvents {
    // Iron'sはslot接頭辞からCastSourceを判定する。剣用CD倍率を適用させない。
    public static final String SPELL_SELECTION_SLOT = Curios.SPELLBOOK_SLOT + "_apprenticecodex_mana_soul_transducer";

    private ManaSoulTransducerEvents() {}

    public static boolean isEquipped(LivingEntity entity) {
        // Item指定検索は同tickの結果をキャッシュする。解除直後の支払い判定には現物を使う。
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> !inventory.findCurios(stack -> stack.is(ItemRegistry.MANA_SOUL_TRANSDUCER.get())).isEmpty())
                .orElse(false);
    }

    public static void updateAttributes(ServerPlayer player) {
        boolean active = MalumStaffChargeBridge.isAvailable() && isEquipped(player);
        double duration = active ? ManaSoulTransducerLogic.durationModifier(
                player.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION), ApprenticeCodexServerConfig.manaSoulTransducerCastRate()) : 0;
        double recovery = active ? ManaSoulTransducerLogic.recoveryModifier(
                player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION), ApprenticeCodexServerConfig.manaSoulTransducerRecoveryRate()) : 0;
        MalumStaffChargeBridge.updateAttributes(player, duration, recovery);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) updateAttributes(player);
    }

    @SubscribeEvent
    public static void onSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var spell = SpellRegistry.SOUL_CONVERSION.get();
        if (spell.isEnabled() && isEquipped(event.getEntity())
                && event.getManager().getSpellsForSlot(SPELL_SELECTION_SLOT).isEmpty()) {
            event.addSelectionOption(new SpellData(spell, 1), SPELL_SELECTION_SLOT, 0);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChanged(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().is(ItemRegistry.MANA_SOUL_TRANSDUCER.get()) || event.getTo().is(ItemRegistry.MANA_SOUL_TRANSDUCER.get()))) {
            updateAttributes(player);
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
            var data = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
            if (!isEquipped(player) && data.isCasting()
                    && data.getCastingSpellId().equals(SpellRegistry.SOUL_CONVERSION.get().getSpellId())) {
                Utils.serverSideCancelCast(player, false);
            }
        }
    }
}
