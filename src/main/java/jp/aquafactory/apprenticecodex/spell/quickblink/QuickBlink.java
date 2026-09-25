package jp.aquafactory.apprenticecodex.spell.quickblink;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class QuickBlink extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quick_blink");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public QuickBlink() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 100;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.recast_count", getRecastCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.distance", getDistance())
        );
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 1 + spellLevel;
    }

    int getDistance() {
        return 5;
    }

    private int getRecastDurationTick() {
        return 40;
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
        // MantleBlink が開始・出現時の音を再生するため、上流の詠唱終了音は重ねない。
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return entity instanceof ServerPlayer player && !QuickBlinkRuntime.active(player)
                && !ShootingStarMantleRuntime.state(player).blink.active(level.getGameTime())
                && super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer player && !QuickBlinkRuntime.active(player)
                && !ShootingStarMantleRuntime.state(player).blink.active(level.getGameTime())) {
            if (!playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
                playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), spellLevel,
                        getRecastCount(spellLevel, player), getRecastDurationTick(), castSource, null), playerMagicData);
            }
            QuickBlinkRuntime.begin(player, getDistance());
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
