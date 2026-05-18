package jp.aquafactory.apprenticecodex.spell.rifthole;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RiftHole extends AbstractSpell implements IClientBlockTargetingSpell {
    private static final int DURATION_TICKS = 20 * 10;
    private static final int TUNNEL_RADIUS = 1;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "rift_hole");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(10)
            .build();

    public RiftHole() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        baseManaCost = 150;
        manaCostPerLevel = 50;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 0)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(DURATION_TICKS, 1))
        );
    }

    private double getRange(int spellLevel, LivingEntity entity) {
        return 8 + 8 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getTunnelDepth(int spellLevel, LivingEntity entity) {
        return Math.max(1, Mth.floor(getRange(spellLevel, entity)));
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange(spellLevel, entity);
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
        return CastType.INSTANT;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_RIFT_HOLE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new RiftHoleCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            var recast = recasts.getRecastInstance(getSpellId());
            if (!(recast.getCastData() instanceof RiftHoleCastData recastData) || !recastData.hasTunnel()) {
                return false;
            }

            playerMagicData.setAdditionalCastData(recastData.createCloseCastData());
            return true;
        }

        if (!ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(level.dimension().location())) {
            sendDimensionNotAllowedMessage(entity);
            return false;
        }

        var tunnelPlanResult = buildTunnelPlan(level, spellLevel, entity);
        if (tunnelPlanResult.status() == TunnelPlanStatus.NO_TARGET) {
            sendTargetBlockMessage(entity);
            return false;
        }
        if (tunnelPlanResult.status() != TunnelPlanStatus.SUCCESS || tunnelPlanResult.plan().isEmpty()) {
            sendCantOpenHoleMessage(entity, tunnelPlanResult.failedState());
            return false;
        }
        var tunnelPlan = tunnelPlanResult.plan().orElseThrow();

        playerMagicData.setAdditionalCastData(RiftHoleCastData.createOpenCastData(UUID.randomUUID(), tunnelPlan));
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        var castData = getRiftHoleCastData(playerMagicData);
        if (castData == null) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        if (castData.isCloseCast()) {
            closeTunnel(serverLevel, castData);
            removeActiveRecast(playerMagicData);
        } else if (castData.isOpenCast()) {
            openTunnel(serverLevel, entity, castSource, spellLevel, playerMagicData, castData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        if (castDataSerializable instanceof RiftHoleCastData castData) {
            closeTunnel(serverPlayer.serverLevel(), castData);
        }

        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            var castData = getRiftHoleCastData(playerMagicData);
            if (castData != null) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }

        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private TunnelPlanResult buildTunnelPlan(Level level, int spellLevel, LivingEntity entity) {
        var targetData = BlockTargetingHelper.getValidatedPendingTarget(level, entity, getSpellResource(), getRange(spellLevel, entity));
        if (targetData.isEmpty()) {
            return TunnelPlanResult.noTarget();
        }

        var hitPos = targetData.get().getHitBlockPos();
        var hitFace = targetData.get().getHitFace();
        if (hitPos == null || hitFace == null) {
            return TunnelPlanResult.noTarget();
        }

        var hitState = level.getBlockState(hitPos);
        if (!RiftHoleBlockSafety.isTargetBlock(level, hitPos)) {
            return TunnelPlanResult.cantOpen(hitState);
        }
        // Portable Hole 系の手触りとして、入口ブロックだけは必ず開く前提にする。
        // 入口が残るまま周囲だけ開くと違和感が強いため、ここで全体失敗へ寄せる。
        if (!RiftHoleBlockSafety.canReplace(level, hitPos)) {
            return TunnelPlanResult.cantOpen(hitState);
        }

        var positions = new ArrayList<BlockPos>();
        var originalStates = new ArrayList<BlockState>();
        var forward = hitFace.getOpposite();
        var depth = getTunnelDepth(spellLevel, entity);
        for (var layer = 0; layer < depth; ++layer) {
            var center = hitPos.relative(forward, layer);
            for (var axisA = -TUNNEL_RADIUS; axisA <= TUNNEL_RADIUS; ++axisA) {
                for (var axisB = -TUNNEL_RADIUS; axisB <= TUNNEL_RADIUS; ++axisB) {
                    var pos = offsetOnPlane(center, hitFace, axisA, axisB);
                    if (!RiftHoleBlockSafety.canReplace(level, pos)) {
                        continue;
                    }

                    positions.add(pos.immutable());
                    originalStates.add(level.getBlockState(pos));
                }
            }
        }

        if (positions.isEmpty()) {
            return TunnelPlanResult.cantOpen(hitState);
        }

        return TunnelPlanResult.success(new TunnelPlan(positions, originalStates));
    }

    private static BlockPos offsetOnPlane(BlockPos centerPos, Direction normal, int axisA, int axisB) {
        return switch (normal.getAxis()) {
            case X -> centerPos.offset(0, axisA, axisB);
            case Y -> centerPos.offset(axisA, 0, axisB);
            case Z -> centerPos.offset(axisA, axisB, 0);
        };
    }

    private static @Nullable RiftHoleCastData getRiftHoleCastData(MagicData playerMagicData) {
        return playerMagicData.getAdditionalCastData() instanceof RiftHoleCastData castData ? castData : null;
    }

    private void openTunnel(ServerLevel level, LivingEntity entity, CastSource castSource, int spellLevel, MagicData playerMagicData,
                            RiftHoleCastData castData) {
        var expireGameTime = level.getGameTime() + DURATION_TICKS;
        for (var index = 0; index < castData.positions().size(); ++index) {
            var pos = castData.positions().get(index);
            if (!RiftHoleBlockSafety.canReplace(level, pos)) {
                continue;
            }

            var originalState = castData.originalStates().get(index);
            level.setBlockAndUpdate(pos, BlockRegistry.RIFT_HOLE.get().defaultBlockState());
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RiftHoleBlockEntity riftHoleBlockEntity) {
                riftHoleBlockEntity.initialize(originalState, entity.getUUID(), castData.tunnelId(), expireGameTime);
            }
        }

        var recastInstance = new RecastInstance(
                getSpellId(),
                spellLevel,
                getRecastCount(spellLevel, entity),
                DURATION_TICKS,
                castSource,
                castData.copy()
        );
        playerMagicData.getPlayerRecasts().addRecast(recastInstance, playerMagicData);
        spawnTunnelParticles(level, castData.positions());
    }

    private void closeTunnel(ServerLevel level, RiftHoleCastData castData) {
        if (!castData.hasTunnel()) {
            return;
        }

        for (var index = castData.positions().size() - 1; index >= 0; --index) {
            var pos = castData.positions().get(index);
            var state = level.getBlockState(pos);
            if (!state.is(BlockRegistry.RIFT_HOLE.get())) {
                continue;
            }

            var blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof RiftHoleBlockEntity riftHoleBlockEntity) || !riftHoleBlockEntity.matchesTunnel(castData.tunnelId())) {
                continue;
            }

            level.setBlockAndUpdate(pos, riftHoleBlockEntity.getOriginalState());
        }

        spawnTunnelParticles(level, castData.positions());
        if (!castData.positions().isEmpty()) {
            level.playSound(null, castData.positions().get(0), SoundRegistry.VANILLA_RIFT_HOLE.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
        }
    }

    private void removeActiveRecast(MagicData playerMagicData) {
        var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
        if (recast != null) {
            playerMagicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }

    private void sendTargetBlockMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.irons_spellbooks.cast_error_target_block").withStyle(ChatFormatting.RED)
            ));
        }
    }

    private void sendCantOpenHoleMessage(LivingEntity entity, @Nullable BlockState failedState) {
        if (entity instanceof ServerPlayer serverPlayer) {
            var targetName = failedState == null ? Blocks.AIR.getName() : failedState.getBlock().getName();
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_open_hole", targetName).withStyle(ChatFormatting.RED)
            ));
        }
    }

    private void sendDimensionNotAllowedMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.rift_hole.dimension_not_allowed").withStyle(ChatFormatting.RED)
            ));
        }
    }

    private void spawnTunnelParticles(ServerLevel level, List<BlockPos> positions) {
        for (var pos : positions) {
            var center = pos.getCenter();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 3, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private record TunnelPlan(List<BlockPos> positions, List<BlockState> originalStates) {
    }

    private record TunnelPlanResult(TunnelPlanStatus status, Optional<TunnelPlan> plan, @Nullable BlockState failedState) {
        private static TunnelPlanResult success(TunnelPlan plan) {
            return new TunnelPlanResult(TunnelPlanStatus.SUCCESS, Optional.of(plan), null);
        }

        private static TunnelPlanResult noTarget() {
            return new TunnelPlanResult(TunnelPlanStatus.NO_TARGET, Optional.empty(), null);
        }

        private static TunnelPlanResult cantOpen(BlockState failedState) {
            return new TunnelPlanResult(TunnelPlanStatus.CANT_OPEN, Optional.empty(), failedState);
        }
    }

    private enum TunnelPlanStatus {
        SUCCESS,
        NO_TARGET,
        CANT_OPEN
    }

    private enum RiftHoleMode {
        NONE,
        OPEN,
        CLOSE
    }

    public static class RiftHoleCastData implements ICastDataSerializable {
        private RiftHoleMode mode = RiftHoleMode.NONE;
        private @Nullable UUID tunnelId;
        private final List<BlockPos> positions = new ArrayList<>();
        private final List<BlockState> originalStates = new ArrayList<>();

        public static RiftHoleCastData createOpenCastData(UUID tunnelId, TunnelPlan tunnelPlan) {
            var castData = new RiftHoleCastData();
            castData.mode = RiftHoleMode.OPEN;
            castData.tunnelId = tunnelId;
            castData.positions.addAll(tunnelPlan.positions());
            castData.originalStates.addAll(tunnelPlan.originalStates());
            return castData;
        }

        public boolean hasTunnel() {
            return tunnelId != null && !positions.isEmpty() && positions.size() == originalStates.size();
        }

        public boolean isOpenCast() {
            return mode == RiftHoleMode.OPEN;
        }

        public boolean isCloseCast() {
            return mode == RiftHoleMode.CLOSE;
        }

        public @Nullable UUID tunnelId() {
            return tunnelId;
        }

        public List<BlockPos> positions() {
            return positions;
        }

        public List<BlockState> originalStates() {
            return originalStates;
        }

        public RiftHoleCastData copy() {
            var copy = new RiftHoleCastData();
            copy.mode = mode;
            copy.tunnelId = tunnelId;
            copy.positions.addAll(positions);
            copy.originalStates.addAll(originalStates);
            return copy;
        }

        public RiftHoleCastData createCloseCastData() {
            var copy = copy();
            copy.mode = RiftHoleMode.CLOSE;
            return copy;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeEnum(mode);
            friendlyByteBuf.writeBoolean(tunnelId != null);
            if (tunnelId == null) {
                return;
            }

            friendlyByteBuf.writeUUID(tunnelId);
            friendlyByteBuf.writeVarInt(positions.size());
            for (var index = 0; index < positions.size(); ++index) {
                friendlyByteBuf.writeBlockPos(positions.get(index));
                friendlyByteBuf.writeNbt(NbtUtils.writeBlockState(originalStates.get(index)));
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            mode = friendlyByteBuf.readEnum(RiftHoleMode.class);
            positions.clear();
            originalStates.clear();
            if (!friendlyByteBuf.readBoolean()) {
                tunnelId = null;
                return;
            }

            tunnelId = friendlyByteBuf.readUUID();
            var size = friendlyByteBuf.readVarInt();
            for (var index = 0; index < size; ++index) {
                positions.add(friendlyByteBuf.readBlockPos());
                var stateTag = friendlyByteBuf.readNbt();
                originalStates.add(stateTag == null
                        ? Blocks.AIR.defaultBlockState()
                        : NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag));
            }
        }

        @Override
        public void reset() {
            mode = RiftHoleMode.NONE;
            tunnelId = null;
            positions.clear();
            originalStates.clear();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            tag.putString("Mode", mode.name());
            if (tunnelId != null) {
                tag.putUUID("TunnelId", tunnelId);
            }

            var positionsTag = new ListTag();
            for (var pos : positions) {
                positionsTag.add(NbtUtils.writeBlockPos(pos));
            }
            tag.put("Positions", positionsTag);

            var statesTag = new ListTag();
            for (var state : originalStates) {
                statesTag.add(NbtUtils.writeBlockState(state));
            }
            tag.put("OriginalStates", statesTag);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            mode = nbt.contains("Mode") ? RiftHoleMode.valueOf(nbt.getString("Mode")) : RiftHoleMode.NONE;
            tunnelId = nbt.hasUUID("TunnelId") ? nbt.getUUID("TunnelId") : null;
            positions.clear();
            originalStates.clear();

            var positionsTag = nbt.getList("Positions", Tag.TAG_COMPOUND);
            for (var index = 0; index < positionsTag.size(); ++index) {
                var posTag = positionsTag.getCompound(index);
                positions.add(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
            }

            var statesTag = nbt.getList("OriginalStates", Tag.TAG_COMPOUND);
            for (var index = 0; index < statesTag.size(); ++index) {
                originalStates.add(NbtUtils.readBlockState(provider.lookupOrThrow(Registries.BLOCK), statesTag.getCompound(index)));
            }
        }
    }
}
