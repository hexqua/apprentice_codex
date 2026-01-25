package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ArcherMultiple  extends AbstractSpell {
    @SuppressWarnings("removal")
    private final ResourceLocation spellId = new ResourceLocation(ApprenticeCodex.MODID, "archer_multiple");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public ArcherMultiple() {
        // todo:バランス調整.
        baseSpellPower = 100;
        spellPowerPerLevel = 0;
        manaCostPerLevel = 30;
        baseManaCost = 70;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.summon_count", 4),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // todo:バランス調整
        return 5;
    }

    private int getProjectileCount(int spellLevel, LivingEntity entity) {
        // todo:バランス調整
        return 20;
    }

    private int getSummonTime(){
        // 20tick = 1秒.
        // 60秒 = 1分.
        return 20 * 60 * 2;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(getSchoolType().getCastSound());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)) {
            var summonedEntitiesCastData = new SummonedEntitiesCastData();

            // todo:仮実装中.
            var summonTestBow = new ArcherMultipleBowEntity(EntityRegistry.ARCHER_MULTIPLE_BOW.get(), level, entity);
            summonTestBow.setPos(entity.position());
            summonTestBow.setXRot(entity.getXRot());
            summonTestBow.setYRot(entity.getYRot());

            level.addFreshEntity(summonTestBow);
            SummonManager.initSummon(entity, summonTestBow, getSummonTime(), summonedEntitiesCastData);

            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getSummonTime(), castSource, summonedEntitiesCastData);
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
