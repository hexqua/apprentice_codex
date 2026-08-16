package jp.aquafactory.apprenticecodex.entity.broom;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class HoverrideBroomEntity extends AbstractBroomEntity {
    private static final BroomMessageKeys MESSAGE_KEYS = new BroomMessageKeys(
            "ui.apprenticecodex.hoverride_broom.warning_dismount",
            "ui.apprenticecodex.hoverride_broom.retrieve_help",
            "ui.apprenticecodex.hoverride_broom.warning_damage",
            "ui.apprenticecodex.hoverride_broom.cannot_mount_damaged",
            Optional.empty(),
            "ui.apprenticecodex.hoverride_broom.warning_low_mana",
            "ui.apprenticecodex.hoverride_broom.warning_depleted_mana",
            "ui.apprenticecodex.hoverride_broom.recover_mana"
    );

    public HoverrideBroomEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getRecoveryItem() {
        return ItemRegistry.HOVERRIDE_BROOM.get();
    }

    @Override
    protected BroomMessageKeys messageKeys() {
        return MESSAGE_KEYS;
    }

    @Override
    protected boolean requiresMountMana() {
        return false;
    }

    @Override
    public Component createControlHelpMessage() {
        return Component.translatable(
                "ui.apprenticecodex.hoverride_broom.control_help",
                Component.keybind("key.jump"),
                Component.keybind("key.jump"),
                Component.keybind("key.sneak")
        );
    }
}
