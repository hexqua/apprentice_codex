package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSatelliteFollowcastAmuletStatePacket;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRequest;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastService;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownPolicy;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerContinuousCastManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerContinuousRuntime;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerManaPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SatelliteFollowcastAmuletCastEvent {
    public static final int CONTINUOUS_FOLLOWCAST_TICKS = 20 * 5;
    private static final CastSource FOLLOWCAST_SOURCE = CastSource.SWORD;

    private SatelliteFollowcastAmuletCastEvent() {
    }

    private enum CastAttemptResult {
        NONE,
        CASTED
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var reservedOriginalManaCost = resolveReservedOriginalManaCost(event, player);
        var equippedAmulets = getEquippedAmulets(player);
        for (var slotResult : equippedAmulets) {
            var stack = slotResult.stack();
            if (!(stack.getItem() instanceof SatelliteFollowcastAmulet amulet)) {
                continue;
            }

            amulet.initializeSpellContainer(stack);
            amulet.normalizeImbuedSpellContainer(stack);

            var result = tryFollowcast(level, player, magicData, slotResult, amulet, reservedOriginalManaCost);
            if (result == CastAttemptResult.NONE) {
                continue;
            }
            return;
        }
    }

    private static CastAttemptResult tryFollowcast(
            ServerLevel level,
            ServerPlayer player,
            MagicData ownerMagicData,
            SlotResult slotResult,
            SatelliteFollowcastAmulet amulet,
            int reservedOriginalManaCost
    ) {
        var stack = slotResult.stack();
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return CastAttemptResult.NONE;
        }

        var maxSpellSlots = SatelliteFollowcastAmulet.clampSpellSlotCount(spellContainer.getMaxSpellCount());
        var startIndex = SatelliteFollowcastAmulet.advanceAndGetSearchStartIndex(stack, maxSpellSlots);
        for (var offset = 0; offset < maxSpellSlots; ++offset) {
            var slotIndex = (startIndex + offset) % maxSpellSlots;
            var spellData = spellContainer.getSpellAtIndex(slotIndex);
            if (spellData == SpellData.EMPTY || !amulet.canImbueSpell(spellData)) {
                continue;
            }
            if (ApprenticeCodexServerConfig.isSatelliteFollowcastAmuletSpellDenied(spellData.getSpell().getSpellResource())) {
                continue;
            }

            var key = ContinuousFollowcastKey.from(player, slotResult, slotIndex);
            if (isContinuousFollowcastActive(level, key)) {
                continue;
            }

            var spell = spellData.getSpell();
            if (ownerMagicData.getPlayerCooldowns().isOnCooldown(spell)) {
                continue;
            }

            try {
                var result = tryCastSelectedSpell(
                        level,
                        player,
                        ownerMagicData,
                        slotResult,
                        spellData,
                        slotIndex,
                        maxSpellSlots,
                        reservedOriginalManaCost,
                        key
                );
                if (result == CastAttemptResult.CASTED) {
                    return result;
                }
            } catch (RuntimeException exception) {
                ApprenticeCodex.LOGGER.warn(
                        "Failed to cast satellite followcast spell {} for player {} from {}[{}]/{}",
                        spell.getSpellId(),
                        player.getGameProfile().getName(),
                        slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(),
                        slotIndex,
                        exception
                );
            }
        }

        return CastAttemptResult.NONE;
    }

    private static CastAttemptResult tryCastSelectedSpell(
            ServerLevel level,
            ServerPlayer player,
            MagicData ownerMagicData,
            SlotResult slotResult,
            SpellData spellData,
            int slotIndex,
            int maxSpellSlots,
            int reservedOriginalManaCost,
            ContinuousFollowcastKey key
    ) {
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        if (spell.requiresLearning() && !spell.isLearned(player)) {
            return CastAttemptResult.NONE;
        }
        if (!spell.canBeCastedBy(spellLevel, FOLLOWCAST_SOURCE, ownerMagicData, player).isSuccess()) {
            return CastAttemptResult.NONE;
        }

        var castingSlot = "satellite_followcast_amulet_" + slotResult.slotContext().identifier()
                + "_" + slotResult.slotContext().index()
                + "_" + slotIndex;
        var crystalPosition = SatelliteFollowcastAmulet.getCrystalPosition(player, slotIndex, maxSpellSlots, 0.0F);
        var forward = player.getLookAngle();

        if (spell.getCastType() == CastType.CONTINUOUS) {
            return tryStartContinuousFollowcast(
                    level,
                    player,
                    slotResult.stack(),
                    spellData,
                    crystalPosition,
                    forward,
                    castingSlot,
                    reservedOriginalManaCost,
                    key
            );
        }

        var result = RemoteOwnerCastService.cast(new RemoteOwnerCastRequest(
                level,
                player,
                slotResult.stack(),
                spellData,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                crystalPosition,
                forward,
                FOLLOWCAST_SOURCE,
                castingSlot,
                false,
                RemoteOwnerManaPolicy.RESERVE_OWNER_MANA,
                reservedOriginalManaCost
        ));
        if (result.handled()) {
            if (!result.succeeded()) {
                return CastAttemptResult.NONE;
            }
            RemoteOwnerCooldownManager.addCooldown(
                    player,
                    spellData,
                    FOLLOWCAST_SOURCE,
                    slotResult.stack(),
                    RemoteOwnerCooldownPolicy.FOLLOWCAST
            );
            return CastAttemptResult.CASTED;
        }

        return CastAttemptResult.NONE;
    }

    private static CastAttemptResult tryStartContinuousFollowcast(
            ServerLevel level,
            ServerPlayer player,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 crystalPosition,
            Vec3 forward,
            String castingSlot,
            int reservedOriginalManaCost,
            ContinuousFollowcastKey key
    ) {
        var spell = spellData.getSpell();
        var castDuration = Math.min(
                CONTINUOUS_FOLLOWCAST_TICKS,
                Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), player))
        );

        var result = RemoteOwnerCastService.startContinuous(new RemoteOwnerCastRequest(
                level,
                player,
                sourceStack,
                spellData,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                crystalPosition,
                forward,
                FOLLOWCAST_SOURCE,
                castingSlot,
                false,
                RemoteOwnerManaPolicy.RESERVE_OWNER_MANA,
                reservedOriginalManaCost,
                castDuration
        ));
        if (!result.handled() || !result.succeeded() || result.continuousSession() == null) {
            return CastAttemptResult.NONE;
        }

        RemoteOwnerContinuousCastManager.register(level, new RemoteOwnerContinuousRuntime(
                key.ownerId(),
                key.toRuntimeKey(),
                sourceStack,
                result.continuousSession(),
                level.getGameTime() + castDuration,
                RemoteOwnerCooldownPolicy.FOLLOWCAST,
                (tickLevel, tickOwner, session) -> prepareContinuousFollowcastTick(tickOwner, session, key),
                (finishedLevel, finishedOwner, cancelled) -> syncContinuousState(finishedOwner, key, false, 0L)
        ));
        syncContinuousState(player, key, true, level.getGameTime() + castDuration);
        return CastAttemptResult.CASTED;
    }

    private static int resolveReservedOriginalManaCost(SpellOnCastEvent event, ServerPlayer player) {
        if (!event.getCastSource().consumesMana() || (player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get())) {
            return 0;
        }
        return Math.max(0, event.getManaCost());
    }

    private static boolean isContinuousFollowcastActive(ServerLevel level, ContinuousFollowcastKey key) {
        return RemoteOwnerContinuousCastManager.hasActive(level, key.ownerId(), key.toRuntimeKey());
    }

    private static Optional<SlotResult> getEquippedAmuletForRuntime(ServerPlayer player, ContinuousFollowcastKey key) {
        return getEquippedAmulets(player).stream()
                .filter(key::matches)
                .findFirst();
    }

    private static boolean prepareContinuousFollowcastTick(
            ServerPlayer player,
            RemoteOwnerCastRunner.ContinuousCastSession session,
            ContinuousFollowcastKey key
    ) {
        var slotResult = getEquippedAmuletForRuntime(player, key);
        if (slotResult.isEmpty()) {
            return false;
        }

        var stack = slotResult.get().stack();
        var spellData = SatelliteFollowcastAmulet.getSpellAtIndex(stack, key.spellSlotIndex());
        var activeSpellData = session.spellData();
        if (spellData == SpellData.EMPTY
                || activeSpellData == null
                || !spellData.getSpell().getSpellId().equals(activeSpellData.getSpell().getSpellId())) {
            return false;
        }

        var maxSpellSlots = SatelliteFollowcastAmulet.getMaxSpellSlots(stack);
        var crystalPosition = SatelliteFollowcastAmulet.getCrystalPosition(
                player,
                key.spellSlotIndex(),
                maxSpellSlots,
                0.0F
        );
        RemoteOwnerCastRunner.syncContinuousCastTransform(session, crystalPosition, player.getLookAngle());
        return true;
    }

    private static void syncContinuousState(
            ServerPlayer player,
            ContinuousFollowcastKey key,
            boolean active,
            long activeUntilGameTime
    ) {
        Networks.sendToTrackingEntityAndSelf(player, new SyncSatelliteFollowcastAmuletStatePacket(
                player.getId(),
                key.slotIdentifier(),
                key.curiosSlotIndex(),
                key.spellSlotIndex(),
                active,
                activeUntilGameTime
        ));
    }

    private static List<SlotResult> getEquippedAmulets(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof SatelliteFollowcastAmulet).stream()
                        .sorted(Comparator
                                .comparing((SlotResult slotResult) -> slotResult.slotContext().identifier())
                                .thenComparingInt(slotResult -> slotResult.slotContext().index()))
                        .toList())
                .orElse(List.of());
    }

    public static boolean hasActiveContinuousFollowcastForGameTest(
            ServerLevel level,
            ServerPlayer player,
            String slotIdentifier,
            int curiosSlotIndex,
            int spellSlotIndex
    ) {
        return isContinuousFollowcastActive(
                level,
                new ContinuousFollowcastKey(player.getUUID(), slotIdentifier, curiosSlotIndex, spellSlotIndex)
        );
    }

    public static void clearPlayerStateForGameTest(ServerPlayer player) {
        RemoteOwnerContinuousCastManager.clearOwner(player, true);
    }

    public static void clearPlayerStateForGameTest(ServerPlayer player, @Nullable ServerLevel ownerLevel) {
        RemoteOwnerContinuousCastManager.clearOwner(player, true, ownerLevel);
    }

    private record ContinuousFollowcastKey(UUID ownerId, String slotIdentifier, int curiosSlotIndex, int spellSlotIndex) {
        private static final String SEPARATOR = "\u0000";

        private static ContinuousFollowcastKey from(ServerPlayer player, SlotResult slotResult, int spellSlotIndex) {
            return new ContinuousFollowcastKey(
                    player.getUUID(),
                    slotResult.slotContext().identifier(),
                    slotResult.slotContext().index(),
                    spellSlotIndex
            );
        }

        private boolean matches(SlotResult slotResult) {
            return slotIdentifier.equals(slotResult.slotContext().identifier())
                    && curiosSlotIndex == slotResult.slotContext().index();
        }

        private String toRuntimeKey() {
            return slotIdentifier + SEPARATOR + curiosSlotIndex + SEPARATOR + spellSlotIndex;
        }
    }
}
