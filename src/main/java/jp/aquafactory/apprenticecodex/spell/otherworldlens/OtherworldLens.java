package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OtherworldLens extends AbstractSpell implements IClientBlockTargetingSpell, IChargecastStaffbowIncompatibleSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "otherworld_lens");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(5)
            .build();

    public OtherworldLens() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 35;
        manaCostPerLevel = 0;
        castTime = 300;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0))
        );
    }

    static float getRange(){
        // レンズを設置できる最大距離.
        return 8;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange();
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
        return Optional.of(SoundRegistry.ABYSSAL_TELEPORT.get());
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
    public ICastDataSerializable getEmptyCastData() {
        return new OtherworldLensCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        var target = BlockTargetingHelper.getValidatedPendingTarget(level, entity, getSpellResource(), getRange());
        if (target.isEmpty()) {
            sendCantPlace(serverPlayer);
            return false;
        }
        var hitPos = target.get().getHitBlockPos();
        var hitFace = target.get().getHitFace();
        if (hitPos == null || hitFace == null || !level.getBlockState(hitPos.relative(hitFace)).isAir()) {
            sendCantPlace(serverPlayer);
            return false;
        }

        var validated = OtherworldLensTargetSafety.validate(level, entity, target.get(), getRange());
        if (validated.isEmpty()) {
            sendInvalidTarget(serverPlayer, target.get());
            return false;
        }

        var castData = new OtherworldLensCastData();
        castData.copyFrom(target.get());
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (!(entity instanceof ServerPlayer serverPlayer)
                || !(playerMagicData.getAdditionalCastData() instanceof OtherworldLensCastData castData)) {
            return;
        }

        var validated = OtherworldLensTargetSafety.validate(level, entity, castData, getRange());
        if (validated.isEmpty()) {
            return;
        }

        var target = validated.get();
        OtherworldLensSessionManager.finish(serverPlayer, OtherworldLensSessionManager.EndReason.CANCELLED);
        if (!level.setBlockAndUpdate(target.placePos(), BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState())) {
            return;
        }
        castData.sessionId = OtherworldLensSessionManager.start(
                serverPlayer, target.placePos(), target.hitPos(), target.targetBlock()
        );
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer serverPlayer) {
            var sessionId = playerMagicData.getAdditionalCastData() instanceof OtherworldLensCastData castData
                    ? castData.sessionId
                    : null;
            var reason = OtherworldLensSessionManager.validate(serverPlayer, sessionId);
            if (reason != null) {
                sendCancellationMessage(serverPlayer, reason);
                OtherworldLensSessionManager.finish(serverPlayer, sessionId, reason);
                Utils.serverSideCancelCast(serverPlayer);
                return;
            }
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                     boolean cancelled) {
        if (entity instanceof ServerPlayer serverPlayer) {
            var sessionId = playerMagicData.getAdditionalCastData() instanceof OtherworldLensCastData castData
                    ? castData.sessionId
                    : null;
            OtherworldLensSessionManager.finish(
                    serverPlayer,
                    sessionId,
                    cancelled ? OtherworldLensSessionManager.EndReason.CANCELLED
                            : OtherworldLensSessionManager.EndReason.COMPLETED
            );
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void sendCantPlace(ServerPlayer player) {
        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.apprenticecodex.cant_place", getDisplayName(player)).withStyle(ChatFormatting.RED)
        ));
    }

    private void sendInvalidTarget(ServerPlayer player, BlockTargetData target) {
        var hitPos = target.getHitBlockPos();
        var targetName = hitPos == null
                ? Component.translatable("block.minecraft.air")
                : player.level().getBlockState(hitPos).getBlock().getName();
        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.apprenticecodex.otherworld_lens.invalid_target", targetName)
                        .withStyle(ChatFormatting.RED)
        ));
    }

    private static void sendCancellationMessage(ServerPlayer player, OtherworldLensSessionManager.EndReason reason) {
        var key = switch (reason) {
            case TOO_FAR -> "ui.apprenticecodex.otherworld_lens.canceled_away";
            case LENS_MISSING -> "ui.apprenticecodex.otherworld_lens.canceled_break";
            case LENS_REPLACED -> "ui.apprenticecodex.otherworld_lens.canceled_other";
            default -> null;
        };
        if (key != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key).withStyle(ChatFormatting.RED)));
        }
    }

    public static final class OtherworldLensCastData extends BlockTargetData {
        private UUID sessionId;

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            super.writeToBuffer(buffer);
            buffer.writeBoolean(sessionId != null);
            if (sessionId != null) {
                buffer.writeUUID(sessionId);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            super.readFromBuffer(buffer);
            sessionId = buffer.readBoolean() ? buffer.readUUID() : null;
        }

        @Override
        public void reset() {
            super.reset();
            sessionId = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = super.serializeNBT(provider);
            if (sessionId != null) {
                tag.putUUID("SessionId", sessionId);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            super.deserializeNBT(provider, tag);
            sessionId = tag.hasUUID("SessionId") ? tag.getUUID("SessionId") : null;
        }
    }
}
