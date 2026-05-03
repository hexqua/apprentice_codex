package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChromaticMagiaDressCastEvent {
    private ChromaticMagiaDressCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getPlayerRecasts().hasRecastForSpell(event.getSpellId())) {
            return;
        }

        var spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null || spell == SpellRegistry.none()) {
            return;
        }

        for (var armorStack : player.getArmorSlots()) {
            if (!(armorStack.getItem() instanceof ChromaticMagiaDressItem dressItem)) {
                continue;
            }
            if (shouldRecord(dressItem.getArmorType(), spell.getCastType(), spell.getRecastCount(event.getSpellLevel(), player))) {
                ChromaticMagiaDressHistory.append(armorStack, event.getSchoolType());
            }
        }
    }

    private static boolean shouldRecord(ArmorItem.Type type, CastType castType, int recastCount) {
        return switch (type) {
            case HELMET -> castType == CastType.LONG;
            case CHESTPLATE -> recastCount > 0;
            case LEGGINGS -> castType == CastType.CONTINUOUS;
            case BOOTS -> castType == CastType.INSTANT;
            case BODY -> false;
        };
    }
}
