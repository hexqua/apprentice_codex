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
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockHitTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
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
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WorldFlatter extends AbstractSummonWeaponSpell<WorldFlatterDrillEntity> implements IClientBlockHitTargetingSpell, ICraftsmansDelightAffectedSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "world_flatter");
    private static final double BLOCK_TARGET_RANGE = 8.0;
    private static final double ENTITY_TARGET_RAYCAST_WIDTH = 0.5;

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
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
                Component.translatable("ui.apprenticecodex.entity_reach_range", (int) getEntityRange(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.penetrated_armor_percent", getArmorReductionPercent(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 3.0f + 2.0f * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.WORLD_FLATTER);
    }

    private double getBlockRange() {
        return BLOCK_TARGET_RANGE;
    }

    private double getEntityRange(int spellLevel, LivingEntity entity) {
        return 8.0 + 4.0 * getSpellPower(spellLevel, entity) / 100.0;
    }

    private int getPenetratedArmorAmplifier(int spellLevel, LivingEntity entity) {
        return Math.min(3, Math.max(0, (int) Math.floor(getSpellPower(spellLevel, entity) / 200.0f)));
    }

    private int getArmorReductionPercent(int spellLevel, LivingEntity entity) {
        return (getPenetratedArmorAmplifier(spellLevel, entity) + 1) * 20;
    }

    private float getBreakSpeed(int spellLevel, LivingEntity entity) {
        // 6=鉄、9=ネザライト、12=金、効率強化1=+2、効率強化5=+26、効率強化6=+37.
        var baseSpeed = 4f * getSpellPower(spellLevel, entity) / 100.0f;
        if (!isCraftsmansDelightBreakSpeedBonusEnabled()) {
            return baseSpeed;
        }

        return CraftsmansDelight.applyBreakSpeedBonus(baseSpeed, entity);
    }

    public static ItemStack createDummyTool(@Nullable LivingEntity entity) {
        var baseTool = new ItemStack(CraftsmansDelight.isEquippedBy(entity) ? Items.NETHERITE_PICKAXE : Items.IRON_PICKAXE);
        return CraftsmansDelight.applyEnchantsToTool(baseTool, entity);
    }

    public static Tier getHarvestTier(@Nullable LivingEntity entity) {
        return CraftsmansDelight.isEquippedBy(entity) ? Tiers.NETHERITE : Tiers.IRON;
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
    public boolean isCraftsmansDelightCastingMobilityEnabled() {
        return true;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getBlockRange();
    }

    @Override
    public boolean ignoresClientBlockTargetingRange() {
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
    public WorldFlatterDrillEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setPenetratedArmorAmplifier(getPenetratedArmorAmplifier(spellLevel, entity));
        summonWeapon.setToolSpeed(getBreakSpeed(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
        AudioTools.playSoundFromEntity(level, entity, SoundRegistry.SAW_START.get(), SoundSource.PLAYERS);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull WorldFlatterDrillEntity weapon) {
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
        updateTarget(level, spellLevel, entity, weapon);
    }

    private void updateTarget(Level level, int spellLevel, LivingEntity entity, WorldFlatterDrillEntity weapon) {
        var entityResult = RaycastTools.raycastFromEye(
                entity,
                getEntityRange(spellLevel, entity),
                ENTITY_TARGET_RAYCAST_WIDTH,
                e -> CombatTools.isValidCombatTarget(e, entity)
        );
        if (entityResult.hitType() == RaycastTools.TargetType.LIVING_ENTITY && entityResult.hitEntity() != null) {
            weapon.updateOwnerTarget(level, entityResult);
            return;
        }

        var clientBlockTarget = BlockTargetingHelper.getPendingHitTargetIgnoringRange(level, entity, getSpellResource())
                .filter(target -> target.getHitBlockPos() != null)
                .filter(target -> WorldFlatterDrillEntity.canBreakTarget(
                        level,
                        entity,
                        target.getHitBlockPos(),
                        level.getBlockState(target.getHitBlockPos()),
                        level.getBlockState(target.getHitBlockPos())
                ))
                .map(target -> new RaycastTools.TargetResult(
                        RaycastTools.TargetType.BLOCK,
                        target.getHitLocation(),
                        null,
                        target.getHitBlockPos()
                ));
        if (clientBlockTarget.isPresent()) {
            weapon.updateOwnerTarget(level, clientBlockTarget.get());
            return;
        }

        var blockResult = RaycastTools.raycast(entity, entity.getViewVector(1.0F), getBlockRange(), 0.0, e -> false);
        if (blockResult.hitType() == RaycastTools.TargetType.BLOCK
                && blockResult.hitBlock() != null
                && WorldFlatterDrillEntity.canBreakTarget(
                        level,
                        entity,
                        blockResult.hitBlock(),
                        level.getBlockState(blockResult.hitBlock()),
                        level.getBlockState(blockResult.hitBlock())
                )) {
            weapon.updateOwnerTarget(level, blockResult);
            return;
        }

        if (!weapon.hasBlockTarget()) {
            weapon.updateOwnerTarget(level, new RaycastTools.TargetResult(
                    RaycastTools.TargetType.NONE,
                    blockResult.hitPosition(),
                    null,
                    null
            ));
        }
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull WorldFlatterDrillEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}

