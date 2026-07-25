package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.spell.IClientBlockHitTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.ClientBlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ClientBlockTargetSyncService {
    @Nullable
    private static TargetSyncSignature lastSentTargetSync;
    private static long lastSentTargetTick = Long.MIN_VALUE;

    private ClientBlockTargetSyncService() {
    }

    public static boolean trySendForSelectedCast(SpellData spellData, int quickCastSlot) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        return trySend(spellData, player, resolveSpellLevel(spellData, player), quickCastSlot, true, null);
    }

    public static boolean trySendForRightClick(RightClickSpellResolver.ResolvedRightClickSpell resolvedSpell) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        return trySend(
                resolvedSpell.spellData(),
                player,
                resolvedSpell.spellLevel(),
                -1,
                false,
                resolvedSpell.targetingRangeOverride()
        );
    }

    public static BlockTargetData captureForEmbeddedCast(SpellData spellData) {
        var player = Minecraft.getInstance().player;
        if (player == null || spellData == null || spellData == SpellData.EMPTY) {
            return new BlockTargetData();
        }

        var spell = spellData.getSpell();
        if (spell == null || spell == SpellRegistry.none() || !(spell instanceof IClientBlockTargetingSpell targetingSpell)) {
            return new BlockTargetData();
        }

        var spellResource = spell.getSpellResource();
        if (spellResource == null) {
            return new BlockTargetData();
        }

        var targetData = captureTargetData(spell, player, resolveSpellLevel(spellData, player), targetingSpell, null);
        ClientPlacementPreviewManager.rememberPendingTarget(spellResource, targetData);
        return targetData;
    }

    public static List<BlockTargetData> captureForAttackcastRings(Player player) {
        return AttackcastRingAttackTrigger.getEquippedSpellData(player).stream()
                .map(ClientBlockTargetSyncService::captureForEmbeddedCast)
                .toList();
    }

    private static boolean trySend(
            SpellData spellData,
            Player player,
            int spellLevel,
            int quickCastSlot,
            boolean initiateCast,
            @Nullable Double targetingRangeOverride
    ) {
        if (spellData == null || spellData == SpellData.EMPTY) {
            return false;
        }

        var spell = spellData.getSpell();
        if (spell == null || spell == SpellRegistry.none() || !(spell instanceof IClientBlockTargetingSpell targetingSpell)) {
            return false;
        }

        var spellResource = spell.getSpellResource();
        if (spellResource == null) {
            return false;
        }

        var targetData = captureTargetData(spell, player, spellLevel, targetingSpell, targetingRangeOverride);
        // 詠唱開始パケットは Iron's の入力 drain を置き換えるため、同 tick でも捨てない。
        if (!initiateCast && shouldSuppressDuplicate(player.level().getGameTime(), spellResource, player.getMainHandItem(),
                player.getOffhandItem(), targetData, quickCastSlot, initiateCast)) {
            return true;
        }

        ClientPlacementPreviewManager.rememberPendingTarget(spellResource, targetData);
        Networks.sendToServer(new ClientBlockTargetCastPacket(quickCastSlot, spellResource, targetData, initiateCast));
        return true;
    }

    private static int resolveSpellLevel(SpellData spellData, Player player) {
        if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return 0;
        }
        return spellData.getSpell().getLevelFor(spellData.getLevel(), player);
    }

    private static BlockTargetData captureTargetData(Object spell, Player player, int spellLevel,
                                                     IClientBlockTargetingSpell targetingSpell,
                                                     @Nullable Double targetingRangeOverride) {
        if (spell instanceof IClientBlockTargetCaptureSpell customCaptureSpell) {
            return customCaptureSpell.captureClientBlockTarget(player, spellLevel);
        }

        if (spell instanceof IClientBlockHitTargetingSpell hitTargetingSpell) {
            return ClientBlockTargetingHelper.captureOutlinedHitTarget(
                    player,
                    hitTargetingSpell.getClientBlockTargetingRange(spellLevel, player),
                    hitTargetingSpell.ignoresClientBlockTargetingRange()
            );
        }

        return ClientBlockTargetingHelper.captureOutlinedTarget(
                player,
                targetingRangeOverride == null
                        ? targetingSpell.getClientBlockTargetingRange(spellLevel, player)
                        : targetingRangeOverride
        );
    }

    private static boolean shouldSuppressDuplicate(long gameTime, ResourceLocation spellResource, ItemStack mainHand,
                                                   ItemStack offHand, BlockTargetData targetData, int quickCastSlot,
                                                   boolean initiateCast) {
        var signature = new TargetSyncSignature(
                spellResource,
                getItemId(mainHand),
                getItemId(offHand),
                quickCastSlot,
                initiateCast,
                targetData.hasTarget(),
                targetData.getHitBlockPos(),
                targetData.getHitFace(),
                targetData.getPlacePos(),
                targetData.getPlaceFacing()
        );

        if (gameTime == lastSentTargetTick && signature.equals(lastSentTargetSync)) {
            return true;
        }

        lastSentTargetTick = gameTime;
        lastSentTargetSync = signature;
        return false;
    }

    @Nullable
    private static ResourceLocation getItemId(ItemStack stack) {
        return stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
    }

    private record TargetSyncSignature(
            ResourceLocation spellResource,
            @Nullable ResourceLocation mainHandItemId,
            @Nullable ResourceLocation offHandItemId,
            int quickCastSlot,
            boolean initiateCast,
            boolean hasTarget,
            @Nullable BlockPos hitBlockPos,
            @Nullable Direction hitFace,
            @Nullable BlockPos placePos,
            @Nullable Direction placeFacing
    ) {
    }
}
