package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
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
                Component.translatable("ui.irons_spellbooks.recast_count", getBulletCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel, caster), 1)),
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

    private int getDuration(int spellLevel, LivingEntity entity){
        // todo:バランス調整.
        return Math.round((20 * 15) * getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getRange(int spellLevel, LivingEntity entity){
        // todo:バランス調整.
        // 一旦狙撃イメージで10チャンク.
        return 16 * 10;
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
        if(isCommenceFireMode(entity)){
            // 攻撃は詠唱時間固定.
            return 10;
        }

        // 通常時はアニメが絡むため、詠唱時間補正は効かない.
        return getCastTime(spellLevel);
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
        if (isCommenceFireMode(serverPlayer)) {
            var modeEffect = EffectRegistry.COMMENCE_FIRE_MODE.get();
            serverPlayer.removeEffect(modeEffect);
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)) {
            // 初回発動なのにモードを持っていたら消してから処理.
            // サーバーサイド動作なのでここで消してOK.
            var modeEffect = EffectRegistry.COMMENCE_FIRE_MODE.get();
            if (isCommenceFireMode(entity)) {
                entity.removeEffect(modeEffect);
            }
            return true;
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            var recast = recasts.getRecastInstance(getSpellId());
            if(!isCommenceFireMode(entity)){
                // モードが解除されているのにリキャストしようとしたため、強制キャンセル.
                if (entity instanceof ServerPlayer serverPlayer && recast.getRemainingRecasts() > 0) {
                    MagicData.getPlayerMagicData(serverPlayer).getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);
                }
            } else {
                // 成立(攻撃)
                var range = getRange(spellLevel, entity);
                var result = RaycastTools.raycastFromEye(entity, range, e -> CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(e), entity));
                if (result.type() == RaycastTools.TargetType.LIVING_ENTITY) {
                    // todo:ダメージとかエフェクトとか.
                }
            }
        } else {
            // 初回詠唱.
            var modeTime = getDuration(spellLevel, entity);
            var castData = new CommenceFireCastData();
            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), modeTime, castSource, castData);
            recasts.addRecast(recastInstance, playerMagicData);
            entity.addEffect(new MobEffectInstance(EffectRegistry.COMMENCE_FIRE_MODE.get(), modeTime, 0, false, false, true));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static boolean isCommenceFireMode(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return entity.hasEffect(EffectRegistry.COMMENCE_FIRE_MODE.get());
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
