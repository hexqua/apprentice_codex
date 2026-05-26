package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SatelliteFollowcastAmuletRenderEvent {
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/satellite_followcast_crystal.png");
    private static final int UNLEARNED_CRYSTAL_LIGHT = LightTexture.pack(4, 4);
    private static final int SCHOOL_PULSE_PERIOD_TICKS = 20 * 10;
    private static final int SCHOOL_PULSE_DURATION_TICKS = 20;

    private SatelliteFollowcastAmuletRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        renderEquippedAmuletCrystals(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPartialTick(),
                event.getEntity()
        );
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        var cameraPosition = event.getCamera().getPosition();
        var playerPosition = player.getPosition(event.getPartialTick());
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(
                playerPosition.x - cameraPosition.x,
                playerPosition.y - cameraPosition.y,
                playerPosition.z - cameraPosition.z
        );
        renderEquippedAmuletCrystals(poseStack, bufferSource, event.getPartialTick(), player);
        poseStack.popPose();

        bufferSource.endBatch();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            SatelliteFollowcastAmuletClientState.clear();
        }
    }

    private static void renderEquippedAmuletCrystals(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float partialTick,
            Player player
    ) {
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory
                .findCurios(stack -> stack.getItem() instanceof SatelliteFollowcastAmulet)
                .forEach(slotResult -> renderAmuletCrystals(
                        poseStack,
                        bufferSource,
                        partialTick,
                        player,
                        slotResult.stack(),
                        slotResult.slotContext().identifier(),
                        slotResult.slotContext().index()
                )));
    }

    private static void renderAmuletCrystals(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float partialTick,
            Player player,
            ItemStack stack,
            String slotIdentifier,
            int curiosSlotIndex
    ) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return;
        }

        var maxSpellSlots = SatelliteFollowcastAmulet.clampSpellSlotCount(spellContainer.getMaxSpellCount());
        for (var slotIndex = 0; slotIndex < maxSpellSlots; ++slotIndex) {
            var spellData = spellContainer.getSpellAtIndex(slotIndex);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            renderCrystal(
                    poseStack,
                    bufferSource,
                    partialTick,
                    player,
                    spellData,
                    slotIdentifier,
                    curiosSlotIndex,
                    slotIndex,
                    maxSpellSlots
            );
        }
    }

    private static void renderCrystal(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float partialTick,
            Player player,
            SpellData spellData,
            String slotIdentifier,
            int curiosSlotIndex,
            int slotIndex,
            int maxSpellSlots
    ) {
        var offset = SatelliteFollowcastAmulet.getCrystalOffset(player, slotIndex, maxSpellSlots, partialTick);
        var time = player.tickCount + partialTick;
        var unlearned = isClientUnlearned(player, spellData);
        var color = unlearned
                ? Color.WHITE
                : resolveCrystalColor(
                        player,
                        spellData,
                        slotIdentifier,
                        curiosSlotIndex,
                        slotIndex,
                        maxSpellSlots,
                        time
                );
        var packedLight = unlearned ? UNLEARNED_CRYSTAL_LIGHT : LightTexture.FULL_BRIGHT;
        var renderMode = unlearned
                ? ExtrudedSpriteRenderer.RenderMode.DEFAULT
                : ExtrudedSpriteRenderer.RenderMode.ADDITIVE_COLOR_ONLY;

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(time * 360.0F / 40.0F)));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(
                poseStack,
                bufferSource,
                packedLight,
                CRYSTAL_TEXTURE,
                renderMode,
                color.red(),
                color.green(),
                color.blue(),
                1.0F
        );
        poseStack.popPose();
    }

    private static boolean isClientUnlearned(Player player, SpellData spellData) {
        var spell = spellData.getSpell();
        return spell.requiresLearning() && !spell.isLearned(player);
    }

    private static Color resolveCrystalColor(
            Player player,
            SpellData spellData,
            String slotIdentifier,
            int curiosSlotIndex,
            int slotIndex,
            int maxSpellSlots,
            float time
    ) {
        var schoolColor = toColor(MagicTools.resolveSchoolTintColor(spellData.getSpell().getSchoolType()));
        if (SatelliteFollowcastAmuletClientState.isContinuousActive(
                player.getId(),
                slotIdentifier,
                curiosSlotIndex,
                slotIndex,
                player.level().getGameTime()
        )) {
            return schoolColor;
        }

        if (isClientCooldownActive(player, spellData)) {
            var blink = 0.25F + 0.25F * (0.5F + 0.5F * (float) Math.sin(time * Math.PI / 5.0D));
            return new Color(blink, 0.0F, 0.0F);
        }

        var delay = (float) slotIndex * SCHOOL_PULSE_PERIOD_TICKS / Math.max(1, maxSpellSlots);
        var local = positiveModulo(time - delay, SCHOOL_PULSE_PERIOD_TICKS);
        if (local >= SCHOOL_PULSE_DURATION_TICKS) {
            return Color.WHITE;
        }

        var pulseProgress = local / SCHOOL_PULSE_DURATION_TICKS;
        var tintWeight = (float) Math.sin(Math.PI * pulseProgress);
        return Color.lerp(Color.WHITE, schoolColor, tintWeight);
    }

    private static boolean isClientCooldownActive(Player player, SpellData spellData) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())) {
            return ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell());
        }

        var magicData = MagicData.getPlayerMagicData(player);
        return magicData != null && magicData.getPlayerCooldowns().isOnCooldown(spellData.getSpell());
    }

    private static float positiveModulo(float value, float modulo) {
        var result = value % modulo;
        return result < 0.0F ? result + modulo : result;
    }

    private static Color toColor(int rgb) {
        return new Color(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F
        );
    }

    private record Color(float red, float green, float blue) {
        private static final Color WHITE = new Color(1.0F, 1.0F, 1.0F);

        private static Color lerp(Color from, Color to, float amount) {
            return new Color(
                    lerp(from.red, to.red, amount),
                    lerp(from.green, to.green, amount),
                    lerp(from.blue, to.blue, amount)
            );
        }

        private static float lerp(float from, float to, float amount) {
            return from + (to - from) * amount;
        }
    }
}
