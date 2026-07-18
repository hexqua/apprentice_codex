package jp.aquafactory.apprenticecodex.spell.automagnet;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class AutoMagnet extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "auto_magnet");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(0)
            .build();

    public AutoMagnet() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        baseManaCost = 40;
        manaCostPerLevel = 10;
        castTime = 0;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 1)),
                Component.translatable("ui.apprenticecodex.collect_cost_mana", (int) getCollectMana())
        );
    }

    private double getRange(int spellLevel){
        return 8 * (spellLevel - 1) * 4;
    }

    public double getCollectMana(){
        if (ApprenticeCodexServerConfig.autoMagnetDisableCollectManaCost()) {
            return 0.0;
        }

        return 10;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            var collectionMode = AutoMagnetCollectionMode.fromCrouching(serverPlayer.isCrouching());
            if (AutoMagnetFamiliarManager.toggle(serverPlayer, getRange(spellLevel), getCollectMana(),
                    collectionMode)) {
                sendModeMessage(serverPlayer, collectionMode);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static void sendModeMessage(ServerPlayer player, AutoMagnetCollectionMode collectionMode) {
        var message = switch (collectionMode) {
            case NORMAL -> Component.translatable("ui.apprenticecodex.auto_magnet.set_normal_mode")
                    .withStyle(ChatFormatting.GREEN);
            case REVERSE -> Component.translatable("ui.apprenticecodex.auto_magnet.set_reverse_mode")
                    .withStyle(ChatFormatting.YELLOW);
        };
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }
}
