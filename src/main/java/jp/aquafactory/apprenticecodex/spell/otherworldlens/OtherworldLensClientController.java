package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class OtherworldLensClientController {
    private static boolean ownsSmartCullOverride;
    private static boolean originalSmartCull;
    private static boolean suppressUntilCastEnds;
    private static ClientLevel activationLevel;

    private OtherworldLensClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            restore(minecraft);
            suppressUntilCastEnds = true;
            return;
        }

        var castingLens = isCastingOtherworldLens(player);
        if (!castingLens) {
            restore(minecraft);
            suppressUntilCastEnds = false;
            return;
        }
        if (!player.isAlive() || (activationLevel != null && activationLevel != level)) {
            restore(minecraft);
            suppressUntilCastEnds = true;
            return;
        }
        if (suppressUntilCastEnds) {
            return;
        }

        if (!ownsSmartCullOverride) {
            originalSmartCull = minecraft.smartCull;
            ownsSmartCullOverride = true;
            activationLevel = level;
        }
        if (minecraft.smartCull) {
            minecraft.smartCull = false;
            minecraft.levelRenderer.needsUpdate();
        }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
        suppressUntilCastEnds = true;
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 前セッション由来の所有状態だけを戻し、ユーザー設定そのものをtrueへ固定しない。
        restore(Minecraft.getInstance());
        suppressUntilCastEnds = true;
    }

    private static boolean isCastingOtherworldLens(net.minecraft.world.entity.player.Player player) {
        var spellData = ClientMagicData.getSyncedSpellData(player);
        return spellData.isCasting()
                && SpellRegistry.OTHERWORLD_LENS.get().getSpellId().equals(spellData.getCastingSpellId());
    }

    private static void restore(Minecraft minecraft) {
        if (ownsSmartCullOverride) {
            minecraft.smartCull = originalSmartCull;
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.needsUpdate();
            }
        }
        ownsSmartCullOverride = false;
        activationLevel = null;
    }
}
