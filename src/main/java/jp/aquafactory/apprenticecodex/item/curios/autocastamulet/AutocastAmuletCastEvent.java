package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AutocastAmuletCastEvent {
    private static final Map<UUID, PendingCreativeCooldown> PENDING_CREATIVE_COOLDOWNS = new HashMap<>();

    private AutocastAmuletCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof AutocastAmulet)) {
            return;
        }

        var adjustedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                castingItem
        );
        event.setEffectiveCooldown(adjustedCooldown);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isCreative()
                || ServerConfigs.CREATIVE_COOLDOWN.get()
                || event.getCastSource() != CastSource.SWORD) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof AutocastAmulet)) {
            return;
        }

        PENDING_CREATIVE_COOLDOWNS.put(player.getUUID(), new PendingCreativeCooldown(event.getSpellId(), event.getCastSource()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCooldown = PENDING_CREATIVE_COOLDOWNS.get(player.getUUID());
        if (pendingCooldown == null) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            PENDING_CREATIVE_COOLDOWNS.remove(player.getUUID());
            return;
        }
        if (magicData.isCasting()) {
            return;
        }

        var spell = SpellRegistry.getSpell(pendingCooldown.spellId());
        if (spell == null || spell == SpellRegistry.none()) {
            PENDING_CREATIVE_COOLDOWNS.remove(player.getUUID());
            return;
        }
        if (magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId())) {
            PENDING_CREATIVE_COOLDOWNS.remove(player.getUUID());
            return;
        }
        if (magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            PENDING_CREATIVE_COOLDOWNS.remove(player.getUUID());
            return;
        }

        MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, pendingCooldown.castSource());
        PENDING_CREATIVE_COOLDOWNS.remove(player.getUUID());
    }

    private record PendingCreativeCooldown(String spellId, CastSource castSource) {
    }
}
