package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.spells.EntityCastData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserCastHelper {
    private static final double FAILURE_NOTICE_RADIUS = 16.0D;
    private static final int CONTINUOUS_CAST_TICK_INTERVAL = 10;
    private static final double DEFAULT_FORWARD_OFFSET = 0.7D;

    private SpellDispenserCastHelper() {
    }

    public static CastResult tryCast(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryCast(level, Vec3.atCenterOf(pos), Vec3.atLowerCornerOf(facing.getNormal()), spellSource, ownerProfile, null);
    }

    public static CastResult tryCast(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        return tryCast(level, Vec3.atCenterOf(pos), Vec3.atLowerCornerOf(facing.getNormal()), spellSource, ownerProfile, manaAccess);
    }

    public static CastResult tryCast(
            ServerLevel level,
            BlockPos pos,
            Vec3 forward,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryCast(level, Vec3.atCenterOf(pos), forward, spellSource, ownerProfile, null);
    }

    public static CastResult tryCast(
            ServerLevel level,
            BlockPos pos,
            Vec3 forward,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        return tryCast(level, Vec3.atCenterOf(pos), forward, spellSource, ownerProfile, manaAccess);
    }

    public static CastResult tryCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryCast(level, castBasePosition, forward, spellSource, ownerProfile, null);
    }

    public static CastResult tryCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        var failurePos = BlockPos.containing(castBasePosition);
        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (!validation.isSupported()) {
            return CastResult.validationFailure(validation);
        }
        if (!isValidOwnerProfile(ownerProfile)) {
            return CastResult.missingOwnerProfile(validation);
        }

        var spellData = validation.spellData();
        var spell = spellData.getSpell();
        var spellId = spell.getSpellResource();
        var profile = SpellDispenserSpellProfileManager.getResolvedProfile(spell);
        var proxy = createProxy(level, castBasePosition, forward, ownerProfile, profile);
        var trackedAnchor = createTrackedAnchorForExplicitProfile(level, proxy, profile, spell.getCastType());
        var spellCaster = resolveSpellCaster(proxy, trackedAnchor);

        var magicData = MagicData.getPlayerMagicData(proxy);
        var cooldownTicks = resolveCooldownTicks(spellData, proxy);
        CastResult result;
        try {
            try {
                // Iron's の initiateCast は getSyncedData を経由せず syncedSpellData を直接参照する。
                // Spell Dispenser の FakePlayer は通常の player tick / sync 経路に乗せないため、ここで同期データを先に用意する。
                magicData.setSyncedData(new SyncedSpellData(proxy));
                // LONG を含め詠唱時間 0 扱いで検証したいため、Spellgun 系と同様に override 値 0 を入れる。
                magicData.initiateCast(spell, spellData.getLevel(), 0, CastSource.COMMAND, SpellSelectionManager.MAINHAND);
                // 短命な proxy にも casting item を持たせ、scroll 起点の通常経路に近い条件で検証する。
                magicData.setPlayerCastingItem(spellSource.copy());
                syncProxyMana(manaAccess, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.INITIATE_CAST, exception, false, 0);
                notifyFailureToNearbyPlayers(level, failurePos, result);
                return result;
            }

            final boolean canCast;
            try {
                syncProxyMana(manaAccess, magicData);
                canCast = spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.CHECK_PRE_CAST, exception, false, 0);
                notifyFailureToNearbyPlayers(level, failurePos, result);
                return result;
            }
            if (!canCast) {
                return CastResult.preCastRejected(validation, spellId);
            }

            try {
                syncProxyMana(manaAccess, magicData);
                spell.onServerPreCast(level, spellData.getLevel(), spellCaster, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_PRE_CAST, exception, false, 0);
                notifyFailureToNearbyPlayers(level, failurePos, result);
                return result;
            }

            if (manaAccess != null && !SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                return CastResult.insufficientMana(validation);
            }

            try {
                // 空撃ち音を suppress する条件は「発射処理へ入れたか」で判断する。
                // そのため onCast 本体へ到達した時点からは失敗でも reachedOnCast=true として扱う。
                syncProxyMana(manaAccess, magicData);
                spell.onCast(level, spellData.getLevel(), spellCaster, CastSource.COMMAND, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.CAST, exception, true, cooldownTicks);
                notifyFailureToNearbyPlayers(level, failurePos, result);
                return result;
            }

            try {
                syncProxyMana(manaAccess, magicData);
                spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, magicData, false);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_CAST_COMPLETE, exception, true, cooldownTicks);
                notifyFailureToNearbyPlayers(level, failurePos, result);
                return result;
            }

            result = CastResult.success(validation, spellId, cooldownTicks);
            return result;
        } finally {
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
        }
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryStartContinuousCast(level, Vec3.atCenterOf(pos), Vec3.atLowerCornerOf(facing.getNormal()), validation, spellSource, ownerProfile, null);
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        return tryStartContinuousCast(level, Vec3.atCenterOf(pos), Vec3.atLowerCornerOf(facing.getNormal()), validation, spellSource, ownerProfile, manaAccess);
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            BlockPos pos,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryStartContinuousCast(level, Vec3.atCenterOf(pos), forward, validation, spellSource, ownerProfile, null);
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            BlockPos pos,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        return tryStartContinuousCast(level, Vec3.atCenterOf(pos), forward, validation, spellSource, ownerProfile, manaAccess);
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
        return tryStartContinuousCast(level, castBasePosition, forward, validation, spellSource, ownerProfile, null);
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
    ) {
        var failurePos = BlockPos.containing(castBasePosition);
        if (!validation.isSupported()) {
            return new ContinuousCastStartResult(CastResult.validationFailure(validation), null);
        }
        if (!isValidOwnerProfile(ownerProfile)) {
            return new ContinuousCastStartResult(CastResult.missingOwnerProfile(validation), null);
        }

        var spellData = validation.spellData();
        var spell = spellData.getSpell();
        if (spell.getCastType() != CastType.CONTINUOUS) {
            return new ContinuousCastStartResult(CastResult.validationFailure(validation), null);
        }

        var spellId = spell.getSpellResource();
        var profile = SpellDispenserSpellProfileManager.getResolvedProfile(spell);
        var proxy = createProxy(level, castBasePosition, forward, ownerProfile, profile);
        var trackedAnchor = createTrackedAnchorForExplicitProfile(level, proxy, profile, spell.getCastType());
        var spellCaster = resolveSpellCaster(proxy, trackedAnchor);
        var magicData = MagicData.getPlayerMagicData(proxy);
        var cooldownTicks = resolveCooldownTicks(spellData, proxy);
        try {
            magicData.setSyncedData(new SyncedSpellData(proxy));
            var castDuration = Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), proxy));
            magicData.initiateCast(spell, spellData.getLevel(), castDuration, CastSource.COMMAND, SpellSelectionManager.MAINHAND);
            magicData.setPlayerCastingItem(spellSource.copy());
            syncProxyMana(manaAccess, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.INITIATE_CAST, exception, false, 0);
            notifyFailureToNearbyPlayers(level, failurePos, result);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }

        final boolean canCast;
        try {
            syncProxyMana(manaAccess, magicData);
            canCast = spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.CHECK_PRE_CAST, exception, false, 0);
            notifyFailureToNearbyPlayers(level, failurePos, result);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }
        if (!canCast) {
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(CastResult.preCastRejected(validation, spellId), null);
        }

        try {
            syncProxyMana(manaAccess, magicData);
            spell.onServerPreCast(level, spellData.getLevel(), spellCaster, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_PRE_CAST, exception, false, 0);
            notifyFailureToNearbyPlayers(level, failurePos, result);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }

        if (manaAccess != null && !SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(CastResult.insufficientMana(validation), null);
        }

        return new ContinuousCastStartResult(
                CastResult.success(validation, spellId, 0),
                new ContinuousCastSession(
                        BlockPos.containing(castBasePosition),
                        validation,
                        spellSource.copy(),
                        profile,
                        proxy,
                        magicData,
                        trackedAnchor,
                        cooldownTicks,
                        manaAccess
                )
        );
    }

    public static void syncContinuousCastTransform(ContinuousCastSession session, Vec3 castBasePosition, Vec3 forward) {
        if (session.isFinished()) {
            return;
        }

        var castTransform = resolveCastTransform(castBasePosition, forward, session.profile());
        moveCaster(session.proxy(), castTransform);
        syncTrackedAnchor(session);
    }

    public static boolean tickContinuousCast(ServerLevel level, ContinuousCastSession session) {
        if (session.isFinished()) {
            return false;
        }

        var spellData = session.validation().spellData();
        var spell = spellData.getSpell();
        var proxy = session.proxy();
        var spellCaster = session.spellCaster();
        var magicData = session.magicData();
        syncProxyMana(session.manaAccess(), magicData);

        proxy.tickCount++;
        syncTrackedAnchor(session);
        magicData.handleCastDuration();

        if (magicData.getCastDurationRemaining() <= 0) {
            // Iron's 本体では cast time が極端に短い連続魔法が固着し得るため、
            // Dispenser 側では上限到達時に必ず止めて RS 再入力待ちへ戻す。
            finishContinuousCast(level, session, false);
            return false;
        }

        if ((magicData.getCastDurationRemaining() + 1) % CONTINUOUS_CAST_TICK_INTERVAL == 0) {
            if (session.manaAccess() != null && !SpellDispenserManaHelper.tryConsumeSpellMana(session.manaAccess(), spellData)) {
                finishContinuousCast(level, session, true);
                return false;
            }

            session.markReachedOnCast();
            try {
                syncProxyMana(session.manaAccess(), magicData);
                spell.onCast(level, spellData.getLevel(), spellCaster, CastSource.COMMAND, magicData);
            } catch (RuntimeException exception) {
                var result = CastResult.exceptionFailure(
                        session.validation(),
                        session.spellId(),
                        CastStage.CAST,
                        exception,
                        true,
                        session.cooldownTicks()
                );
                notifyFailureToNearbyPlayers(level, session.origin(), result);
                finishContinuousCast(level, session, true);
                return false;
            }

            bindTrackedAnchorIfNeeded(level, session);
            if (magicData.getCastDurationRemaining() < CONTINUOUS_CAST_TICK_INTERVAL) {
                finishContinuousCast(level, session, false);
                return false;
            }
        }

        try {
            syncProxyMana(session.manaAccess(), magicData);
            spell.onServerCastTick(level, spellData.getLevel(), spellCaster, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(session.validation(), session.spellId(), CastStage.SERVER_CAST_TICK, exception, session.hasReachedOnCast(), session.hasReachedOnCast() ? session.cooldownTicks() : 0);
            notifyFailureToNearbyPlayers(level, session.origin(), result);
            finishContinuousCast(level, session, true);
            return false;
        }

        return !session.isFinished();
    }

    private static void syncProxyMana(@Nullable SpellDispenserManaHelper.ManaAccess manaAccess, MagicData magicData) {
        if (manaAccess != null) {
            magicData.setMana(manaAccess.getCurrentMana());
        }
    }

    public static void finishContinuousCast(ServerLevel level, ContinuousCastSession session, boolean cancelled) {
        if (session.isFinished()) {
            return;
        }

        session.markFinished(session.hasReachedOnCast() ? session.cooldownTicks() : 0);
        var spellData = session.validation().spellData();
        try {
            syncProxyMana(session.manaAccess(), session.magicData());
            spellData.getSpell().onServerCastComplete(level, spellData.getLevel(), session.spellCaster(), session.magicData(), cancelled);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(
                    session.validation(),
                    session.spellId(),
                    CastStage.SERVER_CAST_COMPLETE,
                    exception,
                    session.hasReachedOnCast(),
                    session.hasReachedOnCast() ? session.cooldownTicks() : 0
            );
            notifyFailureToNearbyPlayers(level, session.origin(), result);
        } finally {
            cleanupProxy(session.spellId(), session.magicData(), session.proxy(), session.trackedAnchor());
        }
    }

    private static void cleanupProxy(
            @Nullable ResourceLocation spellId,
            @Nullable MagicData magicData,
            FakePlayer proxy,
            @Nullable SpellDispenserAnchorEntity trackedAnchor
    ) {
        try {
            if (magicData != null) {
                // FakePlayer 自体は world に参加させないが、MagicData は capability 側に残るため毎回明示的に初期化する。
                magicData.resetCastingState();
            }
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn("Spell Dispenser cleanup failed during reset: {}", spellId, exception);
        }

        if (trackedAnchor != null && !trackedAnchor.isRemoved()) {
            trackedAnchor.discard();
        }
    }

    private static void notifyFailureToNearbyPlayers(ServerLevel level, BlockPos pos, CastResult result) {
        var center = Vec3.atCenterOf(pos);
        var radiusSqr = FAILURE_NOTICE_RADIUS * FAILURE_NOTICE_RADIUS;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                var message = result.createExceptionMessage(player);
                if (message != null) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }

    private static FakePlayer createProxy(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            GameProfile ownerProfile,
            SpellDispenserSpellProfile profile
    ) {
        var castTransform = resolveCastTransform(castBasePosition, forward, profile);
        // Spectral Hammer のように後続 tick で Player を要求する spell があるため、
        // Spell Dispenser は設置者 profile を持つ FakePlayer を caster として扱う。
        var proxy = new FakePlayer(level, new GameProfile(ownerProfile.getId(), ownerProfile.getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        moveCaster(proxy, castTransform);
        return proxy;
    }

    private static void moveCaster(FakePlayer proxy, CastTransform castTransform) {
        var feetY = castTransform.origin().y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(castTransform.origin().x, feetY, castTransform.origin().z, castTransform.yaw(), castTransform.pitch());
        proxy.setYBodyRot(castTransform.yaw());
        proxy.setYHeadRot(castTransform.yaw());
        proxy.yBodyRotO = castTransform.yaw();
        proxy.yHeadRotO = castTransform.yaw();
        proxy.setXRot(castTransform.pitch());
        proxy.xRotO = castTransform.pitch();
    }

    private static CastTransform resolveCastTransform(Vec3 castBasePosition, Vec3 forward, SpellDispenserSpellProfile profile) {
        var normalizedForward = normalizeForward(forward);
        var yaw = resolveYaw(normalizedForward) + profile.yawOffset();
        var pitch = Mth.clamp(resolvePitch(normalizedForward) + profile.pitchOffset(), -90.0F, 90.0F);
        return new CastTransform(resolveCastOrigin(castBasePosition, normalizedForward, profile), yaw, pitch);
    }

    private static Vec3 resolveCastOrigin(Vec3 castBasePosition, Vec3 forward, SpellDispenserSpellProfile profile) {
        var side = resolveSideVector(forward);
        return castBasePosition
                .add(forward.scale(DEFAULT_FORWARD_OFFSET + profile.forwardOffset()))
                .add(side.scale(profile.sideOffset()))
                .add(0.0D, profile.upOffset(), 0.0D);
    }

    private static Vec3 resolveSideVector(Vec3 forward) {
        var referenceUp = Math.abs(forward.y) > 0.9D ? new Vec3(0.0D, 0.0D, 1.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        var side = forward.cross(referenceUp);
        if (side.lengthSqr() < 1.0E-6D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return side.normalize();
    }

    private static void syncTrackedAnchor(ContinuousCastSession session) {
        if (session.trackedAnchor() != null) {
            session.trackedAnchor().syncFromCaster(session.proxy());
        }
    }

    private static void bindTrackedAnchorIfNeeded(ServerLevel level, ContinuousCastSession session) {
        var castData = session.magicData().getAdditionalCastData();
        if (!shouldUseTrackedAnchor(session.profile(), castData)) {
            return;
        }

        var trackedAnchor = session.trackedAnchor();
        if (trackedAnchor == null) {
            trackedAnchor = createTrackedAnchor(level, session.proxy());
            session.setTrackedAnchor(trackedAnchor);
        }
        trackedAnchor.syncFromCaster(session.proxy());

        if (castData instanceof EntityCastData entityCastData && entityCastData.getCastingEntity() instanceof Projectile projectile) {
            // 円錐ブレス系は owner を client でも追跡できないと見た目と当たり判定更新が崩れる。
            // proxy FakePlayer は world 未参加なので、該当 spell だけ tracked anchor へ owner を差し替える。
            projectile.setOwner(trackedAnchor);
        }
    }

    private static boolean shouldUseTrackedAnchor(SpellDispenserSpellProfile profile, @Nullable ICastData castData) {
        return switch (profile.castAnchor()) {
            case FAKE_PLAYER -> false;
            case TRACKED_ANCHOR -> false;
            case AUTO -> castData instanceof EntityCastData entityCastData
                    && entityCastData.getCastingEntity() instanceof AbstractConeProjectile;
        };
    }

    private static @Nullable SpellDispenserAnchorEntity createTrackedAnchorForExplicitProfile(
            ServerLevel level,
            FakePlayer proxy,
            SpellDispenserSpellProfile profile,
            CastType castType
    ) {
        if (profile.castAnchor() != SpellDispenserCastAnchorMode.TRACKED_ANCHOR || castType != CastType.CONTINUOUS) {
            return null;
        }
        return createTrackedAnchor(level, proxy);
    }

    private static SpellDispenserAnchorEntity createTrackedAnchor(ServerLevel level, FakePlayer proxy) {
        var trackedAnchor = new SpellDispenserAnchorEntity(EntityRegistry.SPELL_DISPENSER_ANCHOR.get(), level);
        trackedAnchor.syncFromCaster(proxy);
        level.addFreshEntity(trackedAnchor);
        return trackedAnchor;
    }

    private static LivingEntity resolveSpellCaster(FakePlayer proxy, @Nullable SpellDispenserAnchorEntity trackedAnchor) {
        return trackedAnchor != null ? trackedAnchor : proxy;
    }

    private static boolean isValidOwnerProfile(@Nullable GameProfile ownerProfile) {
        return ownerProfile != null
                && ownerProfile.getId() != null
                && ownerProfile.getName() != null
                && !ownerProfile.getName().isBlank();
    }

    private static int resolveCooldownTicks(SpellData spellData, @Nullable LivingEntity spellCaster) {
        if (spellData == SpellData.EMPTY) {
            return 0;
        }

        var spell = spellData.getSpell();
        var cooldownTicks = Math.max(0, spell.getSpellCooldown());
        if (spell.getCastType() == CastType.LONG) {
            cooldownTicks += Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), spellCaster));
        }
        return cooldownTicks;
    }

    private static Vec3 normalizeForward(Vec3 forward) {
        if (forward.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }

    private static float resolveYaw(Vec3 forward) {
        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    private static float resolvePitch(Vec3 forward) {
        return (float) -Math.toDegrees(Math.asin(Mth.clamp(forward.y, -1.0D, 1.0D)));
    }

    private record CastTransform(Vec3 origin, float yaw, float pitch) {
    }

    public enum CastStage {
        INITIATE_CAST("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.initiate_cast"),
        CHECK_PRE_CAST("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.check_pre_cast"),
        SERVER_PRE_CAST("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.server_pre_cast"),
        CAST("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.cast"),
        SERVER_CAST_TICK("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.server_cast_tick"),
        SERVER_CAST_COMPLETE("chat." + ApprenticeCodex.MODID + ".spell_dispenser.stage.server_cast_complete");

        private final String translationKey;

        CastStage(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component createMessage() {
            return Component.translatable(translationKey);
        }
    }

    public record CastResult(
            boolean succeeded,
            SpellDispenserSpellValidator.ValidationResult validation,
            @Nullable ResourceLocation spellId,
            @Nullable CastStage failedStage,
            @Nullable RuntimeException exception,
            boolean reachedOnCast,
            int cooldownTicks,
            boolean missingOwnerProfile,
            boolean insufficientMana
    ) {
        private static CastResult success(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                int cooldownTicks
        ) {
            return new CastResult(true, validation, spellId, null, null, true, cooldownTicks, false, false);
        }

        public static CastResult validationFailure(SpellDispenserSpellValidator.ValidationResult validation) {
            return cooldownBlocked(validation);
        }

        public static CastResult cooldownBlocked(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, null, null, false, 0, false, false);
        }

        private static CastResult preCastRejected(SpellDispenserSpellValidator.ValidationResult validation, ResourceLocation spellId) {
            return new CastResult(false, validation, spellId, null, null, false, 0, false, false);
        }

        public static CastResult missingOwnerProfile(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, null, null, false, 0, true, false);
        }

        public static CastResult insufficientMana(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, null, null, false, 0, false, true);
        }

        private static CastResult exceptionFailure(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                CastStage failedStage,
                RuntimeException exception,
                boolean reachedOnCast,
                int cooldownTicks
        ) {
            ApprenticeCodex.LOGGER.warn("Spell Dispenser cast failed at {}: {}", failedStage.name(), spellId, exception);
            return new CastResult(false, validation, spellId, failedStage, exception, reachedOnCast, cooldownTicks, false, false);
        }

        public @Nullable Component createExceptionMessage(net.minecraft.server.level.ServerPlayer player) {
            if (failedStage == null || exception == null) {
                return null;
            }

            var spellName = validation.spellData() == SpellData.EMPTY
                    ? Component.literal(spellId == null ? "unknown" : spellId.toString())
                    : validation.spellData().getSpell().getDisplayName(player);
            var exceptionType = Component.literal(exception.getClass().getSimpleName());
            var detailText = exception.getMessage();
            var detail = detailText == null || detailText.isBlank()
                    ? Component.translatable("chat." + ApprenticeCodex.MODID + ".spell_dispenser.no_exception_detail")
                    : Component.literal(detailText);
            return Component.translatable(
                    "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed",
                    spellName,
                    failedStage.createMessage(),
                    exceptionType,
                    detail
            ).withStyle(ChatFormatting.RED);
        }
    }

    public record ContinuousCastStartResult(
            CastResult result,
            @Nullable ContinuousCastSession session
    ) {
    }

    public static final class ContinuousCastSession {
        private final ResourceLocation spellId;
        private final BlockPos origin;
        private final SpellDispenserSpellValidator.ValidationResult validation;
        private final ItemStack spellSource;
        private final SpellDispenserSpellProfile profile;
        private final FakePlayer proxy;
        private final MagicData magicData;
        private final int cooldownTicks;
        @Nullable
        private final SpellDispenserManaHelper.ManaAccess manaAccess;
        @Nullable
        private SpellDispenserAnchorEntity trackedAnchor;
        private boolean finished;
        private boolean reachedOnCast;
        private int finishedCooldownTicks;

        private ContinuousCastSession(
                BlockPos origin,
                SpellDispenserSpellValidator.ValidationResult validation,
                ItemStack spellSource,
                SpellDispenserSpellProfile profile,
                FakePlayer proxy,
                MagicData magicData,
                @Nullable SpellDispenserAnchorEntity trackedAnchor,
                int cooldownTicks,
                @Nullable SpellDispenserManaHelper.ManaAccess manaAccess
        ) {
            this.spellId = validation.spellData().getSpell().getSpellResource();
            this.origin = origin.immutable();
            this.validation = validation;
            this.spellSource = spellSource;
            this.profile = profile;
            this.proxy = proxy;
            this.magicData = magicData;
            this.trackedAnchor = trackedAnchor;
            this.cooldownTicks = Math.max(0, cooldownTicks);
            this.manaAccess = manaAccess;
            this.finished = false;
            this.reachedOnCast = false;
            this.finishedCooldownTicks = 0;
        }

        public @NotNull ResourceLocation spellId() {
            return spellId;
        }

        public @NotNull BlockPos origin() {
            return origin;
        }

        public SpellDispenserSpellValidator.ValidationResult validation() {
            return validation;
        }

        public ItemStack spellSource() {
            return spellSource;
        }

        public SpellDispenserSpellProfile profile() {
            return profile;
        }

        public FakePlayer proxy() {
            return proxy;
        }

        public MagicData magicData() {
            return magicData;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public @Nullable SpellDispenserManaHelper.ManaAccess manaAccess() {
            return manaAccess;
        }

        public @Nullable SpellDispenserAnchorEntity trackedAnchor() {
            return trackedAnchor;
        }

        public LivingEntity spellCaster() {
            return resolveSpellCaster(proxy, trackedAnchor);
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

        private void setTrackedAnchor(@Nullable SpellDispenserAnchorEntity trackedAnchor) {
            this.trackedAnchor = trackedAnchor;
        }

        private void markReachedOnCast() {
            reachedOnCast = true;
        }

        private void markFinished(int cooldownTicks) {
            finished = true;
            finishedCooldownTicks = Math.max(0, cooldownTicks);
        }
    }
}
