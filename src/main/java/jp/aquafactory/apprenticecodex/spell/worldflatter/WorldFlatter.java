package jp.aquafactory.apprenticecodex.spell.worldflatter;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class WorldFlatter extends AbstractSummonWeaponSpell<WorldFlatterDrillEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "world_flatter");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(4)
            .build();

    public WorldFlatter() {
        super(WorldFlatterDrillEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 10;
        manaCostPerLevel = 5;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.deepslate_break_time", Utils.timeFromTicks(getBreakBestTime(spellLevel,caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return 0.5f + 1.5f * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private float getRange(){
        return 8;
    }

    private int getReachSpeed() {
        return 15;
    }

    private float getBreakSpeed(int spellLevel, LivingEntity entity) {
        // 6=鉄、9=ネザライト、12=金、効率強化1=+2、効率強化5=+26、効率強化6=+37.
        return 4f * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getBreakBestTime(int spellLevel, LivingEntity entity){
        // 石は1.5、丸石は2、深層岩は3、適正ツールは補正値30.
        return Math.round((3 * 30) / getBreakSpeed(spellLevel, entity));
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
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public WorldFlatterDrillEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setReachSpeed(getReachSpeed());
        summonWeapon.setToolSpeed(getBreakSpeed(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        AudioTools.playSoundFromEntity(level, entity, SoundRegistry.SAW_START.get(), SoundSource.PLAYERS);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull WorldFlatterDrillEntity weapon) {
        var result = RaycastTools.raycastFromEye(entity, getRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, entity));
        weapon.updateOwnerTarget(level, result);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull WorldFlatterDrillEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
