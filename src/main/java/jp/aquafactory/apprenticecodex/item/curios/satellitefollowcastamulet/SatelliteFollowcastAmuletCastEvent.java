package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSatelliteFollowcastAmuletStatePacket;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SatelliteFollowcastAmuletCastEvent {
    public static final int CONTINUOUS_FOLLOWCAST_TICKS = 20 * 5;
    private static final CastSource FOLLOWCAST_SOURCE = CastSource.SWORD;
    private static final Map<UUID, PendingFollowcastCooldown> PENDING_FOLLOWCAST_COOLDOWNS = new HashMap<>();
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

        if (canUseNonContinuousRemoteOwnerCast(ownerMagicData)
                && ApprenticeCodexServerConfig.satelliteFollowcastUsesRemoteOwnerProfiles()) {
            var remoteProfile = RemoteOwnerCastProfileManager.getUsableProfile(
                    spell,
                    RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
            );
            if (remoteProfile.isPresent()) {
                var result = RemoteOwnerCastRunner.tryCast(
                        level,
                        player,
                        slotResult.stack(),
                        spellData,
                        remoteProfile.get(),
                        RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                        crystalPosition,
                        forward,
                        FOLLOWCAST_SOURCE,
                        castingSlot,
                        false
                );
                if (result.handled()) {
                    if (!result.succeeded()) {
                        return CastAttemptResult.NONE;
                    }
                    addFollowcastCooldown(player, spellData, FOLLOWCAST_SOURCE, slotResult.stack());
                    return CastAttemptResult.CASTED;
                }
            }
        }

        if (SpellDispenserSpellProfileManager.getProfile(spell).isEmpty()
                || !tryCastWithSpellDispenserProfile(level, player, slotResult.stack(), spellData, crystalPosition, forward, manaAccess, castingSlot)) {
            return CastAttemptResult.NONE;
        }

        addFollowcastCooldown(player, spellData, FOLLOWCAST_SOURCE, slotResult.stack());
        return CastAttemptResult.CASTED;
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
        if (ApprenticeCodexServerConfig.satelliteFollowcastUsesRemoteOwnerProfiles()) {
            var remoteProfile = RemoteOwnerCastProfileManager.getUsableProfile(
                    spell,
                    RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
            );
            if (remoteProfile.isPresent()) {
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
                if (remoteStartResult.handled()) {
                    if (!remoteStartResult.succeeded() || remoteStartResult.session() == null) {
                        return CastAttemptResult.NONE;
                    }

                    ACTIVE_CONTINUOUS_CASTS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(
                            new ContinuousFollowcastRuntime(
                                    key,
                                    sourceStack.copy(),
                                    ActiveContinuousCastSession.remote(remoteStartResult.session()),
                                    level.getGameTime() + castDuration
                            )
                    );
                    syncContinuousState(player, key, true, level.getGameTime() + castDuration);
                    return CastAttemptResult.CASTED;
                }
            }
        }

        if (SpellDispenserSpellProfileManager.getProfile(spell).isEmpty()) {
            return CastAttemptResult.NONE;
        }

        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                level,
                crystalPosition,
                forward,
                validation,
                sourceStack,
                player.getGameProfile(),
                manaAccess,
                FOLLOWCAST_SOURCE,
                castingSlot,
                castDuration
        );
        if (!startResult.result().succeeded() || startResult.session() == null) {
            return CastAttemptResult.NONE;
        }

        ACTIVE_CONTINUOUS_CASTS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(
                new ContinuousFollowcastRuntime(
                        key,
                        sourceStack.copy(),
                        ActiveContinuousCastSession.spellDispenser(startResult.session()),
                        level.getGameTime() + castDuration
                )
        );
        syncContinuousState(player, key, true, level.getGameTime() + castDuration);
        return CastAttemptResult.CASTED;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
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
            runtime.session().syncTransform(crystalPosition, player.getLookAngle());

            if (level.getGameTime() >= runtime.finishAtGameTime() && !runtime.session().isFinished()) {
                runtime.session().finish(level, player, false);
            } else if (runtime.session().tick(level, player)) {
                continue;
            }

            finishContinuousFollowcast(level, runtime, player, false);
            iterator.remove();
        }

        if (runtimes.isEmpty()) {
            ACTIVE_CONTINUOUS_CASTS.remove(level);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCooldown = PENDING_FOLLOWCAST_COOLDOWNS.get(player.getUUID());
        if (pendingCooldown == null
                || !pendingCooldown.spellId().equals(event.getSpell().getSpellId())
                || pendingCooldown.castSource() != event.getCastSource()) {
            return;
        }

        event.setEffectiveCooldown(WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                pendingCooldown.castingStack()
        ) + pendingCooldown.extraCooldownTicks());
    }

    private static void addFollowcastCooldown(ServerPlayer player, SpellData spellData, CastSource castSource, ItemStack castingStack) {
        var spell = spellData.getSpell();
        PENDING_FOLLOWCAST_COOLDOWNS.put(player.getUUID(), new PendingFollowcastCooldown(
                spell.getSpellId(),
                castSource,
                castingStack.copy(),
                resolveLongCastCooldownExtensionTicks(player, spellData)
        ));
        try {
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, castSource);
        } finally {
            PENDING_FOLLOWCAST_COOLDOWNS.remove(player.getUUID());
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

    private static int resolveLongCastCooldownExtensionTicks(ServerPlayer player, SpellData spellData) {
        var spell = spellData.getSpell();
        if (spell.getCastType() != CastType.LONG) {
            return 0;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        return Math.max(0, spell.getEffectiveCastTime(spellLevel, player));
    }

    private static boolean tryCastWithSpellDispenserProfile(
            ServerLevel level,
            ServerPlayer player,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 crystalPosition,
            Vec3 forward,
            PlayerManaAccess manaAccess,
            String castingSlot
    ) {
        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var result = SpellDispenserCastHelper.tryCast(
                level,
                crystalPosition,
                forward,
                validation,
                sourceStack,
                player.getGameProfile(),
                manaAccess,
                FOLLOWCAST_SOURCE,
                castingSlot
        );
        return result.succeeded();
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
            runtime.session().finish(level, owner, cancelled);
        }

        if (owner != null) {
            if (runtime.session().consumeFinishedCooldownTicks() > 0) {
                var spellData = runtime.session().spellData();
                var castSource = runtime.session().castSource();
                if (spellData == null || castSource == null) {
                    syncContinuousState(owner, runtime.key(), false, 0L);
                    return;
                }

                addFollowcastCooldown(
                        owner,
                        spellData,
                        castSource,
                        runtime.sourceStack()
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
        PENDING_FOLLOWCAST_COOLDOWNS.remove(player.getUUID());
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

    private record PendingFollowcastCooldown(String spellId, CastSource castSource, ItemStack castingStack, int extraCooldownTicks) {
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
            ActiveContinuousCastSession session,
            long finishAtGameTime
    ) {
    }

    private record ActiveContinuousCastSession(
            @Nullable SpellDispenserCastHelper.ContinuousCastSession spellDispenser,
            @Nullable RemoteOwnerCastRunner.ContinuousCastSession remoteOwner
    ) {
        private static ActiveContinuousCastSession spellDispenser(SpellDispenserCastHelper.ContinuousCastSession session) {
            return new ActiveContinuousCastSession(session, null);
        }

        private static ActiveContinuousCastSession remote(RemoteOwnerCastRunner.ContinuousCastSession session) {
            return new ActiveContinuousCastSession(null, session);
        }

        private void syncTransform(Vec3 position, Vec3 forward) {
            if (remoteOwner != null) {
                RemoteOwnerCastRunner.syncContinuousCastTransform(remoteOwner, position, forward);
            } else if (spellDispenser != null) {
                SpellDispenserCastHelper.syncContinuousCastTransform(spellDispenser, position, forward);
            }
        }

        private boolean tick(ServerLevel level, ServerPlayer owner) {
            if (remoteOwner != null) {
                return RemoteOwnerCastRunner.tickContinuousCast(level, owner, remoteOwner);
            }
            return spellDispenser != null && SpellDispenserCastHelper.tickContinuousCast(level, spellDispenser);
        }

        private void finish(ServerLevel level, @Nullable ServerPlayer owner, boolean cancelled) {
            if (remoteOwner != null) {
                if (owner != null) {
                    RemoteOwnerCastRunner.finishContinuousCast(level, owner, remoteOwner, cancelled);
                } else {
                    RemoteOwnerCastRunner.cancelContinuousCastWithoutOwner(remoteOwner);
                }
            } else if (spellDispenser != null) {
                SpellDispenserCastHelper.finishContinuousCast(level, spellDispenser, cancelled);
            }
        }

        private boolean isFinished() {
            if (remoteOwner != null) {
                return remoteOwner.isFinished();
            }
            return spellDispenser == null || spellDispenser.isFinished();
        }

        private int consumeFinishedCooldownTicks() {
            if (remoteOwner != null) {
                return remoteOwner.consumeFinishedCooldownTicks();
            }
            return spellDispenser == null ? 0 : spellDispenser.consumeFinishedCooldownTicks();
        }

        private @Nullable SpellData spellData() {
            if (remoteOwner != null) {
                return remoteOwner.spellData();
            }
            return spellDispenser == null ? null : spellDispenser.validation().spellData();
        }

        private @Nullable CastSource castSource() {
            if (remoteOwner != null) {
                return remoteOwner.castSource();
            }
            return spellDispenser == null ? null : spellDispenser.castSource();
        }
    }
}
