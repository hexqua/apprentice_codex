package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletFreecastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MagiAgentSuitCooldownEvent {
    private MagiAgentSuitCooldownEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !MagiAgentSuitEffects.isTargetSpell(event.getSpell())
                || !MagiAgentSuitEffects.isWearingSuitPiece(player, net.minecraft.world.item.ArmorItem.Type.BOOTS)
                || event.getEffectiveCooldown() <= 0) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        var cooldownSource = MithrilFreecastStaffCastContext.resolveCooldownSource(
                player.getUUID(),
                castingItem,
                event.getSpell()
        );
        if (cooldownSource.isPresent()) {
            return;
        }
        if (castingItem.getItem() instanceof ScrollcasterGauntlet
                && ScrollcasterGauntletFreecastContext.matches(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }
        var castSource = event.getCastSource();
        var bootsCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                castSource
        );
        var adjustedCooldown = resolveBootsAdjustedCooldown(
                event.getEffectiveCooldown(),
                MagicManager.getEffectiveSpellCooldown(event.getSpell(), player, castSource),
                bootsCooldown
        );
        if (adjustedCooldown < event.getEffectiveCooldown()) {
            event.setEffectiveCooldown(adjustedCooldown);
        }
    }

    static int resolveBootsAdjustedCooldown(int currentEffectiveCooldown, int baseEffectiveCooldown, int bootsCooldown) {
        if (currentEffectiveCooldown <= 0 || bootsCooldown <= 0 || bootsCooldown >= baseEffectiveCooldown) {
            return currentEffectiveCooldown;
        }

        if (currentEffectiveCooldown <= baseEffectiveCooldown) {
            return Math.min(currentEffectiveCooldown, bootsCooldown);
        }

        return Math.max(bootsCooldown, currentEffectiveCooldown - (baseEffectiveCooldown - bootsCooldown));
    }
}
