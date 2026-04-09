package jp.aquafactory.apprenticecodex.spell.harvestmoon;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class HarvestMoon extends AbstractSpell {
    private static final int BLOCK_BUDGET_PER_TICK = 32;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "harvest_moon");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(30)
            .build();

    public HarvestMoon() {
        baseSpellPower = 12;
        spellPowerPerLevel = 12;
        baseManaCost = 60;
        manaCostPerLevel = 20;
        castTime = 50;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    private double getRange(int spellLevel, LivingEntity entity){
        return 8 * getSpellPower(spellLevel, entity) /10.0f;
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
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.SHADOW_SLASH.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer serverPlayer) {
            var range = Math.max(1, Mth.floor(getRange(spellLevel, entity)));
            var actions = HarvestMoonTargetCollector.collect(serverLevel, serverPlayer, range);
            if (!actions.isEmpty()) {
                // 1.20.1 Forge では ServerPlayerGameMode を経由した右クリック/破壊を使うため、
                // 詠唱時点のメインハンドをコピーして以後の tick 処理に渡す。
                var job = new HarvestMoonJob(
                        serverPlayer,
                        getWorkingMainHandStack(serverPlayer),
                        actions,
                        new net.minecraft.world.phys.Vec3(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ()),
                        BLOCK_BUDGET_PER_TICK
                );
                HarvestMoonJobManager.submit(serverLevel, job);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static ItemStack getWorkingMainHandStack(ServerPlayer player) {
        var mainHand = player.getMainHandItem();
        return mainHand.isEmpty() ? ItemStack.EMPTY : mainHand.copy();
    }
}
