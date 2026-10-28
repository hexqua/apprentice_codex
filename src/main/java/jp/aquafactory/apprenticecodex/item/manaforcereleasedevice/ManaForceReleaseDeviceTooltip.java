package jp.aquafactory.apprenticecodex.item.manaforcereleasedevice;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.CastBarHudRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaForceReleaseDeviceTooltip {
    private static final String PREFIX = "item.apprenticecodex.mana_force_release_device.";

    private ManaForceReleaseDeviceTooltip() {}

    public static void append(ManaForceReleaseDevice item, List<Component> lines) {
        lines.add(Component.translatable(PREFIX + "desc_1", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(PREFIX + "desc_2").withStyle(ChatFormatting.GRAY));
        var player = Minecraft.getInstance().player;
        var config = ManaForceReleaseClientConfig.values();
        if (player != null) {
            var damage = item.damage(player.getAttributeValue(AttributeRegistry.MAX_MANA.get()),
                    player.getAttributeValue(AttributeRegistry.SPELL_POWER.get()), config);
            lines.add(Component.translatable(PREFIX + "desc_3",
                    Component.literal(Utils.stringTruncation(damage, 2)).withStyle(ChatFormatting.GREEN))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(PREFIX + "desc_4", ManaForceReleaseDevice.REQUIRED_MANA_PERCENT,
                    Component.literal(Utils.stringTruncation(ManaForceReleaseDevice.requiredMana(player), 2))
                            .withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        }
        if (item.tier() == ManaForceReleaseTier.MASTER) {
            lines.add(Component.translatable(PREFIX + "master_tier_perk").withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable(PREFIX + "low_tier_warning",
                    Component.literal(Utils.stringTruncation(config.softCap(item.tier()), 0)).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent
    public static void renderCharge(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.options.hideGui || !player.isUsingItem()
                || player.getUsedItemHand() != InteractionHand.MAIN_HAND
                || !(player.getUseItem().getItem() instanceof ManaForceReleaseDevice)) return;
        float remaining = Math.max(0, player.getUseItemRemainingTicks() - event.getPartialTick());
        CastBarHudRenderer.render(event.getGuiGraphics(), minecraft.font,
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
                1 - remaining / ManaForceReleaseDevice.CHARGE_TICKS, Utils.timeFromTicks(remaining, 1));
    }
}
