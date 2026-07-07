package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.SpellGunCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MultipurposeStaffrifleCastEvent {
    private MultipurposeStaffrifleCastEvent() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof MultipurposeStaffrifle staffrifle)) {
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        if (!MultipurposeStaffrifleCastContext.isActiveFor(player.getUUID(), castingItem, spell)) {
            return;
        }

        if (MultipurposeStaffrifleCastContext.isActiveRecastFor(player.getUUID(), castingItem, spell)) {
            MultipurposeStaffrifleCastContext.clearPendingIfMatches(player.getUUID(), castingItem, spell);
            return;
        }

        if (!player.isCreative()) {
            if (MagiAgentSuitEffects.shouldSkipAmmoConsumption(player)) {
                if (MagiAgentSuitEffects.shouldSkipStaffrifleManaCostWithAmmo()) {
                    event.setManaCost(0);
                }
                return;
            }
            SpellGunCastEvent.consumeAmmo(player, player.getInventory(), staffrifle.getAmmoItem(castingItem), staffrifle);
        }
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof MultipurposeStaffrifle staffrifle)) {
            return;
        }

        if (!MultipurposeStaffrifleCastContext.isActiveFor(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        event.setEffectiveCooldown(staffrifle.resolveSpecialCooldownTicks(event.getEffectiveCooldown()));
        MultipurposeStaffrifleCastContext.clearPendingIfMatches(player.getUUID(), castingItem, event.getSpell());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MultipurposeStaffrifleCastContext.clearExpiredPending(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MultipurposeStaffrifleRateLimiter.clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MultipurposeStaffrifleRateLimiter.clearAll();
    }
}
