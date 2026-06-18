package jp.aquafactory.apprenticecodex.spell.anchorblink;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AnchorBlink extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "anchor_blink");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(false)
            .build();

    public AnchorBlink() {
        baseSpellPower = 400;
        spellPowerPerLevel = 200;
        baseManaCost = 80;
        manaCostPerLevel = 0;
        castTime = 10;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, @Nullable LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getMaximumRange(spellLevel, caster), 0))
        );
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @javax.annotation.Nullable LivingEntity entity) {
        // 詠唱時間短縮を考慮しない.
        return getCastTime(spellLevel);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ANCHOR_BLINK);
    }

    private float getSpeed() {
        return 1.8f;
    }

    private float getMaximumRange(int spellLevel, @Nullable LivingEntity caster) {
        return Math.min(128, 16 * getSpellPower(spellLevel, caster) / 100.0f);
    }

    public static boolean hasRequiredMirror(Player player) {
        return SpellSideEdgeMirror.isGeneratedMirror(player.getItemInHand(InteractionHand.OFF_HAND));
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
    public final Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_HOLD_WEAPON.get());
    }

    @Override
    public final Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ICE_DAGGER_THROW.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.THROW_SINGLE_ITEM;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player) || !hasRequiredMirror(player)) {
            if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.anchor_blink.requires_mirror")
                                .withStyle(ChatFormatting.RED)
                ));
            }
            return false;
        }

        return super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer serverPlayer) {
            var forward = entity.getLookAngle();
            if (forward.lengthSqr() <= 1.0E-8D) {
                forward = new Vec3(0.0D, 0.0D, 1.0D);
            }
            forward = forward.normalize();

            var dagger = new AnchorBlinkDaggerEntity(EntityRegistry.ANCHOR_BLINK_DAGGER.get(), serverLevel, serverPlayer);
            dagger.setDamage(getDamage(spellLevel, entity));
            dagger.setMaximumRange(getMaximumRange(spellLevel, entity));
            dagger.setPos(entity.getEyePosition().add(forward.scale(0.4D)).add(0.0D, -0.18D, 0.0D));
            dagger.shoot(forward.x, forward.y, forward.z, getSpeed(), 0.0F);
            serverLevel.addFreshEntity(dagger);

            EdgeDancerManager.deactivate(serverPlayer, true);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
