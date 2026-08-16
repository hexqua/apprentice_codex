package jp.aquafactory.apprenticecodex.entity.broom;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class FloatmountBroomEntity extends AbstractBroomEntity {
    private static final BroomMessageKeys MESSAGE_KEYS = new BroomMessageKeys(
            "ui.apprenticecodex.floatmount_broom.warning_dismount",
            "ui.apprenticecodex.floatmount_broom.retrieve_help",
            "ui.apprenticecodex.floatmount_broom.warning_damage",
            "ui.apprenticecodex.floatmount_broom.cannot_mount_damaged",
            Optional.of("ui.apprenticecodex.floatmount_broom.insufficient_mana"),
            "ui.apprenticecodex.floatmount_broom.warning_low_mana",
            "ui.apprenticecodex.floatmount_broom.warning_emergency_landing",
            "ui.apprenticecodex.floatmount_broom.recover_emergency_landing"
    );

    public FloatmountBroomEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getRecoveryItem() {
        return ItemRegistry.FLOATMOUNT_BROOM.get();
    }

    @Override
    protected BroomMessageKeys messageKeys() {
        return MESSAGE_KEYS;
    }

    @Override
    public Component createControlHelpMessage() {
        return Component.translatable(
                "ui.apprenticecodex.floatmount_broom.control_help",
                Component.keybind("key.jump"),
                Component.keybind("key.sprint"),
                Component.keybind("key.sneak")
        );
    }
}
