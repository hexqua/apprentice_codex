package jp.aquafactory.apprenticecodex.spell.treasuredivination;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.lootr.LootrTreasureDivinationCompatBridge;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TreasureDivination extends AbstractSpell {
    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int CASTING_MOBILITY_EFFECT_REFRESH_TICKS = 5;
    private static final int MAX_SEARCH_RANGE = 32;
    private static final int MAX_DETECTABLE_TARGET_COUNT = 64;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "treasure_divination");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public TreasureDivination() {
        baseSpellPower = 400;
        spellPowerPerLevel = 400;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 20 * 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.max_search_distance", getRange(spellLevel, caster))
        );
    }

    private int getRange(int spellLevel, LivingEntity entity) {
        return Math.max(0, Math.min(MAX_SEARCH_RANGE, Mth.floor(getSpellPower(spellLevel, entity) / 100.0f)));
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        applyCastingMobility(entity);

        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer serverPlayer)) {
            super.onServerCastTick(level, spellLevel, entity, playerMagicData);
            return;
        }

        if (!shouldScanThisTick(spellLevel, entity, playerMagicData)) {
            super.onServerCastTick(level, spellLevel, entity, playerMagicData);
            return;
        }

        var range = getRange(spellLevel, entity);
        var result = scanNearestTreasure(serverLevel, serverPlayer, entity.blockPosition(), range);
        sendScanMessage(serverPlayer, range, result);
        if (result.tooNoisy()) {
            MagicTools.cancelCasting(serverPlayer, true);
            return;
        }

        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void applyCastingMobility(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        // TreasureDivination は情報取得そのものが主目的なので、継続詠唱減速だけを共通効果で打ち消す。
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.CASTING_MOBILITY,
                CASTING_MOBILITY_EFFECT_REFRESH_TICKS,
                0,
                false,
                false,
                false
        ));
    }

    private boolean shouldScanThisTick(int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData == null) {
            return entity.tickCount % SEARCH_INTERVAL_TICKS == 0;
        }

        var elapsedTicks = Math.max(0, getEffectiveCastTime(spellLevel, entity) - playerMagicData.getCastDurationRemaining());
        return elapsedTicks > 0 && elapsedTicks % SEARCH_INTERVAL_TICKS == 0;
    }

    private ScanResult scanNearestTreasure(ServerLevel level, ServerPlayer player, BlockPos origin, int range) {
        var minChunkX = SectionPos.blockToSectionCoord(origin.getX() - range);
        var maxChunkX = SectionPos.blockToSectionCoord(origin.getX() + range);
        var minChunkZ = SectionPos.blockToSectionCoord(origin.getZ() - range);
        var maxChunkZ = SectionPos.blockToSectionCoord(origin.getZ() + range);
        var minY = Math.max(level.getMinBuildHeight(), origin.getY() - range);
        var maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + range);
        ScanResult best = ScanResult.notFound();
        int detectedTargetCount = 0;
        var mutablePos = new BlockPos.MutableBlockPos();

        // 未読込チャンクは起こさず、現在読み込まれている範囲だけを 2 秒ごとに走査する。
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                var chunkMinX = Math.max(origin.getX() - range, chunk.getPos().getMinBlockX());
                var chunkMaxX = Math.min(origin.getX() + range, chunk.getPos().getMaxBlockX());
                var chunkMinZ = Math.max(origin.getZ() - range, chunk.getPos().getMinBlockZ());
                var chunkMaxZ = Math.min(origin.getZ() + range, chunk.getPos().getMaxBlockZ());
                var sections = chunk.getSections();
                for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    var sectionY = level.getMinSection() + sectionIndex;
                    var sectionMinY = Math.max(minY, SectionPos.sectionToBlockCoord(sectionY));
                    var sectionMaxY = Math.min(maxY, SectionPos.sectionToBlockCoord(sectionY) + 15);
                    if (sectionMinY > sectionMaxY) {
                        continue;
                    }

                    // 大半が空の section をまとめて飛ばし、最大範囲時の無駄走査を抑える。
                    for (int y = sectionMinY; y <= sectionMaxY; y++) {
                        for (int x = chunkMinX; x <= chunkMaxX; x++) {
                            for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                                mutablePos.set(x, y, z);
                                var state = chunk.getBlockState(mutablePos);
                                if (!isTarget(level, player, mutablePos, state)) {
                                    continue;
                                }

                                detectedTargetCount++;
                                if (detectedTargetCount > MAX_DETECTABLE_TARGET_COUNT) {
                                    // 高密度地点は最後まで走査するより即中断した方が負荷と連打抑止の両面で安全。
                                    return ScanResult.overloaded();
                                }

                                best = best.pickBetter(new ScanResult(manhattanDistance(origin, mutablePos)));
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private boolean isTarget(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!state.is(TagRegistry.Blocks.TREASURE_DIVINATION_TARGETS)) {
            return false;
        }

        return !LootrTreasureDivinationCompatBridge.shouldIgnoreOpenedTarget(level, player, pos, state);
    }

    private int manhattanDistance(BlockPos origin, BlockPos target) {
        return Math.abs(origin.getX() - target.getX())
                + Math.abs(origin.getY() - target.getY())
                + Math.abs(origin.getZ() - target.getZ());
    }

    private void sendScanMessage(ServerPlayer player, int range, ScanResult result) {
        Component message;
        if (result.tooNoisy()) {
            message = Component.translatable("ui.apprenticecodex.treasure_divination.too_noisy")
                    .withStyle(ChatFormatting.RED);
        } else if (result.found()) {
            message = Component.translatable(
                    "ui.apprenticecodex.treasure_divination.found",
                    result.distance()
            ).withStyle(ChatFormatting.YELLOW);
        } else {
            message = Component.translatable(
                    "ui.apprenticecodex.treasure_divination.not_found",
                    range
            ).withStyle(ChatFormatting.RED);
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private record ScanResult(int distance, boolean tooNoisy) {
        private static final int NOT_FOUND_DISTANCE = Integer.MAX_VALUE;

        private static ScanResult notFound() {
            return new ScanResult(NOT_FOUND_DISTANCE, false);
        }

        private static ScanResult overloaded() {
            return new ScanResult(NOT_FOUND_DISTANCE, true);
        }

        private ScanResult(int distance) {
            this(distance, false);
        }

        private boolean found() {
            return !tooNoisy && distance != NOT_FOUND_DISTANCE;
        }

        private ScanResult pickBetter(ScanResult candidate) {
            if (candidate.tooNoisy()) {
                return candidate;
            }
            if (!candidate.found()) {
                return this;
            }
            if (!found()) {
                return candidate;
            }
            if (candidate.distance < distance) {
                return candidate;
            }
            return this;
        }
    }
}
