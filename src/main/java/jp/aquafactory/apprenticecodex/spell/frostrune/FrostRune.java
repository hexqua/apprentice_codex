package jp.aquafactory.apprenticecodex.spell.frostrune;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class FrostRune extends AbstractSpell implements jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "frost_rune");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public FrostRune() {
        baseSpellPower = 1200;
        spellPowerPerLevel = 200;
        baseManaCost = 80;
        manaCostPerLevel = 15;
        castTime = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getPlaceRange(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getSurviveTick(spellLevel, caster), 1))

        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FROST_RUNE);
    }

    private int getSurviveTick(int spellLevel, LivingEntity entity) {
        return Math.round(20 * getSpellPower(spellLevel, entity) / 2f);
    }

    private float getPlaceRange(int spellLevel, LivingEntity entity) {
        return 4 + getSpellPower(spellLevel, entity) / 150.0f;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getPlaceRange(spellLevel, entity);
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
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.ICE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_INSCRIBE_MANA.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new FrostRuneCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var placement = findPlaceData(level, spellLevel, entity);
        if (placement.isEmpty()) {
            sendCantPlaceMessage(entity);
            return false;
        }

        var placeData = placement.get();
        var castData = new FrostRuneCastData();
        castData.position = placeData.pos();
        castData.supportFacing = placeData.facing();
        castData.visualNorth = resolveVisualNorth(placeData.pos(), placeData.facing(), entity);
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel,
                                                                         BlockTargetData targetData) {
        return findPlaceData(level, spellLevel, entity, targetData)
                .map(placeData -> ClientPlacementPreviewData.orientedColumn(
                        surfaceCenter(placeData.pos(), placeData.facing()),
                        0.5F,
                        0.35F,
                        placeData.facing().getOpposite()
                ));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            var placement = restorePlacement(level, playerMagicData)
                    .or(() -> findPlaceData(level, spellLevel, entity));
            if (placement.isEmpty()) {
                sendCantPlaceMessage(entity);
            } else {
                var placeData = placement.get();
                var state = BlockRegistry.FROST_RUNE_TRAP.get()
                        .defaultBlockState()
                        .setValue(FrostRuneTrapBlock.FACING, placeData.facing());
                if (!state.canSurvive(level, placeData.pos()) || !level.getBlockState(placeData.pos()).canBeReplaced()) {
                    sendCantPlaceMessage(entity);
                } else {
                    level.setBlock(placeData.pos(), state, 3);
                    if (level.getBlockEntity(placeData.pos()) instanceof FrostRuneTrapBlockEntity trap) {
                        var visualNorth = playerMagicData.getAdditionalCastData() instanceof FrostRuneCastData castData
                                && castData.visualNorth != null
                                ? castData.visualNorth
                                : resolveVisualNorth(placeData.pos(), placeData.facing(), entity);
                        trap.initialize(entity, visualNorth, getDamage(spellLevel, entity), getSurviveTick(spellLevel, entity));
                    }
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            if (playerMagicData.getAdditionalCastData() instanceof FrostRuneCastData castData) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private Optional<BlockTools.PlaceData> restorePlacement(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof FrostRuneCastData castData)
                || castData.position == null || castData.supportFacing == null) {
            return Optional.empty();
        }

        var state = BlockRegistry.FROST_RUNE_TRAP.get()
                .defaultBlockState()
                .setValue(FrostRuneTrapBlock.FACING, castData.supportFacing);
        if (!level.getBlockState(castData.position).canBeReplaced() || !state.canSurvive(level, castData.position)) {
            return Optional.empty();
        }
        return Optional.of(new BlockTools.PlaceData(castData.position, castData.supportFacing));
    }

    private Optional<BlockTools.PlaceData> findPlaceData(Level level, int spellLevel, LivingEntity entity) {
        var range = getPlaceRange(spellLevel, entity);
        var result = BlockTargetingHelper.findClientPlacePos(level, entity, getSpellResource(), range);
        if (result.isEmpty()) {
            result = BlockTools.findPlacePos(level, entity, range);
        }
        return result.flatMap(placeData -> validatePlacement(level, placeData));
    }

    private Optional<BlockTools.PlaceData> findPlaceData(Level level, int spellLevel, LivingEntity entity, BlockTargetData targetData) {
        if (targetData != null) {
            var validatedTarget = BlockTargetingHelper.validateTarget(level, entity, getPlaceRange(spellLevel, entity), targetData);
            if (validatedTarget.isPresent()) {
                var placeData = new BlockTools.PlaceData(
                        validatedTarget.get().getPlacePos(),
                        validatedTarget.get().getPlaceFacing()
                );
                var validatedPlacement = validatePlacement(level, placeData);
                if (validatedPlacement.isPresent()) {
                    return validatedPlacement;
                }
            }
        }
        return findPlaceData(level, spellLevel, entity);
    }

    private Optional<BlockTools.PlaceData> validatePlacement(Level level, BlockTools.PlaceData placeData) {
        if (placeData.pos() == null || placeData.facing() == null || !level.getBlockState(placeData.pos()).canBeReplaced()) {
            return Optional.empty();
        }
        var state = BlockRegistry.FROST_RUNE_TRAP.get()
                .defaultBlockState()
                .setValue(FrostRuneTrapBlock.FACING, placeData.facing());
        return state.canSurvive(level, placeData.pos()) ? Optional.of(placeData) : Optional.empty();
    }

    private Direction resolveVisualNorth(BlockPos placePos, Direction supportFacing, LivingEntity entity) {
        if (!supportFacing.getAxis().isVertical()) {
            return Direction.DOWN;
        }

        var center = placePos.getCenter();
        var dx = entity.getX() - center.x;
        var dz = entity.getZ() - center.z;
        if (Math.abs(dx) > Math.abs(dz) && Math.abs(dx) > 1.0E-4D) {
            return dx > 0.0D ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(dz) > 1.0E-4D) {
            return dz > 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
        return entity.getDirection().getOpposite();
    }

    private Vec3 surfaceCenter(BlockPos pos, Direction supportFacing) {
        return pos.getCenter().add(Vec3.atLowerCornerOf(supportFacing.getNormal()).scale(0.5D));
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class FrostRuneCastData implements ICastDataSerializable {
        private BlockPos position;
        private Direction supportFacing;
        private Direction visualNorth;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(position != null && supportFacing != null && visualNorth != null);
            if (position != null && supportFacing != null && visualNorth != null) {
                friendlyByteBuf.writeBlockPos(position);
                friendlyByteBuf.writeEnum(supportFacing);
                friendlyByteBuf.writeEnum(visualNorth);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            if (!friendlyByteBuf.readBoolean()) {
                reset();
                return;
            }
            position = friendlyByteBuf.readBlockPos();
            supportFacing = friendlyByteBuf.readEnum(Direction.class);
            visualNorth = friendlyByteBuf.readEnum(Direction.class);
        }

        @Override
        public void reset() {
            position = null;
            supportFacing = null;
            visualNorth = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            if (position != null && supportFacing != null && visualNorth != null) {
                tag.putInt("PositionX", position.getX());
                tag.putInt("PositionY", position.getY());
                tag.putInt("PositionZ", position.getZ());
                tag.putInt("SupportFacing", supportFacing.get3DDataValue());
                tag.putInt("VisualNorth", visualNorth.get3DDataValue());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            if (!nbt.contains("PositionX")) {
                reset();
                return;
            }
            position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
            supportFacing = Direction.from3DDataValue(nbt.getInt("SupportFacing"));
            visualNorth = Direction.from3DDataValue(nbt.getInt("VisualNorth"));
        }
    }
}
