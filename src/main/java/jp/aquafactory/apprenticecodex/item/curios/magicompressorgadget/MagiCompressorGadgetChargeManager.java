package jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.compat.create.MagiCompressorGadgetAirBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class MagiCompressorGadgetChargeManager {
    private static final int CONVERSION_INTERVAL_TICKS = 10;
    private static final float TICKS_PER_SECOND = 20.0F;

    private MagiCompressorGadgetChargeManager() {
    }

    public static void onCurioTick(SlotContext slotContext, ItemStack stack) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!isPrimaryEquippedCurio(slotContext)) {
            return;
        }
        if (player.tickCount % CONVERSION_INTERVAL_TICKS != 0) {
            return;
        }

        convertManaToAir(player, stack);
    }

    public static boolean convertManaToAir(ServerPlayer player, ItemStack stack) {
        var maxAir = MagiCompressorGadgetAirBridge.getMaxAir(stack);
        if (maxAir <= 0) {
            return false;
        }

        var storedAir = MagiCompressorGadgetAirBridge.getStoredAir(stack);
        var remainingAir = Math.max(0.0F, maxAir - storedAir);
        if (remainingAir <= 0.0F) {
            return false;
        }

        var conversionSeconds = CONVERSION_INTERVAL_TICKS / TICKS_PER_SECOND;
        var scheduledAirToAdd = Math.max(0.0F,
                ApprenticeCodexServerConfig.magiCompressorGadgetAirFillPerSecond()) * conversionSeconds;
        if (scheduledAirToAdd <= 0.0F) {
            return false;
        }

        var airToAdd = Math.min(scheduledAirToAdd, remainingAir);
        var conversionRatio = airToAdd / scheduledAirToAdd;
        var manaCost = Math.max(0.0F,
                ApprenticeCodexServerConfig.magiCompressorGadgetManaCostPerSecond()) * conversionSeconds * conversionRatio;

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() + 1.0e-4F < manaCost) {
            return false;
        }

        if (manaCost > 0.0F) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            syncMana(player, magicData);
        }
        MagiCompressorGadgetAirBridge.setStoredAir(stack, storedAir + airToAdd);
        return true;
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof MagiCompressorGadget))
                .map(slotResult -> slotResult.slotContext().index() == slotContext.index()
                        && slotResult.slotContext().identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    private static void syncMana(ServerPlayer player, MagicData magicData) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }
}
