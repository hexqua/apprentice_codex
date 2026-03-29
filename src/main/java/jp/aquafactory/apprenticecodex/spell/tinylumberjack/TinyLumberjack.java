package jp.aquafactory.apprenticecodex.spell.tinylumberjack;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TinyLumberjack extends AbstractSummonWeaponSpell<TinyLumberjackSawEntity> implements ICraftsmansDelightAffectedSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "tiny_lumberjack");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(4)
            .build();

    public TinyLumberjack() {
        super(TinyLumberjackSawEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 10;
        manaCostPerLevel = 5;
        castTime = 400;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.tree_cut_time", Utils.timeFromTicks(getBreakBestTime(spellLevel,caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.TINY_LUMBERJACK);
    }

    private float getRange(){
        return 6;
    }

    private int getReachSpeed() {
        return 15;
    }

    private float getBreakSpeed(int spellLevel, LivingEntity entity) {
        // 6=鉄、9=ネザライト、12=金、効率強化1=+2、効率強化5=+26、効率強化6=+37.
        var baseSpeed = 2f + 1.5f * getSpellPower(spellLevel, entity) / 100.0f;
        if (!isCraftsmansDelightBreakSpeedBonusEnabled()) {
            return baseSpeed;
        }

        return CraftsmansDelight.applyBreakSpeedBonus(baseSpeed, entity);
    }

    public static ItemStack createDummyTool(@Nullable LivingEntity entity) {
        var baseTool = new ItemStack(Items.IRON_AXE);
        return CraftsmansDelight.applyEnchantsToTool(baseTool, entity);
    }

    private int getBreakBestTime(int spellLevel, LivingEntity entity){
        // 原木は大抵硬さ2、適正ツールは補正値30.
        return Math.round((2 * 30) / getBreakSpeed(spellLevel, entity));
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public boolean isCraftsmansDelightCastingMobilityEnabled() {
        return true;
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
    public TinyLumberjackSawEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new TinyLumberjackSawEntity(EntityRegistry.TINY_LUMBERJACK_SAW.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setReachSpeed(getReachSpeed());
        summonWeapon.setToolSpeed(getBreakSpeed(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
        AudioTools.playSoundFromEntity(level, entity, SoundRegistry.SAW_START.get(), SoundSource.PLAYERS);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull TinyLumberjackSawEntity weapon) {
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
        var result = RaycastTools.raycastFromEye(entity, getRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, entity));
        weapon.updateOwnerTarget(result);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull TinyLumberjackSawEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
