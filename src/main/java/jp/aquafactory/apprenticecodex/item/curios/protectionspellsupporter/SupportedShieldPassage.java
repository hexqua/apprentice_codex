package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SupportedShieldPassage {
    // 生成前の通知もUUIDで保持する。サーバーはこの予測用情報を参照しない。
    private static final Map<Level, Set<UUID>> CLIENT_PERMISSIONS = new WeakHashMap<>();
    private SupportedShieldPassage() {}

    public static boolean canPass(Entity mover, Entity obstacle) {
        if (!(mover instanceof Player player) || !(obstacle instanceof ShieldPart part)
                || !(part.getParent() instanceof SupportedShieldEntity shield)) return false;
        return player.level().isClientSide
                ? player.isLocalPlayer() && CLIENT_PERMISSIONS.getOrDefault(player.level(), Set.of()).contains(shield.getUUID())
                : shield.allowsPassage(player);
    }

    public static void receive(Level level, UUID shield, boolean allowed) {
        if (!level.isClientSide) return;
        if (allowed) CLIENT_PERMISSIONS.computeIfAbsent(level, ignored -> new HashSet<>()).add(shield);
        else {
            var permissions = CLIENT_PERMISSIONS.get(level);
            if (permissions != null) permissions.remove(shield);
        }
    }

    @SubscribeEvent
    public static void entityLeft(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof SupportedShieldEntity shield) {
            receive(event.getLevel(), shield.getUUID(), false);
        }
    }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide()) {
            CLIENT_PERMISSIONS.remove(level);
        }
    }
}
