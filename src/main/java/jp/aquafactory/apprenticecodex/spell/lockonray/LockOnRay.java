package jp.aquafactory.apprenticecodex.spell.lockonray;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class LockOnRay extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "lock_on_ray");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public LockOnRay() {
        baseSpellPower = 200;
        spellPowerPerLevel = 50;
        baseManaCost = 15;
        manaCostPerLevel = 5;
        castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.LOCK_ON_RAY);
    }

    static float getRange() {
        return 48;
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
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.SUNBEAM_WINDUP.get());
    }

    @Override
    public boolean canBeInterrupted(@Nullable Player player) {
        // CONTINUOUSだが被弾でキャンセルされうる.
        return !ItemRegistry.CONCENTRATION_AMULET.get().isEquippedBy(player);
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
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!(level instanceof ServerLevel) || data == null) return false;
        clearTarget(data);
        var target = CombatTools.findLookCombatTarget(caster, getRange(), 1);
        if (target == null) {
            if (caster instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
            }
            return false;
        }
        data.setAdditionalCastData(new LockOnRayCastData(target));
        if (caster instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    "ui.irons_spellbooks.spell_target_success", target.getDisplayName().getString(), getDisplayName(player))
                    .withStyle(ChatFormatting.GREEN)));
        }
        return true;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData data) {
        if (data == null) return;
        // Mobなど開始条件を省略する呼び出しでも、開始時の一度だけ対象を解決する。
        if (!(data.getAdditionalCastData() instanceof LockOnRayCastData)
                && !checkPreCastConditions(level, spellLevel, caster, data)) {
            cancel(caster, data);
            return;
        }
        if (level instanceof ServerLevel server && caster instanceof ServerPlayer player
                && data.getAdditionalCastData() instanceof LockOnRayCastData cast
                && cast.resolve(server) instanceof LivingEntity target) {
            // Iron's標準の対象表示レイヤーはLivingEntity専用。詠唱完了通知で表示も解除される。
            PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(target, this));
        }
        super.onServerPreCast(level, spellLevel, caster, data);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData data) {
        if (!(level instanceof ServerLevel server) || data == null) return;
        var cast = data.getAdditionalCastData() instanceof LockOnRayCastData stored ? stored : null;
        var target = cast == null ? null : cast.resolve(server);
        if (target == null || !caster.isAlive() || caster.isRemoved() || caster.level() != level) {
            cancel(caster, data);
            return;
        }
        if (cast.tickAndShouldFire()) {
            var origin = caster.getEyePosition().add(0, -0.4, 0).add(caster.getLookAngle().scale(0.5));
            var laser = new LockOnRayLaserEntity(EntityRegistry.LOCK_ON_RAY_LASER.get(), level);
            laser.launch(caster, origin, target, getDamage(spellLevel, caster));
            server.addFreshEntity(laser);
            level.playSound(null, origin.x, origin.y, origin.z, SoundRegistry.GUIDING_BOLT_CAST.get(),
                    SoundSource.PLAYERS, 1, 0.9f + caster.getRandom().nextFloat() * 0.2f);
        }
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData data, boolean cancelled) {
        clearTarget(data);
        super.onServerCastComplete(level, spellLevel, caster, data, cancelled);
    }

    public static void cancel(LivingEntity caster, MagicData data) {
        clearTarget(data);
        if (caster instanceof AbstractSpellCastingMob mob) mob.cancelCast();
        else MagicTools.cancelCasting(caster, true);
    }

    private static void clearTarget(MagicData data) {
        if (data.getAdditionalCastData() instanceof LockOnRayCastData cast) {
            cast.reset();
            data.setAdditionalCastData(null);
        }
    }

    // onCastはIron's標準のマナ消費周期で呼ばれる。発射は毎tick側だけで行う。
}
