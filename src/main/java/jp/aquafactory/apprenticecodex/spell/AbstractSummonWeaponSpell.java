package jp.aquafactory.apprenticecodex.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.core.HolderLookup;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractSummonWeaponSpell<T extends SummonWeaponEntity> extends AbstractSpell {

    public enum CompleteCastTypes{
        RELEASE_WEAPON,
        KEEP_WEAPON,
    }

    private final Class<T> weaponType;

    protected AbstractSummonWeaponSpell(Class<T> weaponType) {
        super();
        this.weaponType = weaponType;
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return createCastData();
    }

    protected SummonWeaponSpellCastData createCastData() {
        return new SummonWeaponSpellCastData();
    }

    public abstract T onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData);
    // pre-cast で生成済みの武器を、本番の castSpell 実行時にだけ有効化する魔法向け。
    protected void onInitialCastWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                           MagicData playerMagicData, @NotNull T weapon) {
    }
    public abstract void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull T weapon);
    public abstract CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull T weapon);

    // 詠唱tickを経由しない発動でも、攻撃に必須の準備を完了させる。演出の時間は進めない。
    protected void prepareWeaponForRelease(Level level, int spellLevel, LivingEntity entity,
                                           MagicData magicData, @NotNull T weapon) {
    }

    // 呼び出し元で事前条件の判定前からAdditionalCastDataを分離し、終了時に元の参照へ戻す。
    public final void castInstantWeapon(ServerLevel level, int spellLevel, ServerPlayer player,
                                        CastSource castSource, MagicData magicData) {
        if (getCastType() == CastType.CONTINUOUS) {
            throw new IllegalStateException("Continuous weapons cannot be cast through the instant weapon path");
        }
        T weapon = null;
        boolean completed = false;
        try {
            onServerPreCast(level, spellLevel, player, magicData);
            weapon = getFirearmEntityFromMagicData(magicData, level);
            if (weapon == null) {
                throw new IllegalStateException("Instant weapon cast did not create a weapon");
            }
            MulticastEchoStaffAttackHandler.trackWeaponAttack(weapon);
            castSpell(level, spellLevel, player, castSource, false);
            completeWeaponCast(level, spellLevel, player, magicData, false, weapon);
            completed = true;
        } finally {
            if (!completed) {
                var abandoned = weapon != null ? weapon : getFirearmEntityFromMagicData(magicData, level);
                if (abandoned != null) {
                    abandoned.releaseWeapon();
                }
            }
        }
    }

    private void completeWeaponCast(Level level, int spellLevel, LivingEntity entity, MagicData magicData,
                                    boolean cancelled, T weapon) {
        if (!cancelled) {
            prepareWeaponForRelease(level, spellLevel, entity, magicData, weapon);
        }
        if (onCastCompleteWithWeapon(level, spellLevel, entity, magicData, cancelled, weapon)
                == CompleteCastTypes.RELEASE_WEAPON) {
            weapon.releaseWeapon();
        }
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (getCastType() != CastType.CONTINUOUS && playerMagicData != null) {
            if (!(playerMagicData.getAdditionalCastData() instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData)) {
                var castData = createCastData();
                var summon = onCastNoWeapon(level, spellLevel, entity, playerMagicData);
                castData.setEntity(summon);
                playerMagicData.setAdditionalCastData(castData);
            }
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public final void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        var summon = getFirearmEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            onCastTickWithWeapon(level, spellLevel, entity, playerMagicData, summon);
        }

        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public final void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (getCastType() == CastType.CONTINUOUS) {
            if (!(playerMagicData.getAdditionalCastData() instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData)) {
                var castData = createCastData();
                var summon = onCastNoWeapon(level, spellLevel, entity, playerMagicData);
                castData.setEntity(summon);
                playerMagicData.setAdditionalCastData(castData);
            }
        }

        if (getCastType() != CastType.CONTINUOUS) {
            var summon = getFirearmEntityFromMagicData(playerMagicData, level);
            if (summon != null) {
                onInitialCastWithWeapon(level, spellLevel, entity, playerMagicData, summon);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public final void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        var summon = getFirearmEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            completeWeaponCast(level, spellLevel, entity, playerMagicData, cancelled, summon);
        }

        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    protected final T getFirearmEntityFromMagicData(MagicData playerMagicData, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (playerMagicData == null) {
            return null;
        }

        if (!(playerMagicData.getAdditionalCastData() instanceof AbstractSummonWeaponSpell.SummonWeaponSpellCastData castData)) {
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

    public static class SummonWeaponSpellCastData implements ICastDataSerializable {
        private UUID entityId;

        public void setEntity(Entity entity) {
            entityId = entity.getUUID();
        }

        public Entity getEntity(ServerLevel level) {
            if (entityId == null) {
                return null;
            }
            return level.getEntity(entityId);
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            var hasEntity = entityId != null;
            friendlyByteBuf.writeBoolean(hasEntity);
            if (hasEntity) {
                friendlyByteBuf.writeUUID(entityId);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            entityId = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readUUID() : null;
        }

        @Override
        public void reset() {
            entityId = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            var tag = new CompoundTag();
            if (entityId != null) {
                tag.putUUID("Entity", entityId);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
            entityId = nbt.hasUUID("Entity") ? nbt.getUUID("Entity") : null;
        }
    }
}
