package jp.aquafactory.apprenticecodex.spell.waveringstar;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class WaveringStar extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wavering_star");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1)
            .setAllowCrafting(false)
            .build();

    public WaveringStar() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 0;
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.STELLAR_LAUNCH.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer player) || !ShootingStarMantleRuntime.toggle(player)) return;
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(entity instanceof Player player)) return false;
        var error = ShootingStarMantleRuntime.castError(player);
        if (error != null) {
            ShootingStarMantleRuntime.notify(player, "ui.apprenticecodex.wavering_star." + error, ChatFormatting.RED);
            return false;
        }
        return super.checkPreCastConditions(level, spellLevel, entity, magicData);
    }
}
