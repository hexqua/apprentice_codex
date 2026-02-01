package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CommenceFire extends AbstractSpell {
    @SuppressWarnings("removal")
    private final ResourceLocation spellId = new ResourceLocation(ApprenticeCodex.MODID, "commence_fire");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(45)
            .build();

    public CommenceFire() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        manaCostPerLevel = 25;
        baseManaCost = 150;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.recast_count", getBulletCount(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.headshot_damage_multiplier", getHeadshotPercent(spellLevel, caster)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getOverSpellPower(int spellLevel, LivingEntity entity){
        return getSpellPower(spellLevel, entity) - baseSpellPower;
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return 3 * (getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getBulletCount(int spellLevel, LivingEntity entity) {
        return Math.min(10, 4 + Math.round(2 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
    }

    private int getDuration() {
        return 20 * 10;
    }

    private int getRange(){
        // DMRイメージなので中距離(4チャンク程度)
        return 16 * 4;
    }

    private int getHeadshotPercent(int spellLevel, LivingEntity entity) {
        return Math.min(300, 150 + Math.round(25 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
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
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        if (entity == null) {
            return getCastTime(spellLevel);
        }

        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null){
            return super.getEffectiveCastTime(spellLevel, entity);
        }

        var recasts = magicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)){
            return super.getEffectiveCastTime(spellLevel, entity);
        }

        // 射撃は固定値(0.5秒)
        return 10;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        // 再詠唱で制御できなさそうなのでこちらは音を無しにする.
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // 再詠唱で制御できなさそうなのでこちらは音を無しにする.
        return Optional.empty();
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        var summon = getCommenceFireEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 2.0f);
        } else {
            AudioTools.playSoundFromEntity(level, entity, getSchoolType().getCastSound(), SoundSource.PLAYERS, 2.0f);
        }
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        // 初回発動含め弾の数にする.
        return getBulletCount(spellLevel, entity) + 1;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new CommenceFireCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (castDataSerializable instanceof CommenceFireCastData castData) {
            var serverLevel = serverPlayer.serverLevel();
            var entity = castData.getEntity(serverLevel);
            if(entity != null){
                entity.discard();
            }
        }

        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        var summon = getCommenceFireEntityFromMagicData(playerMagicData, level);
        if (summon == null) {
            return;
        }

        var range = getRange();
        var result = RaycastTools.raycastFromEye(entity, range, e -> CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(e), entity));

        // 上の判定式で非nullが保証.
        //noinspection DataFlowIssue
        var castTick = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        summon.setCastingReticleEffect(castTick, playerMagicData.getCastDuration(), result.hitPosition());
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summon = getCommenceFireEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            if (summon.duringRecoil()) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.commence_fire.during_recoil", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                }
                return false;
            }
        } else if(playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.commence_fire.no_rifle", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                if (recast.getRemainingRecasts() > 0) {
                    playerMagicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
                }
            }

            return false;
        }

        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            var summon = getCommenceFireEntityFromMagicData(playerMagicData, level);
            if (summon != null) {
                var range = getRange();
                var result = RaycastTools.raycastFromEye(entity, range, e -> CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(e), entity));
                var isHeadShot = result.hitEntity() instanceof LivingEntity living && CombatTools.isHeadShot(living, result.hitPosition());
                if (result.hitEntity() != null) {
                    summon.damageTarget(result.hitEntity(), isHeadShot, level);
                }

                var hitType = switch (result.hitType()) {
                    case NONE -> CommenceFireRifleEntity.HitTypes.MISS;
                    case BLOCK -> CommenceFireRifleEntity.HitTypes.BLOCK;
                    case LIVING_ENTITY -> CommenceFireRifleEntity.HitTypes.ENTITY;
                };

                summon.fire(result.hitPosition(), level, hitType, isHeadShot);
            }
        } else {
            var castData = new CommenceFireCastData();
            var summonWeapon = new CommenceFireRifleEntity(EntityRegistry.COMMENCE_FIRE_RIFLE.get(), level, entity);
            summonWeapon.locateAimingPosition();
            summonWeapon.setDamage(getDamage(spellLevel, entity), getHeadshotPercent(spellLevel, entity));
            castData.setEntity(summonWeapon);
            level.addFreshEntity(summonWeapon);

            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getDuration(), castSource, castData);
            recasts.addRecast(recastInstance, playerMagicData);
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 2.0f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private CommenceFireRifleEntity getCommenceFireEntityFromMagicData(MagicData playerMagicData, Level level){
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

        if (!(recast.getCastData() instanceof CommenceFireCastData castData)) {
            return null;
        }

        if (!(castData.getEntity(serverLevel) instanceof CommenceFireRifleEntity summon)) {
            return null;
        }

        return summon;
    }

    public class CommenceFireCastData implements ICastDataSerializable {
        private UUID entityId;

        public void setEntity(Entity entity){
            entityId = entity.getUUID();
        }

        public Entity getEntity(ServerLevel level){
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
