package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.ClientBlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientPlacementPreviewManager {
    private static final long PENDING_TARGET_EXPIRE_TICKS = 3L;
    @Nullable
    private static PendingTarget pendingTarget;
    @Nullable
    private static ActivePreview activePreview;

    private ClientPlacementPreviewManager() {
    }

    public static void rememberPendingTarget(ResourceLocation spellId, @Nullable BlockTargetData targetData) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || targetData == null || !targetData.hasTarget()) {
            if (pendingTarget != null && pendingTarget.spellId().equals(spellId)) {
                pendingTarget = null;
            }
            return;
        }

        pendingTarget = new PendingTarget(
                spellId,
                targetData.copy(),
                minecraft.level.getGameTime() + PENDING_TARGET_EXPIRE_TICKS
        );
    }

    public static void beginPreview(AbstractSpell spell, Player player, int spellLevel) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        activePreview = null;
        if (spell.getCastType() != CastType.LONG || !(spell instanceof IClientPlacementPreviewSpell previewSpell)) {
            return;
        }
        if (ClientMagicData.getRecasts().hasRecastForSpell(spell)) {
            return;
        }

        var level = player.level();
        var targetData = consumePendingTarget(spell.getSpellResource())
                .orElseGet(() -> captureCurrentTarget(spell, player, spellLevel).orElse(null));
        var previewData = previewSpell.getClientPlacementPreview(level, player, spellLevel, targetData);
        if (previewData.isEmpty()) {
            return;
        }

        activePreview = new ActivePreview(
                spell.getSpellResource(),
                previewData.get(),
                SchoolAffinityRegistry.resolveColor(spell.getSchoolType())
        );
    }

    public static void finishPreview(ResourceLocation spellId) {
        if (activePreview != null && activePreview.spellId().equals(spellId)) {
            activePreview = null;
        }
        if (pendingTarget != null && pendingTarget.spellId().equals(spellId)) {
            pendingTarget = null;
        }
    }

    @Nullable
    public static ActivePreview getActivePreview() {
        return activePreview;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            pendingTarget = null;
            activePreview = null;
            return;
        }

        if (pendingTarget != null && level.getGameTime() > pendingTarget.expireGameTime()) {
            pendingTarget = null;
        }

        if (activePreview == null) {
            return;
        }

        var spellData = ClientMagicData.getSyncedSpellData(player);
        if (!spellData.isCasting() || !Objects.equals(activePreview.spellId().toString(), spellData.getCastingSpellId())) {
            activePreview = null;
        }
    }

    private static Optional<BlockTargetData> consumePendingTarget(ResourceLocation spellId) {
        var minecraft = Minecraft.getInstance();
        if (pendingTarget == null || minecraft.level == null) {
            return Optional.empty();
        }
        if (minecraft.level.getGameTime() > pendingTarget.expireGameTime()) {
            pendingTarget = null;
            return Optional.empty();
        }
        if (!pendingTarget.spellId().equals(spellId)) {
            return Optional.empty();
        }

        var target = pendingTarget.targetData().copy();
        pendingTarget = null;
        return Optional.of(target);
    }

    private static Optional<BlockTargetData> captureCurrentTarget(AbstractSpell spell, Player player, int spellLevel) {
        if (spell instanceof IClientBlockTargetCaptureSpell captureSpell) {
            var captured = captureSpell.captureClientBlockTarget(player, spellLevel);
            return captured.hasTarget() ? Optional.of(captured) : Optional.empty();
        }
        if (spell instanceof IClientPlacementPreviewSpell previewSpell) {
            var captured = ClientBlockTargetingHelper.captureOutlinedTarget(
                    player,
                    previewSpell.getClientBlockTargetingRange(spellLevel, player)
            );
            return captured.hasTarget() ? Optional.of(captured) : Optional.empty();
        }
        return Optional.empty();
    }

    private record PendingTarget(ResourceLocation spellId, BlockTargetData targetData, long expireGameTime) {
    }

    public record ActivePreview(ResourceLocation spellId, jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData previewData,
                                int color) {
    }
}
