package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MagiAgentSuitCooldownEvent {
    private MagiAgentSuitCooldownEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !MagiAgentSuitEffects.isTargetSpell(event.getSpell())
                || !MagiAgentSuitEffects.isWearingSuitPiece(player, net.minecraft.world.item.ArmorItem.Type.BOOTS)
                || event.getEffectiveCooldown() <= 0) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        var bootsCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                castingItem
        );
        if (bootsCooldown > 0 && bootsCooldown < event.getEffectiveCooldown()) {
            event.setEffectiveCooldown(bootsCooldown);
        }
    }
}
