package jp.aquafactory.apprenticecodex.common.spells.personalshelf;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

public class PersonalShelf extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "personal_shelf");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public PersonalShelf() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 100;
        manaCostPerLevel = 100;
        castTime = 20;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private double getRange(){
        return 8;
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
        return Optional.of(SoundEvents.ENDER_CHEST_OPEN);
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
        var position = findPlacePos(level, entity);
        if (position.isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        var castData = new PersonalShelfCastData();
        castData.position = position.get();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var castData = (PersonalShelfCastData) playerMagicData.getAdditionalCastData();
        if (castData != null) {
            var targetPosition = castData.position;
            if (targetPosition != null) {
                var placeState = level.getBlockState(targetPosition);
                if (!placeState.canBeReplaced()) {
                    if (entity instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                    }
                } else {
                    // todo:パーソナルシェルフにデータを色々渡すようにする.
                    level.setBlockAndUpdate(targetPosition, BlockRegistry.PERSONAL_SHELF_CHEST.get().defaultBlockState());
                    AudioTools.playSoundFromBlock(level, targetPosition.getCenter(), SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS);

                    if (level instanceof ServerLevel server) {
                        var position = targetPosition.getCenter();
                        server.sendParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y, position.z, 32, 0.5, 0.5, 0.5, 0.05);
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

    private Optional<BlockPos> findPlacePos(Level level, LivingEntity entity) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(getRange()));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }

        var hitPos = hit.getBlockPos();
        var hitState = level.getBlockState(hitPos);
        var placePos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hit.getDirection());
        var placeState = level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return Optional.empty();
        }

        return Optional.of(placePos);
    }

    public static class PersonalShelfCastData implements ICastDataSerializable {
        private BlockPos position;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBlockPos(position);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            position = friendlyByteBuf.readBlockPos();
        }

        @Override
        public void reset() {
            position = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putInt("PositionX", position.getX());
            tag.putInt("PositionY", position.getY());
            tag.putInt("PositionZ", position.getZ());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
        }
    }
}
