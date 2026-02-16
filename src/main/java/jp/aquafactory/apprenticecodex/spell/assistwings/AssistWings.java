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
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
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
        spellPowerPerLevel = 25;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 0;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.jumps_count", Utils.stringTruncation(getJumpCount(spellLevel, caster), 1)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private int getJumpCount(int spellLevel, LivingEntity entity){
        // todo:バランス調整.
        return 5;
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
        return Optional.of(SoundEvents.HORSE_JUMP);
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Capabilities.withSpellData(entity, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, spell -> {
            // まずは翼が既にいるかどうかチェック.
            var wing = level.getEntity(spell.localEntityId);
            if (wing == null || wing.isRemoved() || !(wing instanceof AssistWingsWingEntity)) {
                var summonWing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), level, entity);
                level.addFreshEntity(summonWing);
                spell.localEntityId = summonWing.getId();
            }

            // 足をつけていればそのジャンプは空中ジャンプとして処理しない.
            if (!entity.onGround()) {
                ++spell.doneJump;
            }

            // ジャンプ高度は気持ち高め.
            var jumpHeight = 0.6f + entity.getJumpBoostPower();
            var currentDelta = entity.getDeltaMovement();
            entity.setDeltaMovement(currentDelta.x, jumpHeight, currentDelta.z);
            entity.hasImpulse = true;
            entity.hurtMarked = true;
            entity.fallDistance = 0;
        }));

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
