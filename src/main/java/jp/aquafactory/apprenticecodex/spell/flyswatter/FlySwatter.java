package jp.aquafactory.apprenticecodex.spell.flyswatter;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import jp.aquafactory.apprenticecodex.spell.IMagiAgentSuitAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCastData;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FlySwatter extends AbstractSummonWeaponRecastSpell<FlySwatterLauncherEntity> implements IMagiAgentSuitAffectedSpell {
    private static final int RECAST_WINDOW_TICKS = 120;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "fly_swatter");
    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public FlySwatter() {
        super(FlySwatterLauncherEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        manaCostPerLevel = 20;
        baseManaCost = 90;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.lock_on_count", getActivateCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.distance", 128));
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return (3 + 2 * getSpellPower(spellLevel, caster) / 100.0f)
                * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FLY_SWATTER);
    }

    @Override
    public int getActivateCount(int spellLevel, @Nullable LivingEntity caster) {
        return Math.max(1, Math.min(8, Math.round(2 * getSpellPower(spellLevel, caster) / 100)));
    }

    @Override
    public int getDurationTick() {
        return RECAST_WINDOW_TICKS;
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
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity caster) {
        if (caster != null && MagicData.getPlayerMagicData(caster).getPlayerRecasts().hasRecastForSpell(this)) {
            // Recastは固定時間。通常詠唱の倍率補正を重ねない。
            return MagiAgentSuitEffects.applyBootsRecastCastTime(this, 10, 5, caster);
        }
        return super.getEffectiveCastTime(spellLevel, caster);
    }

    @Override
    public boolean canBeInterrupted(@Nullable Player player) {
        return canBeInterruptedWithMagiAgentSuit(this, player, super.canBeInterrupted(player));
    }

    @Override
    public Optional<SoundEvent> getPreFireSound() {
        return Optional.of(SoundRegistry.VANILLA_HOLD_WEAPON.get());
    }
    @Override
    public Optional<SoundEvent> getPreSummonSound() {
        return Optional.of(getSchoolType().getCastSound());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public Optional<SoundEvent> getFireSound() {
        return Optional.of(SoundEvents.ITEM_PICKUP);
    }

    @Override
    public Optional<SoundEvent> getSummonSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    protected PendingSummonCastData createPendingCastData(Level level, @Nullable FlySwatterLauncherEntity weapon) {
        return new PendingLock(level, weapon);
    }

    @Override
    protected boolean isSummonWeaponValid(FlySwatterLauncherEntity weapon) {
        return !weapon.isReleased();
    }

    @Override
    protected boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity caster, MagicData data,
                                           @NotNull FlySwatterLauncherEntity weapon) {
        if (!(caster instanceof ServerPlayer player)) return false;
        var target = CombatTools.findLookCombatTarget(caster, 128, 1);
        if (target == null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
            return false;
        }
        if (!(data.getAdditionalCastData() instanceof PendingLock pending)) return false;
        pending.target = new LockOnRayCastData(target);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                "ui.irons_spellbooks.spell_target_success", target.getDisplayName().getString(), getDisplayName(player))
                .withStyle(ChatFormatting.GREEN)));
        return true;
    }

    @Override
    protected boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return false;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData data) {
        super.onServerPreCast(level, spellLevel, caster, data);
        if (data != null && caster instanceof ServerPlayer player && level instanceof ServerLevel server
                && data.getAdditionalCastData() instanceof PendingLock pending && pending.target != null
                && pending.target.resolve(server) instanceof LivingEntity target) {
            PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, List.of(target.getUUID())));
        }
    }

    @Override
    public FlySwatterLauncherEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        var launcher = new FlySwatterLauncherEntity(EntityRegistry.FLY_SWATTER_LAUNCHER.get(), level, caster);
        launcher.setRadius(3);
        launcher.setDamage(getDamage(spellLevel, caster));
        level.addFreshEntity(launcher);
        return launcher;
    }

    @Override
    public void onCastWithWeapon(Level level, int spellLevel, LivingEntity caster, MagicData data,
                                 @NotNull FlySwatterLauncherEntity weapon) {
        // FocusStaffbowの補正はcastSpell中だけ有効なので、正常完了した最新キャストで保持する。
        weapon.setDamage(getDamage(spellLevel, caster));
        if (level instanceof ServerLevel server && data.getAdditionalCastData() instanceof PendingLock pending
                && pending.target != null) {
            var target = pending.target.resolve(server);
            if (target != null) weapon.addLockOnTarget(target);
            AudioTools.playSoundFromEntity(level, weapon, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.5f, 1.5f);
        }
    }

    @Override
    public CompleteRecastTypes onRecastFinishedWithWeapon(Level level, ServerPlayer player,
                                                          @NotNull FlySwatterLauncherEntity weapon,
                                                          RecastInstance recast, RecastResult result) {
        if (result == RecastResult.TIMEOUT || result == RecastResult.USED_ALL_RECASTS) {
            weapon.startFiring(level, player);
            return CompleteRecastTypes.RELEASE_WEAPON;
        }
        return CompleteRecastTypes.DISCARD_WEAPON;
    }

    private static final class PendingLock extends PendingSummonCastData {
        @Nullable
        private LockOnRayCastData target;

        private PendingLock(Level level, @Nullable Entity weapon) {
            super(level, weapon);
        }

        @Override
        public void reset() {
            super.reset();
            if (target != null) target.reset();
            target = null;
        }
    }
}
