package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HeavenlyFistPulsePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class MirageAvoidance extends AbstractSpell {
    private static final float STARTUP_PULSE_RADIUS = 1.0F;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mirage_avoidance");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(3)
            .build();

    public MirageAvoidance() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private int getDuration() {
        // 無敵時間であり、効果時間は別.
        return MirageAvoidanceEvents.INVULNERABLE_TICKS;
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        if (MirageAvoidanceEvents.isInputLocked(player)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.during_effect").withStyle(ChatFormatting.RED)
                ));
            }
            return false;
        }

        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        // todo:字幕用に自前のsounds.jsonに切り出すか音を作る.
        return Optional.of(SoundEvents.PORTAL_TRAVEL);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_TWO_HANDS;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof Player player) {
            var input = MirageAvoidanceInput.consumePending(player);
            Capabilities.withSpellData(player, data -> {
                data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, state -> {
                    state.startGameTime = level.getGameTime();
                    state.activeUntilGameTime = level.getGameTime() + MirageAvoidanceEvents.EFFECT_DURATION_TICKS;
                    state.invulnerableUntilGameTime = level.getGameTime() + getDuration();
                    state.movementForward = input.forward();
                    state.movementStrafe = input.strafe();
                    state.suppressFallDamageUntilGround = true;
                });
                if (player instanceof ServerPlayer serverPlayer) {
                    MirageAvoidanceSync.syncToClient(serverPlayer, data.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
                }
            });
            spawnStartupPulse(player);
            player.fallDistance = 0.0F;
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static void spawnStartupPulse(Player player) {
        var center = new Vec3(
                player.getX(),
                player.getY() + 0.1,
                player.getZ()
        );

        // 見た目が似てるので天からの鉄拳の波紋エフェクトを流用.
        Networks.sendToTrackingEntityAndSelf(player, new HeavenlyFistPulsePacket(center, STARTUP_PULSE_RADIUS));
    }
}
