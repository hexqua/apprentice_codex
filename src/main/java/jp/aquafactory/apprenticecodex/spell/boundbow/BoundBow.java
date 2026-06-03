package jp.aquafactory.apprenticecodex.spell.boundbow;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class BoundBow extends AbstractSpell {
    private static final int POWER_START_SPELL_POWER = 130;
    private static final int POWER_SPELL_POWER_STEP = 30;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bound_bow");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public BoundBow() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 50;
        manaCostPerLevel = 150;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.bound_bow.enchantment_power_level", getPowerLevel(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.bound_bow.forge_arrow_mana_cost",
                        Utils.stringTruncation(ApprenticeCodexServerConfig.boundBowForgeArrowManaCost(), 0)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    public int getPowerLevel(int spellLevel, LivingEntity entity) {
        return getPowerLevelForSpellPower(getSpellPower(spellLevel, entity));
    }

    public static int getPowerLevelForSpellPower(float spellPower) {
        if (spellPower < POWER_START_SPELL_POWER) {
            return 0;
        }

        var rawLevel = 1 + (int) Math.floor((spellPower - POWER_START_SPELL_POWER) / POWER_SPELL_POWER_STEP);
        return Mth.clamp(rawLevel, 0, ApprenticeCodexServerConfig.boundBowMaxPowerEnchantmentLevel());
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
                BoundBowManager.deactivate(serverPlayer, true);
            } else {
                BoundBowManager.activate(serverPlayer, spellLevel, castSource, playerMagicData, this,
                        getPowerLevel(spellLevel, entity),
                        (float) entity.getAttributeValue(AttributeRegistry.SUMMON_DAMAGE.get()));
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
        BoundBowManager.deactivate(serverPlayer, false);
        if (io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()
                .isEquippedBy(serverPlayer)) {
            return;
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }
}
