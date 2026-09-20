package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncMultipurposeStaffrifleFireEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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

        var spell = SpellRegistry.getSpell(event.getSpellId());
        if (!MultipurposeStaffrifleCastContext.isActiveFor(player.getUUID(), castingItem, spell)) {
            return;
        }

        // 発射確定後に通知し、初弾の向きへ反動を混入させない。
        Networks.sendToTrackingEntityAndSelf(player, new SyncMultipurposeStaffrifleFireEffectPacket(player.getId()));
        event.setManaCost(0);
        if (MultipurposeStaffrifleCastContext.isActiveRecastFor(player.getUUID(), castingItem, spell)) {
            MultipurposeStaffrifleCastContext.clearPendingIfMatches(player.getUUID(), castingItem, spell);
            return;
        }

        if (!player.isCreative()) {
            if (MagiAgentSuitEffects.shouldSkipAmmoConsumption(player)) {
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
        if (!(castingItem.getItem() instanceof MultipurposeStaffrifle)) {
            return;
        }

        if (!MultipurposeStaffrifleCastContext.isActiveFor(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        var spell = event.getSpell();
        if (spell.getCastType() == CastType.LONG
                && MultipurposeStaffrifle.hasSilverRing(castingItem, player.level().registryAccess())
                && !MultipurposeStaffrifleCastContext.isActiveRecastFor(player.getUUID(), castingItem, spell)) {
            // 即時化前の実効詠唱時間を加算し、CD短縮を二重に適用しない。
            event.setEffectiveCooldown(event.getEffectiveCooldown()
                    + spell.getEffectiveCastTime(Math.max(1, magicData.getCastingSpellLevel()), player));
        }
        MultipurposeStaffrifleCastContext.clearPendingIfMatches(player.getUUID(), castingItem, event.getSpell());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
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
