package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.world.item.ItemStack;

public final class SwingcastStaffClientEffectState {
    private static final float ORB_NONE_ALPHA = 1.0f;
    private static final float ORB_CONTAIN_ALPHA = 1.0f;

    private SwingcastStaffClientEffectState() {
    }

    public static boolean shouldRenderContainedOrb(ItemStack stack) {
        return MagicTools.getImbuedSpellSchool(stack) != null;
    }

    public static OrbRenderState resolveOrb(ItemStack stack) {
        var color = MagicTools.resolveSchoolTintColor(MagicTools.getImbuedSpellSchool(stack));
        return new OrbRenderState(
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                ORB_CONTAIN_ALPHA
        );
    }

    public static float getEmptyOrbAlpha() {
        return ORB_NONE_ALPHA;
    }

    public record OrbRenderState(float red, float green, float blue, float alpha) {
    }
}
