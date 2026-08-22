package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public final class AlchemyBrewerScreen extends AbstractContainerScreen<AlchemyBrewerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/alchemy_brewer.png");
    private static final int TANK_X = 12, TANK_Y = 20, TANK_WIDTH = 8, TANK_HEIGHT = 48;
    private static final int BUTTON_X = 50, BUTTON_Y = 47, BUTTON_SIZE = 22;
    private static final int GAUGE_X = 56, GAUGE_Y = 18, GAUGE_WIDTH = 11, GAUGE_HEIGHT = 28;

    public AlchemyBrewerScreen(AlchemyBrewerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 166; inventoryLabelY = 72;
    }

    @Override public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTicks);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var brewer = menu;
        boolean enabled = brewer.isAutoBrewing();
        boolean hovered = isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);
        gui.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_Y,
                176, enabled ? 22 : 0, BUTTON_SIZE, BUTTON_SIZE);
        if (hovered) gui.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_Y,
                198, enabled ? 22 : 0, BUTTON_SIZE, BUTTON_SIZE);
        gui.renderItem(new ItemStack(Items.BREWING_STAND), leftPos + 53, topPos + 50);
        renderTank(gui, brewer);
        renderGauge(gui, brewer);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        gui.drawString(font, Component.translatable("container.apprenticecodex.alchemy_brewer.material_label"), 80, 22, 0x404040, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void renderGauge(GuiGraphics gui, AlchemyBrewerMenu brewer) {
        if (!brewer.isProcessing() || brewer.getTotalTicks() <= 0) return;
        int height = net.minecraft.util.Mth.clamp((int) ((long) brewer.getElapsedTicks() * GAUGE_HEIGHT / brewer.getTotalTicks()), 0, GAUGE_HEIGHT);
        if (height > 0) gui.blit(TEXTURE, leftPos + GAUGE_X, topPos + GAUGE_Y + GAUGE_HEIGHT - height,
                0, 166 + GAUGE_HEIGHT - height, GAUGE_WIDTH, height);
    }

    private void renderTank(GuiGraphics gui, AlchemyBrewerMenu brewer) {
        if (brewer.getDisplayPotionId() == null || brewer.getDisplayAmountMb() <= 0) return;
        var potion = ForgeRegistries.POTIONS.getValue(brewer.getDisplayPotionId());
        if (potion == null) return;
        var representative = PotionContentsHelper.createPotionStack(Items.POTION, potion);
        var fluid = Minecraft.getInstance().level == null ? null : SpellcastersFlask.createFluidForStoredItem(
                Minecraft.getInstance().level, representative, AlchemyBrewerBlockEntity.DOSE_AMOUNT_MB);
        int height = Math.max(1, brewer.getDisplayAmountMb() * TANK_HEIGHT / AlchemyBrewerBlockEntity.TANK_CAPACITY_MB);
        int x = leftPos + TANK_X, y = topPos + TANK_Y + TANK_HEIGHT - height;
        int tint = SpellcastersFlask.getStoredItemTintColorForDisplay(representative);
        float alpha = brewer.isDisplayPreview() ? 0.35F : ((tint >>> 24) & 255) / 255F;
        if (fluid == null || fluid.isEmpty()) {
            gui.fill(x, y, x + TANK_WIDTH, y + height, (int)(alpha * 255) << 24 | tint & 0xFFFFFF);
        } else {
            var texture = IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);
            var sprite = texture == null ? null : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                gui.fill(x, y, x + TANK_WIDTH, y + height, (int)(alpha * 255) << 24 | tint & 0xFFFFFF);
            } else {
                float red = ((tint >>> 16) & 255) / 255F, green = ((tint >>> 8) & 255) / 255F, blue = (tint & 255) / 255F;
                for (int drawn = 0; drawn < height;) {
                    int tile = Math.min(16, height - drawn);
                    gui.blit(x, y + height - drawn - tile, 0, TANK_WIDTH, tile, sprite, red, green, blue, alpha);
                    drawn += tile;
                }
            }
        }
    }

    @Override protected void renderTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
        var brewer = menu;
        if (isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            boolean enabled = brewer.isAutoBrewing();
            var key = "container.apprenticecodex.alchemy_brewer.auto_brew_button." + (enabled ? "enabled" : "disabled");
            gui.renderTooltip(font, java.util.List.of(
                    Component.translatable(key).getVisualOrderText(),
                    Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY).getVisualOrderText()), mouseX, mouseY);
            return;
        }
        if (isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY) && brewer.getDisplayPotionId() != null) {
            var potion = ForgeRegistries.POTIONS.getValue(brewer.getDisplayPotionId());
            if (potion != null) {
                var lines = new ArrayList<Component>();
                var potionName = PotionContentsHelper.createPotionStack(Items.POTION, potion).getHoverName();
                if (brewer.getTankAmountMb() > 0) lines.add(potionName);
                else if (brewer.isProcessing()) lines.add(Component.translatable(
                        "container.apprenticecodex.alchemy_brewer.fluid.brewing", potionName));
                else lines.add(Component.translatable("container.apprenticecodex.alchemy_brewer.fluid.preview", potionName));
                lines.add(Component.translatable(brewer.isDisplayPreview()
                        ? "container.apprenticecodex.alchemy_brewer.fluid.product_amount"
                        : "container.apprenticecodex.alchemy_brewer.fluid.current_amount",
                        brewer.getDisplayAmountMb(), brewer.getDisplayAmountMb() / AlchemyBrewerBlockEntity.DOSE_AMOUNT_MB).withStyle(ChatFormatting.GRAY));
                if (brewer.getTankAmountMb() == 0 && brewer.getTotalTicks() > 0) {
                    int processingSeconds = Math.max(1, (brewer.getTotalTicks() + 19) / 20);
                    lines.add(Component.translatable("container.apprenticecodex.alchemy_brewer.fluid.process_time",
                            processingSeconds).withStyle(ChatFormatting.GRAY));
                }
                appendPotionEffects(lines, PotionContentsHelper.createPotionStack(Items.POTION, potion));
                if (brewer.getTankAmountMb() > 0 && brewer.getTankAmountMb() < AlchemyBrewerBlockEntity.DOSE_AMOUNT_MB)
                    lines.add(Component.translatable("container.apprenticecodex.alchemy_brewer.fluid.residual_warning").withStyle(ChatFormatting.RED));
                gui.renderTooltip(font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }

    private static void appendPotionEffects(ArrayList<Component> lines, ItemStack representative) {
        var effects = PotionContentsHelper.getMobEffects(representative);
        if (effects.isEmpty()) return;

        lines.add(Component.translatable("container.apprenticecodex.alchemy_brewer.fluid.effects")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (var effect : effects) {
            var effectLine = effect.getEffect().getDisplayName().copy();
            if (effect.getAmplifier() > 0) {
                effectLine = Component.translatable("potion.withAmplifier", effectLine,
                        Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            if (!effect.endsWithin(20)) {
                effectLine = Component.translatable("potion.withDuration", effectLine,
                        net.minecraft.world.effect.MobEffectUtil.formatDuration(effect, 1.0F));
            }
            effectLine.withStyle(effect.getEffect().getCategory().getTooltipFormatting());
            lines.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(effectLine));
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)
                && minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AlchemyBrewerMenu.TOGGLE_AUTO_BUTTON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
