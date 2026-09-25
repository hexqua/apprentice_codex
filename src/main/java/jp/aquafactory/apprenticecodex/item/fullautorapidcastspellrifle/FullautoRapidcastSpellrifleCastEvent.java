package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunRecastCompletion;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncFullautoRapidcastSpellrifleFireEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FullautoRapidcastSpellrifleCastEvent {
    private FullautoRapidcastSpellrifleCastEvent() {
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
        if (!(castingItem.getItem() instanceof FullautoRapidcastSpellrifle staffrifle)) {
            return;
        }

        var spell = SpellRegistry.getSpell(event.getSpellId());
        if (!FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), castingItem, spell)) {
            return;
        }

        // 詠唱開始の通知では、実行までに届いた反動の視線同期が初弾の向きを変えてしまう。
        // 同期処理へ戻る前に魔法を実行するこのイベントで、発射後のリコイルを通知する。
        Networks.sendToTrackingEntityAndSelf(player, new SyncFullautoRapidcastSpellrifleFireEffectPacket(player.getId()));

        if (FullautoRapidcastSpellrifleCastContext.isActiveRecastFor(player.getUUID(), castingItem, spell)) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEchoManaCost(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) return;
        var spell = SpellRegistry.getSpell(event.getSpellId());
        // Hoodなどによる免除・割引を残し、実消費だけに一度乗算する。
        var multiplier = FullautoEchoCasting.manaMultiplier(player, magicData.getPlayerCastingItem(), spell);
        if (multiplier != 1.0D) event.setManaCost(FullautoEchoCasting.scaleMana(event.getManaCost(), multiplier));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Boots等の装備短縮を終えてから、詠唱時間と銃の固定量短縮を一度だけ反映する。
        var completion = SpellgunRecastCompletion.find(player, event.getSpell());
        if (completion != null && completion.cooldown() != null) {
            var policy = completion.cooldown();
            if (policy.fullauto()) {
                event.setEffectiveCooldown(FullautoCooldownPolicy.resolve(event.getSpell().getSpellCooldown(),
                        event.getEffectiveCooldown(), policy.castTime()));
            }
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof FullautoRapidcastSpellrifle staffrifle)) {
            return;
        }

        if (!FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        var castTime = 0;
        var spell = event.getSpell();
        if (spell.getCastType() == CastType.LONG
                && FullautoRapidcastSpellrifle.hasSilverRing(castingItem, player.level().registryAccess())) {
            castTime = spell.getEffectiveCastTime(Math.max(1, magicData.getCastingSpellLevel()), player);
        }
        event.setEffectiveCooldown(staffrifle.resolveSpecialCooldownTicks(spell.getSpellCooldown(), event.getEffectiveCooldown(), castTime));
        FullautoRapidcastSpellrifleCastContext.clearPendingIfMatches(player.getUUID(), castingItem, event.getSpell());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        FullautoRapidcastSpellrifleCastContext.clearExpiredPending(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FullautoRapidcastSpellrifleRateLimiter.clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FullautoRapidcastSpellrifleRateLimiter.clearAll();
    }
}
