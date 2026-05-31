package jp.aquafactory.apprenticecodex.item.zenithstaff;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.ZenithStaff;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ZenithStaffClientRenderState {
    private static final float SCHOOL_COLOR_PERIOD_TICKS = 40.0F;
    private static final float STONE_ALPHA = 0.85F;

    private ZenithStaffClientRenderState() {
    }

    public static StoneRenderState resolveStone(
            @Nullable ItemStack renderingStack,
            @Nullable ItemDisplayContext perspective,
            float partialTick
    ) {
        if (perspective == ItemDisplayContext.GUI
                || renderingStack == null
                || renderingStack.isEmpty()
                || !(renderingStack.getItem() instanceof ZenithStaff)) {
            return StoneRenderState.hidden();
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !isRenderedHeldStack(player, renderingStack, perspective)) {
            return StoneRenderState.hidden();
        }

        var snapshot = ZenithStaffPowerHelper.resolvePowerSnapshot(player);
        if (!snapshot.hasSchoolBonus()) {
            return StoneRenderState.hidden();
        }

        return resolveSchoolColor(snapshot.strongestSchools(), resolveTime(partialTick));
    }

    private static StoneRenderState resolveSchoolColor(List<SchoolType> schools, float time) {
        if (schools.isEmpty()) {
            return StoneRenderState.hidden();
        }

        if (schools.size() == 1) {
            return toState(MagicTools.resolveSchoolTintColor(schools.get(0)), STONE_ALPHA);
        }

        long segment = (long)Math.floor(time / SCHOOL_COLOR_PERIOD_TICKS);
        int fromIndex = (int)(segment % schools.size());
        int toIndex = (fromIndex + 1) % schools.size();
        float progress = (time % SCHOOL_COLOR_PERIOD_TICKS) / SCHOOL_COLOR_PERIOD_TICKS;
        float eased = easeInOutSine(progress);

        return lerpColor(
                MagicTools.resolveSchoolTintColor(schools.get(fromIndex)),
                MagicTools.resolveSchoolTintColor(schools.get(toIndex)),
                eased,
                STONE_ALPHA
        );
    }

    private static StoneRenderState lerpColor(int fromColor, int toColor, float progress, float alpha) {
        float fromRed = ((fromColor >> 16) & 0xFF) / 255.0F;
        float fromGreen = ((fromColor >> 8) & 0xFF) / 255.0F;
        float fromBlue = (fromColor & 0xFF) / 255.0F;
        float toRed = ((toColor >> 16) & 0xFF) / 255.0F;
        float toGreen = ((toColor >> 8) & 0xFF) / 255.0F;
        float toBlue = (toColor & 0xFF) / 255.0F;

        return new StoneRenderState(
                true,
                Mth.lerp(progress, fromRed, toRed),
                Mth.lerp(progress, fromGreen, toGreen),
                Mth.lerp(progress, fromBlue, toBlue),
                alpha
        );
    }

    private static StoneRenderState toState(int color, float alpha) {
        return new StoneRenderState(
                true,
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                alpha
        );
    }

    private static float resolveTime(float partialTick) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return partialTick;
        }
        return minecraft.level.getGameTime() + partialTick;
    }

    private static boolean isRenderedHeldStack(Player player, ItemStack renderingStack, @Nullable ItemDisplayContext perspective) {
        var hand = resolveRenderedHand(player, perspective);
        return hand != null && matchesRenderingStack(player.getItemInHand(hand), renderingStack);
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(Player player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(Player player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static boolean matchesRenderingStack(ItemStack expectedHandStack, ItemStack renderingStack) {
        return !expectedHandStack.isEmpty() && ItemStack.isSameItemSameTags(expectedHandStack, renderingStack);
    }

    private static float easeInOutSine(float progress) {
        return 0.5F - 0.5F * Mth.cos(Mth.clamp(progress, 0.0F, 1.0F) * Mth.PI);
    }

    public record StoneRenderState(boolean visible, float red, float green, float blue, float alpha) {
        private static final StoneRenderState HIDDEN = new StoneRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F);

        public static StoneRenderState hidden() {
            return HIDDEN;
        }
    }
}
