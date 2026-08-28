package jp.aquafactory.apprenticecodex.spell.uniteluna;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class UniteLuna extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "unite_luna");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public UniteLuna() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 160;
        manaCostPerLevel = 160;
        castTime = 0;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 20.0f * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.UNITE_LUNA);
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.STELLAR_FIRE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        // 武器固有なのでモーションはさせない.
        return AnimationHolder.pass();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // 武器固有なのでモーションはさせない.
        return AnimationHolder.pass();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            spawnMoon(level, spellLevel, entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnMoon(Level level, int spellLevel, LivingEntity caster) {
        var direction = normalizeLookDirection(caster);
        var moon = new UniteLunaMoonEntity(EntityRegistry.UNITE_LUNA_MOON.get(), level, caster);
        moon.setDamage(getDamage(spellLevel, caster));
        var spawnPosition = pickSpawnPosition(level, caster, direction);
        moon.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        moon.shoot(direction);
        level.addFreshEntity(moon);
    }

    private static Vec3 normalizeLookDirection(LivingEntity caster) {
        var direction = caster.getLookAngle();
        if (direction.lengthSqr() <= 1.0e-6) {
            direction = Vec3.directionFromRotation(caster.getXRot(), caster.getYRot());
        }
        return direction.normalize();
    }

    private static Vec3 pickSpawnPosition(Level level, LivingEntity caster, Vec3 direction) {
        // ArcaneBeam と同様に杖の先寄りへ出したいが、埋まりは避ける。
        var basePosition = caster.getEyePosition().add(0.0, -0.5, 0.0);
        var half = UniteLunaMoonEntity.CUBE_SIZE * 0.5f;
        var distances = new double[]{0.9, 0.7, 0.48, 0.24, 0.0};
        for (var distance : distances) {
            var candidate = basePosition.add(direction.scale(distance));
            var box = new AABB(
                    candidate.x - half, candidate.y - half, candidate.z - half,
                    candidate.x + half, candidate.y + half, candidate.z + half
            );
            if (!level.getBlockCollisions(null, box).iterator().hasNext()) {
                return candidate;
            }
        }

        return basePosition;
    }
}
