package jp.aquafactory.apprenticecodex.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
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
            var result = onCastCompleteWithWeapon(level, spellLevel, entity, playerMagicData, cancelled, summon);
            if (result == CompleteCastTypes.RELEASE_WEAPON){
                summon.releaseWeapon();
            }
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
