package jp.aquafactory.apprenticecodex.spell.spectralwing;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SpectralWingState;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SpectralWing extends AbstractSpell {
    private static final double LAUNCH_HORIZONTAL_SPEED = 0.75;
    private static final double LAUNCH_VERTICAL_SPEED = 1.05;
    private static final double BOOST_SPEED = 2.75;
    private static final double BOOST_UPWARD_SPEED = 0.22;
    private static final int BOOST_SPIN_TICKS = 10;
    private static final int VISUAL_EFFECT_DURATION_TICKS = 20;
    private static final int START_GRACE_TICKS = 10;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spectral_wing");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(2)
            .build();

    public SpectralWing() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 150;
        manaCostPerLevel = 0;
        castTime = 0;
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
        return Optional.of(SoundRegistry.THRUST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        if (hasWingConflict(player)) {
            notifyConflict(player);
            return false;
        }

        if (player.isInWaterOrBubble() || player.isSwimming()) {
            notifyUnderwater(player);
            return false;
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof Player player) {
            Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, state -> {
                if (state.active && state.startedBySpell) {
                    applyBoost(player);
                    state.launchGraceTicks = START_GRACE_TICKS;
                } else {
                    activateWing(player, state);
                }
            }));
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static void activateWing(Player player, SpectralWingState state) {
        var look = player.getLookAngle().normalize();
        var launchVelocity = new Vec3(
                look.x * LAUNCH_HORIZONTAL_SPEED,
                Math.max(LAUNCH_VERTICAL_SPEED, LAUNCH_VERTICAL_SPEED + look.y * 0.35),
                look.z * LAUNCH_HORIZONTAL_SPEED
        );

        player.setDeltaMovement(launchVelocity);
        player.startFallFlying();
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
        state.active = true;
        state.startedBySpell = true;
        state.launchGraceTicks = START_GRACE_TICKS;
        state.waterGraceTicks = 0;
        refreshVisualEffect(player);
    }

    private static void applyBoost(Player player) {
        var look = player.getLookAngle().normalize();
        var boostVelocity = look.scale(BOOST_SPEED).add(0.0, BOOST_UPWARD_SPEED, 0.0);
        player.setDeltaMovement(player.getDeltaMovement().scale(0.15).add(boostVelocity));
        player.startFallFlying();
        // 激流の見た目だけを再利用し、加速の主処理は SpectralWing 側で維持する。
        player.startAutoSpinAttack(BOOST_SPIN_TICKS);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
        refreshVisualEffect(player);
    }

    public static void refreshVisualEffect(Player player) {
        var effect = player.getEffect(EffectRegistry.SPECTRAL_WING.get());
        if (effect == null || effect.getDuration() <= 8) {
            player.addEffect(new MobEffectInstance(
                    EffectRegistry.SPECTRAL_WING.get(),
                    VISUAL_EFFECT_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ), player);
        }
    }

    public static boolean hasWingConflict(Player player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA)
                || player.hasEffect(MobEffectRegistry.ANGEL_WINGS.get());
    }

    public static void notifyConflict(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.spectral_wing.conflict").withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static void notifyUnderwater(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.spectral_wing.underwater").withStyle(ChatFormatting.RED)
            ));
        }
    }
}
