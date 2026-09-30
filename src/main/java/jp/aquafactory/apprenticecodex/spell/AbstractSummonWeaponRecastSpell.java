package jp.aquafactory.apprenticecodex.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
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

public abstract class AbstractSummonWeaponRecastSpell<T extends SummonWeaponEntity> extends AbstractSpell {
    public enum CompleteRecastTypes {
        RELEASE_WEAPON,
        KEEP_WEAPON,
        DISCARD_WEAPON
    }

    private final Class<T> weaponType;

    protected AbstractSummonWeaponRecastSpell(Class<T> weaponType) {
        this.weaponType = weaponType;
    }

    public abstract int getActivateCount(int spellLevel, @Nullable LivingEntity entity);
    public abstract int getDurationTick();

    @Override
    public final Optional<SoundEvent> getCastStartSound() {
        // 初回とRecastで音を分けるため、onServerPreCastで再生する。
        return Optional.empty();
    }

    @Override
    public final Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    public abstract Optional<SoundEvent> getPreFireSound();
    public abstract Optional<SoundEvent> getPreSummonSound();
    public abstract Optional<SoundEvent> getFireSound();
    public abstract Optional<SoundEvent> getSummonSound();

    @Override
    public final int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return getActivateCount(spellLevel, entity) + 1;
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new SummonWeaponRecastSpellData();
    }

    protected abstract boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                                    MagicData data, @NotNull T weapon);
    protected abstract boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData data);
    public abstract CompleteRecastTypes onRecastFinishedWithWeapon(Level level, ServerPlayer player,
                                                                  @NotNull T weapon, RecastInstance recast,
                                                                  RecastResult result);
    public abstract void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData data, @NotNull T weapon);
    public abstract T onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData data);

    protected PendingSummonCastData createPendingCastData(Level level, @Nullable T weapon) {
        return new PendingSummonCastData(level, weapon);
    }

    protected boolean isSummonWeaponValid(T weapon) {
        return true;
    }

    @Override
    public final void onRecastFinished(ServerPlayer player, RecastInstance recast, RecastResult result,
                                      ICastDataSerializable rawData) {
        var data = MagicData.getPlayerMagicData(player);
        // Recastはこのコールバックの前に管理表から消えるので、渡されたセッションと照合する。
        // 使い切りは正常完了の途中に発生するため、その詠唱を中断しない。
        if (result != RecastResult.USED_ALL_RECASTS && data.isCasting()
                && getSpellId().equals(data.getCastingSpellId())
                && data.getAdditionalCastData() instanceof PendingSummonCastData pending
                && rawData instanceof SummonWeaponRecastSpellData session && pending.matches(session)) {
            cancelPendingCast(player.level(), player, data);
        }
        if (rawData instanceof SummonWeaponRecastSpellData session) {
            var weapon = resolveWeapon(player.level(), session);
            if (weapon != null) {
                switch (onRecastFinishedWithWeapon(player.level(), player, weapon, recast, result)) {
                    case RELEASE_WEAPON -> weapon.releaseWeapon();
                    case DISCARD_WEAPON -> weapon.discard();
                    case KEEP_WEAPON -> { }
                }
            }
        }
        super.onRecastFinished(player, recast, result, rawData);
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData data) {
        if (!(level instanceof ServerLevel) || data == null) return false;
        data.resetAdditionalCastData();
        var recast = data.getPlayerRecasts().getRecastInstance(getSpellId());
        var weapon = getSummonEntityFromMagicData(data, level);
        if (recast != null && weapon == null) {
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                        "ui.apprenticecodex.recast_summon_spell.no_weapon", getDisplayName(player))
                        .withStyle(ChatFormatting.RED)));
            }
            data.getPlayerRecasts().removeRecast(recast, RecastResult.USER_CANCEL);
            onPreRecastNoWeapon(level, spellLevel, entity, data);
            return false;
        }
        data.setAdditionalCastData(createPendingCastData(level, weapon));
        boolean allowed = weapon == null
                ? super.checkPreCastConditions(level, spellLevel, entity, data)
                : onPreRecastWithWeapon(level, spellLevel, entity, data, weapon);
        if (!allowed) data.resetAdditionalCastData();
        return allowed;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData data) {
        if (data == null) return;
        if (!(data.getAdditionalCastData() instanceof PendingSummonCastData)
                && !checkPreCastConditions(level, spellLevel, entity, data)) {
            cancelPendingCast(level, entity, data);
            return;
        }
        super.onServerPreCast(level, spellLevel, entity, data);
        var sound = getSummonEntityFromMagicData(data, level) == null ? getPreSummonSound() : getPreFireSound();
        sound.ifPresent(event -> AudioTools.playSoundFromEntity(level, entity, event, SoundSource.PLAYERS, 2.0f));
    }

    @Override
    public final void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean triggerCooldown) {
        var data = MagicData.getPlayerMagicData(player);
        // onCastで拒否するだけではIron'sのマナ消費が先に実行されてしまう。
        if (!isPendingSessionValid(level, data)) {
            finishMissingWeaponSession(level, data);
            cancelPendingCast(level, player, data);
            return;
        }
        super.castSpell(level, spellLevel, player, source, triggerCooldown);
    }

    private boolean isPendingSessionValid(Level level, MagicData data) {
        if (!(data.getAdditionalCastData() instanceof PendingSummonCastData pending)
                || !level.dimension().location().equals(pending.dimension)) return false;
        var recast = data.getPlayerRecasts().getRecastInstance(getSpellId());
        if (pending.weaponId == null) return recast == null;
        return recast != null && recast.getCastData() instanceof SummonWeaponRecastSpellData session
                && pending.matches(session) && getSummonEntityFromMagicData(data, level) != null;
    }

    private void finishMissingWeaponSession(Level level, MagicData data) {
        var recast = data.getPlayerRecasts().getRecastInstance(getSpellId());
        if (recast != null && getSummonEntityFromMagicData(data, level) == null) {
            data.getPlayerRecasts().removeRecast(recast, RecastResult.USER_CANCEL);
        }
    }

    private void cancelPendingCast(Level level, LivingEntity entity, MagicData data) {
        if (!data.isCasting() || !getSpellId().equals(data.getCastingSpellId())) return;
        if (entity instanceof ServerPlayer player && MagicData.getPlayerMagicData(player) == data) {
            MagicTools.cancelCasting(player, false);
        } else {
            // リモートの独立MagicDataを取り消す際、所有者の別の詠唱を中断しない。
            onServerCastComplete(level, data.getCastingSpellLevel(), entity, data, true);
        }
    }

    @Override
    public final void onCast(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData data) {
        if (!isPendingSessionValid(level, data)) {
            finishMissingWeaponSession(level, data);
            cancelPendingCast(level, entity, data);
            return;
        }
        var pending = (PendingSummonCastData) data.getAdditionalCastData();
        if (pending.weaponId != null) {
            var weapon = getSummonEntityFromMagicData(data, level);
            if (weapon == null) return;
            getFireSound().ifPresent(event -> AudioTools.playSoundFromEntity(level, entity, event, SoundSource.PLAYERS, 2.0f));
            onCastWithWeapon(level, spellLevel, entity, data, weapon);
        } else {
            var session = new SummonWeaponRecastSpellData();
            var weapon = onCastNoWeapon(level, spellLevel, entity, data);
            session.setEntity(weapon);
            data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity),
                    getDurationTick(), source, session), data);
            getSummonSound().ifPresent(event -> AudioTools.playSoundFromEntity(level, entity, event, SoundSource.PLAYERS, 2.0f));
        }
        super.onCast(level, spellLevel, entity, source, data);
    }

    @Nullable
    private T resolveWeapon(Level level, SummonWeaponRecastSpellData session) {
        if (!(level instanceof ServerLevel server)) return null;
        var entity = session.getEntity(server);
        if (!weaponType.isInstance(entity)) return null;
        var weapon = weaponType.cast(entity);
        return isSummonWeaponValid(weapon) ? weapon : null;
    }

    @Nullable
    protected final T getSummonEntityFromMagicData(@Nullable MagicData data, Level level) {
        if (data == null) return null;
        var recast = data.getPlayerRecasts().getRecastInstance(getSpellId());
        return recast != null && recast.getCastData() instanceof SummonWeaponRecastSpellData session
                ? resolveWeapon(level, session) : null;
    }

    public static class PendingSummonCastData implements ICastData {
        @Nullable
        private UUID weaponId;
        private final ResourceLocation dimension;

        protected PendingSummonCastData(Level level, @Nullable Entity weapon) {
            weaponId = weapon == null ? null : weapon.getUUID();
            dimension = level.dimension().location();
        }

        private boolean matches(SummonWeaponRecastSpellData session) {
            return weaponId != null && weaponId.equals(session.entityId)
                    && (session.dimension == null || dimension.equals(session.dimension));
        }

        @Override
        public void reset() {
            weaponId = null;
        }
    }

    public static class SummonWeaponRecastSpellData implements ICastDataSerializable {
        @Nullable
        private UUID entityId;
        @Nullable
        private ResourceLocation dimension;

        public void setEntity(Entity entity) {
            entityId = entity.getUUID();
            dimension = entity.level().dimension().location();
        }

        @Nullable
        public Entity getEntity(ServerLevel level) {
            // 旧NBTにはDimensionがない。UUIDの解決は現在レベルに限定し、チャンクはロードしない。
            if (entityId == null || (dimension != null && !dimension.equals(level.dimension().location()))) return null;
            var entity = level.getEntity(entityId);
            return entity != null && entity.isAlive() && !entity.isRemoved() && entity.level() == level
                    && level.hasChunkAt(entity.blockPosition()) ? entity : null;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeBoolean(entityId != null);
            if (entityId != null) {
                buffer.writeUUID(entityId);
                buffer.writeBoolean(dimension != null);
                if (dimension != null) buffer.writeResourceLocation(dimension);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            reset();
            if (buffer.readBoolean()) {
                entityId = buffer.readUUID();
                if (buffer.readBoolean()) dimension = buffer.readResourceLocation();
            }
        }

        @Override
        public void reset() {
            entityId = null;
            dimension = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            if (entityId != null) tag.putUUID("Entity", entityId);
            if (dimension != null) tag.putString("Dimension", dimension.toString());
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            reset();
            entityId = tag.hasUUID("Entity") ? tag.getUUID("Entity") : null;
            if (tag.contains("Dimension")) dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        }
    }
}
