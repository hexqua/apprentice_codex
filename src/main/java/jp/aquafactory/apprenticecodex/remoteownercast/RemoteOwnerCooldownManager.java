package jp.aquafactory.apprenticecodex.remoteownercast;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RemoteOwnerCooldownManager {
    private static final Map<UUID, PendingRemoteOwnerCooldown> PENDING_COOLDOWNS = new HashMap<>();

    private RemoteOwnerCooldownManager() {
    }

    public static void addCooldown(
            ServerPlayer owner,
            SpellData spellData,
            CastSource castSource,
            RemoteOwnerCooldownPolicy policy
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(spellData, "spellData");
        Objects.requireNonNull(castSource, "castSource");
        Objects.requireNonNull(policy, "policy");
        if (spellData == SpellData.EMPTY) {
            return;
        }

        var spell = spellData.getSpell();
        if (policy.skipRecastCooldown() && spell.getRecastCount(spellData.getLevel(), owner) > 0) {
            return;
        }

        PENDING_COOLDOWNS.put(owner.getUUID(), new PendingRemoteOwnerCooldown(
                spell.getSpellId(),
                castSource,
                resolveExtraCooldownTicks(owner, spellData, policy)
        ));
        try {
            MagicHelper.MAGIC_MANAGER.addCooldown(owner, spell, castSource);
        } finally {
            PENDING_COOLDOWNS.remove(owner.getUUID());
        }
    }

    public static void clearPending(ServerPlayer owner) {
        Objects.requireNonNull(owner, "owner");
        PENDING_COOLDOWNS.remove(owner.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCooldown = PENDING_COOLDOWNS.get(player.getUUID());
        if (pendingCooldown == null
                || !pendingCooldown.spellId().equals(event.getSpell().getSpellId())
                || pendingCooldown.castSource() != event.getCastSource()) {
            return;
        }

        event.setEffectiveCooldown(WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource()
        ) + pendingCooldown.extraCooldownTicks());
    }

    private static int resolveExtraCooldownTicks(
            ServerPlayer owner,
            SpellData spellData,
            RemoteOwnerCooldownPolicy policy
    ) {
        if (!policy.addLongCastExtension() || spellData.getSpell().getCastType() != CastType.LONG) {
            return 0;
        }

        var spell = spellData.getSpell();
        var spellLevel = policy.useResolvedSpellLevelForLongCastExtension()
                ? spell.getLevelFor(spellData.getLevel(), owner)
                : spellData.getLevel();
        return Math.max(0, spell.getEffectiveCastTime(spellLevel, owner));
    }

    private record PendingRemoteOwnerCooldown(String spellId, CastSource castSource, int extraCooldownTicks) {
    }
}
