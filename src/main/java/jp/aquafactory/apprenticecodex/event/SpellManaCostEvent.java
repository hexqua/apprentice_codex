package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellManaCostEvent {
    private SpellManaCostEvent() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var affectedSpell = findCraftsmansDelightAffectedSpell(event.getSpellId());
        if (affectedSpell == null || !affectedSpell.isCraftsmansDelightManaCostDiscountEnabled()) {
            return;
        }

        event.setManaCost(CraftsmansDelight.applyManaCostDiscount(event.getManaCost(), player));
    }

    private static @Nullable ICraftsmansDelightAffectedSpell findCraftsmansDelightAffectedSpell(String spellId) {
        for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
            var spell = spellEntry.get();
            if (!spell.getSpellId().equals(spellId)) {
                continue;
            }

            if (spell instanceof ICraftsmansDelightAffectedSpell affectedSpell) {
                return affectedSpell;
            }
            return null;
        }

        return null;
    }
}
