package jp.aquafactory.apprenticecodex.spell.assistwings;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class AssistWings extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "assist_wings");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(0)
            .build();

    public AssistWings() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 0;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.jumps_count", Utils.stringTruncation(getJumpCount(spellLevel), 1))
        );
    }

    private int getJumpCount(int spellLevel){
        return 1 + spellLevel;
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
        return Optional.of(SoundRegistry.VANILLA_HIGH_JUMP.get());
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }


    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var vehicle = entity.getVehicle();
        if (vehicle != null) {
            if (!(vehicle instanceof HoverrideBroomEntity broom)) {
                sendRejectedMessage(entity, "ui.apprenticecodex.assist_wings.cannot_mounting", vehicle.getDisplayName());
                return false;
            }
            if (!(entity instanceof Player player) || !broom.canUseAssistWings(player)) {
                sendRejectedMessage(entity, "ui.apprenticecodex.assist_wings.cannot_broom_condition", broom.getDisplayName());
                return false;
            }
        }

        if (isGroundedForAssistWings(entity)) {
            return true;
        }

        var codexData = Capabilities.getSpellDataOrNull(entity);
        if (codexData == null){
            return true;
        }

        var canJump = codexData.get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE).doneJump < getJumpCount(spellLevel);
        if (!canJump){
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_jump_more", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }

            return false;
        }

        return true;
    }

    private static void sendRejectedMessage(LivingEntity entity, String key, Component argument) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(key, argument).withStyle(ChatFormatting.RED)
            ));
        }
    }

    private static boolean isGroundedForAssistWings(LivingEntity entity) {
        return entity.onGround()
                || entity.getVehicle() instanceof HoverrideBroomEntity broom
                && broom.isWithinAssistWingsLandingDistance();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var broom = entity.getVehicle() instanceof HoverrideBroomEntity mountedBroom
                && entity instanceof Player player
                && mountedBroom.canUseAssistWings(player)
                ? mountedBroom
                : null;
        if (entity.getVehicle() != null && broom == null) {
            return;
        }

        var grounded = isGroundedForAssistWings(entity);
        var jumpHeight = 0.6f + entity.getJumpBoostPower();
        if (broom != null && !broom.acceptServerAssistWingsJump((Player) entity, jumpHeight)) {
            return;
        }

        Capabilities.withSpellData(entity, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, spell -> {
            // まずは翼が既にいるかどうかチェック.
            var wing = level.getEntity(spell.localEntityId);
            AssistWingsWingEntity assistWing;
            if (wing instanceof AssistWingsWingEntity existingWing
                    && !existingWing.isRemoved()
                    && existingWing.getOwner() == entity) {
                assistWing = existingWing;
            } else {
                assistWing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), level, entity);
                level.addFreshEntity(assistWing);
                spell.localEntityId = assistWing.getId();
            }
            assistWing.restartRemovalGrace();

            // 足をつけていればそのジャンプは空中ジャンプとして処理しない.
            if (grounded) {
                spell.doneJump = 0;
            } else {
                ++spell.doneJump;
            }

            // ラスト1回は音でわかりやすくする.
            if (spell.doneJump == getJumpCount(spellLevel)) {
                playAirJumpLimitSound(level, entity);
            }

            // 箒への騎乗中は翼の所有者ではなく、移動を担う箒へジャンプを適用する。
            if (broom == null) {
                applyJump(entity, jumpHeight);
            }
            if (broom == null && entity instanceof ServerPlayer serverPlayer) {
                // Iron's Spells の追加詠唱データは送信直後に reset されるため、エンコード時機に左右されない専用パケットを使う。
                Networks.sendToPlayer(serverPlayer, new SyncAssistWingsJumpPacket(jumpHeight));
            }
        }));

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static void playAirJumpLimitSound(Level level, LivingEntity entity) {
        AudioTools.playSoundFromEntity(level, entity, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0f, 0.75f);
    }

    private static void applyJump(LivingEntity entity, float jumpHeight) {
        SyncAssistWingsJumpPacket.applyTo(entity, jumpHeight);
        if (!entity.level().isClientSide && !(entity instanceof ServerPlayer)) {
            // ServerPlayer の hurtMarked は操作中の本人にも XYZ 全軸の速度を送り返す。
            // サーバーが保持していない水平速度でクライアントの移動を上書きしないよう、本人には Y 速度だけを専用同期する。
            entity.hurtMarked = true;
        }
    }
}
