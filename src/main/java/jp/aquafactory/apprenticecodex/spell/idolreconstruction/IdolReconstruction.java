package jp.aquafactory.apprenticecodex.spell.idolreconstruction;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
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

public class IdolReconstruction extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "idol_reconstruction");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1)
            .setAllowCrafting(false)
            .build();

    public IdolReconstruction() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 10;
        manaCostPerLevel = 0;
        castTime = 20 * 60;
    }

    @Override
    public boolean allowLooting(){
        // アイテム専用化.
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel,@Nullable LivingEntity caster) {
        if (caster == null) {
            return List.of(
                    Component.translatable("ui.apprenticecodex.idol_reconstrcution.accelerate_repair_speed", getAccelerateSpeedPercent())
            );
        }

        return List.of(
                Component.translatable("ui.apprenticecodex.idol_reconstrcution.accelerate_repair_speed", getAccelerateSpeedPercent()),
                Component.translatable("ui.apprenticecodex.idol_reconstrcution.remain_repair_time", Utils.timeFromTicks(getRemainRepairTimeTick(caster), 1))
        );
    }

    private int getAccelerateSpeedPercent(){
        return Math.round(100 * getAccelerateSpeedRate());
    }

    float getAccelerateSpeedRate(){
        // todo:サーバー設定値から持ってくる.
        return 10.0f;
    }

    private int getRemainRepairTimeTick(LivingEntity entity){
        // todo:実際の修復完了までの時間にする.
        // 今は完全にダミー値.
        return 100;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)){
            return getCastTime(spellLevel);
        }

        // todo:実際の修復完了までの時間にする.
        return getCastTime(spellLevel);
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
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        // todo:恐らくここで不死のエンブレムの加速処理を行う?
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
