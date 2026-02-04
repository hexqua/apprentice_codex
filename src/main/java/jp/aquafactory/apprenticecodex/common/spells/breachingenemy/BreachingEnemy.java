package jp.aquafactory.apprenticecodex.common.spells.breachingenemy;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.AbstractFirearmSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BreachingEnemy extends AbstractFirearmSpell<BreachingEnemyShotgunEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "breaching_enemy");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(6)
            .build();

    public BreachingEnemy() {
        super(BreachingEnemyShotgunEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 10;
        manaCostPerLevel = 5;
        baseManaCost = 30;
        castTime = 0;
    }


    @Override
    public int getBulletCount(int spellLevel, @Nullable LivingEntity entity) {
        // todo:実装.
        return 1;
    }

    @Override
    public int getDurationTick() {
        // todo:実装.
        return 20 * 5;
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
    public Optional<SoundEvent> getPreFireSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getPreSummonSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getFireSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getSummonSound() {
        return Optional.empty();
    }

    @Override
    protected boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull BreachingEnemyShotgunEntity weapon) {
        return false;
    }

    @Override
    protected boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return false;
    }

    @Override
    public void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull BreachingEnemyShotgunEntity weapon) {

    }

    @Override
    public BreachingEnemyShotgunEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return null;
    }
}
