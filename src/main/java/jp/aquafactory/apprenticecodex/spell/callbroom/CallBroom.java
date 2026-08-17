package jp.aquafactory.apprenticecodex.spell.callbroom;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class CallBroom extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "call_broom");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1)
            .setAllowCrafting(false)
            .build();

    public CallBroom() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 20;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        // todo:現状の状況を表示するかは使い勝手など見てから.
        return List.of();
    }

    @Override
    public boolean allowLooting(){
        // アイテム専用のため.
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
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return !player.isPassenger()
                    && BroomCurioSupport.findUniqueEquippedBroom(player).isPresent()
                    && super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
        }

        var validation = CallBroomDeploymentManager.validate(serverPlayer);
        if (!validation.canCast()) {
            var key = validation.failure() == CallBroomDeploymentManager.Failure.NOT_FOUND
                    ? "ui.apprenticecodex.call_broom.not_found_broom"
                    : "ui.apprenticecodex.call_broom.cannot_recall_mounting";

            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(key).withStyle(ChatFormatting.RED)
            ));
            return false;
        }
        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer player && CallBroomDeploymentManager.execute(player)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
        }
    }
}
