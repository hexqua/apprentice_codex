package jp.aquafactory.apprenticecodex.spell.edgedancer;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class EdgeDancer extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "edge_dancer");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(false)
            .build();

    public EdgeDancer() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 40;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel, caster), 1))
        );
    }

    int getDuration(int spellLevel, LivingEntity caster) {
        return Math.round(20 * 60 * getSpellPower(spellLevel, caster) / 100.0f);
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
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new EdgeDancerCastData();
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity,
                                @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
                EdgeDancerManager.deactivate(serverPlayer, true);
            } else {
                EdgeDancerManager.activate(serverPlayer, spellLevel, castSource, playerMagicData, this);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void castSpell(Level world, int spellLevel, ServerPlayer serverPlayer, CastSource castSource,
                          boolean triggerCooldown) {
        var magicData = MagicData.getPlayerMagicData(serverPlayer);
        var wasEdgeDancerRecast = magicData.getPlayerRecasts().hasRecastForSpell(this);
        super.castSpell(world, spellLevel, serverPlayer, castSource, triggerCooldown);
        if (wasEdgeDancerRecast && hasGreaterConjurersTalisman(serverPlayer)
                && magicData.getPlayerCooldowns().removeCooldown(getSpellId())) {
            magicData.getPlayerCooldowns().syncToPlayer(serverPlayer);
        }
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return entity instanceof Player player
                && (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)
                || SpellSideEdge.isSpellSideEdge(player.getMainHandItem()))
                && super.checkPreCastConditions(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        EdgeDancerManager.deactivate(serverPlayer, false);
        if (hasGreaterConjurersTalisman(serverPlayer)) {
            return;
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    private static boolean hasGreaterConjurersTalisman(ServerPlayer serverPlayer) {
        return io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()
                .isEquippedBy(serverPlayer);
    }

    public static class EdgeDancerCastData implements ICastDataSerializable {
        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
        }

        @Override
        public void reset() {
        }

        @Override
        public CompoundTag serializeNBT() {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
        }
    }
}
