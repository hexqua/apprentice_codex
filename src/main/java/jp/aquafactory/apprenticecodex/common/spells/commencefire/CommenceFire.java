package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CommenceFire extends AbstractSpell {
    @SuppressWarnings("removal")
    private final ResourceLocation spellId = new ResourceLocation(ApprenticeCodex.MODID, "commence_fire");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(45)
            .build();

    public CommenceFire() {
        // todo:バランス調整.
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        manaCostPerLevel = 20;
        baseManaCost = 200;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getBulletCount(int spellLevel, LivingEntity entity) {
        // todo:バランス調整.
        return 4;
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
        // todo:再詠唱時は変更する.
        return super.getEffectiveCastTime(spellLevel, entity);
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
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        // 初回発動含め弾の数にする.
        return getBulletCount(spellLevel, entity) + 1;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        // todo:Recast関係で変えられないので別のところでアニメを再生するか検討.
        return SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new CommenceFireCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        // todo:撃ち切った後の後始末を入れる.
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            var recast = playerMagicData.getPlayerRecasts().getRecastInstance(getSpellId());
            // 再詠唱.
        } else {
            // 初回詠唱.
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public class CommenceFireCastData implements ICastDataSerializable {
        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            // todo: 保存するものを決めて書き込む.
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            // todo: 保存したものを読み込む.
        }

        @Override
        public void reset() {
            // do nothing.
        }

        @Override
        public CompoundTag serializeNBT() {
            // todo: NBTシリアライズを調べて対応.
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            // todo: NBTデシリアライズを調べて対応.
        }
    }
}
