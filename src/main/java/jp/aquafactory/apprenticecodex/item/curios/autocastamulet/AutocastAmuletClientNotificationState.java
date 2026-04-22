package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AutocastAmuletClientNotificationState {
    private static final AutocastAmuletNotificationController CONTROLLER = new AutocastAmuletNotificationController();

    private AutocastAmuletClientNotificationState() {
    }

    public static void queueCooldownCast(ResourceLocation spellId, ResourceLocation spellIcon, int cooldownTicks) {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return;
        }

        CONTROLLER.queueCooldownCast(gameTime, spellId, spellIcon, cooldownTicks);
    }

    public static void queueManaLow(ResourceLocation spellId, ResourceLocation spellIcon) {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return;
        }

        CONTROLLER.queueManaLow(gameTime, spellId, spellIcon);
    }

    public static void tick() {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            CONTROLLER.clear();
            return;
        }

        CONTROLLER.advance(gameTime);
    }

    public static void clear() {
        CONTROLLER.clear();
    }

    @Nullable
    public static AutocastAmuletNotificationController.NotificationEntry getActiveNotification() {
        return CONTROLLER.getActiveNotification();
    }

    public static List<Component> buildCooldownTooltipLines(ItemStack stack) {
        var lines = new ArrayList<Component>();
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return lines;
        }

        var activeCooldowns = new ArrayList<SpellCooldownView>();
        for (SpellData spellData : AutocastAmulet.getImbuedSpells(stack)) {
            var spell = spellData.getSpell();
            if (spell == null) {
                continue;
            }

            var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spell.getSpellId());
            if (cooldown == null || cooldown.getCooldownRemaining() <= 0.0F) {
                continue;
            }

            activeCooldowns.add(new SpellCooldownView(
                    spell.getDisplayName(player),
                    AutocastAmuletNotificationController.toDisplayCooldownSeconds(cooldown.getCooldownRemaining())
            ));
        }

        if (activeCooldowns.isEmpty()) {
            return lines;
        }

        lines.add(Component.empty());
        lines.add(Component.translatable("item.apprenticecodex.autocast_amulet.cooldown_title").withStyle(ChatFormatting.GRAY));
        for (var cooldownView : activeCooldowns) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.autocast_amulet.cooldown_line",
                    cooldownView.spellName(),
                    cooldownView.remainingSeconds()
            ).withStyle(ChatFormatting.DARK_AQUA));
        }
        return lines;
    }

    public static float getActiveNotificationAlpha() {
        var gameTime = resolveCurrentGameTime();
        if (gameTime < 0L) {
            return 0.0F;
        }

        return CONTROLLER.getActiveNotificationAlpha(gameTime);
    }

    private static long resolveCurrentGameTime() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return -1L;
        }

        return minecraft.level.getGameTime();
    }

    private record SpellCooldownView(Component spellName, int remainingSeconds) {
    }
}
