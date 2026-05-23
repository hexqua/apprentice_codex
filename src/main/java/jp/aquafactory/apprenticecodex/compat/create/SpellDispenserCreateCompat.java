package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;

public final class SpellDispenserCreateCompat {
    private SpellDispenserCreateCompat() {
    }

    public static void register() {
        // Create 未導入時は bridge から反射呼び出しされないため、ここでは Create API を素直に使う.
        // 1.21.1 では mounted storage / movement API が変わる可能性が高いため、接着コードは compat/create に隔離する.
        registerSpellDispenser(BlockRegistry.SPELL_DISPENSER.get());
        registerSpellDispenser(BlockRegistry.CREATIVE_SPELL_DISPENSER.get());
    }

    private static void registerSpellDispenser(net.minecraft.world.level.block.Block block) {
        MountedItemStorageType.REGISTRY.register(block, AllMountedStorageTypes.SIMPLE.get());
        MovementBehaviour.REGISTRY.register(block, new SpellDispenserMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(block, new SpellDispenserInteractionBehaviour());
    }
}
