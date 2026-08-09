package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ImbuedSpellCoreClientEffectState {
    private static final float CORE_IDLE_BASE = 0.82f;
    private static final float CORE_IDLE_PULSE = 0.18f;
    private static final float CORE_IDLE_SPEED = 0.18f;
    private static final float COOLDOWN_MIN_STRENGTH = 0.25f;
    private static final float COOLDOWN_MAX_STRENGTH = 0.75f;
    private static final float COOLDOWN_PULSE_PERIOD_TICKS = 8.0f;
    private static final float COOLDOWN_FADE_TICKS = 12.0f;

    private ImbuedSpellCoreClientEffectState() {
    }

    public static CoreRenderState resolve(ItemStack stack, float partialTick) {
        return resolve(stack, partialTick, false);
    }

    public static CoreRenderState resolveWithManaRequirement(
            ItemStack stack,
            @Nullable LivingEntity renderingEntity,
            float partialTick,
            double requiredMana
    ) {
        return resolve(stack, partialTick, isManaRequirementUnmet(renderingEntity, requiredMana));
    }

    private static CoreRenderState resolve(ItemStack stack, float partialTick, boolean manaWarningActive) {
        float time = resolveTime(partialTick);
        var cooldownRemaining = resolveCooldownRemaining(stack, partialTick);
        if (cooldownRemaining > 0.0f || manaWarningActive) {
            var strengthMultiplier = 1.0f;
            if (!manaWarningActive && cooldownRemaining < COOLDOWN_FADE_TICKS) {
                strengthMultiplier = Mth.clamp(cooldownRemaining / COOLDOWN_FADE_TICKS, 0.0f, 1.0f);
            }
            return resolveWarning(time, strengthMultiplier);
        }

        var brightness = CORE_IDLE_BASE + CORE_IDLE_PULSE * (0.5f + 0.5f * Mth.sin(time * CORE_IDLE_SPEED));
        return new CoreRenderState(brightness, brightness, brightness, 0.95f);
    }

    public static CoreRenderState resolveWarning(float partialTick) {
        return resolveWarning(resolveTime(partialTick), 1.0f);
    }

    private static CoreRenderState resolveWarning(float time, float strengthMultiplier) {
        var pulse = (Mth.sin((float) (time * (Math.PI * 2.0 / COOLDOWN_PULSE_PERIOD_TICKS))) + 1.0f) * 0.5f;
        var strength = Mth.lerp(pulse, COOLDOWN_MIN_STRENGTH, COOLDOWN_MAX_STRENGTH) * strengthMultiplier;
        return new CoreRenderState(strength, 0.12f * strength, 0.12f * strength, 0.95f);
    }

    private static boolean isManaRequirementUnmet(
            @Nullable LivingEntity renderingEntity,
            double requiredMana
    ) {
        if (!Double.isFinite(requiredMana) || requiredMana <= 0.0D) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        if (player == null || renderingEntity != player || player.getAbilities().instabuild) {
            return false;
        }

        // サーバーのMagicDataはマナを保持していないため、クライアントから参照する必要がある.
        var clientMana = ClientMagicData.getPlayerMana();
        return clientMana < requiredMana;
    }

    public static boolean isCooldownActive(ItemStack stack, float partialTick) {
        return resolveCooldownRemaining(stack, partialTick) > 0.0f;
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
}
