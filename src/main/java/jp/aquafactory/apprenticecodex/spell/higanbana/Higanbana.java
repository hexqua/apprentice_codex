package jp.aquafactory.apprenticecodex.spell.higanbana;

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
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Higanbana extends AbstractSummonWeaponRecastSpell<HiganbanaKatanaEntity> {
    private static final int FIRST_SLASH_DELAY_TICK = 5;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "higanbana");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public Higanbana() {
        super(HiganbanaKatanaEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 40;
        manaCostPerLevel = 20;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.recast_count", getActivateCount(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 2 + getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.HIGANBANA);
    }

    @Override
    public int getActivateCount(int spellLevel, @Nullable LivingEntity entity) {
        return Math.min(8, Math.round(getSpellPower(spellLevel, entity) / 100.0f));
    }

    @Override
    public int getDurationTick() {
        return 20 * 5;
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
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
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
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    protected boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull HiganbanaKatanaEntity weapon) {
        if (!weapon.canSlash()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.during_standby", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }

    @Override
    protected boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return false;
    }

    @Override
    public CompleteRecastTypes onRecastFinishedWithWeapon(Level level, ServerPlayer serverPlayer, @NotNull HiganbanaKatanaEntity weapon) {
        // 最終Recast直後に消すと斬撃演出が途切れるため、少し待ってから消す.
        weapon.scheduleRelease(10);
        return CompleteRecastTypes.KEEP_WEAPON;
    }

    @Override
    public void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull HiganbanaKatanaEntity weapon) {
        weapon.slash(level);
    }

    @Override
    public HiganbanaKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setFirstSlashStandby(FIRST_SLASH_DELAY_TICK);
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }
}
