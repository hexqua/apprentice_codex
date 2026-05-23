package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class MulticastEchoStaffClientRenderState {
    private static final float SHARD_ROTATION_PERIOD_TICKS = 20.0F * 8.0F;
    private static final float SHARD_PULSE_PERIOD_TICKS = 20.0F * 2.5F;
    private static final float SHARD_PULSE_DURATION_TICKS = 20.0F * 0.5F;
    private static final float SHARD_MIN_ALPHA = 0.5F;

    private MulticastEchoStaffClientRenderState() {
    }

    public static float resolveShardRotation(
            @Nullable ItemStack renderingStack,
            @Nullable ItemDisplayContext perspective,
            @Nullable LivingEntity owner,
            float partialTick
    ) {
        var multiplier = resolveShardRotationMultiplier(renderingStack, perspective, owner);
        return resolveTime(partialTick) * Mth.TWO_PI / SHARD_ROTATION_PERIOD_TICKS * multiplier;
    }

    public static float resolveShardAlpha(float partialTick) {
        var phase = resolveTime(partialTick) % SHARD_PULSE_PERIOD_TICKS;
        if (phase >= SHARD_PULSE_DURATION_TICKS) {
            return 1.0F;
        }

        var halfDuration = SHARD_PULSE_DURATION_TICKS * 0.5F;
        if (phase < halfDuration) {
            var ease = sineEaseInOut(phase / halfDuration);
            return Mth.lerp(ease, 1.0F, SHARD_MIN_ALPHA);
        }

        var ease = sineEaseInOut((phase - halfDuration) / halfDuration);
        return Mth.lerp(ease, SHARD_MIN_ALPHA, 1.0F);
    }

    private static float resolveShardRotationMultiplier(
            @Nullable ItemStack renderingStack,
            @Nullable ItemDisplayContext perspective,
            @Nullable LivingEntity owner
    ) {
        if (!isRenderedHeldMulticastEchoStaff(renderingStack, perspective, owner)) {
            return 1.0F;
        }

        if (isOwnerCastingEchoCast(owner, perspective)) {
            return 8.0F;
        }

        var echoSpell = owner.getEffect(EffectRegistry.ECHO_SPELL);
        if (echoSpell == null) {
            return 1.0F;
        }

        return Mth.clamp(1.0F + (echoSpell.getAmplifier() + 1.0F) / 4.0F, 1.0F, 4.0F);
    }

    private static boolean isRenderedHeldMulticastEchoStaff(
            @Nullable ItemStack renderingStack,
            @Nullable ItemDisplayContext perspective,
            @Nullable LivingEntity owner
    ) {
        if (renderingStack == null || renderingStack.isEmpty() || !(renderingStack.getItem() instanceof MulticastEchoStaff)) {
            return false;
        }
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        var hand = resolveRenderedHand(owner, perspective);
        if (hand == null) {
            return ItemStack.isSameItemSameComponents(owner.getMainHandItem(), renderingStack)
                    || ItemStack.isSameItemSameComponents(owner.getOffhandItem(), renderingStack);
        }

        return ItemStack.isSameItemSameComponents(owner.getItemInHand(hand), renderingStack);
    }

    private static boolean isOwnerCastingEchoCast(LivingEntity owner, @Nullable ItemDisplayContext perspective) {
        if (!(owner instanceof Player player)) {
            return false;
        }

        var spellData = ClientMagicData.getSyncedSpellData(player);
        if (!spellData.isCasting() || !Objects.equals(spellData.getCastingSpellId(), SpellRegistry.ECHO_CAST.get().getSpellId())) {
            return false;
        }

        var renderedHand = resolveRenderedHand(owner, perspective);
        if (renderedHand == InteractionHand.MAIN_HAND) {
            return SpellSelectionManager.MAINHAND.equals(spellData.getCastingEquipmentSlot());
        }
        if (renderedHand == InteractionHand.OFF_HAND) {
            return SpellSelectionManager.OFFHAND.equals(spellData.getCastingEquipmentSlot());
        }

        return true;
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(LivingEntity owner, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(owner, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(owner, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(LivingEntity owner, HumanoidArm arm) {
        return owner.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static float resolveTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static float sineEaseInOut(float progress) {
        return 0.5F - 0.5F * Mth.cos(Mth.clamp(progress, 0.0F, 1.0F) * Mth.PI);
    }
}
