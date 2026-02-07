package jp.aquafactory.apprenticecodex.common.spells.arcanebeam;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArcaneBeam extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_beam");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public ArcaneBeam() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        baseManaCost = 5;
        manaCostPerLevel = 10;
        castTime = 100;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // todo:バランス調整.
        return 1;
    }

    private double getRange(){
        return 32;
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
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.BLACK_HOLE_CAST.get());
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
    public final ICastDataSerializable getEmptyCastData() {
        return new BeamCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof BeamCastData)) {
            var castData = new BeamCastData();
            var beam = EntityRegistry.ARCANE_BEAM.get().create(level);
            if (beam == null) {
                return;
            }

            beam.moveTo(entity.getX(), entity.getEyeY() - 0.2, entity.getZ(), entity.getYRot(), entity.getXRot());
            beam.setOwner(entity);
            beam.setup(0x4433D6FF, 0xFFFFFFFF, 64, 0.1f);
            level.addFreshEntity(beam);

            castData.setEntity(beam);
            playerMagicData.setAdditionalCastData(castData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData.getAdditionalCastData() instanceof BeamCastData castData && level instanceof ServerLevel serverLevel) {
            var beam = castData.getEntity(serverLevel);
            if (beam == null) {
                return;
            }

            beam.discard();
        }

        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    public static class BeamCastData implements ICastDataSerializable {
        private UUID entityId;

        public void setEntity(Entity entity) {
            entityId = entity.getUUID();
        }

        public Entity getEntity(ServerLevel level) {
            return level.getEntity(entityId);
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeUUID(entityId);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            entityId = friendlyByteBuf.readUUID();
        }

        @Override
        public void reset() {
            entityId = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putUUID("Entity", entityId);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            entityId = nbt.getUUID("Entity");
        }
    }
}
