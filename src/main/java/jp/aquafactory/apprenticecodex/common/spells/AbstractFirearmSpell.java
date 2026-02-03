package jp.aquafactory.apprenticecodex.common.spells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractFirearmSpell<T extends SummonWeaponEntity> extends AbstractSpell {

    private final Class<T> weaponType;

    protected AbstractFirearmSpell(Class<T> weaponType) {
        super();
        this.weaponType = weaponType;
    }

    public abstract int getBulletCount(int spellLevel, @Nullable LivingEntity entity);

    public abstract int getDurationTick();

    @Override
    public final Optional<SoundEvent> getCastStartSound() {
        // 再詠唱で制御できなさそうなのでこちらは音を無しにする.
        return Optional.empty();
    }

    @Override
    public final Optional<SoundEvent> getCastFinishSound() {
        // 再詠唱で制御できなさそうなのでこちらは音を無しにする.
        return Optional.empty();
    }

    public abstract Optional<SoundEvent> getPreFireSound();
    public abstract Optional<SoundEvent> getPreSummonSound();
    public abstract Optional<SoundEvent> getFireSound();
    public abstract Optional<SoundEvent> getSummonSound();

    @Override
    public final int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        // 初回発動含め弾の数にする.
        return getBulletCount(spellLevel, entity) + 1;
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new FirearmCastData();
    }

    protected abstract boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull T weapon);

    protected abstract boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData);

    @Override
    public final void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (castDataSerializable instanceof FirearmCastData castData) {
            var serverLevel = serverPlayer.serverLevel();
            var entity = castData.getEntity(serverLevel);
            if (weaponType.isInstance(entity)) {
                weaponType.cast(entity).releaseWeapon();
            }
        }

        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summon = getFirearmEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            return onPreRecastWithWeapon(level, spellLevel, entity, playerMagicData, summon);
        } else if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.firearm_spell.no_firearm", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                if (recast.getRemainingRecasts() > 0) {
                    playerMagicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
                }
            }

            return onPreRecastNoWeapon(level, spellLevel, entity, playerMagicData);
        }

        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    public abstract void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull T weapon);
    public abstract T onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData);

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        var summon = getFirearmEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            var sound = getPreFireSound();
            sound.ifPresent(soundEvent -> AudioTools.playSoundFromEntity(level, entity, soundEvent, SoundSource.PLAYERS, 2.0f));
        } else {
            var sound = getPreSummonSound();
            sound.ifPresent(soundEvent -> AudioTools.playSoundFromEntity(level, entity, soundEvent, SoundSource.PLAYERS, 2.0f));
        }
    }

    @Override
    public final void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            var summon = getFirearmEntityFromMagicData(playerMagicData, level);
            if (summon != null) {
                var sound = getFireSound();
                sound.ifPresent(soundEvent -> AudioTools.playSoundFromEntity(level, entity, soundEvent, SoundSource.PLAYERS, 2.0f));
                onCastWithWeapon(level, spellLevel, entity, playerMagicData, summon);
            } else {
                ApprenticeCodex.LOGGER.error("Failed to get firearm entity from magic data.");
            }
        } else {
            var castData = new FirearmCastData();
            var summon = onCastNoWeapon(level, spellLevel, entity, playerMagicData);
            castData.setEntity(summon);

            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getDurationTick(), castSource, castData);
            recasts.addRecast(recastInstance, playerMagicData);

            var sound = getSummonSound();
            sound.ifPresent(soundEvent -> AudioTools.playSoundFromEntity(level, entity, soundEvent, SoundSource.PLAYERS, 2.0f));
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }


    protected final T getFirearmEntityFromMagicData(MagicData playerMagicData, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (playerMagicData == null) {
            return null;
        }

        var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
        if (recast == null) {
            return null;
        }

        if (!(recast.getCastData() instanceof FirearmCastData castData)) {
            return null;
        }

        var summon = castData.getEntity(serverLevel);
        if (summon == null) {
            return null;
        }

        if (!weaponType.isInstance(summon)) {
            return null;
        }

        return weaponType.cast(summon);
    }

    public static class FirearmCastData implements ICastDataSerializable {
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
