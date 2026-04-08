package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SearchBeacon extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "search_beacon");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(60)
            .build();

    public SearchBeacon() {
        baseSpellPower = 500;
        spellPowerPerLevel = 500;
        baseManaCost = 100;
        manaCostPerLevel = 40;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.search_beacon.initial_distance", Utils.stringTruncation(getInitialRange(spellLevel, caster), 0)),
                Component.translatable("ui.apprenticecodex.search_beacon.addition_distance", Utils.stringTruncation(getAdditionalRangePerItem(spellLevel, caster), 0)),
                Component.translatable("ui.apprenticecodex.max_search_distance", SearchBeaconSearchService.getMaxSearchRange())
        );
    }

    public int getInitialRange(int spellLevel, LivingEntity entity) {
        return getBaseRange(spellLevel, entity) * 2;
    }

    public int getAdditionalRangePerItem(int spellLevel, LivingEntity entity) {
        return getBaseRange(spellLevel, entity);
    }

    private int getBaseRange(int spellLevel, LivingEntity entity) {
        return Math.round(getSpellPower(spellLevel, entity));
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
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer serverPlayer) {
            var summonPosition = resolveSummonPosition(serverLevel, entity);
            if (summonPosition == null) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.cant_place", getDisplayName(serverPlayer))
                                .withStyle(ChatFormatting.RED)
                ));
            } else {
                var beacon = new SearchBeaconEntity(EntityRegistry.SEARCH_BEACON.get(), serverLevel);
                beacon.setOwner(serverPlayer);
                beacon.setAnchor(summonPosition);
                beacon.setSearchTuning(
                        getInitialRange(spellLevel, entity),
                        getAdditionalRangePerItem(spellLevel, entity)
                );
                beacon.moveTo(summonPosition.x, summonPosition.y, summonPosition.z, entity.getYRot(), 0.0f);
                serverLevel.addFreshEntity(beacon);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private @Nullable Vec3 resolveSummonPosition(ServerLevel level, LivingEntity caster) {
        var hit = caster.pick(12.0, 1.0f, false);
        Vec3 desired = switch (hit.getType()) {
            case BLOCK -> {
                var blockHit = (BlockHitResult) hit;
                yield blockHit.getLocation().add(
                        blockHit.getDirection().getStepX() * 0.35,
                        Math.max(0, blockHit.getDirection().getStepY()) * 0.35 + 0.05,
                        blockHit.getDirection().getStepZ() * 0.35
                );
            }
            case ENTITY, MISS -> caster.getEyePosition().add(caster.getLookAngle().scale(4.5));
        };

        var halfWidth = SearchBeaconEntity.WIDTH / 2.0;
        for (int i = 0; i < 5; i++) {
            var y = desired.y + i * 0.25;
            var box = new AABB(
                    desired.x - halfWidth,
                    y,
                    desired.z - halfWidth,
                    desired.x + halfWidth,
                    y + SearchBeaconEntity.HEIGHT,
                    desired.z + halfWidth
            );
            if (level.noCollision(null, box) && level.getEntities(caster, box, EntitySelector.NO_SPECTATORS).isEmpty()) {
                return new Vec3(desired.x, y, desired.z);
            }
        }

        return null;
    }
}
