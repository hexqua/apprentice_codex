package jp.aquafactory.apprenticecodex.common.spells.quickarms;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
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

public class QuickArms extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quick_arms");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public QuickArms() {
        // todo:バランス調整.
        baseSpellPower = 100;
        spellPowerPerLevel = 10;
        manaCostPerLevel = 5;
        baseManaCost = 20;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.recast_count", getBulletCount(spellLevel, caster)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        // todo:バランス調整.
        return 4;
    }

    private int getBulletCount(int spellLevel, LivingEntity entity) {
        // todo:バランス調整.
        return 2;
    }

    private int getDuration() {
        // todo:バランス調整.
        return 20 * 3;
    }

    private int getRange(){
        // ハンドガンイメージなので近距離(2チャンク程度)
        return 16 * 2;
    }

    private int getFirstDelay(){
        // todo:バランス調整.
        return 20;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(getSchoolType().getCastSound());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        // 初回発動含め弾の数にする.
        return getBulletCount(spellLevel, entity) + 1;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new QuickArmsCastData();
    }


    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (castDataSerializable instanceof QuickArmsCastData castData) {
            var serverLevel = serverPlayer.serverLevel();
            var entity = castData.getEntity(serverLevel);
            if(entity != null){
                entity.discard();
            }
        }

        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summon = getQuickArmsEntityFromMagicData(playerMagicData, level);
        if (summon != null) {
            if (!summon.canFire()){
                if (entity instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.quick_arms.during_standby", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
                }
                return false;
            }
        } else if(playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.firearm_spell.no_firearm", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
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
            var summon = getQuickArmsEntityFromMagicData(playerMagicData, level);
            if (summon != null) {
                summon.fire(level);
            }
        } else {
            var castData = new QuickArmsCastData();
            var summonWeapon = new QuickArmsHandgunEntity(EntityRegistry.QUICK_ARMS_HANDGUN.get(), level, entity);

            summonWeapon.locateAimingPosition();
            summonWeapon.setDamage(getDamage(spellLevel, entity));
            summonWeapon.setRange(getRange());
            summonWeapon.setFireStandby(getFirstDelay());

            castData.setEntity(summonWeapon);
            level.addFreshEntity(summonWeapon);

            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getDuration(), castSource, castData);
            recasts.addRecast(recastInstance, playerMagicData);
            AudioTools.playSoundFromEntity(level, entity, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }


    private QuickArmsHandgunEntity getQuickArmsEntityFromMagicData(MagicData playerMagicData, Level level){
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

        if (!(recast.getCastData() instanceof QuickArmsCastData castData)) {
            return null;
        }

        if (!(castData.getEntity(serverLevel) instanceof QuickArmsHandgunEntity summon)) {
            return null;
        }

        return summon;
    }

    public class QuickArmsCastData implements ICastDataSerializable {
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
