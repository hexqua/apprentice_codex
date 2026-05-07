package jp.aquafactory.apprenticecodex.spell.personalshelf;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class PersonalShelf extends AbstractSpell implements jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "personal_shelf");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public PersonalShelf() {
        baseSpellPower = 100;
        spellPowerPerLevel = 0;
        baseManaCost = 100;
        manaCostPerLevel = 0;
        castTime = 20;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDurationTicks(), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1))
        );
    }

    private int getDurationTicks(){
        return 20 * 60;
    }

    private double getRange(){
        return 8;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange();
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
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.BLACK_HOLE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_INTERFACE_OPEN.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new PersonalShelfCastData();
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var position = findPlaceData(level, entity);
        if (position.isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        var castData = new PersonalShelfCastData();
        castData.position = position.get().pos();
        castData.exportFacing = position.get().facing();
        castData.exportMode = entity.isShiftKeyDown();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel, BlockTargetData targetData) {
        return findPlaceData(level, entity, targetData)
                .map(placeData -> ClientPlacementPreviewData.singleBlockColumn(new Vec3(
                        placeData.pos().getX() + 0.5,
                        placeData.pos().getY(),
                        placeData.pos().getZ() + 0.5
                )));
    }

    private Optional<BlockTools.PlaceData> findPlaceData(Level level, LivingEntity entity) {
        var result = BlockTargetingHelper.findClientPlacePos(level, entity, getSpellResource(), getRange());
        if (result.isEmpty()) {
            result = BlockTools.findPlacePos(level, entity, getRange());
        }
        return result;
    }

    private Optional<BlockTools.PlaceData> findPlaceData(Level level, LivingEntity entity, BlockTargetData targetData) {
        if (targetData != null) {
            var validatedTarget = BlockTargetingHelper.validateTarget(level, entity, getRange(), targetData);
            if (validatedTarget.isPresent()) {
                return Optional.of(new BlockTools.PlaceData(
                        validatedTarget.get().getPlacePos(),
                        validatedTarget.get().getPlaceFacing()
                ));
            }
        }

        return findPlaceData(level, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var castData = (PersonalShelfCastData) playerMagicData.getAdditionalCastData();
        if (castData != null) {
            if (castData.position != null) {
                var placeState = level.getBlockState(castData.position);
                if (!placeState.canBeReplaced()) {
                    if (entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                    }
                } else if (entity instanceof Player caster) {
                    level.setBlockAndUpdate(castData.position, BlockRegistry.PERSONAL_SHELF_CHEST.get().defaultBlockState());
                    var check = level.getBlockEntity(castData.position);
                    if (check instanceof PersonalShelfChestBlockEntity chestEntity) {
                        chestEntity.setShelfData(
                                caster,
                                castData.exportMode,
                                castData.exportFacing
                        );
                        chestEntity.setLifeData(
                                getDurationTicks(),
                                getRange()
                        );

                        var effectCenter = castData.position.getCenter();
                        if (level instanceof ServerLevel server) {
                            server.sendParticles(ParticleTypes.REVERSE_PORTAL, effectCenter.x, effectCenter.y, effectCenter.z, 32, 0.5, 0.5, 0.5, 0.05);
                        }
                    }
                }
            }

        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            var castData = (PersonalShelfCastData) playerMagicData.getAdditionalCastData();
            if (castData != null) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    public static class PersonalShelfCastData implements ICastDataSerializable {
        private BlockPos position;
        private boolean exportMode;
        private Direction exportFacing;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBlockPos(position);
            friendlyByteBuf.writeBoolean(exportMode);
            friendlyByteBuf.writeInt(exportFacing.get3DDataValue());
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            position = friendlyByteBuf.readBlockPos();
            exportMode = friendlyByteBuf.readBoolean();
            exportFacing = Direction.from3DDataValue(friendlyByteBuf.readInt());
        }

        @Override
        public void reset() {
            position = null;
            exportMode = false;
            exportFacing = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putInt("PositionX", position.getX());
            tag.putInt("PositionY", position.getY());
            tag.putInt("PositionZ", position.getZ());
            tag.putBoolean("ExportMode", exportMode);
            tag.putInt("ExportFacing", exportFacing.get3DDataValue());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
            exportMode = nbt.getBoolean("ExportMode");
            exportFacing = Direction.from3DDataValue(nbt.getInt("ExportFacing"));
        }
    }
}
