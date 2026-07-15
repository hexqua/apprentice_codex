package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RevolvercastStaffClientEffectState {
    private static final float STAR_PERIOD_TICKS = 40.0F;
    private static final float STAR_MIN_BRIGHTNESS = 0.9F;
    private static final float STAR_MAX_BRIGHTNESS = 1.0F;
    private static final float ORB_TRANSITION_TICKS = 10.0F;
    private static final float ORB_ALPHA = 1.0F;
    private static final float CYLINDER_ALPHA = 0.85F;

    private static final Map<Long, OrbTransitionState> ORB_TRANSITIONS = new HashMap<>();

    private RevolvercastStaffClientEffectState() {
    }

    public static ColorRenderState resolveStar(float partialTick) {
        var time = resolveTime(partialTick);
        var brightness = STAR_MIN_BRIGHTNESS
                + (STAR_MAX_BRIGHTNESS - STAR_MIN_BRIGHTNESS)
                * (0.5F + 0.5F * Mth.cos(time * (Mth.PI * 2.0F) / STAR_PERIOD_TICKS));
        return ColorRenderState.visible(brightness, brightness, brightness, 1.0F);
    }

    public static ColorRenderState resolveOrb(ItemStack stack, float partialTick) {
        var school = resolveSchoolAtOffset(stack, 0);
        if (school == null) {
            return ColorRenderState.hidden();
        }

        var targetColor = MagicTools.resolveSchoolTintColor(school);
        var instanceId = GeoItem.getId(stack);
        if (instanceId == Long.MAX_VALUE) {
            return toVisibleState(targetColor, ORB_ALPHA);
        }

        var time = resolveTime(partialTick);
        var schoolId = school.getId();
        var transition = ORB_TRANSITIONS.get(instanceId);
        if (transition == null) {
            transition = new OrbTransitionState(schoolId, targetColor, targetColor, time);
            ORB_TRANSITIONS.put(instanceId, transition);
            trimStateMapIfNeeded(ORB_TRANSITIONS);
        } else if (!schoolId.equals(transition.targetSchoolId)) {
            transition.fromColor = transition.currentColor(time);
            transition.targetColor = targetColor;
            transition.targetSchoolId = schoolId;
            transition.startTime = time;
        }

        return toVisibleState(transition.currentColor(time), ORB_ALPHA);
    }

    public static ColorRenderState resolveCylinder(ItemStack stack, int cylinderNumber) {
        var school = resolveSchoolAtOffset(stack, resolveCylinderOffset(cylinderNumber));
        return school == null
                ? ColorRenderState.hidden()
                : toVisibleState(MagicTools.resolveSchoolTintColor(school), CYLINDER_ALPHA);
    }

    @Nullable
    private static SchoolType resolveSchoolAtOffset(ItemStack stack, int offset) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof RevolvercastStaff)) {
            return null;
        }

        var schools = collectValidScrollSchoolsFromSelected(stack);
        if (schools.isEmpty()) {
            return null;
        }

        return schools.get(Math.floorMod(offset, schools.size()));
    }

    private static int resolveCylinderOffset(int cylinderNumber) {
        return cylinderNumber >= 1 && cylinderNumber <= 3 ? cylinderNumber : 0;
    }

    private static ArrayList<SchoolType> collectValidScrollSchoolsFromSelected(ItemStack stack) {
        var schools = new ArrayList<SchoolType>();
        var enabledSlotCount = RevolvercastStaff.getEnabledCalibrationScrollSlotCount(stack);
        if (enabledSlotCount <= 0) {
            return schools;
        }

        var selectedIndex = resolveSelectedIndex(stack, enabledSlotCount);
        if (selectedIndex < 0) {
            return schools;
        }

        for (var offset = 0; offset < enabledSlotCount; ++offset) {
            var slot = (selectedIndex + offset) % enabledSlotCount;
            var school = resolveScrollSchool(RevolvercastStaff.getCalibrationScroll(stack, slot));
            if (school != null) {
                schools.add(school);
            }
        }
        return schools;
    }

    private static int resolveSelectedIndex(ItemStack stack, int enabledSlotCount) {
        var selectedIndex = RevolvercastStaff.getSelectedScrollIndex(stack);
        if (selectedIndex >= 0
                && selectedIndex < enabledSlotCount
                && resolveScrollSchool(RevolvercastStaff.getCalibrationScroll(stack, selectedIndex)) != null) {
            return selectedIndex;
        }

        var startIndex = selectedIndex < 0 ? 0 : selectedIndex + 1;
        for (var offset = 0; offset < enabledSlotCount; ++offset) {
            var slot = (startIndex + offset) % enabledSlotCount;
            if (resolveScrollSchool(RevolvercastStaff.getCalibrationScroll(stack, slot)) != null) {
                return slot;
            }
        }
        return -1;
    }

    @Nullable
    private static SchoolType resolveScrollSchool(ItemStack scrollStack) {
        if (scrollStack == null || scrollStack.isEmpty()) {
            return null;
        }

        var spellContainer = ISpellContainer.get(scrollStack);
        if (spellContainer == null) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return null;
        }
        return spellData.getSpell().getSchoolType();
    }

    private static ColorRenderState toVisibleState(int color, float alpha) {
        return ColorRenderState.visible(
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

    private static int lerpColor(int fromColor, int toColor, float progress) {
        var clamped = Mth.clamp(progress, 0.0F, 1.0F);
        var red = Mth.lerp(clamped, (fromColor >> 16) & 0xFF, (toColor >> 16) & 0xFF);
        var green = Mth.lerp(clamped, (fromColor >> 8) & 0xFF, (toColor >> 8) & 0xFF);
        var blue = Mth.lerp(clamped, fromColor & 0xFF, toColor & 0xFF);
        return ((int)red << 16) | ((int)green << 8) | (int)blue;
    }

    private static <T> void trimStateMapIfNeeded(Map<Long, T> states) {
        if (states.size() <= 128) {
            return;
        }

        var iterator = states.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static final class OrbTransitionState {
        private ResourceLocation targetSchoolId;
        private int fromColor;
        private int targetColor;
        private float startTime;

        private OrbTransitionState(ResourceLocation targetSchoolId, int fromColor, int targetColor, float startTime) {
            this.targetSchoolId = targetSchoolId;
            this.fromColor = fromColor;
            this.targetColor = targetColor;
            this.startTime = startTime;
        }

        private int currentColor(float time) {
            return lerpColor(fromColor, targetColor, (time - startTime) / ORB_TRANSITION_TICKS);
        }
    }

    public record ColorRenderState(boolean visible, float red, float green, float blue, float alpha) {
        private static final ColorRenderState HIDDEN = new ColorRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F);

        private static ColorRenderState visible(float red, float green, float blue, float alpha) {
            return new ColorRenderState(true, red, green, blue, alpha);
        }

        public static ColorRenderState hidden() {
            return HIDDEN;
        }
    }
}
