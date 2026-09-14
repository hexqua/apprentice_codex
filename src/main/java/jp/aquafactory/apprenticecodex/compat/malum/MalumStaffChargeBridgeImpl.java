package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.data.attachment.StaffAbilityData;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

final class MalumStaffChargeBridgeImpl {
    private static final ResourceLocation DURATION = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_soul_transducer_duration");
    private static final ResourceLocation RECOVERY = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_soul_transducer_recovery");

    private MalumStaffChargeBridgeImpl() {}

    static boolean needsRecovery(LivingEntity entity) {
        return entity.hasData(MalumAttachmentTypes.STAFF_ABILITIES)
                && entity.getData(MalumAttachmentTypes.STAFF_ABILITIES).getStaffChargeDebt() > 0;
    }

    static void recoverFully(ServerPlayer player) {
        // 満回復は共有の消費量を消す。手持ちによる容量変動と、回復音の大量再生を避ける。
        player.setData(MalumAttachmentTypes.STAFF_ABILITIES, new StaffAbilityData(0, 0));
        player.syncData(MalumAttachmentTypes.STAFF_ABILITIES);
    }

    static void updateAttributes(ServerPlayer player, double duration, double recovery) {
        update(player.getAttribute(MalumAttributes.CHARGE_DURATION), DURATION, duration, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        update(player.getAttribute(MalumAttributes.CHARGE_RECOVERY_RATE), RECOVERY, recovery, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void update(AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        var previous = attribute.getModifier(id);
        if (previous != null && previous.amount() == amount && previous.operation() == operation) return;
        attribute.removeModifier(id);
        if (amount != 0D) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
}
