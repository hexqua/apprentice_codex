package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.server.SPSkillExecutionFeedback;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightWindLeapSkill extends WeaponInnateSkill implements ChargeableSkill {
    private static final ResourceLocation SKILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/weapon_innate/sweeping_edge.png");

    static final int MIN_CHARGING_TICKS = 0;
    static final int MAX_CHARGING_TICKS = 30;
    static final int ALLOWED_MAX_CHARGING_TICKS = 60;
    static final double TARGET_DISTANCE = 2.0D;
    static final double TARGET_RAYCAST_DISTANCE = 8.0D;
    static final double MIN_UPWARD_IMPULSE = 1.0D;
    static final double MAX_UPWARD_IMPULSE = SmashcastScepter.calculateReleaseBounceImpulse(1);

    public EpicFightWindLeapSkill(SkillBuilder<? extends EpicFightWindLeapSkill> builder) {
        super(builder);
    }

    public static SkillBuilder<EpicFightWindLeapSkill> createWindLeapBuilder() {
        return new SkillBuilder<EpicFightWindLeapSkill>()
                .setActivateType(Skill.ActivateType.HELD)
                .setResource(Skill.Resource.WEAPON_CHARGE)
                .setCategory(SkillCategories.WEAPON_INNATE);
    }

    @Override
    public boolean isExecutableState(PlayerPatch<?> playerpatch) {
        return super.isExecutableState(playerpatch)
                && playerpatch.getOriginal().onGround()
                && playerpatch.getOriginal().getMainHandItem().getItem() instanceof SmashcastScepter;
    }

    @Override
    public void startHolding(SkillContainer skillContainer) {
        var playerpatch = skillContainer.getExecutor();
        if (!playerpatch.isLogicalClient()) {
            playerpatch.playAnimationSynchronized(Animations.STEEL_WHIRLWIND_CHARGING, 0.0F);
        }
    }

    @Override
    public void resetHolding(SkillContainer skillContainer) {
        var playerpatch = skillContainer.getExecutor();
        if (playerpatch.isLogicalClient()) {
            playerpatch.getAnimator().stopPlaying(Animations.STEEL_WHIRLWIND_CHARGING);
        } else {
            playerpatch.stopPlaying(Animations.STEEL_WHIRLWIND_CHARGING);
        }
    }

    @Override
    public void onStopHolding(SkillContainer skillContainer, SPSkillExecutionFeedback feedback) {
        var playerpatch = skillContainer.getServerExecutor();
        var chargeTicks = Math.min(playerpatch.getAccumulatedChargeAmount(), getMaxChargingTicks());
        EpicFightSmashcastScepterCompat.launchWindLeap(playerpatch, Math.max(0, chargeTicks));
        cancelOnServer(skillContainer, null);
    }

    @Override
    public int getAllowedMaxChargingTicks() {
        return ALLOWED_MAX_CHARGING_TICKS;
    }

    @Override
    public int getMaxChargingTicks() {
        return MAX_CHARGING_TICKS;
    }

    @Override
    public int getMinChargingTicks() {
        return MIN_CHARGING_TICKS;
    }

    @Override
    public void holdTick(SkillContainer skillContainer) {
        ChargeableSkill.super.holdTick(skillContainer);
    }

    @Override
    public KeyMapping getKeyMapping() {
        return EpicFightKeyMappings.WEAPON_INNATE_SKILL;
    }

    @Override
    public ResourceLocation getSkillTexture() {
        return SKILL_TEXTURE;
    }
}
