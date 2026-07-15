package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ReflectcastShieldClientEffectState {
    private static final int SUCCESS_FLASH_TICKS = 20;
    private static final int COOLDOWN_FADE_TICKS = 20;
    private static final int COOLDOWN_WAIT_TICKS = 40;
    private static final float FLASH_WHITE_BLEND = 0.4f;
    private static final float PULSE_PERIOD_TICKS = 18.0f;
    private static final float PULSE_MIN_STRENGTH = 0.2f;
    private static final float PULSE_MAX_STRENGTH = 0.5f;
    private static final float PULSE_RED = 1.0f;
    private static final float PULSE_GREEN = 0.18f;
    private static final float PULSE_BLUE = 0.18f;
    private static final RenderState NONE = new RenderState(0.0f, 0.0f, 0.0f, 0.0f);

    @Nullable
    private static ActiveEffect activeEffect;

    private ReflectcastShieldClientEffectState() {
    }

    public static void beginLocalSuccessFlash(InteractionHand hand, String spellId) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || spellId == null || spellId.isBlank()) {
            return;
        }

        activeEffect = new ActiveEffect(hand, spellId, minecraft.level.getGameTime());
    }

    public static RenderState resolve(ItemStack renderingStack, ItemDisplayContext perspective, float partialTick) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            clear();
            return NONE;
        }

        var effect = activeEffect;
        if (effect == null || !isVisiblePerspective(perspective)) {
            return NONE;
        }

        var handStack = player.getItemInHand(effect.hand);
        if (!(handStack.getItem() instanceof ReflectcastShield)) {
            clear();
            return NONE;
        }

        if (!isRenderingReflectcastShield(renderingStack)) {
            return NONE;
        }

        if (!matchesTrackedSpell(handStack, effect.spellId)) {
            clear();
            return NONE;
        }

        var age = (float) (level.getGameTime() - effect.flashStartGameTime) + partialTick;
        if (age < SUCCESS_FLASH_TICKS) {
            var strength = 1.0f - easeOutCubic(age / SUCCESS_FLASH_TICKS);
            return tintFlashState(resolveSchoolFlashColor(handStack), strength);
        }

        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(effect.spellId);
        if (cooldown != null) {
            effect.cooldownObserved = true;
            var remainingTicks = cooldown.getCooldownRemaining() - partialTick;
            var strength = getCooldownPulseStrength((float) level.getGameTime() + partialTick, remainingTicks);
            return new RenderState(PULSE_RED * strength, PULSE_GREEN * strength, PULSE_BLUE * strength, strength);
        }

        if (effect.cooldownObserved
                || (!effect.waitForCooldown && age >= SUCCESS_FLASH_TICKS + COOLDOWN_WAIT_TICKS)) {
            clear();
        }

        return NONE;
    }

    public static void clear() {
        activeEffect = null;
    }

    private static boolean isVisiblePerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static boolean isRenderingReflectcastShield(ItemStack renderingStack) {
        return !renderingStack.isEmpty() && renderingStack.getItem() instanceof ReflectcastShield;
    }

    private static boolean matchesTrackedSpell(ItemStack stack, String spellId) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY && spellData.getSpell() != null && spellId.equals(spellData.getSpell().getSpellId());
    }

    private static RenderState tintFlashState(float[] color, float strength) {
        return new RenderState(
                color[0] * strength,
                color[1] * strength,
                color[2] * strength,
                strength
        );
    }

    private static float[] resolveSchoolFlashColor(ItemStack stack) {
        var schoolType = MagicTools.getImbuedSpellSchool(stack);
        var tintColor = schoolType != null && schoolType.getDisplayName().getStyle().getColor() != null
                ? schoolType.getDisplayName().getStyle().getColor().getValue()
                : PastelStaff.DEFAULT_STONE_TINT_COLOR;

        var red = ((tintColor >> 16) & 0xFF) / 255.0f;
        var green = ((tintColor >> 8) & 0xFF) / 255.0f;
        var blue = (tintColor & 0xFF) / 255.0f;
        return new float[] {
                Mth.lerp(FLASH_WHITE_BLEND, red, 1.0f),
                Mth.lerp(FLASH_WHITE_BLEND, green, 1.0f),
                Mth.lerp(FLASH_WHITE_BLEND, blue, 1.0f)
        };
    }

    private static float getCooldownPulseStrength(float time, float remainingTicks) {
        if (remainingTicks <= 0.0f) {
            return 0.0f;
        }

        var pulse = (Mth.sin((float) (time * (Math.PI * 2.0 / PULSE_PERIOD_TICKS))) + 1.0f) * 0.5f;
        var baseStrength = Mth.lerp(pulse, PULSE_MIN_STRENGTH, PULSE_MAX_STRENGTH);
        // 残り 20tick は脈動をそのまま切らずに、線形で沈めて終端だけ静かに抜く。
        var fadeOut = remainingTicks >= COOLDOWN_FADE_TICKS
                ? 1.0f
                : Mth.clamp(remainingTicks / COOLDOWN_FADE_TICKS, 0.0f, 1.0f);
        return baseStrength * fadeOut;
    }

    private static float easeOutCubic(float progress) {
        var clamped = Mth.clamp(progress, 0.0f, 1.0f);
        var inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    public record RenderState(float red, float green, float blue, float alpha) {
        public boolean isVisible() {
            return alpha > 0.0f;
        }
    }

    private static final class ActiveEffect {
        private final InteractionHand hand;
        private final String spellId;
        private final long flashStartGameTime;
        private final boolean waitForCooldown;
        private boolean cooldownObserved;

        private ActiveEffect(InteractionHand hand, String spellId, long flashStartGameTime) {
            this.hand = hand;
            this.spellId = spellId;
            this.flashStartGameTime = flashStartGameTime;
            var spell = SpellRegistry.getSpell(spellId);
            this.waitForCooldown = spell != null && spell.getCastType() == CastType.CONTINUOUS;
        }
    }
}
