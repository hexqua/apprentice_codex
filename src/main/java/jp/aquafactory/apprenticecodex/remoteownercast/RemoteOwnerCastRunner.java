package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.spells.EntityCastData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RemoteOwnerCastRunner {
    private static final int CONTINUOUS_CAST_TICK_INTERVAL = 10;

    private RemoteOwnerCastRunner() {
    }

    public static CastResult tryCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return CastResult.notHandled();
        }

        var spell = spellData.getSpell();
        if (spell.getCastType() == CastType.CONTINUOUS && profile.castMode() != RemoteOwnerCastMode.LEGACY_SPELL_DISPENSER) {
            return CastResult.notHandled();
        }
        if (spell.getRecastCount(spellData.getLevel(), owner) > 0) {
            if (!profile.allowInitialRecast()) {
                return CastResult.failed();
            }
            var ownerMagicData = MagicData.getPlayerMagicData(owner);
            if (ownerMagicData == null || ownerMagicData.getPlayerRecasts().hasRecastForSpell(spell)) {
                return CastResult.failed();
            }
        }

        var origin = resolveOrigin(owner, profile, providedOrigin);
        var forward = resolveForward(owner, profile, providedForward);
        return switch (profile.castMode()) {
            case LEGACY_SPELL_DISPENSER -> tryLegacySpellDispenserCast(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    origin,
                    forward,
                    castSource,
                    castingSlot
            );
            case PLAYER_SELF -> tryOwnerMagicCast(
                    level,
                    owner,
                    owner,
                    null,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    owner.getEyePosition(),
                    owner.getLookAngle(),
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
            case PROXY_OWNER_MAGIC -> tryOwnerMagicCast(
                    level,
                    owner,
                    createProxy(level, owner, origin, forward),
                    null,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    origin,
                    forward,
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
            case REMOTE_ANCHOR_OWNER_MAGIC -> tryRemoteAnchorOwnerMagicCast(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    origin,
                    forward,
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
            case REMOTE_PLAYER_GEOMETRY -> tryOwnerMagicCast(
                    level,
                    owner,
                    owner,
                    null,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    origin,
                    forward,
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
        };
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            @Nullable Integer castDurationOverrideTicks,
            boolean postSpellPreCastEvent
    ) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return ContinuousCastStartResult.notHandled();
        }
        if (!supportsRemoteContinuous(profile)) {
            return ContinuousCastStartResult.notHandled();
        }

        var spell = spellData.getSpell();
        if (spell.getCastType() != CastType.CONTINUOUS) {
            return ContinuousCastStartResult.failed();
        }

        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        if (ownerMagicData == null) {
            return ContinuousCastStartResult.failed();
        }

        var manaAccess = new PlayerManaAccess(owner);
        if (!canOwnerCastWithManaAccess(owner, spellData, castSource, ownerMagicData, manaAccess)) {
            return ContinuousCastStartResult.failed();
        }
        if (postSpellPreCastEvent
                && MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(owner, spell.getSpellId(), spellData.getLevel(), spell.getSchoolType(), castSource))) {
            return ContinuousCastStartResult.failed();
        }

        var origin = resolveOrigin(owner, profile, providedOrigin);
        var forward = resolveForward(owner, profile, providedForward);
        var useRemoteGeometry = usesRemotePlayerGeometryContext(profile);
        var useRemotePosition = useRemoteGeometry || usesRemoteAnchorOwnerMagic(profile);
        var contextOrigin = useRemotePosition ? origin : owner.getEyePosition();
        var contextForward = useRemotePosition ? forward : owner.getLookAngle();
        var sessionMagicData = new MagicData();
        RemoteOwnerCastAnchorEntity anchor = null;
        try {
            if (usesRemoteAnchorOwnerMagic(profile)) {
                anchor = createRemoteOwnerAnchor(level, owner, origin, forward);
            }
            var spellCaster = anchor != null ? anchor : owner;
            sessionMagicData.setSyncedData(new SyncedSpellData(spellCaster));
            var castDuration = castDurationOverrideTicks != null
                    ? Math.max(0, castDurationOverrideTicks)
                    : Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), owner));
            sessionMagicData.initiateCast(spell, spellData.getLevel(), castDuration, castSource, castingSlot);
            sessionMagicData.setPlayerCastingItem(sourceStack.copy());
            syncOwnerManaForCast(manaAccess, sessionMagicData);

            try (var ignored = useRemoteGeometry
                    ? RemoteOwnerCastContext.push(owner, contextOrigin, contextForward, castOrigin)
                    : null) {
                if (!spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, sessionMagicData)) {
                    cleanupContinuousSession(anchor, sessionMagicData);
                    return ContinuousCastStartResult.failed();
                }
                syncOwnerManaForCast(manaAccess, sessionMagicData);
                spell.onServerPreCast(level, spellData.getLevel(), spellCaster, sessionMagicData);
            }

            if (!SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                cleanupContinuousSession(anchor, sessionMagicData);
                return ContinuousCastStartResult.failed();
            }
            syncOwnerManaForCast(manaAccess, sessionMagicData);

            return ContinuousCastStartResult.success(new ContinuousCastSession(
                    spellData,
                    sourceStack.copy(),
                    profile,
                    castOrigin,
                    contextOrigin,
                    contextForward,
                    castSource,
                    castingSlot,
                    sessionMagicData,
                    manaAccess,
                    anchor
            ));
        } catch (RuntimeException exception) {
            cleanupContinuousSession(anchor, sessionMagicData);
            ApprenticeCodex.LOGGER.warn(
                    "Remote Owner Continuous Cast start exception: spell={}, castMode={}, origin={}",
                    spell.getSpellResource(),
                    profile.castMode().getSerializedName(),
                    castOrigin.getSerializedName(),
                    exception
            );
            return ContinuousCastStartResult.failed();
        }
    }

    public static void syncContinuousCastTransform(ContinuousCastSession session, Vec3 origin, Vec3 forward) {
        if (session.isFinished()) {
            return;
        }
        session.setContext(origin, forward);
        if (session.anchor() != null) {
            session.anchor().syncFromRemoteGeometry(origin, forward);
        }
    }

    public static boolean tickContinuousCast(ServerLevel level, ServerPlayer owner, ContinuousCastSession session) {
        if (session.isFinished()) {
            return false;
        }

        var spellData = session.spellData();
        var spell = spellData.getSpell();
        var magicData = session.magicData();
        syncOwnerManaForCast(session.manaAccess(), magicData);
        magicData.handleCastDuration();

        if (magicData.getCastDurationRemaining() <= 0) {
            finishContinuousCast(level, owner, session, false);
            return false;
        }

        if ((magicData.getCastDurationRemaining() + 1) % CONTINUOUS_CAST_TICK_INTERVAL == 0) {
            if (!SpellDispenserManaHelper.tryConsumeSpellMana(session.manaAccess(), spellData)) {
                finishContinuousCast(level, owner, session, true);
                return false;
            }

            try {
                session.markReachedOnCast();
                syncAnchorCasterFromOwner(owner, session);
                var spellCaster = resolveContinuousSpellCaster(owner, session);
                runWithContinuousContext(owner, session,
                        () -> spell.onCast(level, spellData.getLevel(), spellCaster, session.castSource(), magicData));
                syncOwnerManaForCast(session.manaAccess(), magicData);
                bindAnchorIfNeeded(level, owner, session);
            } catch (RuntimeException exception) {
                ApprenticeCodex.LOGGER.warn(
                        "Remote Owner Continuous Cast exception during onCast: spell={}, origin={}",
                        spell.getSpellResource(),
                        session.castOrigin().getSerializedName(),
                        exception
                );
                finishContinuousCast(level, owner, session, true);
                return false;
            }

            if (magicData.getCastDurationRemaining() < CONTINUOUS_CAST_TICK_INTERVAL) {
                finishContinuousCast(level, owner, session, false);
                return false;
            }
        }

        try {
            syncAnchorCasterFromOwner(owner, session);
            var spellCaster = resolveContinuousSpellCaster(owner, session);
            runWithContinuousContext(owner, session,
                    () -> spell.onServerCastTick(level, spellData.getLevel(), spellCaster, magicData));
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Remote Owner Continuous Cast exception during tick: spell={}, origin={}",
                    spell.getSpellResource(),
                    session.castOrigin().getSerializedName(),
                    exception
            );
            finishContinuousCast(level, owner, session, true);
            return false;
        }

        return !session.isFinished();
    }

    public static void finishContinuousCast(ServerLevel level, ServerPlayer owner, ContinuousCastSession session, boolean cancelled) {
        if (session.isFinished()) {
            return;
        }

        session.markFinished(session.hasReachedOnCast() ? 1 : 0);
        var spellData = session.spellData();
        try {
            syncOwnerManaForCast(session.manaAccess(), session.magicData());
            syncAnchorCasterFromOwner(owner, session);
            var spellCaster = resolveContinuousSpellCaster(owner, session);
            runWithContinuousContext(owner, session,
                    () -> spellData.getSpell().onServerCastComplete(level, spellData.getLevel(), spellCaster, session.magicData(), cancelled));
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Remote Owner Continuous Cast exception during complete: spell={}, origin={}",
                    spellData.getSpell().getSpellResource(),
                    session.castOrigin().getSerializedName(),
                    exception
            );
        } finally {
            cleanupContinuousSession(session.anchor(), session.magicData());
        }
    }

    public static void cancelContinuousCastWithoutOwner(ContinuousCastSession session) {
        if (session.isFinished()) {
            return;
        }

        session.markFinished(0);
        cleanupContinuousSession(session.anchor(), session.magicData());
    }

    private static boolean supportsRemoteContinuous(RemoteOwnerCastProfile profile) {
        return profile.castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY
                || profile.castMode() == RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC
                || profile.castMode() == RemoteOwnerCastMode.PLAYER_SELF;
    }

    private static void runWithContinuousContext(ServerPlayer owner, ContinuousCastSession session, Runnable runnable) {
        var useRemoteGeometry = usesRemotePlayerGeometryContext(session.profile());
        try (var ignored = useRemoteGeometry
                ? RemoteOwnerCastContext.push(owner, session.contextOrigin(), session.contextForward(), session.castOrigin())
                : null) {
            runnable.run();
        }
    }

    private static void bindAnchorIfNeeded(ServerLevel level, ServerPlayer owner, ContinuousCastSession session) {
        var castData = session.magicData().getAdditionalCastData();
        if (!shouldUseRemoteAnchor(castData)) {
            return;
        }

        var anchor = session.anchor();
        if (anchor == null) {
            anchor = createRemoteOwnerAnchor(level, owner, session.contextOrigin(), session.contextForward());
            session.setAnchor(anchor);
        } else {
            syncRemoteOwnerAnchor(anchor, owner, session.contextOrigin(), session.contextForward());
        }

        if (castData instanceof EntityCastData entityCastData && entityCastData.getCastingEntity() instanceof Projectile projectile) {
            projectile.setOwner(anchor);
        }
    }

    private static LivingEntity resolveContinuousSpellCaster(ServerPlayer owner, ContinuousCastSession session) {
        if (usesRemoteAnchorOwnerMagic(session.profile()) && session.anchor() != null) {
            return session.anchor();
        }
        return owner;
    }

    private static boolean usesRemotePlayerGeometryContext(RemoteOwnerCastProfile profile) {
        return profile.castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY;
    }

    private static boolean usesRemoteAnchorOwnerMagic(RemoteOwnerCastProfile profile) {
        return profile.castMode() == RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC;
    }

    private static RemoteOwnerCastAnchorEntity createRemoteOwnerAnchor(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 origin,
            Vec3 forward
    ) {
        var anchor = new RemoteOwnerCastAnchorEntity(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), level);
        syncRemoteOwnerAnchor(anchor, owner, origin, forward);
        level.addFreshEntity(anchor);
        return anchor;
    }

    private static void syncRemoteOwnerAnchor(
            RemoteOwnerCastAnchorEntity anchor,
            ServerPlayer owner,
            Vec3 origin,
            Vec3 forward
    ) {
        anchor.bindOwnerName(owner);
        anchor.syncFromRemoteGeometry(origin, forward);
        syncAnchorAttributesFromOwner(owner, anchor);
    }

    private static void syncAnchorCasterFromOwner(ServerPlayer owner, ContinuousCastSession session) {
        if (usesRemoteAnchorOwnerMagic(session.profile()) && session.anchor() != null) {
            syncRemoteOwnerAnchor(session.anchor(), owner, session.contextOrigin(), session.contextForward());
        }
    }

    private static void syncAnchorAttributesFromOwner(ServerPlayer owner, RemoteOwnerCastAnchorEntity anchor) {
        RemoteOwnerCastAnchorAttributes.syncFromOwner(owner, anchor);
    }

    private static boolean shouldUseRemoteAnchor(@Nullable ICastData castData) {
        return castData instanceof EntityCastData entityCastData
                && entityCastData.getCastingEntity() instanceof AbstractConeProjectile;
    }

    private static void cleanupContinuousSession(
            @Nullable RemoteOwnerCastAnchorEntity anchor,
            @Nullable MagicData magicData
    ) {
        if (magicData != null) {
            magicData.resetCastingState();
        }
        if (anchor != null && !anchor.isRemoved()) {
            anchor.discard();
        }
    }

    private static CastResult tryLegacySpellDispenserCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 origin,
            Vec3 forward,
            CastSource castSource,
            String castingSlot
    ) {
        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var result = SpellDispenserCastHelper.tryCast(
                level,
                origin,
                forward,
                validation,
                sourceStack,
                owner.getGameProfile(),
                new PlayerManaAccess(owner),
                castSource,
                castingSlot
        );
        return result.succeeded() ? CastResult.success() : CastResult.failed();
    }

    private static CastResult tryRemoteAnchorOwnerMagicCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 contextOrigin,
            Vec3 contextForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        var anchor = createRemoteOwnerAnchor(level, owner, contextOrigin, contextForward);
        return tryOwnerMagicCast(
                level,
                owner,
                anchor,
                anchor,
                sourceStack,
                spellData,
                profile,
                castOrigin,
                contextOrigin,
                contextForward,
                castSource,
                castingSlot,
                postSpellPreCastEvent
        );
    }

    private static CastResult tryOwnerMagicCast(
            ServerLevel level,
            ServerPlayer owner,
            LivingEntity spellCaster,
            @Nullable RemoteOwnerCastAnchorEntity spellCasterAnchor,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 contextOrigin,
            Vec3 contextForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        var spell = spellData.getSpell();
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        if (ownerMagicData == null) {
            discardAnchorIfUnused(spellCasterAnchor);
            return CastResult.failed();
        }

        var manaAccess = new PlayerManaAccess(owner);
        if (!canOwnerCastWithManaAccess(owner, spellData, castSource, ownerMagicData, manaAccess)) {
            discardAnchorIfUnused(spellCasterAnchor);
            return CastResult.failed();
        }
        if (postSpellPreCastEvent
                && MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(owner, spell.getSpellId(), spellData.getLevel(), spell.getSchoolType(), castSource))) {
            discardAnchorIfUnused(spellCasterAnchor);
            return CastResult.failed();
        }

        var originalMana = ownerMagicData.getMana();
        var restoreManaAfterCast = manaAccess.isManaConsumptionExempt();
        var originalSyncedData = ownerMagicData.getSyncedData();
        var retainAnchor = false;
        try {
            ownerMagicData.setSyncedData(new SyncedSpellData(spellCaster));
            ownerMagicData.initiateCast(spell, spellData.getLevel(), 0, castSource, castingSlot);
            ownerMagicData.setPlayerCastingItem(sourceStack.copy());
            syncOwnerManaForCast(manaAccess, ownerMagicData);

            var useRemoteGeometry = profile.castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY;
            try (var ignored = useRemoteGeometry
                    ? RemoteOwnerCastContext.push(owner, contextOrigin, contextForward, castOrigin)
                    : null) {
                if (!spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, ownerMagicData)) {
                    return CastResult.failed();
                }
                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onServerPreCast(level, spellData.getLevel(), spellCaster, ownerMagicData);

                if (!SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                    return CastResult.failed();
                }

                if (spell.getCastType() == CastType.LONG) {
                    spellCaster.tickCount++;
                    syncOwnerManaForCast(manaAccess, ownerMagicData);
                    spell.onServerCastTick(level, spellData.getLevel(), spellCaster, ownerMagicData);
                }

                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onCast(level, spellData.getLevel(), spellCaster, castSource, ownerMagicData);
                retainAnchor = retainAnchorForSummonWeapon(level, ownerMagicData.getAdditionalCastData(), spellCasterAnchor);
                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, ownerMagicData, false);
                if (!retainAnchor) {
                    retainAnchor = retainAnchorForSummonWeapon(level, ownerMagicData.getAdditionalCastData(), spellCasterAnchor);
                }
            }
            return CastResult.success();
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Remote Owner Cast exception: spell={}, castMode={}, origin={}",
                    spell.getSpellResource(),
                    profile.castMode().getSerializedName(),
                    castOrigin.getSerializedName(),
                    exception
            );
            return CastResult.failed();
        } finally {
            try {
                ownerMagicData.resetCastingState();
            } finally {
                if (restoreManaAfterCast) {
                    ownerMagicData.setMana(originalMana);
                }
                ownerMagicData.setSyncedData(originalSyncedData);
                originalSyncedData.syncToPlayer(owner);
                if (!retainAnchor && spellCasterAnchor != null && !spellCasterAnchor.isRemoved()) {
                    spellCasterAnchor.discard();
                }
            }
        }
    }

    private static boolean retainAnchorForSummonWeapon(
            ServerLevel level,
            @Nullable ICastData castData,
            @Nullable RemoteOwnerCastAnchorEntity anchor
    ) {
        if (anchor == null || !(castData instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData summonCastData)) {
            return false;
        }

        var summon = summonCastData.getEntity(level);
        if (!(summon instanceof TraceableEntity traceable) || traceable.getOwner() != anchor) {
            return false;
        }

        anchor.retainWhileOwnerOf(summon);
        return true;
    }

    private static void discardAnchorIfUnused(@Nullable RemoteOwnerCastAnchorEntity anchor) {
        if (anchor != null && !anchor.isRemoved()) {
            anchor.discard();
        }
    }

    private static boolean canOwnerCastWithManaAccess(
            ServerPlayer owner,
            SpellData spellData,
            CastSource castSource,
            MagicData ownerMagicData,
            PlayerManaAccess manaAccess
    ) {
        var originalMana = ownerMagicData.getMana();
        try {
            syncOwnerManaForCast(manaAccess, ownerMagicData);
            return spellData.getSpell().canBeCastedBy(spellData.getLevel(), castSource, ownerMagicData, owner).isSuccess();
        } finally {
            if (manaAccess.isManaConsumptionExempt()) {
                ownerMagicData.setMana(originalMana);
            }
        }
    }

    private static net.minecraftforge.common.util.FakePlayer createProxy(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 eyePosition,
            Vec3 forward
    ) {
        var proxy = FakePlayerFactory.get(level, new GameProfile(owner.getUUID(), owner.getGameProfile().getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : owner.getLookAngle();
        var rotation = RemoteOwnerCastGeometry.rotationFromForward(normalizedForward);
        var yaw = rotation.yaw();
        var pitch = rotation.pitch();
        var feetY = eyePosition.y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(eyePosition.x, feetY, eyePosition.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.setXRot(pitch);
        proxy.xRotO = pitch;
        return proxy;
    }

    private static Vec3 resolveOrigin(ServerPlayer owner, RemoteOwnerCastProfile profile, Vec3 providedOrigin) {
        return profile.originMode() == RemoteOwnerOriginMode.PLAYER_SELF ? owner.getEyePosition() : providedOrigin;
    }

    private static Vec3 resolveForward(ServerPlayer owner, RemoteOwnerCastProfile profile, Vec3 providedForward) {
        return profile.directionMode() == RemoteOwnerDirectionMode.PLAYER_LOOK ? owner.getLookAngle() : providedForward;
    }

    private static void syncOwnerManaForCast(PlayerManaAccess manaAccess, MagicData magicData) {
        magicData.setMana(manaAccess.isManaConsumptionExempt()
                ? SpellDispenserManaHelper.MAX_MANA
                : manaAccess.getCurrentMana());
    }

    public record CastResult(boolean handled, boolean succeeded) {
        private static CastResult success() {
            return new CastResult(true, true);
        }

        private static CastResult failed() {
            return new CastResult(true, false);
        }

        private static CastResult notHandled() {
            return new CastResult(false, false);
        }
    }

    public record ContinuousCastStartResult(
            boolean handled,
            boolean succeeded,
            @Nullable ContinuousCastSession session
    ) {
        private static ContinuousCastStartResult success(ContinuousCastSession session) {
            return new ContinuousCastStartResult(true, true, session);
        }

        private static ContinuousCastStartResult failed() {
            return new ContinuousCastStartResult(true, false, null);
        }

        private static ContinuousCastStartResult notHandled() {
            return new ContinuousCastStartResult(false, false, null);
        }
    }

    public static final class ContinuousCastSession {
        private final SpellData spellData;
        private final ItemStack sourceStack;
        private final RemoteOwnerCastProfile profile;
        private final RemoteOwnerCastOrigin castOrigin;
        private Vec3 contextOrigin;
        private Vec3 contextForward;
        private final CastSource castSource;
        private final String castingSlot;
        private final MagicData magicData;
        private final PlayerManaAccess manaAccess;
        @Nullable
        private RemoteOwnerCastAnchorEntity anchor;
        private boolean finished;
        private boolean reachedOnCast;
        private int finishedCooldownTicks;

        private ContinuousCastSession(
                SpellData spellData,
                ItemStack sourceStack,
                RemoteOwnerCastProfile profile,
                RemoteOwnerCastOrigin castOrigin,
                Vec3 contextOrigin,
                Vec3 contextForward,
                CastSource castSource,
                String castingSlot,
                MagicData magicData,
                PlayerManaAccess manaAccess,
                @Nullable RemoteOwnerCastAnchorEntity anchor
        ) {
            this.spellData = spellData;
            this.sourceStack = sourceStack;
            this.profile = profile;
            this.castOrigin = castOrigin;
            this.contextOrigin = contextOrigin;
            this.contextForward = contextForward;
            this.castSource = castSource;
            this.castingSlot = castingSlot;
            this.magicData = magicData;
            this.manaAccess = manaAccess;
            this.anchor = anchor;
        }

        public SpellData spellData() {
            return spellData;
        }

        public ItemStack sourceStack() {
            return sourceStack;
        }

        public RemoteOwnerCastProfile profile() {
            return profile;
        }

        public RemoteOwnerCastOrigin castOrigin() {
            return castOrigin;
        }

        public Vec3 contextOrigin() {
            return contextOrigin;
        }

        public Vec3 contextForward() {
            return contextForward;
        }

        public CastSource castSource() {
            return castSource;
        }

        public String castingSlot() {
            return castingSlot;
        }

        public MagicData magicData() {
            return magicData;
        }

        private PlayerManaAccess manaAccess() {
            return manaAccess;
        }

        public @Nullable RemoteOwnerCastAnchorEntity anchor() {
            return anchor;
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean hasReachedOnCast() {
            return reachedOnCast;
        }

        public int consumeFinishedCooldownTicks() {
            var cooldown = finishedCooldownTicks;
            finishedCooldownTicks = 0;
            return cooldown;
        }

        private void setContext(Vec3 contextOrigin, Vec3 contextForward) {
            this.contextOrigin = contextOrigin;
            this.contextForward = contextForward;
        }

        private void setAnchor(@Nullable RemoteOwnerCastAnchorEntity anchor) {
            this.anchor = anchor;
        }

        private void markReachedOnCast() {
            reachedOnCast = true;
        }

        private void markFinished(int cooldownTicks) {
            finished = true;
            finishedCooldownTicks = Math.max(0, cooldownTicks);
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
}
