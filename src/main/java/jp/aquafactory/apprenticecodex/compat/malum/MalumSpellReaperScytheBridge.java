package jp.aquafactory.apprenticecodex.compat.malum;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class MalumSpellReaperScytheBridge {
    private MalumSpellReaperScytheBridge() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MalumSpellReaperScytheBridge::onCommonSetup);
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MalumCompatibility.MOD_ID);
    }

    public static float throwMagicDamage(LivingEntity owner) {
        return isAvailable() ? MalumSpellReaperScytheBridgeImpl.throwMagicDamage(owner) : 0;
    }

    public static float scytheProficiency(LivingEntity owner) {
        return isAvailable() ? MalumSpellReaperScytheBridgeImpl.scytheProficiency(owner) : 1;
    }

    public static int reboundLevel(Level level, ItemStack stack) {
        return isAvailable() ? MalumSpellReaperScytheBridgeImpl.reboundLevel(level, stack) : 0;
    }

    public static boolean hasNarrowEdge(LivingEntity owner) {
        return isAvailable() && MalumSpellReaperScytheBridgeImpl.hasNarrowEdge(owner);
    }

    public static boolean shouldUseNoSweepCombo(LivingEntity attacker) {
        return attacker != null
                && isAvailable()
                && MalumSpellReaperScytheBridgeImpl.shouldUseNoSweepCombo(attacker);
    }

    public static InteractionResult tryTriggerAscension(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (!isAvailable() || stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        try {
            return MalumSpellReaperScytheBridgeImpl.tryTriggerAscension(level, player, hand, stack);
        } catch (LinkageError error) {
            throw new IllegalStateException("Failed to trigger Malum Ascension for Spell Reaper Scythe", error);
        }
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!isAvailable()) {
            return;
        }

        event.enqueueWork(() -> {
            try {
                MalumSpellReaperScytheBridgeImpl.register();
            } catch (LinkageError error) {
                // バニラスイープだけを止めた半端な状態にせず、対応Malum APIとの差異を起動時に明示する。
                throw new IllegalStateException("Failed to initialize Malum compatibility for Spell Reaper Scythe", error);
            }
        });
    }
}
