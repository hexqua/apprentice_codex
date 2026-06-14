package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadget;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

final class MagiCompressorGadgetAirBridgeImpl {
    private static final String AIR_TAG = "Air";

    private MagiCompressorGadgetAirBridgeImpl() {
    }

    static float getMaxAir(ItemStack stack) {
        if (!(stack.getItem() instanceof MagiCompressorGadget)) {
            return 0.0F;
        }
        return Math.min(
                Math.max(0.0F, ApprenticeCodexServerConfig.magiCompressorGadgetMaxAir()),
                BacktankUtil.maxAirWithoutEnchants()
        );
    }

    static float getStoredAir(ItemStack stack) {
        var storedAir = Math.min(readStoredAir(stack), getMaxAir(stack));
        var exposedAir = stack.getOrDefault(AllDataComponents.BACKTANK_AIR, 0);
        if (exposedAir < Math.floor(storedAir)) {
            setStoredAir(stack, exposedAir);
            return exposedAir;
        }
        return storedAir;
    }

    static void setStoredAir(ItemStack stack, float air) {
        var maxAir = getMaxAir(stack);
        if (maxAir <= 0) {
            writeStoredAir(stack, 0.0F);
            return;
        }

        writeStoredAir(stack, Math.min(Math.max(0.0F, air), maxAir));
    }

    static void registerBacktankSupplier() {
        // Create の圧縮空気探索 API は LivingEntity から Backtank 相当の ItemStack を集める。
        // 1.21.1 側では API 形状が変わる可能性があるため、この接着コードは Create compat に閉じ込める。
        BacktankUtil.addBacktankSupplier(MagiCompressorGadgetAirBridgeImpl::findEquippedGadgetsWithAir);
    }

    private static List<ItemStack> findEquippedGadgetsWithAir(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inventory -> inventory.findFirstCurio(stack ->
                        stack.getItem() instanceof MagiCompressorGadget && getStoredAir(stack) > 0.0F))
                .map(slotResult -> List.of(normalizeStoredAirForCreate(slotResult.stack())))
                .orElseGet(List::of);
    }

    private static ItemStack normalizeStoredAirForCreate(ItemStack stack) {
        // Create は BACKTANK_AIR を直接読むため、設定変更後の旧上限分を渡す前に実データへ反映する。
        setStoredAir(stack, getStoredAir(stack));
        return stack;
    }

    private static float readStoredAir(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0.0F;
        }
        return Math.max(0.0F, customData.copyTag().getFloat(AIR_TAG));
    }

    private static void writeStoredAir(ItemStack stack, float air) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(AIR_TAG, air));
        stack.set(AllDataComponents.BACKTANK_AIR, Math.max(0, (int) Math.floor(air)));
    }
}
