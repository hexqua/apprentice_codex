package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServantGaze extends AbstractSpell{
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "servant_gaze");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public ServantGaze() {
        baseSpellPower = 200;
        spellPowerPerLevel = 150;
        baseManaCost = 30;
        manaCostPerLevel = 15;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(), 0)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SERVANT_GAZE);
    }

    private double getRadius() {
        // 強化はされない前提
        return 32;
    }

    private int getDuration() {
        // RaiseDead感覚で使う想定なので長め固定(5分)
        return 20 * 60 * 5;
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
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ServantGazeCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void castSpell(Level world, int spellLevel, ServerPlayer serverPlayer, CastSource castSource,
                          boolean triggerCooldown) {
        var magicData = MagicData.getPlayerMagicData(serverPlayer);
        var wasServantGazeRecast = magicData.getPlayerRecasts().hasRecastForSpell(this);
        super.castSpell(world, spellLevel, serverPlayer, castSource, triggerCooldown);
        if (wasServantGazeRecast && hasGreaterConjurersTalisman(serverPlayer)
                && magicData.getPlayerCooldowns().removeCooldown(getSpellId())) {
            magicData.getPlayerCooldowns().syncToPlayer(serverPlayer);
        }
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        // todo:リキャスト終了処理.
        if (hasGreaterConjurersTalisman(serverPlayer)) {
            return;
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    private static boolean hasGreaterConjurersTalisman(ServerPlayer serverPlayer) {
        return io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()
                .isEquippedBy(serverPlayer);
    }

    public static class ServantGazeCastData implements ICastDataSerializable {
        private BlockPos position;
        private UUID staffUuid;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(position != null);
            if (position != null) {
                friendlyByteBuf.writeBlockPos(position);
            }
            friendlyByteBuf.writeBoolean(staffUuid != null);
            if (staffUuid != null) {
                friendlyByteBuf.writeUUID(staffUuid);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            position = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readBlockPos() : null;
            staffUuid = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readUUID() : null;
        }

        @Override
        public void reset() {
            position = null;
            staffUuid = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            if (position != null) {
                tag.putInt("PositionX", position.getX());
                tag.putInt("PositionY", position.getY());
                tag.putInt("PositionZ", position.getZ());
            }
            if (staffUuid != null) {
                tag.putUUID("StaffUUID", staffUuid);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains("PositionX")) {
                position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
            } else {
                position = null;
            }
            staffUuid = nbt.hasUUID("StaffUUID") ? nbt.getUUID("StaffUUID") : null;
        }
    }
}
