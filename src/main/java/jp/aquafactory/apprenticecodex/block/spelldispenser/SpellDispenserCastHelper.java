package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserCastHelper {
    private static final double FAILURE_NOTICE_RADIUS = 16.0D;
    private static final int CONTINUOUS_CAST_TICK_INTERVAL = 10;

    private SpellDispenserCastHelper() {
    }

    public static CastResult tryCast(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            ItemStack spellSource,
            @Nullable GameProfile ownerProfile
    ) {
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
        var proxy = createProxy(level, pos, facing, ownerProfile);

        var magicData = MagicData.getPlayerMagicData(proxy);
        CastResult result = CastResult.validationFailure(validation);
        try {
            try {
                // Iron's の initiateCast は getSyncedData を経由せず syncedSpellData を直接参照する。
                // Spell Dispenser の FakePlayer は通常の player tick / sync 経路に乗せないため、ここで同期データを先に用意する。
                magicData.setSyncedData(new SyncedSpellData(proxy));
                // LONG を含め詠唱時間 0 扱いで検証したいため、Spellgun 系と同様に override 値 0 を入れる。
                magicData.initiateCast(spell, spellData.getLevel(), 0, CastSource.COMMAND, SpellSelectionManager.MAINHAND);
                // 短命な proxy にも casting item を持たせ、scroll 起点の通常経路に近い条件で検証する。
                magicData.setPlayerCastingItem(spellSource.copy());
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.INITIATE_CAST, exception);
                notifyFailureToNearbyPlayers(level, pos, result);
                return result;
            }

            final boolean canCast;
            try {
                canCast = spell.checkPreCastConditions(level, spellData.getLevel(), proxy, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.CHECK_PRE_CAST, exception);
                notifyFailureToNearbyPlayers(level, pos, result);
                return result;
            }
            if (!canCast) {
                return CastResult.preCastRejected(validation, spellId);
            }

            try {
                spell.onServerPreCast(level, spellData.getLevel(), proxy, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_PRE_CAST, exception);
                notifyFailureToNearbyPlayers(level, pos, result);
                return result;
            }

            try {
                spell.onCast(level, spellData.getLevel(), proxy, CastSource.COMMAND, magicData);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.CAST, exception);
                notifyFailureToNearbyPlayers(level, pos, result);
                return result;
            }

            try {
                spell.onServerCastComplete(level, spellData.getLevel(), proxy, magicData, false);
            } catch (RuntimeException exception) {
                result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_CAST_COMPLETE, exception);
                notifyFailureToNearbyPlayers(level, pos, result);
                return result;
            }

            result = CastResult.success(validation, spellId);
            return result;
        } finally {
            cleanupProxy(spellId, magicData, proxy);
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
        var proxy = createProxy(level, pos, facing, ownerProfile);
        var magicData = MagicData.getPlayerMagicData(proxy);
        try {
            magicData.setSyncedData(new SyncedSpellData(proxy));
            var castDuration = Math.max(0, spell.getEffectiveCastTime(spellData.getLevel(), proxy));
            magicData.initiateCast(spell, spellData.getLevel(), castDuration, CastSource.COMMAND, SpellSelectionManager.MAINHAND);
            magicData.setPlayerCastingItem(spellSource.copy());
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.INITIATE_CAST, exception);
            notifyFailureToNearbyPlayers(level, pos, result);
            cleanupProxy(spellId, magicData, proxy);
            return new ContinuousCastStartResult(result, null);
        }

        final boolean canCast;
        try {
            canCast = spell.checkPreCastConditions(level, spellData.getLevel(), proxy, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.CHECK_PRE_CAST, exception);
            notifyFailureToNearbyPlayers(level, pos, result);
            cleanupProxy(spellId, magicData, proxy);
            return new ContinuousCastStartResult(result, null);
        }
        if (!canCast) {
            cleanupProxy(spellId, magicData, proxy);
            return new ContinuousCastStartResult(CastResult.preCastRejected(validation, spellId), null);
        }

        try {
            spell.onServerPreCast(level, spellData.getLevel(), proxy, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(validation, spellId, CastStage.SERVER_PRE_CAST, exception);
            notifyFailureToNearbyPlayers(level, pos, result);
            cleanupProxy(spellId, magicData, proxy);
            return new ContinuousCastStartResult(result, null);
        }

        return new ContinuousCastStartResult(
                CastResult.success(validation, spellId),
                new ContinuousCastSession(pos, validation, spellSource.copy(), proxy, magicData)
        );
    }

    public static boolean tickContinuousCast(ServerLevel level, ContinuousCastSession session) {
        if (session.isFinished()) {
            return false;
        }

        var spellData = session.validation().spellData();
        var spell = spellData.getSpell();
        var proxy = session.proxy();
        var magicData = session.magicData();

        proxy.tickCount++;
        magicData.handleCastDuration();

        if (magicData.getCastDurationRemaining() <= 0) {
            // Iron's 本体では cast time が極端に短い連続魔法が固着し得るため、
            // Dispenser 側では上限到達時に必ず止めて RS 再入力待ちへ戻す。
            finishContinuousCast(level, session, false);
            return false;
        }

        if ((magicData.getCastDurationRemaining() + 1) % CONTINUOUS_CAST_TICK_INTERVAL == 0) {
            try {
                spell.onCast(level, spellData.getLevel(), proxy, CastSource.COMMAND, magicData);
            } catch (RuntimeException exception) {
                var result = CastResult.exceptionFailure(session.validation(), session.spellId(), CastStage.CAST, exception);
                notifyFailureToNearbyPlayers(level, session.origin(), result);
                finishContinuousCast(level, session, true);
                return false;
            }

            if (magicData.getCastDurationRemaining() < CONTINUOUS_CAST_TICK_INTERVAL) {
                finishContinuousCast(level, session, false);
                return false;
            }
        }

        try {
            spell.onServerCastTick(level, spellData.getLevel(), proxy, magicData);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(session.validation(), session.spellId(), CastStage.SERVER_CAST_TICK, exception);
            notifyFailureToNearbyPlayers(level, session.origin(), result);
            finishContinuousCast(level, session, true);
            return false;
        }

        return !session.isFinished();
    }

    public static void finishContinuousCast(ServerLevel level, ContinuousCastSession session, boolean cancelled) {
        if (session.isFinished()) {
            return;
        }

        session.markFinished();
        var spellData = session.validation().spellData();
        try {
            spellData.getSpell().onServerCastComplete(level, spellData.getLevel(), session.proxy(), session.magicData(), cancelled);
        } catch (RuntimeException exception) {
            var result = CastResult.exceptionFailure(session.validation(), session.spellId(), CastStage.SERVER_CAST_COMPLETE, exception);
            notifyFailureToNearbyPlayers(level, session.origin(), result);
        } finally {
            cleanupProxy(session.spellId(), session.magicData(), session.proxy());
        }
    }

    private static void cleanupProxy(@Nullable ResourceLocation spellId, @Nullable MagicData magicData, FakePlayer proxy) {
        try {
            if (magicData != null) {
                // FakePlayer 自体は world に参加させないが、MagicData は capability 側に残るため毎回明示的に初期化する。
                magicData.resetCastingState();
            }
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn("Spell Dispenser cleanup failed during reset: {}", spellId, exception);
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

    private static FakePlayer createProxy(ServerLevel level, BlockPos pos, Direction facing, GameProfile ownerProfile) {
        var muzzlePos = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.7D));
        var yaw = resolveYaw(facing);
        var pitch = resolvePitch(facing);
        // Spectral Hammer のように後続 tick で Player を要求する spell があるため、
        // Spell Dispenser は設置者 profile を持つ FakePlayer を caster として扱う。
        var proxy = new FakePlayer(level, new GameProfile(ownerProfile.getId(), ownerProfile.getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var feetY = muzzlePos.y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(muzzlePos.x, feetY, muzzlePos.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.setXRot(pitch);
        proxy.xRotO = pitch;
        return proxy;
    }

    private static boolean isValidOwnerProfile(@Nullable GameProfile ownerProfile) {
        return ownerProfile != null
                && ownerProfile.getId() != null
                && ownerProfile.getName() != null
                && !ownerProfile.getName().isBlank();
    }

    private static float resolveYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            case UP, DOWN -> 0.0F;
        };
    }

    private static float resolvePitch(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
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
            boolean missingOwnerProfile
    ) {
        private static CastResult success(SpellDispenserSpellValidator.ValidationResult validation, ResourceLocation spellId) {
            return new CastResult(true, validation, spellId, null, null, false);
        }

        public static CastResult validationFailure(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, null, null, false);
        }

        private static CastResult preCastRejected(SpellDispenserSpellValidator.ValidationResult validation, ResourceLocation spellId) {
            return new CastResult(false, validation, spellId, null, null, false);
        }

        public static CastResult missingOwnerProfile(SpellDispenserSpellValidator.ValidationResult validation) {
            var spellId = validation.spellData() == SpellData.EMPTY ? null : validation.spellData().getSpell().getSpellResource();
            return new CastResult(false, validation, spellId, null, null, true);
        }

        private static CastResult exceptionFailure(
                SpellDispenserSpellValidator.ValidationResult validation,
                ResourceLocation spellId,
                CastStage failedStage,
                RuntimeException exception
        ) {
            ApprenticeCodex.LOGGER.warn("Spell Dispenser cast failed at {}: {}", failedStage.name(), spellId, exception);
            return new CastResult(false, validation, spellId, failedStage, exception, false);
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
        private final FakePlayer proxy;
        private final MagicData magicData;
        private boolean finished;

        private ContinuousCastSession(
                BlockPos origin,
                SpellDispenserSpellValidator.ValidationResult validation,
                ItemStack spellSource,
                FakePlayer proxy,
                MagicData magicData
        ) {
            this.spellId = validation.spellData().getSpell().getSpellResource();
            this.origin = origin.immutable();
            this.validation = validation;
            this.spellSource = spellSource;
            this.proxy = proxy;
            this.magicData = magicData;
            this.finished = false;
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

        public FakePlayer proxy() {
            return proxy;
        }

        public MagicData magicData() {
            return magicData;
        }

        public boolean isFinished() {
            return finished;
        }

        private void markFinished() {
            finished = true;
        }
    }
}
