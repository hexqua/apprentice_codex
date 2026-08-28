package jp.aquafactory.apprenticecodex.spell.archermultiple;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArcherMultiple  extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "archer_multiple");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public ArcherMultiple() {
        baseSpellPower = 200;
        spellPowerPerLevel = 50;
        manaCostPerLevel = 30;
        baseManaCost = 70;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.summon_count", getSummonCount()),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ARCHER_MULTIPLE);
    }


    private int getSummonCount(){
        // 数は総出力ダメージに強く影響するので基本固定.
        return 4;
    }

    public int getDuration(){
        return 20 * 30;
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
        return Optional.of(getSchoolType().getCastSound());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ArcherMultipleCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        ArcherMultipleManager.finishRecast(serverPlayer, castDataSerializable);
        // 短時間の固定効果として扱うため、timeoutやcounterspellでも必ず通常cooldownを適用する。
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            // リキャスト判定時はメッセージをやターゲット関連を出さないようにする.
            return true;
        }

        if (!Utils.preCastTargetHelper(level, entity, playerMagicData, this, 64, 1, false)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.archer_multiple.spell_target_none", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }
        }
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this) && level instanceof ServerLevel serverLevel
                && entity instanceof ServerPlayer player) {
            var castData = new ArcherMultipleCastData();

            Entity targetEntity = null;
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                targetEntity = castTargetingData.getTarget(serverLevel);
            }

            for(var count = 0; count < getSummonCount(); ++count){
                var summonTestBow = new ArcherMultipleBowEntity(
                        EntityRegistry.ARCHER_MULTIPLE_BOW.get(),
                        level, entity, count, getSummonCount()
                );
                summonTestBow.setPriorityTarget(targetEntity);
                summonTestBow.setDamage(getDamage(spellLevel, entity));
                ArcherMultipleManager.initialize(player, summonTestBow, getDuration());
                if (serverLevel.addFreshEntity(summonTestBow)) {
                    castData.bindBow(summonTestBow);
                }
            }

            var recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity),
                    getDuration(), castSource, castData);
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static class ArcherMultipleCastData implements ICastDataSerializable {
        private final java.util.ArrayList<UUID> bowUuids = new java.util.ArrayList<>();
        private ResourceLocation dimension;

        void bindBow(ArcherMultipleBowEntity bow) {
            if (!bowUuids.contains(bow.getUUID())) {
                bowUuids.add(bow.getUUID());
            }
            dimension = bow.level().dimension().location();
        }

        boolean matches(ArcherMultipleBowEntity bow) {
            return bowUuids.contains(bow.getUUID())
                    && dimension != null
                    && dimension.equals(bow.level().dimension().location());
        }

        boolean removeBow(UUID bowUuid) {
            bowUuids.remove(bowUuid);
            return bowUuids.isEmpty();
        }

        public List<UUID> getBowUuids() {
            return List.copyOf(bowUuids);
        }

        public ResourceLocation getDimension() {
            return dimension;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeVarInt(bowUuids.size());
            bowUuids.forEach(buffer::writeUUID);
            buffer.writeBoolean(dimension != null);
            if (dimension != null) buffer.writeResourceLocation(dimension);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            bowUuids.clear();
            var count = buffer.readVarInt();
            for (var i = 0; i < count; i++) bowUuids.add(buffer.readUUID());
            dimension = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        }

        @Override
        public void reset() {
            bowUuids.clear();
            dimension = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            var tag = new CompoundTag();
            var bows = new net.minecraft.nbt.ListTag();
            bowUuids.forEach(uuid -> {
                var bow = new CompoundTag();
                bow.putUUID("Uuid", uuid);
                bows.add(bow);
            });
            tag.put("Bows", bows);
            if (dimension != null) tag.putString("Dimension", dimension.toString());
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
            bowUuids.clear();
            var bows = tag.getList("Bows", net.minecraft.nbt.Tag.TAG_COMPOUND);
            bows.forEach(raw -> {
                if (raw instanceof CompoundTag bow && bow.hasUUID("Uuid")) bowUuids.add(bow.getUUID("Uuid"));
            });
            dimension = tag.contains("Dimension") ? ResourceLocation.tryParse(tag.getString("Dimension")) : null;
        }
    }
}
