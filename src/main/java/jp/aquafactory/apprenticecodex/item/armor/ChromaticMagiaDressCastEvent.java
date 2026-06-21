package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChromaticMagiaDressCastEvent {
    private static final Map<UUID, ContinuousCastSessionKey> RECORDED_CONTINUOUS_CASTS = new HashMap<>();

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

        var castType = spell.getCastType();
        for (var armorStack : player.getArmorSlots()) {
            if (!(armorStack.getItem() instanceof ChromaticMagiaDressItem dressItem)) {
                continue;
            }
            if (shouldRecord(dressItem.getArmorType(), castType, spell.getRecastCount(event.getSpellLevel(), player))
                    && shouldRecordContinuousCast(player, magicData, event, castType)) {
                ChromaticMagiaDressHistory.append(armorStack, event.getSchoolType());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (!isActiveContinuousCast(magicData)) {
            RECORDED_CONTINUOUS_CASTS.remove(player.getUUID());
            return;
        }

        var activeKey = ContinuousCastSessionKey.from(player, magicData);
        var previousKey = RECORDED_CONTINUOUS_CASTS.get(player.getUUID());
        if (previousKey != null && !previousKey.equals(activeKey)) {
            RECORDED_CONTINUOUS_CASTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RECORDED_CONTINUOUS_CASTS.remove(event.getEntity().getUUID());
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

    private static boolean shouldRecordContinuousCast(
            ServerPlayer player,
            MagicData magicData,
            SpellOnCastEvent event,
            CastType castType
    ) {
        if (castType != CastType.CONTINUOUS || !matchesActiveContinuousCast(magicData, event)) {
            return true;
        }

        var key = ContinuousCastSessionKey.from(player, magicData);
        var previousKey = RECORDED_CONTINUOUS_CASTS.get(player.getUUID());
        if (key.equals(previousKey)) {
            return false;
        }

        // Iron's 1.20.1 Forge は CONTINUOUS の効果 tick ごとに SpellOnCastEvent を出すため、同じ詠唱中は初回だけ記録する。
        RECORDED_CONTINUOUS_CASTS.put(player.getUUID(), key);
        return true;
    }

    private static boolean matchesActiveContinuousCast(MagicData magicData, SpellOnCastEvent event) {
        return isActiveContinuousCast(magicData)
                && magicData.getCastingSpellId().equals(event.getSpellId())
                && magicData.getCastingSpellLevel() == event.getSpellLevel()
                && magicData.getCastSource() == event.getCastSource();
    }

    private static boolean isActiveContinuousCast(MagicData magicData) {
        return magicData != null && magicData.isCasting() && magicData.getCastType() == CastType.CONTINUOUS;
    }

    private record ContinuousCastSessionKey(
            UUID playerId,
            String spellId,
            int spellLevel,
            CastSource castSource,
            String castingEquipmentSlot
    ) {
        static ContinuousCastSessionKey from(ServerPlayer player, MagicData magicData) {
            return new ContinuousCastSessionKey(
                    player.getUUID(),
                    magicData.getCastingSpellId(),
                    magicData.getCastingSpellLevel(),
                    magicData.getCastSource(),
                    magicData.getCastingEquipmentSlot()
            );
        }
    }
}
