package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MithrilFreecastStaffClientRenderState {
    private static final float CORE_IDLE_BASE = 0.82f;
    private static final float CORE_IDLE_PULSE = 0.18f;
    private static final float CORE_IDLE_SPEED = 0.18f;
    private static final float COOLDOWN_MIN_STRENGTH = 0.25f;
    private static final float COOLDOWN_MAX_STRENGTH = 0.75f;
    private static final float COOLDOWN_PULSE_PERIOD_TICKS = 8.0f;
    private static final float COOLDOWN_FADE_TICKS = 12.0f;
    private static final float ORB_TINT_PERIOD_TICKS = 100.0f;
    private static final float ORB_TINT_DURATION_TICKS = 20.0f;

    private MithrilFreecastStaffClientRenderState() {
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

    public static OrbRenderState resolveOrb(float partialTick) {
        var spell = resolveSelectedSpell();
        if (spell == null) {
            return new OrbRenderState(1.0f, 1.0f, 1.0f, 1.0f);
        }

        var color = MagicTools.resolveSchoolTintColor(spell.getSchoolType());
        var tintRed = ((color >> 16) & 0xFF) / 255.0f;
        var tintGreen = ((color >> 8) & 0xFF) / 255.0f;
        var tintBlue = (color & 0xFF) / 255.0f;
        var tintStrength = resolveOrbTintStrength(partialTick);
        return new OrbRenderState(
                Mth.lerp(tintStrength, 1.0f, tintRed),
                Mth.lerp(tintStrength, 1.0f, tintGreen),
                Mth.lerp(tintStrength, 1.0f, tintBlue),
                1.0f
        );
    }

    public static boolean selectedSpellMatches(@Nullable AbstractSpell spell) {
        var selectedSpell = resolveSelectedSpell();
        return spell != null && selectedSpell != null && selectedSpell.equals(spell);
    }

    @Nullable
    public static String resolveSelectedSpellId() {
        var selectedSpell = resolveSelectedSpell();
        return selectedSpell != null ? selectedSpell.getSpellId() : null;
    }

    private static float resolveOrbTintStrength(float partialTick) {
        var phase = resolveTime(partialTick) % ORB_TINT_PERIOD_TICKS;
        if (phase >= ORB_TINT_DURATION_TICKS) {
            return 0.0f;
        }

        var progress = phase / ORB_TINT_DURATION_TICKS;
        return 0.5f - 0.5f * Mth.cos((float) (progress * Math.PI * 2.0));
    }

    private static float resolveTime(float partialTick) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return partialTick;
        }
        return minecraft.level.getGameTime() + partialTick;
    }

    private static float resolveCooldownRemaining(ItemStack stack, float partialTick) {
        var spell = resolveSelectedSpell();
        if (spell == null) {
            return 0.0f;
        }

        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spell.getSpellId());
        if (cooldown == null) {
            return 0.0f;
        }
        return Math.max(0.0f, cooldown.getCooldownRemaining() - partialTick);
    }

    @Nullable
    private static AbstractSpell resolveSelectedSpell() {
        var selectionManager = ClientMagicData.getSpellSelectionManager();
        if (selectionManager == null) {
            return null;
        }

        var spellData = selectionManager.getSelectedSpellData();
        if (spellData == SpellData.EMPTY || spellData.getSpell() == SpellRegistry.none()) {
            return null;
        }

        return spellData.getSpell();
    }

    public record CoreRenderState(float red, float green, float blue, float alpha) {
    }

    public record OrbRenderState(float red, float green, float blue, float alpha) {
    }
}
