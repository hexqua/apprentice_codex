package jp.aquafactory.apprenticecodex.common.spells.magelight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

public class MageLight extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mage_light");
    private static final double PLACE_RANGE = 10.0;

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.5)
            .build();

    public MageLight() {
        baseSpellPower = 100;
        spellPowerPerLevel = 0;
        baseManaCost = 10;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) / 10.0f;
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
        // todo:それっぽい音を割り当てる.
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // todo:ここに松明を置けるかの判定を先に行わせる.
        if (findPlacePos(level, entity).isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.mage_light.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }


    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        findPlacePos(level, entity)
                .ifPresent(pos -> {
                    level.setBlockAndUpdate(pos, BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState());
                    AudioTools.playSoundFromBlock(level, pos.getCenter(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS);
                }
        );
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<BlockPos> findPlacePos(Level level, LivingEntity entity) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(PLACE_RANGE));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }

        var hitPos = hit.getBlockPos();
        var hitState = level.getBlockState(hitPos);
        var placePos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hit.getDirection());
        var placeState = level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return Optional.empty();
        }

        var torchState = BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState();
        if (!torchState.canSurvive(level, placePos)) {
            return Optional.empty();
        }

        return Optional.of(placePos);
    }
}
