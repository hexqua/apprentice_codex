package jp.aquafactory.apprenticecodex.spell.senseevil;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SenseEvil extends AbstractSpell {
    private static final float MIN_ENTITY_HIGHLIGHT_SCALE = 0.85f;
    private static final float MAX_ENTITY_HIGHLIGHT_SCALE = 1.35f;
    private static final float SPAWNER_HIGHLIGHT_SCALE = 1.10f;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sense_evil");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public SenseEvil() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 70;
        manaCostPerLevel = 25;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    private double getRange(int spellLevel, LivingEntity entity){
        return 16f + 16f * getSpellPower(spellLevel, entity) / 100.0f;
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
        return Optional.of(SoundRegistry.SENSE_EVIL.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer serverPlayer) {
            var highlights = collectHighlights(serverLevel, spellLevel, entity);
            if (!highlights.isEmpty()) {
                Networks.sendToPlayer(serverPlayer, new SenseEvilHighlightsPacket(highlights));
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private List<SenseEvilHighlightsPacket.TargetData> collectHighlights(ServerLevel level, int spellLevel, LivingEntity caster) {
        var range = getRange(spellLevel, caster);
        var highlights = new ArrayList<SenseEvilHighlightsPacket.TargetData>();
        collectUndeadEntities(level, caster, range, highlights);
        collectUndeadSpawners(level, caster, range, highlights);
        return highlights;
    }

    private static void collectUndeadEntities(ServerLevel level, LivingEntity caster, double range,
                                              List<SenseEvilHighlightsPacket.TargetData> highlights) {
        var searchBox = caster.getBoundingBox().inflate(range);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, searchBox, living ->
                living.isAlive() && living != caster && living.getType().is(EntityTypeTags.UNDEAD))) {
            var scale = Mth.clamp((float) target.getBbWidth() * 1.2f, MIN_ENTITY_HIGHLIGHT_SCALE, MAX_ENTITY_HIGHLIGHT_SCALE);
            highlights.add(new SenseEvilHighlightsPacket.TargetData(target.getBoundingBox().getCenter(), scale));
        }
    }

    private static void collectUndeadSpawners(ServerLevel level, LivingEntity caster, double range,
                                              List<SenseEvilHighlightsPacket.TargetData> highlights) {
        var origin = caster.position();
        var rangeSqr = range * range;
        var minChunkX = SectionPos.blockToSectionCoord(Mth.floor(origin.x - range));
        var maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(origin.x + range));
        var minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(origin.z - range));
        var maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(origin.z + range));

        // 発動時スナップショットだけ欲しいので、未読込チャンクは触らず現在の block entity 一覧だけを使う。
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
                        continue;
                    }

                    var center = Vec3.atCenterOf(spawner.getBlockPos());
                    if (center.distanceToSqr(origin) > rangeSqr) {
                        continue;
                    }
                    if (isUndeadSpawner(level, spawner)) {
                        highlights.add(new SenseEvilHighlightsPacket.TargetData(center, SPAWNER_HIGHLIGHT_SCALE));
                    }
                }
            }
        }
    }

    private static boolean isUndeadSpawner(ServerLevel level, SpawnerBlockEntity spawner) {
        var tag = new CompoundTag();
        spawner.getSpawner().save(tag);
        if (containsUndeadSpawnData(level, tag.getCompound("SpawnData"))) {
            return true;
        }

        var spawnPotentials = tag.getList("SpawnPotentials", Tag.TAG_COMPOUND);
        for (int i = 0; i < spawnPotentials.size(); i++) {
            var wrappedData = spawnPotentials.getCompound(i);
            if (containsUndeadSpawnData(level, wrappedData.getCompound("data"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsUndeadSpawnData(ServerLevel level, CompoundTag spawnDataTag) {
        if (spawnDataTag.getAllKeys().isEmpty()) {
            return false;
        }
        if (spawnDataTag.contains("entity", Tag.TAG_COMPOUND)) {
            return isUndeadEntity(spawnDataTag.getCompound("entity"));
        }
        return isUndeadEntity(spawnDataTag);
    }

    private static boolean isUndeadEntity(CompoundTag entityTag) {
        var entityType = EntityType.by(entityTag);
        if (entityType.isEmpty()) {
            return false;
        }
        return entityType.get().is(EntityTypeTags.UNDEAD);
    }
}
