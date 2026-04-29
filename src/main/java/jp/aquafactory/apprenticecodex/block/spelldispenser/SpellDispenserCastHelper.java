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
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpellDispenserCastHelper {
    private static final double FAILURE_NOTICE_RANGE = 8.0D;
    private static final long FAILURE_NOTICE_COOLDOWN_TICKS = 40L;
    private static final int CONTINUOUS_CAST_TICK_INTERVAL = 10;
    private static final double DEFAULT_FORWARD_OFFSET = 0.7D;
    private static final UUID OWNER_OPTIONAL_FALLBACK_UUID =
            UUID.nameUUIDFromBytes("apprenticecodex:spell_dispenser_owner_optional".getBytes(StandardCharsets.UTF_8));
    private static final String OWNER_OPTIONAL_FALLBACK_NAME = "[SpellDispenser]";

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
        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (!validation.isSupported()) {
            return CastResult.validationFailure(validation);
        }

        return tryCast(
                level,
                castBasePosition,
                forward,
                validation,
                spellSource,
                ownerProfile,
                manaAccess,
                CastSource.COMMAND,
                SpellSelectionManager.MAINHAND
        );
    }

    public static CastResult tryCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess,
            CastSource castSource,
            String castingSlot
    ) {
        var failurePos = BlockPos.containing(castBasePosition);
        if (!validation.isSupported()) {
            return CastResult.validationFailure(validation);
        }

        var spellData = validation.spellData();
        var spell = spellData.getSpell();
        var spellId = spell.getSpellResource();
        var profile = SpellDispenserSpellProfileManager.getResolvedProfile(spell);
        var casterProfile = resolveCasterProfile(ownerProfile, profile);
        if (casterProfile == null) {
            return CastResult.missingOwnerProfile(validation);
        }
        var proxy = createProxy(level, castBasePosition, forward, casterProfile, profile);
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
                magicData.initiateCast(spell, spellData.getLevel(), 0, castSource, castingSlot);
                // 短命な proxy にも casting item を持たせ、scroll 起点の通常経路に近い条件で検証する。
                magicData.setPlayerCastingItem(spellSource.copy());
                syncProxyMana(manaAccess, magicData);
            } catch (RuntimeException exception) {
                result = exceptionFailure(level, failurePos, validation, spellId, CastStage.INITIATE_CAST, exception, false, 0, ownerProfile);
                return result;
            }

            final boolean canCast;
            try {
                syncProxyMana(manaAccess, magicData);
                canCast = spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, magicData);
            } catch (RuntimeException exception) {
                result = exceptionFailure(level, failurePos, validation, spellId, CastStage.CHECK_PRE_CAST, exception, false, 0, ownerProfile);
                return result;
            }
            if (!canCast) {
                return CastResult.preCastRejected(validation, spellId, proxy.consumeActionBarMessage());
            }

            try {
                syncProxyMana(manaAccess, magicData);
                spell.onServerPreCast(level, spellData.getLevel(), spellCaster, magicData);
            } catch (RuntimeException exception) {
                result = exceptionFailure(level, failurePos, validation, spellId, CastStage.SERVER_PRE_CAST, exception, false, 0, ownerProfile);
                return result;
            }

            if (manaAccess != null && !SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                return CastResult.insufficientMana(validation, spell.getManaCost(spellData.getLevel()), manaAccess.getCurrentMana());
            }

            if (spell.getCastType() == CastType.LONG) {
                proxy.tickCount++;
                try {
                    syncProxyMana(manaAccess, magicData);
                    spell.onServerCastTick(level, spellData.getLevel(), spellCaster, magicData);
                } catch (RuntimeException exception) {
                    result = exceptionFailure(level, failurePos, validation, spellId, CastStage.SERVER_CAST_TICK, exception, false, 0, ownerProfile);
                    return result;
                }
            }

            try {
                // 空撃ち音を suppress する条件は「発射処理へ入れたか」で判断する。
                // そのため onCast 本体へ到達した時点からは失敗でも reachedOnCast=true として扱う。
                syncProxyMana(manaAccess, magicData);
                spell.onCast(level, spellData.getLevel(), spellCaster, castSource, magicData);
            } catch (RuntimeException exception) {
                result = exceptionFailure(level, failurePos, validation, spellId, CastStage.CAST, exception, true, cooldownTicks, ownerProfile);
                return result;
            }

            try {
                syncProxyMana(manaAccess, magicData);
                spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, magicData, false);
            } catch (RuntimeException exception) {
                result = exceptionFailure(level, failurePos, validation, spellId, CastStage.SERVER_CAST_COMPLETE, exception, true, cooldownTicks, ownerProfile);
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
        return tryStartContinuousCast(
                level,
                castBasePosition,
                forward,
                validation,
                spellSource,
                ownerProfile,
                manaAccess,
                CastSource.COMMAND,
                SpellSelectionManager.MAINHAND,
                null
        );
    }

    public static ContinuousCastStartResult tryStartContinuousCast(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            SpellDispenserSpellValidator.ValidationResult validation,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile,
            @Nullable SpellDispenserManaHelper.ManaAccess manaAccess,
            CastSource castSource,
            String castingSlot,
            @Nullable Integer castDurationOverrideTicks
    ) {
        var failurePos = BlockPos.containing(castBasePosition);
        if (!validation.isSupported()) {
            return new ContinuousCastStartResult(CastResult.validationFailure(validation), null);
        }
        var profile = SpellDispenserSpellProfileManager.getResolvedProfile(validation.spellData());
        var casterProfile = resolveCasterProfile(ownerProfile, profile);
        if (casterProfile == null) {
            return new ContinuousCastStartResult(CastResult.missingOwnerProfile(validation), null);
        }

        var spellData = validation.spellData();
        var spell = spellData.getSpell();
        if (spell.getCastType() != CastType.CONTINUOUS) {
            return new ContinuousCastStartResult(CastResult.validationFailure(validation), null);
        }

        var spellId = spell.getSpellResource();
        var proxy = createProxy(level, castBasePosition, forward, casterProfile, profile);
        var trackedAnchor = createTrackedAnchorForExplicitProfile(level, proxy, profile, spell.getCastType());
        var spellCaster = resolveSpellCaster(proxy, trackedAnchor);
        var magicData = MagicData.getPlayerMagicData(proxy);
        var cooldownTicks = resolveCooldownTicks(spellData, proxy);
        try {
            magicData.setSyncedData(new SyncedSpellData(proxy));
            var castDuration = castDurationOverrideTicks != null
                    ? Math.max(0, castDurationOverrideTicks)
                    : Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), proxy));
            magicData.initiateCast(spell, spellData.getLevel(), castDuration, castSource, castingSlot);
            magicData.setPlayerCastingItem(spellSource.copy());
            syncProxyMana(manaAccess, magicData);
        } catch (RuntimeException exception) {
            var result = exceptionFailure(level, failurePos, validation, spellId, CastStage.INITIATE_CAST, exception, false, 0, ownerProfile);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }

        final boolean canCast;
        try {
            syncProxyMana(manaAccess, magicData);
            canCast = spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, magicData);
        } catch (RuntimeException exception) {
            var result = exceptionFailure(level, failurePos, validation, spellId, CastStage.CHECK_PRE_CAST, exception, false, 0, ownerProfile);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }
        if (!canCast) {
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(CastResult.preCastRejected(validation, spellId, proxy.consumeActionBarMessage()), null);
        }

        try {
            syncProxyMana(manaAccess, magicData);
            spell.onServerPreCast(level, spellData.getLevel(), spellCaster, magicData);
        } catch (RuntimeException exception) {
            var result = exceptionFailure(level, failurePos, validation, spellId, CastStage.SERVER_PRE_CAST, exception, false, 0, ownerProfile);
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(result, null);
        }

        if (manaAccess != null && !SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
            cleanupProxy(spellId, magicData, proxy, trackedAnchor);
            return new ContinuousCastStartResult(
                    CastResult.insufficientMana(validation, spell.getManaCost(spellData.getLevel()), manaAccess.getCurrentMana()),
                    null
            );
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
                        manaAccess,
                        castSource,
                        castingSlot
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
                spell.onCast(level, spellData.getLevel(), spellCaster, session.castSource(), magicData);
            } catch (RuntimeException exception) {
                exceptionFailure(
                        level,
                        session.origin(),
                        session.validation(),
                        session.spellId(),
                        CastStage.CAST,
                        exception,
                        true,
                        session.cooldownTicks(),
                        session.proxy().getGameProfile()
                );
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
            exceptionFailure(
                    level,
                    session.origin(),
                    session.validation(),
                    session.spellId(),
                    CastStage.SERVER_CAST_TICK,
                    exception,
                    session.hasReachedOnCast(),
                    session.hasReachedOnCast() ? session.cooldownTicks() : 0,
                    session.proxy().getGameProfile()
            );
            finishContinuousCast(level, session, true);
            return false;
        }

        return !session.isFinished();
    }

    private static void syncProxyMana(@Nullable SpellDispenserManaHelper.ManaAccess manaAccess, MagicData magicData) {
        if (manaAccess != null) {
            magicData.setMana(manaAccess.isManaConsumptionExempt()
                    ? SpellDispenserManaHelper.MAX_MANA
                    : manaAccess.getCurrentMana());
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
            exceptionFailure(
                    level,
                    session.origin(),
                    session.validation(),
                    session.spellId(),
                    CastStage.SERVER_CAST_COMPLETE,
                    exception,
                    session.hasReachedOnCast(),
                    session.hasReachedOnCast() ? session.cooldownTicks() : 0,
                    session.proxy().getGameProfile()
            );
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

    public static void notifyFailureToNearbyPlayers(
            ServerLevel level,
            BlockPos pos,
            CastResult result,
            Map<String, Long> recentFailureNoticeTicks
    ) {
        if (!result.shouldNotifyPlayers()) {
            return;
        }

        var gameTime = level.getGameTime();
        pruneExpiredFailureNoticeTicks(recentFailureNoticeTicks, gameTime);
        var noticeKey = result.createFailureNoticeKey();
        var previous = recentFailureNoticeTicks.get(noticeKey);
        if (previous != null && gameTime - previous < FAILURE_NOTICE_COOLDOWN_TICKS) {
            return;
        }
        recentFailureNoticeTicks.put(noticeKey, gameTime);

        var center = Vec3.atCenterOf(pos);
        var noticeBox = new AABB(center, center).inflate(FAILURE_NOTICE_RANGE);
        for (var player : level.getEntitiesOfClass(ServerPlayer.class, noticeBox)) {
            for (var message : result.createFailureMessages(player)) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static void pruneExpiredFailureNoticeTicks(Map<String, Long> recentFailureNoticeTicks, long gameTime) {
        recentFailureNoticeTicks.entrySet().removeIf(entry -> gameTime - entry.getValue() >= FAILURE_NOTICE_COOLDOWN_TICKS);
    }

    private static CastResult exceptionFailure(
            ServerLevel level,
            BlockPos pos,
            SpellDispenserSpellValidator.ValidationResult validation,
            @Nullable ResourceLocation spellId,
            CastStage failedStage,
            RuntimeException exception,
            boolean reachedOnCast,
            int cooldownTicks,
            @Nullable GameProfile ownerProfile
    ) {
        ApprenticeCodex.LOGGER.warn(
                "Spell Dispenser cast exception: stage={}, pos={}, spell={}, castType={}, ownerPresent={}, reachedOnCast={}, cooldownTicks={}",
                failedStage.logName(),
                pos,
                spellId,
                resolveCastTypeName(validation),
                isValidOwnerProfile(ownerProfile),
                reachedOnCast,
                cooldownTicks,
                exception
        );
        return CastResult.exceptionFailure(validation, spellId, reachedOnCast, cooldownTicks);
    }

    private static String resolveCastTypeName(SpellDispenserSpellValidator.ValidationResult validation) {
        if (validation.spellData() == SpellData.EMPTY || validation.spellData().getSpell() == null) {
            return "unknown";
        }
        return validation.spellData().getSpell().getCastType().name();
    }

    private static CapturingFakePlayer createProxy(
            ServerLevel level,
            Vec3 castBasePosition,
            Vec3 forward,
            GameProfile casterProfile,
            SpellDispenserSpellProfile profile
    ) {
        var castTransform = resolveCastTransform(castBasePosition, forward, profile);
        // owner 任意 spell でも内部は Player 経路を通るため、
        // owner 不在時だけ共有ダミー profile を使って FakePlayer caster を維持する。
        var proxy = new CapturingFakePlayer(level, new GameProfile(casterProfile.getId(), casterProfile.getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        moveCaster(proxy, castTransform);
        return proxy;
    }

    private static @Nullable GameProfile resolveCasterProfile(
            @Nullable GameProfile ownerProfile,
            SpellDispenserSpellProfile profile
    ) {
        if (isValidOwnerProfile(ownerProfile)) {
            return ownerProfile;
        }
        if (profile.ownerRequired()) {
            return null;
        }
        return new GameProfile(OWNER_OPTIONAL_FALLBACK_UUID, OWNER_OPTIONAL_FALLBACK_NAME);
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
        INITIATE_CAST,
        CHECK_PRE_CAST,
        SERVER_PRE_CAST,
        CAST,
        SERVER_CAST_TICK,
        SERVER_CAST_COMPLETE;

        public String logName() {
            return name().toLowerCase();
        }
    }

    public enum FailureType {
        NONE,
        NO_SCROLL,
        INVALID_SPELL,
        COOLDOWN,
        INSUFFICIENT_MANA,
        PRE_CAST,
        OWNER_MISSING,
        EXCEPTION
    }

    public record CastResult(
            boolean succeeded,
            SpellDispenserSpellValidator.ValidationResult validation,
            @Nullable ResourceLocation spellId,
            boolean reachedOnCast,
            int cooldownTicks,
            FailureType failureType,
            int remainingCooldownTicks,
            int requiredMana,
            int currentMana,
            @Nullable Component preCastActionBar
    ) {
        private static CastResult success(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                int cooldownTicks
        ) {
            return new CastResult(true, validation, spellId, true, cooldownTicks, FailureType.NONE, 0, 0, 0, null);
        }

        public static CastResult validationFailure(SpellDispenserSpellValidator.ValidationResult validation) {
            if (validation.failureReason() == SpellDispenserSpellValidator.FailureReason.EMPTY) {
                return noScroll(validation);
            }
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, false, 0, FailureType.INVALID_SPELL, 0, 0, 0, null);
        }

        public static CastResult noScroll(SpellDispenserSpellValidator.ValidationResult validation) {
            return new CastResult(false, validation, null, false, 0, FailureType.NO_SCROLL, 0, 0, 0, null);
        }

        public static CastResult cooldownBlocked(SpellDispenserSpellValidator.ValidationResult validation, int remainingCooldownTicks) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(
                    false,
                    validation,
                    spellId,
                    false,
                    0,
                    FailureType.COOLDOWN,
                    Math.max(0, remainingCooldownTicks),
                    0,
                    0,
                    null
            );
        }

        public static CastResult cooldownBlocked(SpellDispenserSpellValidator.ValidationResult validation) {
            return cooldownBlocked(validation, 0);
        }

        private static CastResult preCastRejected(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                @Nullable Component preCastActionBar
        ) {
            return new CastResult(false, validation, spellId, false, 0, FailureType.PRE_CAST, 0, 0, 0, copyComponent(preCastActionBar));
        }

        public static CastResult missingOwnerProfile(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, false, 0, FailureType.OWNER_MISSING, 0, 0, 0, null);
        }

        public static CastResult insufficientMana(
                SpellDispenserSpellValidator.ValidationResult validation,
                int requiredMana,
                int currentMana
        ) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(
                    false,
                    validation,
                    spellId,
                    false,
                    0,
                    FailureType.INSUFFICIENT_MANA,
                    0,
                    Math.max(0, requiredMana),
                    Math.max(0, currentMana),
                    null
            );
        }

        private static CastResult exceptionFailure(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                boolean reachedOnCast,
                int cooldownTicks
        ) {
            return new CastResult(false, validation, spellId, reachedOnCast, cooldownTicks, FailureType.EXCEPTION, 0, 0, 0, null);
        }

        public boolean missingOwnerProfile() {
            return failureType == FailureType.OWNER_MISSING;
        }

        public boolean insufficientMana() {
            return failureType == FailureType.INSUFFICIENT_MANA;
        }

        public boolean shouldNotifyPlayers() {
            return !succeeded && failureType != FailureType.NONE && failureType != FailureType.EXCEPTION;
        }

        public String createFailureNoticeKey() {
            return switch (failureType) {
                case NO_SCROLL -> "no_scroll";
                case INVALID_SPELL -> "invalid_spell:" + resolveFailureSubjectKey();
                case COOLDOWN -> "cooldown:" + getRemainingCooldownSeconds();
                case INSUFFICIENT_MANA -> "insufficient_mana:" + requiredMana + ":" + currentMana;
                case PRE_CAST -> "pre_cast:" + resolveFailureSubjectKey() + ":" + resolveActionBarKey();
                case OWNER_MISSING -> "owner_missing";
                case NONE, EXCEPTION -> "none";
            };
        }

        public List<Component> createFailureMessages(ServerPlayer player) {
            var messages = new ArrayList<Component>();
            if (!shouldNotifyPlayers()) {
                return messages;
            }

            messages.add(Component.translatable("chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.title").withStyle(ChatFormatting.RED));
            switch (failureType) {
                case NO_SCROLL -> messages.add(Component.translatable(
                        "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.no_scroll"
                ).withStyle(ChatFormatting.RED));
                case INVALID_SPELL -> messages.add(Component.translatable(
                        "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.invalid_spell",
                        resolveFailureSubject(player)
                ).withStyle(ChatFormatting.RED));
                case COOLDOWN -> messages.add(Component.translatable(
                        "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.cooldown",
                        getRemainingCooldownSeconds()
                ).withStyle(ChatFormatting.RED));
                case INSUFFICIENT_MANA -> messages.add(Component.translatable(
                        "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.insufficient_mana",
                        requiredMana,
                        currentMana
                ).withStyle(ChatFormatting.RED));
                case PRE_CAST -> {
                    messages.add(Component.translatable(
                            "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.pre_cast",
                            resolveFailureSubject(player)
                    ).withStyle(ChatFormatting.RED));
                    if (preCastActionBar != null) {
                        messages.add(Component.translatable(
                                "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.pre_cast.action_bar",
                                preCastActionBar
                        ).withStyle(ChatFormatting.RED));
                    }
                }
                case OWNER_MISSING -> messages.add(Component.translatable(
                        "chat." + ApprenticeCodex.MODID + ".spell_dispenser.cast_failed.reason.owner_missing"
                ).withStyle(ChatFormatting.RED));
            }
            return messages;
        }

        private int getRemainingCooldownSeconds() {
            return Math.max(1, Mth.ceil(remainingCooldownTicks / 20.0D));
        }

        private String resolveFailureSubjectKey() {
            if (spellId != null) {
                return spellId.toString();
            }
            if (!validation.sourceStack().isEmpty()) {
                return validation.sourceStack().getDescriptionId();
            }
            return "unknown";
        }

        private String resolveActionBarKey() {
            return preCastActionBar == null ? "" : preCastActionBar.getString();
        }

        private Component resolveFailureSubject(ServerPlayer player) {
            if (validation.spellData() != SpellData.EMPTY && validation.spellData().getSpell() != null) {
                return validation.spellData().getSpell().getDisplayName(player);
            }
            if (!validation.sourceStack().isEmpty()) {
                return validation.sourceStack().getHoverName();
            }
            return Component.literal(spellId == null ? "unknown" : spellId.toString());
        }
    }

    public record ContinuousCastStartResult(
            CastResult result,
            @Nullable ContinuousCastSession session
    ) {
    }

    private static @Nullable Component copyComponent(@Nullable Component component) {
        return component == null ? null : component.copy();
    }

    private static final class CapturingFakePlayer extends FakePlayer {
        @Nullable
        private Component lastActionBarMessage;

        private CapturingFakePlayer(ServerLevel level, GameProfile name) {
            super(level, name);
            this.connection = new CapturingFakePlayerNetHandler(level.getServer(), this);
        }

        @Override
        public void displayClientMessage(Component chatComponent, boolean actionBar) {
            if (actionBar) {
                captureActionBarMessage(chatComponent);
            }
        }

        private void captureActionBarMessage(@Nullable Component message) {
            lastActionBarMessage = copyComponent(message);
        }

        private @Nullable Component consumeActionBarMessage() {
            var captured = lastActionBarMessage;
            lastActionBarMessage = null;
            return copyComponent(captured);
        }
    }

    private static final class CapturingFakePlayerNetHandler extends ServerGamePacketListenerImpl {
        private static final Connection DUMMY_CONNECTION = new Connection(PacketFlow.CLIENTBOUND);
        private static final Method ACTION_BAR_COMPONENT_METHOD = resolveActionBarComponentMethod();

        private final CapturingFakePlayer player;

        private CapturingFakePlayerNetHandler(net.minecraft.server.MinecraftServer server, CapturingFakePlayer player) {
            super(server, DUMMY_CONNECTION, player);
            this.player = player;
        }

        @Override
        public void send(@NotNull Packet<?> packet) {
            capturePacket(packet);
        }

        @Override
        public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener sendListener) {
            capturePacket(packet);
        }

        @Override
        public void disconnect(@NotNull Component message) {
        }

        @Override
        public void tick() {
        }

        @Override
        public void resetPosition() {
        }

        private void capturePacket(Packet<?> packet) {
            if (!(packet instanceof ClientboundSetActionBarTextPacket)) {
                return;
            }
            if (ACTION_BAR_COMPONENT_METHOD == null) {
                return;
            }
            try {
                var value = ACTION_BAR_COMPONENT_METHOD.invoke(packet);
                if (value instanceof Component component) {
                    player.captureActionBarMessage(component);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        private static @Nullable Method resolveActionBarComponentMethod() {
            for (var method : ClientboundSetActionBarTextPacket.class.getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == Component.class) {
                    return method;
                }
            }
            return null;
        }
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
        private final CastSource castSource;
        private final String castingSlot;
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
                @Nullable SpellDispenserManaHelper.ManaAccess manaAccess,
                CastSource castSource,
                String castingSlot
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
            this.castSource = castSource;
            this.castingSlot = castingSlot;
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

        public CastSource castSource() {
            return castSource;
        }

        public String castingSlot() {
            return castingSlot;
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
