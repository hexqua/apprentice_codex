package jp.aquafactory.apprenticecodex.spell.manifestationgrimoire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class ManifestationGrimoire extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "manifestation_grimoire");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60)
            .setAllowCrafting(false)
            .build();

    public ManifestationGrimoire() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 100;
        manaCostPerLevel = 0;
        castTime = 50;
    }

    @Override
    public boolean allowLooting() {
        return false;
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
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.BLACK_HOLE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
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
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // ちょっと先に出す.
        var location = entity.getEyePosition(1.0f).add(RotationTools.getFlatForward(entity).scale(1.5f));
        var item = new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get(), 1);
        var grimoire = new ItemEntity(level, location.x, location.y, location.z, item);

        // 1秒は拾えないようにしておく.
        grimoire.setNoGravity(true);
        grimoire.setDeltaMovement(0, 0, 0);
        grimoire.setPickUpDelay(20);
        level.addFreshEntity(grimoire);

        // パーティクルはサーバー側で適当に.
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, location.x, location.y, location.z, 4, 0.2, 0.2, 0.2, 0.01);
            server.sendParticles(ParticleTypes.FIREWORK, location.x, location.y, location.z, 6, 0.2, 0.2, 0.2, 0.01);
            server.sendParticles(ParticleTypes.PORTAL, location.x, location.y, location.z, 32, 0.2, 0.2, 0.2, 0.01);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
