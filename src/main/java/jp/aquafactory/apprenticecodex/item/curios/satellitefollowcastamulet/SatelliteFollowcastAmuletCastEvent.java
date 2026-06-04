package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSatelliteFollowcastAmuletStatePacket;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRequest;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastService;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SatelliteFollowcastAmuletCastEvent {
    public static final int CONTINUOUS_FOLLOWCAST_TICKS = 20 * 5;
    private static final CastSource FOLLOWCAST_SOURCE = CastSource.SWORD;
    private static final Map<ServerLevel, List<ContinuousFollowcastRuntime>> ACTIVE_CONTINUOUS_CASTS = new WeakHashMap<>();

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
            if (!canConsumeFollowcastManaAfterOriginal(player, ownerMagicData, spellData, reservedOriginalManaCost)) {
                continue;
            }

            try {
                var result = tryCastSelectedSpell(level, player, ownerMagicData, slotResult, spellData, slotIndex, maxSpellSlots, key);
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
        var manaAccess = new PlayerManaAccess(player);

        if (spell.getCastType() == CastType.CONTINUOUS) {
            return tryStartContinuousFollowcast(
                    level,
                    player,
                    slotResult.stack(),
                    spellData,
                    crystalPosition,
                    forward,
                    manaAccess,
                    castingSlot,
                    key
            );
        }

        if (canUseNonContinuousRemoteOwnerCast(ownerMagicData)) {
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
                    false
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
        }

        return CastAttemptResult.NONE;
    }

    private static boolean canUseNonContinuousRemoteOwnerCast(MagicData ownerMagicData) {
        // Iron's の SpellOnCastEvent は元の詠唱状態が残ったまま発火する。
        // 非継続 RemoteOwner 発動は所有者の MagicData を一時利用して reset するため、元の詠唱を壊さない場面だけ許可する。
        return !ownerMagicData.isCasting();
    }

    private static CastAttemptResult tryStartContinuousFollowcast(
            ServerLevel level,
            ServerPlayer player,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 crystalPosition,
            Vec3 forward,
            PlayerManaAccess manaAccess,
            String castingSlot,
            ContinuousFollowcastKey key
    ) {
        var spell = spellData.getSpell();
        var castDuration = Math.min(
                CONTINUOUS_FOLLOWCAST_TICKS,
                Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), player))
        );
        var remoteProfile = RemoteOwnerCastProfileManager.getUsableProfile(
                spell,
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
        );
        if (remoteProfile.isEmpty()) {
            return CastAttemptResult.NONE;
        }

        var remoteStartResult = RemoteOwnerCastRunner.tryStartContinuousCast(
                level,
                player,
                sourceStack,
                spellData,
                remoteProfile.get(),
                RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                crystalPosition,
                forward,
                FOLLOWCAST_SOURCE,
                castingSlot,
                castDuration,
                false
        );
        if (!remoteStartResult.handled() || !remoteStartResult.succeeded() || remoteStartResult.session() == null) {
            return CastAttemptResult.NONE;
        }

        ACTIVE_CONTINUOUS_CASTS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(
                        new ContinuousFollowcastRuntime(
                                key,
                                sourceStack.copy(),
                                remoteStartResult.session(),
                                level.getGameTime() + castDuration
                        )
                );
        syncContinuousState(player, key, true, level.getGameTime() + castDuration);
        return CastAttemptResult.CASTED;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        var runtimes = ACTIVE_CONTINUOUS_CASTS.get(level);
        if (runtimes == null || runtimes.isEmpty()) {
            return;
        }

        var iterator = runtimes.iterator();
        while (iterator.hasNext()) {
            var runtime = iterator.next();
            var owner = level.getPlayerByUUID(runtime.key().ownerId());
            if (!(owner instanceof ServerPlayer player) || player.isDeadOrDying() || player.isSpectator()) {
                finishContinuousFollowcast(level, runtime, null, true);
                iterator.remove();
                continue;
            }

            var slotResult = getEquippedAmuletForRuntime(player, runtime);
            if (slotResult.isEmpty()) {
                finishContinuousFollowcast(level, runtime, player, true);
                iterator.remove();
                continue;
            }

            var stack = slotResult.get().stack();
            var spellData = SatelliteFollowcastAmulet.getSpellAtIndex(stack, runtime.key().spellSlotIndex());
            var activeSpellData = runtime.session().spellData();
            if (spellData == SpellData.EMPTY
                    || activeSpellData == null
                    || !spellData.getSpell().getSpellId().equals(activeSpellData.getSpell().getSpellId())) {
                finishContinuousFollowcast(level, runtime, player, true);
                iterator.remove();
                continue;
            }

            var maxSpellSlots = SatelliteFollowcastAmulet.getMaxSpellSlots(stack);
            var crystalPosition = SatelliteFollowcastAmulet.getCrystalPosition(
                    player,
                    runtime.key().spellSlotIndex(),
                    maxSpellSlots,
                    0.0F
            );
            RemoteOwnerCastRunner.syncContinuousCastTransform(runtime.session(), crystalPosition, player.getLookAngle());

            if (level.getGameTime() >= runtime.finishAtGameTime() && !runtime.session().isFinished()) {
                RemoteOwnerCastRunner.finishContinuousCast(level, player, runtime.session(), false);
            } else if (RemoteOwnerCastRunner.tickContinuousCast(level, player, runtime.session())) {
                continue;
            }

            finishContinuousFollowcast(level, runtime, player, false);
            iterator.remove();
        }

        if (runtimes.isEmpty()) {
            ACTIVE_CONTINUOUS_CASTS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player, true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerState(player, true);
        }
    }

    private static int resolveReservedOriginalManaCost(SpellOnCastEvent event, ServerPlayer player) {
        if (!event.getCastSource().consumesMana() || (player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get())) {
            return 0;
        }
        return Math.max(0, event.getManaCost());
    }

    private static boolean canConsumeFollowcastManaAfterOriginal(
            ServerPlayer player,
            MagicData ownerMagicData,
            SpellData spellData,
            int reservedOriginalManaCost
    ) {
        if (player.isCreative()) {
            return true;
        }
        var followcastManaCost = Math.max(0, SpellDispenserManaHelper.getSpellManaCost(spellData));
        return ownerMagicData.getMana() >= reservedOriginalManaCost + followcastManaCost;
    }

    private static boolean isContinuousFollowcastActive(ServerLevel level, ContinuousFollowcastKey key) {
        var runtimes = ACTIVE_CONTINUOUS_CASTS.get(level);
        if (runtimes == null) {
            return false;
        }
        return runtimes.stream()
                .anyMatch(runtime -> runtime.key().equals(key) && !runtime.session().isFinished());
    }

    private static Optional<SlotResult> getEquippedAmuletForRuntime(ServerPlayer player, ContinuousFollowcastRuntime runtime) {
        return getEquippedAmulets(player).stream()
                .filter(slotResult -> runtime.key().matches(slotResult))
                .findFirst();
    }

    private static void finishContinuousFollowcast(
            ServerLevel level,
            ContinuousFollowcastRuntime runtime,
            @Nullable ServerPlayer owner,
            boolean cancelled
    ) {
        if (!runtime.session().isFinished()) {
            if (owner != null) {
                RemoteOwnerCastRunner.finishContinuousCast(level, owner, runtime.session(), cancelled);
            } else {
                RemoteOwnerCastRunner.cancelContinuousCastWithoutOwner(runtime.session());
            }
        }

        if (owner != null) {
            if (runtime.session().consumeFinishedCooldownTicks() > 0) {
                var spellData = runtime.session().spellData();
                var castSource = runtime.session().castSource();
                if (spellData == null || castSource == null) {
                    syncContinuousState(owner, runtime.key(), false, 0L);
                    return;
                }

                RemoteOwnerCooldownManager.addCooldown(
                        owner,
                        spellData,
                        castSource,
                        runtime.sourceStack(),
                        RemoteOwnerCooldownPolicy.FOLLOWCAST
                );
            }
            syncContinuousState(owner, runtime.key(), false, 0L);
        }
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
        clearPlayerState(player, true);
    }

    public static void clearPlayerStateForGameTest(ServerPlayer player, @Nullable ServerLevel ownerLevel) {
        clearPlayerState(player, true, ownerLevel);
    }

    private static void clearPlayerState(ServerPlayer player, boolean cancelled) {
        clearPlayerState(player, cancelled, player.serverLevel());
    }

    private static void clearPlayerState(ServerPlayer player, boolean cancelled, @Nullable ServerLevel ownerLevel) {
        RemoteOwnerCooldownManager.clearPending(player);
        var levelIterator = ACTIVE_CONTINUOUS_CASTS.entrySet().iterator();
        while (levelIterator.hasNext()) {
            var levelEntry = levelIterator.next();
            var level = levelEntry.getKey();
            var runtimes = levelEntry.getValue();
            var runtimeIterator = runtimes.iterator();
            while (runtimeIterator.hasNext()) {
                var runtime = runtimeIterator.next();
                if (!runtime.key().ownerId().equals(player.getUUID())) {
                    continue;
                }
                finishContinuousFollowcast(level, runtime, level == ownerLevel ? player : null, cancelled);
                runtimeIterator.remove();
            }
            if (runtimes.isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    private static final class PlayerManaAccess implements SpellDispenserManaHelper.ManaAccess {
        private final ServerPlayer player;

        private PlayerManaAccess(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int getCurrentMana() {
            return Mth.floor(MagicData.getPlayerMagicData(player).getMana());
        }

        @Override
        public void setCurrentMana(int mana) {
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(Math.max(0.0F, mana));
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }

        @Override
        public int getInventorySlotCount() {
            return 0;
        }

        @Override
        public @NotNull ItemStack getInventoryStack(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventoryStack(int slot, @NotNull ItemStack stack) {
        }

        @Override
        public boolean isManaConsumptionExempt() {
            return player.isCreative();
        }
    }

    private record ContinuousFollowcastKey(UUID ownerId, String slotIdentifier, int curiosSlotIndex, int spellSlotIndex) {
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
    }

    private record ContinuousFollowcastRuntime(
            ContinuousFollowcastKey key,
            ItemStack sourceStack,
            RemoteOwnerCastRunner.ContinuousCastSession session,
            long finishAtGameTime
    ) {
    }
}
