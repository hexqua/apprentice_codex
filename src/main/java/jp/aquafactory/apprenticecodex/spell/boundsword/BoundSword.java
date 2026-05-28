package jp.aquafactory.apprenticecodex.spell.boundsword;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class BoundSword extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bound_sword");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public BoundSword() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 50;
        manaCostPerLevel = 150;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.bound_sword.sword_damage_attribute", Utils.stringTruncation(getWeaponDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getWeaponDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 6 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.BOUND_SWORD);
    }

    int getDuration() {
        return 20 * 120;
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
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
                BoundSwordManager.deactivate(serverPlayer, true);
            } else {
                BoundSwordManager.activate(serverPlayer, spellLevel, castSource, playerMagicData, this,
                        getWeaponDamage(spellLevel, entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return entity instanceof Player && super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        BoundSwordManager.deactivate(serverPlayer, false);
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }
}
