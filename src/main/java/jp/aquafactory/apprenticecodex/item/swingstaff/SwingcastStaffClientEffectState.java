package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SwingcastStaffClientEffectState {
    private static final float CORE_IDLE_BASE = 0.82f;
    private static final float CORE_IDLE_PULSE = 0.18f;
    private static final float CORE_IDLE_SPEED = 0.18f;
    private static final float COOLDOWN_MIN_STRENGTH = 0.25f;
    private static final float COOLDOWN_MAX_STRENGTH = 0.75f;
    private static final float COOLDOWN_PULSE_PERIOD_TICKS = 8.0f;
    private static final float COOLDOWN_FADE_TICKS = 12.0f;
    private static final float ORB_NONE_ALPHA = 1.0f;
    private static final float ORB_CONTAIN_ALPHA = 1.0f;

    private SwingcastStaffClientEffectState() {
    }

    public static CoreRenderState resolveCore(ItemStack stack, float partialTick) {
        float time = resolveTime(partialTick);
        var cooldownRemaining = resolveCooldownRemaining(stack, partialTick);
        if (cooldownRemaining > 0.0f) {
            var pulse = (Mth.sin((float) (time * (Math.PI * 2.0 / COOLDOWN_PULSE_PERIOD_TICKS))) + 1.0f) * 0.5f;
            var strength = Mth.lerp(pulse, COOLDOWN_MIN_STRENGTH, COOLDOWN_MAX_STRENGTH);
            if (cooldownRemaining < COOLDOWN_FADE_TICKS) {
                strength *= Mth.clamp(cooldownRemaining / COOLDOWN_FADE_TICKS, 0.0f, 1.0f);
            }
            return new CoreRenderState(strength, 0.12f * strength, 0.12f * strength, 0.95f);
        }

        var brightness = CORE_IDLE_BASE + CORE_IDLE_PULSE * (0.5f + 0.5f * Mth.sin(time * CORE_IDLE_SPEED));
        return new CoreRenderState(brightness, brightness, brightness, 0.95f);
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

    private static float resolveTime(float partialTick) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return partialTick;
        }
        return minecraft.level.getGameTime() + partialTick;
    }

    private static float resolveCooldownRemaining(ItemStack stack, float partialTick) {
        var spellId = resolveSpellId(stack);
        if (spellId == null) {
            return 0.0f;
        }

        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spellId);
        if (cooldown == null) {
            return 0.0f;
        }
        return Math.max(0.0f, cooldown.getCooldownRemaining() - partialTick);
    }

    @Nullable
    private static String resolveSpellId(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return null;
        }

        return spellData.getSpell().getSpellId();
    }

    public record CoreRenderState(float red, float green, float blue, float alpha) {
    }

    public record OrbRenderState(float red, float green, float blue, float alpha) {
    }
}
