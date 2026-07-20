package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ChargecastCatalystbookClientRenderState {
    private static final float COOLDOWN_PULSE_PERIOD_TICKS = 8.0F;
    private static final float COOLDOWN_MIN_BRIGHTNESS = 0.35F;
    private static final float COOLDOWN_MAX_BRIGHTNESS = 1.0F;

    private ChargecastCatalystbookClientRenderState() {
    }

    public static RenderState resolve(ItemStack stack, float partialTick) {
        var runeColor = resolveRuneColor(stack);
        var selectedSpell = resolveSelectedSpell(stack);
        if (selectedSpell == null) {
            return new RenderState(runeColor, null, CooldownState.hidden());
        }

        return new RenderState(
                runeColor,
                selectedSpell.getSpellIconResource(),
                resolveCooldownState(selectedSpell.getSpellId(), partialTick)
        );
    }

    private static ColorState resolveRuneColor(ItemStack stack) {
        var school = ChargecastCatalystbook.getResolvedCalibrationSchool(stack);
        return school == null ? ColorState.WHITE : ColorState.fromRgb(MagicTools.resolveSchoolTintColor(school));
    }

    private static @Nullable AbstractSpell resolveSelectedSpell(ItemStack stack) {
        var spellData = ChargecastCatalystbook.getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null
                || spellData.getSpell() == SpellRegistry.none()) {
            return null;
        }
        // WisdomShard のホイール選択ではなく、触媒書自身が保持する選択魔法だけを表示に使う。
        return spellData.getSpell();
    }

    private static CooldownState resolveCooldownState(String spellId, float partialTick) {
        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spellId);
        if (cooldown == null || cooldown.getCooldownRemaining() - partialTick <= 0.0F) {
            return CooldownState.hidden();
        }

        var phase = (resolveRenderTick(partialTick) % COOLDOWN_PULSE_PERIOD_TICKS)
                / COOLDOWN_PULSE_PERIOD_TICKS * Mth.TWO_PI;
        var progress = (Mth.sin(phase) + 1.0F) * 0.5F;
        var brightness = Mth.lerp(progress, COOLDOWN_MIN_BRIGHTNESS, COOLDOWN_MAX_BRIGHTNESS);
        return CooldownState.visible(brightness);
    }

    private static float resolveRenderTick(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() + partialTick : Util.getMillis() / 50.0F;
    }

    public record RenderState(ColorState runeColor, @Nullable ResourceLocation spellIcon,
                              CooldownState cooldown) {
    }

    public record ColorState(float red, float green, float blue) {
        private static final ColorState WHITE = new ColorState(1.0F, 1.0F, 1.0F);

        private static ColorState fromRgb(int color) {
            return new ColorState(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F
            );
        }
    }

    public record CooldownState(boolean visible, float brightness) {
        private static final CooldownState HIDDEN = new CooldownState(false, 0.0F);

        private static CooldownState hidden() {
            return HIDDEN;
        }

        private static CooldownState visible(float brightness) {
            return new CooldownState(true, brightness);
        }
    }
}
